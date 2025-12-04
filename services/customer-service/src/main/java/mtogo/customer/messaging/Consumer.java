package mtogo.customer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.service.MenuService;
import mtogo.customer.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    public static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";
    private static StringWriter sw = new StringWriter();
    private static PrintWriter pw = new PrintWriter(sw);

    static ConnectionFactory connectionFactory = createDefaultFactory();

    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
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

            log.info("Consumer started, listening on binding keys: {}", String.join(", ", bindingKeys));

        } catch (IOException | TimeoutException e) {
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

            log.debug("Received message with routing key: {}", routingKey);

            switch (routingKey) {

                case "customer:menu_response" -> {
                    try {
                        String body = new String(
                                delivery.getBody(),
                                java.nio.charset.StandardCharsets.UTF_8
                        );

                        // Expected format: "correlationId::[{...json array...}]"
                        int separatorIndex = body.indexOf("::");
                        if (separatorIndex == -1) {
                            log.error("Invalid menu response format - missing '::' separator");
                            channel.basicNack(deliveryTag, false, false); // Don't requeue malformed messages
                            return;
                        }

                        String correlationId = body.substring(0, separatorIndex);
                        String jsonArray = body.substring(separatorIndex + 2);

                        List<menuItemDTO> menuItems =
                                objectMapper.readValue(
                                        jsonArray,
                                        objectMapper.getTypeFactory()
                                                .constructCollectionType(List.class, menuItemDTO.class)
                                );

                        log.info("Received {} menu items for correlation {}",
                                menuItems.size(), correlationId);

                        MenuService.getInstance().completeMenuRequest(correlationId, menuItems);

                        channel.basicAck(deliveryTag, false);

                    } catch (Exception e) {
                        log.error("Error handling customer:menu_response", e);
                        try {
                            channel.basicNack(deliveryTag, false, false);
                        } catch (IOException nackError) {
                            log.error("Failed to NACK message", nackError);
                        }
                    }
                }

                case "customer:supplier_response" -> {
                    try {
                        String body = new String(
                                delivery.getBody(),
                                java.nio.charset.StandardCharsets.UTF_8
                        );

                        log.debug("Received supplier response: {}", body);

                        int separatorIndex = body.indexOf("::");
                        if (separatorIndex == -1) {
                            log.error("Invalid supplier response format - missing '::' separator");
                            channel.basicNack(deliveryTag, false, false);
                            return;
                        }

                        String correlationId = body.substring(0, separatorIndex);
                        String jsonArray = body.substring(separatorIndex + 2);

                        List<SupplierDTO> suppliers = objectMapper.readValue(
                                jsonArray,
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(List.class, SupplierDTO.class)
                        );

                        log.info("Received {} suppliers for correlation {}",
                                suppliers.size(), correlationId);

                        SupplierService.getInstance().completeSupplierRequest(correlationId, suppliers);

                        channel.basicAck(deliveryTag, false);

                    } catch (Exception e) {
                        log.error("Error handling customer:supplier_response", e);
                        try {
                            channel.basicNack(deliveryTag, false, false);
                        } catch (IOException nackError) {
                            log.error("Failed to NACK message", nackError);
                        }
                    }
                }

                default -> {
                    log.warn("Received message with unknown routing key: {}", routingKey);
                    channel.basicAck(deliveryTag, false);
                }
            }
        };
    }
}