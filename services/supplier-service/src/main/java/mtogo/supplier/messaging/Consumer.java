package mtogo.supplier.messaging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.AMQP.Basic.Cancel;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private static final String EXCHANGE_NAME = "order";
    private static StringWriter sw = new StringWriter();
    private static PrintWriter pw = new PrintWriter(sw);
    

    /**
     * Consumes messages from RabbitMQ based on the provided binding keys.
     *
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public void consumeMessages(String[] bindingKeys, Connection connection, MessageRouter msgRouter) throws Exception {

        log.debug("Registering binding keys: {}", bindingKeys.toString());

        try {
            if (connection == null) {
                throw new IOException("Connection to rabbitmq failed");
            }
            log.info("Connected to rabbitmq");
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            String queueName = channel.queueDeclare().getQueue();

            for (String bindingKey : bindingKeys) {
                channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            }

            channel.basicConsume(queueName, false, deliverCallback(channel, msgRouter), consumerTag -> {
            });
        } catch (IOException e) {
            log.error("Error consuming messages:\n" + e.getMessage());
            e.printStackTrace(pw);
            log.error("Stacktrace:\n" + sw.toString());
        }

    }

    // Opens a channel for a single exclusive response and deletes it after response
    public String consumeExclusiveResponse(Connection connection, DeliverCallback deliverCallback, CancelCallback cancelCallback)
            throws InterruptedException, IOException {

        if (connection == null) {
            throw new IOException("Connection to rabbitmq failed");
        }
        Channel channel = connection.createChannel();
        String replyQueue = channel.queueDeclare("", false, true, true, null).getQueue();

        channel.basicConsume(replyQueue, true, (consumerTag, delivery) -> {

            deliverCallback.handle(consumerTag, delivery);
            try {
                channel.close();
            } catch (TimeoutException e) {
                log.error(e.getMessage());
            }

        }, cancelCallback);
        return replyQueue;
    }

    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks
     * functionality can vary on keyword
     *
     * @return the DeliverCallback function
     */
    private DeliverCallback deliverCallback(Channel channel, MessageRouter router) {
        return (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();
            log.info("Consumer received message with key: {}", routingKey);

            try {
                router.getMessageHandler(routingKey).handle(delivery, channel);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        };
    }
}
