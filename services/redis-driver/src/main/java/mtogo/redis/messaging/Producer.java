package mtogo.redis.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.AMQBasicProperties;

public class Producer {

    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    // Routing Key
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

    // Used to overwrite connectionfactory (For testing)
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * publishes a message to RabbitMQ using the specified routing key and waits for
     * RabbitMQ to confirm the message.
     *
     * @param routingKey the RabbitMQ routing key used for the message
     * @param message    the message body to publish
     * @return true if the message was successfully confirmed by RabbitMQ
     *
     */

    public static boolean publishMessage(String routingKey, String message) {

        try (Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.confirmSelect();

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            // Maybe log the message that is being sent?

            if (!channel.waitForConfirms(5000)) {
                throw new TimeoutException("Publish confirm time exceeded 5000ms");
            }
            log.info("Message published");
            return true;
        } catch (IOException | TimeoutException | InterruptedException e) {
            log.error(e.getMessage());
        }
        return false;
    }    
    
    public static boolean publishMessage(String routingKey, String message, AMQP.BasicProperties props) {

        try (Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.confirmSelect();

            channel.basicPublish(EXCHANGE_NAME, routingKey, props, message.getBytes("UTF-8"));
            log.debug("published to routingKey: {}, with payload: {}", routingKey, message);

            if (!channel.waitForConfirms(5000)) {
                throw new TimeoutException("Publish confirm time exceeded 5000ms");
            }
            log.info("Message published");
            return true;
        } catch (IOException | TimeoutException | InterruptedException e) {
            log.error(e.getMessage());
        }
        return false;
    }
}