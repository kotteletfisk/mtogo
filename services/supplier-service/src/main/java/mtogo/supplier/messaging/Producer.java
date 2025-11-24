package mtogo.supplier.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Producer {

    // Routing Key
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
}
