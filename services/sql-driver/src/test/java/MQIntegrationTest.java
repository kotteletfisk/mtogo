
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import mtogo.sql.adapter.out.RabbitMQEventProducer;
import mtogo.sql.model.DTO.OrderDTO;
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
    void publishObjectTest() throws IOException, TimeoutException, InterruptedException {

        UUID id = UUID.randomUUID();
        OrderDTO dto = new OrderDTO(id, 1);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());

        RabbitMQEventProducer producer = new RabbitMQEventProducer(new ObjectMapper(), factory.newConnection());

        try {
            assertTrue(producer.publishObject("test:create_order", dto));
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }
}
