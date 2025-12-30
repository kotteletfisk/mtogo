package mtogo;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
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

import mtogo.supplier.DTO.LegacyOrderDetailsDTO;
import mtogo.supplier.DTO.OrderLineDTO;
import mtogo.supplier.messaging.Producer;

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
    void publishObjectTest() {

        UUID id = UUID.randomUUID();
        LegacyOrderDetailsDTO dto = new LegacyOrderDetailsDTO(
                id,
                "11111111",
                List.of(
                        new OrderLineDTO(id, 1, 1.0f, 1),
                        new OrderLineDTO(id, 2, 2.0f, 2)
                )
        );

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());

        Producer.setConnectionFactory(factory);

        try {
            assertTrue(Producer.publishObject("test:create_order", dto));
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }
}
