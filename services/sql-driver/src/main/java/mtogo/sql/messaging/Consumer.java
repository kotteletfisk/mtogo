package mtogo.sql.messaging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import mtogo.sql.DTO.menuItemDTO;
import mtogo.sql.persistence.SQLConnector;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";
    private static StringWriter sw = new StringWriter();
    private static PrintWriter pw = new PrintWriter(sw);

    private static MessageRouter router;
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

    public static void setMessageRouter(MessageRouter msgRouter) {
        router = msgRouter;
    }

    /**
     * Consumes messages from RabbitMQ based on the provided binding keys.
     *
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public static void consumeMessages(String[] bindingKeys, MessageRouter msgRouter) throws Exception {

        router = msgRouter;

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
    private static DeliverCallback deliverCallback(Channel channel) {
        return (consumerTag, delivery) -> {
            log.info("Consumer received message");
            String routingKey = delivery.getEnvelope().getRoutingKey();

            switch (routingKey) {

                case "customer:order_creation" -> {
                    router.getMessageHandler(routingKey).handle(delivery);
                }
                
                // TODO: TEST FOR NOW. REPLACES SWITCH CASE
                case "supplier:order_creation" -> {
                    try {
                        router.getMessageHandler(routingKey).handle(delivery);
                    } catch (IllegalArgumentException e) {
                        log.error(e.getMessage());
                    }
                }
                case "customer:menu_request" -> {
                    try {
                        String body = new String(
                                delivery.getBody(),
                                java.nio.charset.StandardCharsets.UTF_8
                        );
                        log.info(" [x] Received '{}' with payload: {}", routingKey, body);

                        int supplierId = Integer.parseInt(body.trim());
                        log.info(" [x] Supplier ID: {}", supplierId);

                        SQLConnector sqlConnector = new SQLConnector();
                        List<menuItemDTO> items;

                        try (java.sql.Connection conn = sqlConnector.getConnection()) {
                            log.info(" [x] Fetching menu items from DB for supplier {}", supplierId);
                            items = sqlConnector.getMenuItemsBySupplierId(supplierId, conn);
                            log.info(" [x] Found {} menu items for supplier {}",
                                    (items == null ? 0 : items.size()), supplierId);
                        }

                        if (items == null) {
                            items = java.util.Collections.emptyList();
                        }

                        String payload = objectMapper.writeValueAsString(items);
                        log.info(" [x] Sending menu response, length={} bytes", payload.length());

                        Producer.publishMessage("customer:menu_response", payload);

                    } catch (Exception e) {
                        log.error("Error handling customer:menu_request", e);
                    }
                }
                case "auth:login" -> {
                    try {
                        log.info(" [x] Received '{}' with correlationId '{}': '{}'", routingKey,
                                delivery.getProperties().getCorrelationId(),
                                new String(delivery.getBody(), StandardCharsets.UTF_8));
                        var body = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                        var reqJson = objectMapper.readTree(body);
                        String action = reqJson.get("action").asText();

                        if ("find_user_by_email".equals(action)) {
                            String email = reqJson.get("email").asText();
                            AuthReceiver ar = new AuthReceiver();
                            String resp = ar.handleAuthLookup(email);
                            var props = new AMQP.BasicProperties.Builder()
                                    .correlationId(delivery.getProperties().getCorrelationId())
                                    .contentType("application/json")
                                    .build();

                            channel.basicPublish(
                                    "",
                                    delivery.getProperties().getReplyTo(),
                                    props,
                                    resp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        }
                    } catch (Exception ex) {
                        log.info("RPC handler error: {}", ex.getMessage());
                        try {
                            var props = new AMQP.BasicProperties.Builder()
                                    .correlationId(delivery.getProperties().getCorrelationId())
                                    .contentType("application/json")
                                    .build();
                            channel.basicPublish(
                                    "",
                                    delivery.getProperties().getReplyTo(),
                                    props,
                                    "{\"status\":\"error\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        } catch (Exception ignored) {
                        }
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                }
            }
        };
    }
}
