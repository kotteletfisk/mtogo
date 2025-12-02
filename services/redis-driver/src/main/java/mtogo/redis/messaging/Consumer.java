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

            channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> {
            });
        } catch (Exception e) {
            log.error("Error consuming message:\n" + e.getMessage());
            e.printStackTrace(pw);
            log.error("Stacktrace:\n" + sw.toString());
        }

    }

    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks
     * functionality can vary on keyword
     * 
     * @return the DeliverCallback function
     */
    private static DeliverCallback deliverCallback() {
        return (consumerTag, delivery) -> {
            log.info("Consumer received message");
            if (delivery.getEnvelope().getRoutingKey().equals("customer:order_creation")) {
                objectMapper.registerModule(new JavaTimeModule());

                OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(), OrderDetailsDTO.class);
                OrderDTO orderDTO = new OrderDTO(orderDetailsDTO);
                List<OrderLineDTO> orderLineDTOS = new ArrayList<>();
                for (OrderLineDTO line : orderDetailsDTO.getOrderLines()) {
                    orderLineDTOS.add(new OrderLineDTO(line.getOrderLineId(), line.getOrderId(), line.getItemId(),
                            line.getPriceSnapshot(), line.getAmount()));
                }
                log.debug(
                        " [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + orderDetailsDTO + "'");
                // Persist orderDTO and orderDTO lines to Redis

                UUID orderId = orderDetailsDTO.getOrderId();
                for (OrderLineDTO orderLineDTO : orderLineDTOS) {
                    orderLineDTO.setOrderId(orderId);
                }
                RedisConnector redisConnector = RedisConnector.getInstance();
                redisConnector.createOrder(orderDTO);
                redisConnector.createOrderLines(orderLineDTOS);

            }
            if (delivery.getEnvelope().getRoutingKey().equals("customer:supplier_request")) {
                try {
                    String zipCode = objectMapper.readValue(delivery.getBody(), String.class);
                    RedisConnector redisConnector = RedisConnector.getInstance();

                    List<SupplierDTO> suppliers = redisConnector.findSuppliersByZipAndStatus(zipCode, SupplierDTO.status.active);

                    String payload = objectMapper.writeValueAsString(suppliers);
                    Producer.publishMessage("customer:supplier_response", payload);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

        };
    }
}
