package mtogo.sql.messaging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";
    private static final String QUEUE_NAME = "sql-driver-queue";
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
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000); // Retry every 5 seconds
        factory.setRequestedHeartbeat(30); // Heartbeat every 30 seconds
        factory.setConnectionTimeout(10000); // 10 second connection timeout
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
        log.debug("Registering binding keys: {}", bindingKeys.toString());

        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(
                    QUEUE_NAME,  // queue name
                    true,        // durable
                    false,       // not exclusive
                    false,       // not auto-delete
                    null         // no arguments
            );

            for (String bindingKey : bindingKeys) {
                channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, bindingKey);
                log.info("Bound queue '{}' to routing key '{}'", QUEUE_NAME, bindingKey);
            }

            channel.basicConsume(QUEUE_NAME, false, deliverCallback(channel), consumerTag -> {
            });

            log.info("SQL Driver consumer listening on queue: {}", QUEUE_NAME);
        } catch (IOException | TimeoutException e) {
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
