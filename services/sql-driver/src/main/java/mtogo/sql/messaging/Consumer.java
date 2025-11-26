package mtogo.sql.messaging;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.persistence.SQLConnector;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

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

            channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> {
            });
        } catch (Exception e) {
            log.error("Error consuming message:\n" + e.getMessage());
            e.printStackTrace();
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
            String routingKey = delivery.getEnvelope().getRoutingKey();

            switch (routingKey) {

                case "customer:order_creation" -> {
                    try {
                        OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                                OrderDetailsDTO.class);

                        OrderDTO order = new OrderDTO(orderDetailsDTO);

                        List<OrderLineDTO> orderLines = new ArrayList<>();
                        for (OrderLineDTO line : orderDetailsDTO.getOrderLines()) {
                            orderLines.add(
                                    new OrderLineDTO(
                                            line.getOrderLineId(),
                                            line.getOrderId(),
                                            line.getItemId(),
                                            line.getPriceSnapshot(),
                                            line.getAmount()));
                        }

                        log.info(" [x] Received '" + routingKey + "':'" + orderDetailsDTO + "'");
                        String bodyStr = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                        log.info(" [x] Raw message body: " + bodyStr);

                        SQLConnector sqlConnector = new SQLConnector();
                        try (java.sql.Connection conn = sqlConnector.getConnection()) {
                            sqlConnector.createOrder(order, orderLines, conn);
                        }

                        String payload = objectMapper.writeValueAsString(orderDetailsDTO);
                        Producer.publishMessage("supplier:order_persisted", payload);

                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }

                case "supplier:order_creation" -> {
                    try {
                        handleLegacyOrder(delivery);
                    } catch (SQLException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        };
    }

    private static void handleLegacyOrder(Delivery delivery) throws IOException, SQLException {
        LegacyOrderDetailsDTO legacyOrderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                LegacyOrderDetailsDTO.class);

        SQLConnector sqlConnector = new SQLConnector();
        try (java.sql.Connection conn = sqlConnector.getConnection()) {
            sqlConnector.createLegacyOrder(legacyOrderDetailsDTO, conn);
        }
    }
}
