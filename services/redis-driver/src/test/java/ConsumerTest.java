import com.rabbitmq.client.*;
import mtogo.redis.messaging.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

    @Mock
    ConnectionFactory factory;

    @Mock
    Connection connection;

    @Mock
    Channel channel;

    @Mock
    AMQP.Queue.DeclareOk declareOk;

    @Test
    void consumeMessages_declaresExchange_bindsKeys_andStartsConsuming() throws Exception {
        String[] bindingKeys = {"customer:order_creation", "customer:supplier_request"};
        String queueName = "test-queue";

        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        when(channel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn(queueName);

        // Inject mocked factory into Consumer
        Consumer.setConnectionFactory(factory);

        Consumer.consumeMessages(bindingKeys);

        // Verify exchange is declared
        verify(channel).exchangeDeclare("order", "topic", true);

        // Verify queue is declared
        verify(channel).queueDeclare();
        verify(declareOk).getQueue();

        // Verify each binding key is bound
        for (String bindingKey : bindingKeys) {
            verify(channel).queueBind(queueName, "order", bindingKey);
        }

        verify(channel).basicConsume(
                eq(queueName),
                eq(false),  // Changed from true to false
                any(DeliverCallback.class),
                any(CancelCallback.class)
        );

        // Optionally verify no extra unexpected calls:
        verifyNoMoreInteractions(channel);
    }
}