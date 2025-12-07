package mtogo.sql.messaging;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import mtogo.sql.exceptions.APIException;

public class Producer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    // Routing Key
    private static final String EXCHANGE_NAME = "order";
    static ConnectionFactory connectionFactory = createDefaultFactory();

    // Connection pooling
    private static Connection connection;
    private static BlockingQueue<Channel> channelPool;
    private static final int POOL_SIZE = 10;
    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();

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

    // Lazy initialization - only runs when first message is published
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    try {
                        connection = connectionFactory.newConnection();
                        channelPool = new ArrayBlockingQueue<>(POOL_SIZE);

                        for (int i = 0; i < POOL_SIZE; i++) {
                            Channel channel = connection.createChannel();
                            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                            channel.confirmSelect();
                            channelPool.offer(channel);
                        }

                        initialized = true;
                        log.info("Producer initialized with {} channels", POOL_SIZE);
                    } catch (Exception e) {
                        log.error("Failed to initialize Producer", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * publishes a message to RabbitMQ using the specified routing key and waits
     * for RabbitMQ to confirm the message.
     *
     * @param routingKey the RabbitMQ routing key used for the message
     * @param message the message body to publish
     * @return true if the message was successfully confirmed by RabbitMQ
     * @throws APIException if connection fails or if RabbitMQ does not confirm
     * the message.
     *
     */
    public static boolean publishMessage(String routingKey, String message){
        ensureInitialized(); // Initialize on first use

        Channel channel = null;
        try {
            // Get channel from pool
            channel = channelPool.poll(5, TimeUnit.SECONDS);
            if (channel == null) {
                log.error("No channels available in pool");
                return false;
            }

            // Recreate channel if closed
            if (!channel.isOpen()) {
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.confirmSelect();
            }

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            // Maybe log the message that is being sent?

            if (!channel.waitForConfirms(5000)) {
                throw new IOException("Confirm wait time exceeded 5000ms");
            }
            return true;

        } catch (IOException | TimeoutException | InterruptedException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            // Return channel to pool
            if (channel != null && channel.isOpen()) {
                channelPool.offer(channel);
            }
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

        } catch (IOException | IllegalArgumentException e) {
            log.error("Error publishing object: " + e.getMessage());
        }
        return false;
    }
}