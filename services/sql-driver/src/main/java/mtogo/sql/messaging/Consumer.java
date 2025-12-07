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
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownSignalException;

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

        // Connection recovery
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);
        factory.setTopologyRecoveryEnabled(true);

        // Heartbeat
        factory.setRequestedHeartbeat(60);

        // Timeouts
        factory.setConnectionTimeout(10000);
        factory.setHandshakeTimeout(10000);

        log.info("ConnectionFactory configured with heartbeat=60s, recovery enabled");
        return factory;
    }

    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    public static void setMessageRouter(MessageRouter msgRouter) {
        router = msgRouter;
    }

    public static void consumeMessages(String[] bindingKeys, MessageRouter msgRouter) throws Exception {
        router = msgRouter;
        log.debug("Registering binding keys: {}", bindingKeys.toString());

        try {
            Connection connection = connectionFactory.newConnection();
            log.info("RabbitMQ connection established");

            // Connection shutdown listener
            connection.addShutdownListener(cause -> {
                if (cause.isInitiatedByApplication()) {
                    log.info("Connection closed by application");
                } else {
                    log.error("CONNECTION LOST! Reason: {}", cause.getMessage());
                }
            });

            // Recovery listener
            if (connection instanceof Recoverable) {
                ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                    @Override
                    public void handleRecovery(Recoverable recoverable) {
                        log.info("Connection RECOVERED!");
                    }

                    @Override
                    public void handleRecoveryStarted(Recoverable recoverable) {
                        log.warn("Connection recovery STARTED...");
                    }
                });
            }

            Channel channel = connection.createChannel();
            log.info("Channel created");

            // Channel shutdown listener
            channel.addShutdownListener(cause -> {
                if (cause.isInitiatedByApplication()) {
                    log.info("Channel closed by application");
                } else {
                    log.error("CHANNEL LOST! Reason: {}", cause.getMessage());
                    log.error("Hard close: {}, Cause: {}", cause.isHardError(), cause.getReason());
                }
            });

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            for (String bindingKey : bindingKeys) {
                channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, bindingKey);
                log.info("Bound queue '{}' to routing key '{}'", QUEUE_NAME, bindingKey);
            }

            channel.basicConsume(QUEUE_NAME, false, deliverCallback(channel), consumerTag -> {
                log.error("CONSUMER CANCELLED! Tag: {}", consumerTag);
            });

            log.info("Consumer listening on queue: {}", QUEUE_NAME);
            log.info("Heartbeat: 60s, Recovery: enabled, Monitoring: ACTIVE");

        } catch (IOException | TimeoutException e) {
            log.error("Error consuming message:\n" + e.getMessage());
            e.printStackTrace(pw);
            log.error("Stacktrace:\n" + sw.toString());
            throw e;
        }
    }

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