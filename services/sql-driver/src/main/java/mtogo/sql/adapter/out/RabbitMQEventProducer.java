/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.out;

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

/**
 *
 * @author kotteletfisk
 */
public class RabbitMQEventProducer {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Default Exchange
    private final String EXCHANGE_NAME = "order";

    // Connection pooling
    private final Connection connection;
    /*
     * private BlockingQueue<Channel> channelPool;
     * private final int POOL_SIZE = 10;
     * private static final Object initLock = new Object();
     */
    private final ObjectMapper mapper;

    public RabbitMQEventProducer(ObjectMapper mapper, Connection connection)
            throws IOException, TimeoutException, InterruptedException {
        this.mapper = mapper;
        this.connection = connection;
        // fillChannelPool();
    }

    public boolean publishMessage(String routingKey, String message) {

        /*
         * try {
         * fillChannelPool();
         * } catch (IOException | TimeoutException | InterruptedException ex) {
         * log.error("Failed to get producer connection: {}", ex.getLocalizedMessage());
         * return false;
         * }
         */

        // Channel channel = null;
        try {
            // Get channel from pool
            /*
             * channel = channelPool.poll(5, TimeUnit.SECONDS);
             * if (channel == null) {
             * log.error("No channels available in pool");
             * return false;
             * }
             */

            // Recreate channel if closed
            /*
             * if (!channel.isOpen()) {
             * channel = connection.createChannel();
             * channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
             * channel.confirmSelect();
             * }
             */

            try (Channel channel = connection.createChannel()) {
                
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.confirmSelect();

                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
                // Maybe log the message that is being sent?

                if (!channel.waitForConfirms(5000)) {
                    throw new IOException("Confirm wait time exceeded 5000ms");
                }
                return true;
            }

        } catch (IOException | TimeoutException | InterruptedException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            // Return channel to pool
            /*
             * if (channel != null && channel.isOpen()) {
             * channelPool.offer(channel);
             * } else {
             * try {
             * fillChannelPool();
             * } catch (TimeoutException | InterruptedException | IOException ex) {
             * log.error("Error refiling channel pool: {}", ex.getLocalizedMessage());
             * }
             * }
             */
        }
    }

    public boolean publishObject(String routingKey, Object value) throws IOException {
        try {
            if (value == null) {
                throw new IllegalArgumentException("Object was null!");
            }

            String valStr = mapper.writeValueAsString(value);
            log.debug("DTO mapped to string:\n" + valStr);

            if (publishMessage(routingKey, valStr)) {
                log.info("Object published to MQ");
                log.debug("Payload: \n" + valStr);
                return true;
            } else {
                log.error("Object publish failed!");
                log.debug("Payload: \n" + valStr);
            }

        } catch (IOException | IllegalArgumentException e) {
            log.error("Error publishing object: " + e.getMessage());
            throw e;
        }
        return false;
    }

    /*
     * private void fillChannelPool() throws TimeoutException, InterruptedException,
     * IOException {
     * synchronized (initLock) {
     * try {
     * 
     * if (channelPool == null) {
     * channelPool = new ArrayBlockingQueue<>(POOL_SIZE);
     * }
     * 
     * while (channelPool.size() < POOL_SIZE) {
     * Channel channel = this.connection.createChannel();
     * channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
     * channel.confirmSelect();
     * channelPool.offer(channel);
     * }
     * 
     * log.info("Producer initialized with {} channels", POOL_SIZE);
     * } catch (IOException e) {
     * log.error("Failed to initialize Producer", e);
     * throw e;
     * }
     * 
     * }
     * }
     */
}
