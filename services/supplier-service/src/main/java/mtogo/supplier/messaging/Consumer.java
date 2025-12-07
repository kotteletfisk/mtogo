package mtogo.supplier.messaging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

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
