package mtogo.customer.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import mtogo.customer.exceptions.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Producer {
    private static final Logger log = LoggerFactory.getLogger(Producer.class);
    private static final String EXCHANGE_NAME = "order";

    // Make these mutable for testing
    private static ConnectionFactory connectionFactory;
    private static Connection connection;
    private static BlockingQueue<Channel> channelPool;
    private static int POOL_SIZE = 10;
    private static boolean initialized = false;
    private static final Object initLock = new Object();

    // Remove static initializer - use lazy initialization instead

    /**
     * Lazy initialization - only runs when first needed
     */
    private static void ensureInitialized() {
        synchronized (initLock) {
            if (!initialized) {
                initializeProducer(null, 10);
            }
        }
    }

    /**
     * Initialize or reinitialize the producer.
     * Package-private for testing.
     */
    static void initializeProducer(ConnectionFactory factory, int poolSize) {
        synchronized (initLock) {
            try {
                // Close existing connection if reinitializing
                if (initialized && connection != null && connection.isOpen()) {
                    closeInternal();
                }

                POOL_SIZE = poolSize;

                if (factory == null) {
                    // Production: create real factory
                    connectionFactory = new ConnectionFactory();
                    String host = System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq");
                    String user =  System.getenv().getOrDefault("RABBITMQ_USERNAME", "guest");
                    String pass = System.getenv().getOrDefault("RABBITMQ_PASSWORD", "guest");

                    connectionFactory.setHost(host);
                    connectionFactory.setRequestedHeartbeat(30);
                    connectionFactory.setConnectionTimeout(5000);
                    connectionFactory.setUsername(user);
                    connectionFactory.setPassword(pass);
                } else {
                    // Testing: use injected factory
                    connectionFactory = factory;
                }

                channelPool = new ArrayBlockingQueue<>(POOL_SIZE);

                // Single connection
                connection = connectionFactory.newConnection();
                log.info("Producer connection established to RabbitMQ");

                // Create pool of channels
                for (int i = 0; i < POOL_SIZE; i++) {
                    Channel channel = connection.createChannel();
                    channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                    channel.confirmSelect();
                    channelPool.offer(channel);
                }

                initialized = true;
                log.info("Producer initialized with {} channels", POOL_SIZE);

            } catch (Exception e) {
                log.error("FATAL: Failed to initialize Producer: {}", e.getMessage(), e);
                throw new RuntimeException("Cannot initialize RabbitMQ Producer", e);
            }
        }
    }

    /**
     * For testing: inject a mock ConnectionFactory and reinitialize
     */
    public static void setConnectionFactoryForTesting(ConnectionFactory factory) {
        synchronized (initLock) {
            initialized = false; // Force reinitialization
            initializeProducer(factory, 10);
        }
    }

    /**
     * For testing: reset to uninitialized state
     */
    public static void resetForTesting() {
        synchronized (initLock) {
            if (initialized) {
                closeInternal();
            }
            initialized = false;
            connectionFactory = null;
            connection = null;
            channelPool = null;
        }
    }

    public static boolean publishMessage(String routingKey, String message) throws APIException {
        ensureInitialized(); // Lazy init on first use

        Channel channel = null;
        try {
            // Acquire channel from pool (blocks if none available)
            channel = channelPool.poll(5, TimeUnit.SECONDS);

            if (channel == null) {
                log.error("Channel pool exhausted - no channels available after 5s");
                throw new APIException(503, "Service temporarily unavailable - too many requests");
            }

            // Check if channel is still open
            if (!channel.isOpen()) {
                log.warn("Channel was closed, creating new one");
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.confirmSelect();
            }

            // Publish message
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));

            // Wait for confirmation
            if (!channel.waitForConfirms(5000)) {
                log.error("RabbitMQ did not confirm message for routing key: {}", routingKey);
                throw new APIException(500, "Message delivery not confirmed");
            }

            log.debug("Published message to {}", routingKey);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for channel: {}", e.getMessage());
            throw new APIException(503, "Request interrupted");

        } catch (APIException e) {
            throw e; // Re-throw API exceptions

        } catch (Exception e) {
            log.error("Failed to publish message to {}: {}", routingKey, e.getMessage(), e);
            throw new APIException(500, "Failed to publish message");

        } finally {
            // Return channel to pool (DON'T close it!)
            if (channel != null && channel.isOpen()) {
                channelPool.offer(channel);
            }
        }
    }

    /**
     * Get current pool statistics (for monitoring)
     */
    public static int getAvailableChannels() {
        ensureInitialized();
        return channelPool.size();
    }

    /**
     * Internal close without lock (assumes caller has lock)
     */
    private static void closeInternal() {
        try {
            // Close all channels in pool
            if (channelPool != null) {
                Channel ch;
                while ((ch = channelPool.poll()) != null) {
                    if (ch.isOpen()) {
                        ch.close();
                    }
                }
            }

            // Close connection
            if (connection != null && connection.isOpen()) {
                connection.close();
            }

            log.info("Producer connection and channels closed");
        } catch (Exception e) {
            log.error("Error closing Producer: {}", e.getMessage(), e);
        }
    }

    /**
     * Graceful shutdown
     */
    public static void close() {
        synchronized (initLock) {
            if (initialized) {
                closeInternal();
                initialized = false;
            }
        }
    }
}