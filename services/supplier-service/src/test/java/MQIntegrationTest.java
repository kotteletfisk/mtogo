
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import mtogo.supplier.messaging.Producer;
import mtogo.supplier.server.LegacyDBAdapter;

/**
 *
 * @author kotteletfisk
 */
class MQIntegrationTest {

    @Container
    RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @BeforeEach
    void setupMQ() {
        rabbit.start();
    }

    @Test
    void canPublishAndConsumeTest() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());

        try (Connection conn = factory.newConnection(); Channel channel = conn.createChannel()) {

            channel.queueDeclare("test.queue", false, false, false, null);
            channel.basicPublish("", "test.queue", null, "hello".getBytes());

            GetResponse msg = channel.basicGet("test.queue", true);

            assertEquals("hello", new String(msg.getBody()));
        } catch (IOException | TimeoutException e) {
            fail(e.getMessage());
        }

    }

    @Test
    void succesFullPublisherConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());

        Producer.setConnectionFactory(factory);

        try {
            assertTrue(Producer.publishMessage("test:create_order", "hello"));
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @Test
    void legacyAdapterPublishTest() {

        LegacyDBAdapter la = LegacyDBAdapter.getAdapter();

        
    }
}
