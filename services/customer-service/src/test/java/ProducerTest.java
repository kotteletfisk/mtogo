import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import mtogo.customer.messaging.Producer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ProducerTest {

    private ConnectionFactory mockFactory;
    private Connection mockConnection;
    private Channel mockChannel;

    @BeforeEach
    public void setUp() throws Exception {
        // Reset Producer to clean state
        Producer.resetForTesting();

        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        mockChannel = mock(Channel.class);

        // Setup mock behavior
        when(mockFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.isOpen()).thenReturn(true);
        when(mockConnection.isOpen()).thenReturn(true);
        when(mockChannel.waitForConfirms(5000)).thenReturn(true);

        // Initialize Producer with mock factory
        Producer.setConnectionFactoryForTesting(mockFactory);
    }

    @AfterEach
    public void tearDown() {
        // Clean up after test
        Producer.close();
        Producer.resetForTesting();
    }

    @Test
    public void testPublishMessageSuccess() throws Exception {
        String routingKey = "customer:order_creation";
        String body = "Test-message";

        boolean result = Producer.publishMessage(routingKey, body);

        assertTrue(result, "Message should be published successfully");

        // Verify exchange was declared during initialization (called once per channel in pool)
        verify(mockChannel, times(10)).exchangeDeclare("order", "topic", true);

        // Verify confirmSelect was called during initialization
        verify(mockChannel, times(10)).confirmSelect();

        // Verify the message was published
        byte[] expectedBytes = body.getBytes(StandardCharsets.UTF_8);
        verify(mockChannel, times(1)).basicPublish(
                eq("order"),
                eq(routingKey),
                isNull(),
                eq(expectedBytes)
        );

        // Verify confirmation was checked
        verify(mockChannel, times(1)).waitForConfirms(5000);

        // Verify channel was NOT closed (it's returned to pool)
        verify(mockChannel, never()).close();
        verify(mockConnection, never()).close();
    }

    @Test
    public void testPublishMessageConfirmationFails() throws Exception {
        // Setup: confirmation fails
        when(mockChannel.waitForConfirms(5000)).thenReturn(false);

        String routingKey = "customer:order_creation";
        String body = "Test-message";

        // Should throw APIException
        assertThrows(Exception.class, () -> {
            Producer.publishMessage(routingKey, body);
        });

        // Verify message was published
        verify(mockChannel, times(1)).basicPublish(
                eq("order"),
                eq(routingKey),
                isNull(),
                any(byte[].class)
        );

        // Channel should still be returned to pool (not closed)
        verify(mockChannel, never()).close();
    }

    @Test
    public void testPublishMultipleMessages() throws Exception {
        String routingKey = "customer:order_creation";

        // Publish 3 messages
        for (int i = 0; i < 3; i++) {
            boolean result = Producer.publishMessage(routingKey, "Message " + i);
            assertTrue(result);
        }

        // Verify 3 messages were published
        verify(mockChannel, times(3)).basicPublish(
                eq("order"),
                eq(routingKey),
                isNull(),
                any(byte[].class)
        );

        // Verify confirmations were checked
        verify(mockChannel, times(3)).waitForConfirms(5000);

        // Channel should not be closed (reused from pool)
        verify(mockChannel, never()).close();
    }

    @Test
    public void testChannelPoolReuse() throws Exception {
        // All 10 channels from pool should be available
        assertEquals(10, Producer.getAvailableChannels());

        // Publish a message (acquires and returns a channel)
        Producer.publishMessage("test", "message");

        // Pool should still have 10 channels (channel was returned)
        assertEquals(10, Producer.getAvailableChannels());
    }

    @Test
    public void testGracefulShutdown() throws Exception {
        // Trigger initialization by publishing
        Producer.publishMessage("test", "message");

        // Close producer
        Producer.close();

        // Verify all channels were closed
        verify(mockChannel, times(10)).close();

        // Verify connection was closed
        verify(mockConnection, times(1)).close();
    }
}