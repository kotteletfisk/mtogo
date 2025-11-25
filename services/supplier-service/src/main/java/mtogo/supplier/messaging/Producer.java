package mtogo.supplier.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import tools.jackson.databind.ObjectMapper;

public class Producer {

    // Routing Key
    private static final String EXCHANGE_NAME = "order";
    static ConnectionFactory connectionFactory = createDefaultFactory();
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitMQ");
        factory.setPort(5672);
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASS"));
        return factory;
    }

    // Used to overwrite connectionfactory (For testing)
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * publishes a message to RabbitMQ using the specified routing key and waits
     * for RabbitMQ to confirm the message.
     *
     * @param routingKey the RabbitMQ routing key used for the message
     * @param message the message body to publish
     * @return true if the message was successfully confirmed by RabbitMQ
     * @throws IOException if connection fails
     * @throws TimeoutException if publish confirm exceeds 5000ms
     * @throws InterruptedException if Thread gets interrrupted while waiting
     *
     */
    public static boolean publishMessage(String routingKey, String message) throws IOException, TimeoutException, InterruptedException {

        try (Connection connection = connectionFactory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.confirmSelect();

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));

            if (!channel.waitForConfirms(5000)) {
                throw new TimeoutException("Confirm exceeded 5000ms wait");
            }
            return true;
        }
    }

    public static boolean publishObject(String routingKey, Object value) {

        try {
            if (value == null) {
                throw new IllegalArgumentException("Object was null!");
            }

            String valStr = objectMapper.writeValueAsString(value);
            log.debug("DTO mapped to string:\n" + valStr);

            if (publishMessage(routingKey, valStr)) {
                log.info("Object published to MQ");
                log.debug("Payload: \n" + valStr);
                return true;
            }

        } catch (IOException | InterruptedException | TimeoutException | IllegalArgumentException e) {
            log.error("Error publishing object: " + e.getMessage());
        }
        return false;
    }
}
