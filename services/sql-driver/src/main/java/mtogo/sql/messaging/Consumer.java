package mtogo.sql.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.persistence.SQLConnector;
import java.sql.*;



import java.util.ArrayList;
import java.util.List;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

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
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public static void consumeMessages(String[] bindingKeys) throws Exception{
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
        String queueName = channel.queueDeclare().getQueue();

        for (String bindingKey : bindingKeys) {
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
        }

        channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> { });
    }


    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks functionality can vary on keyword
     * @return the DeliverCallback function
     */
    private static DeliverCallback deliverCallback() {
        return (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();

            if ("customer:order_creation".equals(routingKey)) {
                try {
                    OrderDetailsDTO orderDetailsDTO =
                            objectMapper.readValue(delivery.getBody(), OrderDetailsDTO.class);

                    OrderDTO order = new OrderDTO(orderDetailsDTO);

                    List<OrderLineDTO> orderLines = new ArrayList<>();
                    for (OrderLineDTO line : orderDetailsDTO.getOrderLines()) {
                        orderLines.add(
                                new OrderLineDTO(
                                        line.getOrderLineId(),
                                        line.getOrderId(),
                                        line.getItem_id(),
                                        line.getPrice_snapshot(),
                                        line.getAmount()
                                )
                        );
                    }

                    System.out.println(" [x] Received '" + routingKey + "':'" + orderDetailsDTO + "'");

                    SQLConnector sqlConnector = new SQLConnector();
                    try (java.sql.Connection conn = sqlConnector.getConnection()) {
                        sqlConnector.createOrder(order, orderLines, conn);
                    }

                    String payload = objectMapper.writeValueAsString(orderDetailsDTO);
                    Producer.publishMessage("supplier:order_persisted", payload);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
