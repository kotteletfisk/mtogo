import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import mtogo.customer.messaging.Producer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


public class ProducerTest {


    @Test
    public void publishMessageOrderCreation() throws Exception{
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);


        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        when(channel.waitForConfirms(5000)).thenReturn(true);

        Producer.setConnectionFactory(factory);

        String routingKey = "order";
        String body = "Test-message";

        boolean result = Producer.publishMessage(routingKey, body);

        assertTrue(result);

        verify(channel).exchangeDeclare("order", "topic", true);
        verify(channel).confirmSelect();

        byte[] expectedBytes = body.getBytes(StandardCharsets.UTF_8);
        verify(channel).basicPublish(
                eq("order"),
                eq(routingKey),
                isNull(),
                eq(expectedBytes)
        );

        verify(channel).waitForConfirms(5000);
        verify(channel).close();
        verify(connection).close();

    }
}
