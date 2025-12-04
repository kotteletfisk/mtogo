package mtogo.redis.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import mtogo.redis.DTO.OrderDTO;
import mtogo.redis.DTO.OrderDetailsDTO;
import mtogo.redis.DTO.OrderLineDTO;
import mtogo.redis.DTO.SupplierDTO;
import mtogo.redis.persistence.RedisConnector;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);
    private static StringWriter sw = new StringWriter();
    private static PrintWriter pw = new PrintWriter(sw);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";

    static ConnectionFactory connectionFactory = createDefaultFactory();

    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitMQ");
        factory.setPort(5672);
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASS"));
        return factory;
    }

    // Injectable connectionfactory for testing
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * Consumes messages from RabbitMQ based on the provided binding keys.
     *
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public static void consumeMessages(String[] bindingKeys) throws Exception {

        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            String queueName = channel.queueDeclare().getQueue();

            for (String bindingKey : bindingKeys) {
                channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            }

            channel.basicConsume(queueName, false, deliverCallback(channel), consumerTag -> {
            });

            log.info("Redis Consumer started, listening on binding keys: {}",
                    String.join(", ", bindingKeys));

        } catch (Exception e) {
            log.error("Error consuming message:\n" + e.getMessage());
            e.printStackTrace(pw);
            log.error("Stacktrace:\n" + sw.toString());
            throw e;
        }
    }

    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks
     * functionality can vary on keyword
     *
     * @param channel the RabbitMQ channel for acknowledging messages
     * @return the DeliverCallback function
     */
    private static DeliverCallback deliverCallback(Channel channel) {
        return (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();

            log.info("Consumer received message with routing key: {}", routingKey);

            switch (routingKey) {
                case "customer:order_creation" -> {
                    try {
                        objectMapper.registerModule(new JavaTimeModule());

                        OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(
                                delivery.getBody(),
                                OrderDetailsDTO.class
                        );

                        OrderDTO orderDTO = new OrderDTO(orderDetailsDTO);
                        List<OrderLineDTO> orderLineDTOS = new ArrayList<>();

                        for (OrderLineDTO line : orderDetailsDTO.getOrderLines()) {
                            orderLineDTOS.add(new OrderLineDTO(
                                    line.getOrderLineId(),
                                    line.getOrderId(),
                                    line.getItemId(),
                                    line.getPriceSnapshot(),
                                    line.getAmount()
                            ));
                        }

                        log.debug(" [x] Received order: {}", orderDetailsDTO);

                        // Persist orderDTO and orderDTO lines to Redis
                        UUID orderId = orderDetailsDTO.getOrderId();
                        for (OrderLineDTO orderLineDTO : orderLineDTOS) {
                            orderLineDTO.setOrderId(orderId);
                        }

                        RedisConnector redisConnector = RedisConnector.getInstance();
                        redisConnector.createOrder(orderDTO);
                        redisConnector.createOrderLines(orderLineDTOS);

                        log.info("Successfully persisted order {} to Redis", orderId);

                        // basickAck makes sure thatwe acknowledge the message after successful processing
                        channel.basicAck(deliveryTag, false);

                    } catch (Exception e) {
                        log.error("Error handling customer:order_creation", e);
                        try {
                            channel.basicNack(deliveryTag, false, true); // Requeue for retry
                        } catch (IOException nackError) {
                            log.error("Failed to NACK message", nackError);
                        }
                    }
                }

                case "customer:supplier_request" -> {
                    try {
                        String body = new String(
                                delivery.getBody(),
                                java.nio.charset.StandardCharsets.UTF_8
                        );
                        log.info(" [x] Received supplier request payload: {}", body);

                        // Parse "correlationId:zipcode"
                        int separatorIndex = body.indexOf(":");
                        if (separatorIndex == -1) {
                            log.error("Invalid supplier request format - missing ':' separator");
                            channel.basicNack(deliveryTag, false, false); // Don't requeue bad format
                            return;
                        }

                        String correlationId = body.substring(0, separatorIndex);
                        String zip = body.substring(separatorIndex + 1).trim();

                        log.info(" [x] Looking up active suppliers for zip {} with correlation {}",
                                zip, correlationId);

                        RedisConnector redis = RedisConnector.getInstance();
                        List<SupplierDTO> suppliers = redis.findSuppliersByZipAndStatus(
                                zip,
                                SupplierDTO.status.active
                        );

                        log.info(" [x] Found {} active suppliers for zip {}",
                                suppliers == null ? 0 : suppliers.size(),
                                zip);

                        if (suppliers == null) {
                            suppliers = java.util.Collections.emptyList();
                        }

                        String suppliersJson = objectMapper.writeValueAsString(suppliers);
                        // Format: "correlationId::[json]"
                        String payload = correlationId + "::" + suppliersJson;
                        log.info(" [x] Sending supplier response, length={} bytes", payload.length());

                        Producer.publishMessage("customer:supplier_response", payload);

                        channel.basicAck(deliveryTag, false);

                    } catch (Exception e) {
                        log.error("Error handling customer:supplier_request", e);
                        try {
                            channel.basicNack(deliveryTag, false, true); // Requeue for retry
                        } catch (IOException nackError) {
                            log.error("Failed to NACK message", nackError);
                        }
                    }
                }

                default -> {
                    log.warn("Received message with unknown routing key: {}", routingKey);
                    try {
                        channel.basicAck(deliveryTag, false);
                    } catch (IOException ackError) {
                        log.error("Failed to ACK unknown message", ackError);
                    }
                }
            }
        };
    }
}