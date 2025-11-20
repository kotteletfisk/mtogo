import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.messaging.Consumer;
import mtogo.sql.messaging.Producer;
import mtogo.sql.persistence.SQLConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

    // Helper: get the private deliverCallback() method
    private DeliverCallback getDeliverCallback() throws Exception {
        Method m = Consumer.class.getDeclaredMethod("deliverCallback");
        m.setAccessible(true);
        return (DeliverCallback) m.invoke(null);
    }

    @Mock
    ConnectionFactory factory;

    @Mock
    com.rabbitmq.client.Connection connection;

    @Mock
    Channel channel;

    @Mock
    AMQP.Queue.DeclareOk declareOk;

    @Test
    void consumeMessages_declaresExchange_bindsKeys_andStartsConsuming() throws Exception {
        String[] bindingKeys = {"customer:order_creation", "something.else"};
        String queueName = "test-queue";

        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        when(channel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn(queueName);

        // Inject mocked factory into Consumer
        Consumer.setConnectionFactory(factory);

        Consumer.consumeMessages(bindingKeys);

        verify(channel).exchangeDeclare("order", "topic", true);
        verify(channel).queueDeclare();
        verify(declareOk).getQueue();

        for (String bindingKey : bindingKeys) {
            verify(channel).queueBind(queueName, "order", bindingKey);
        }

        verify(channel).basicConsume(
                eq(queueName),
                eq(true),
                any(DeliverCallback.class),
                any(CancelCallback.class)
        );

        verifyNoMoreInteractions(channel);
    }

    // Helper: build a real OrderDetailsDTO with a non-null orderLines list.
    private OrderDetailsDTO buildOrderDetailsDTO() {
        UUID orderId = UUID.randomUUID();

        OrderLineDTO line = new OrderLineDTO(
                1,          // orderLineId
                orderId,    // orderId (UUID)
                10,         // item_id
                50.0f,      // price_snapshot
                2           // amount
        );

        // Assuming SQL OrderDetailsDTO looks like:
        // new OrderDetailsDTO(UUID orderId, int customerId,
        //                     orderStatus status, List<OrderLineDTO> orderLines)
        return new OrderDetailsDTO(
                orderId,
                1,
                OrderDetailsDTO.orderStatus.created,
                List.of(line)
        );
    }

    @Test
    void onOrderCreation_persistsAndPublishes() throws Exception {
        // Arrange: real DTO → JSON body
        OrderDetailsDTO dto = buildOrderDetailsDTO();
        ObjectMapper mapper = new ObjectMapper();
        byte[] body = mapper.writeValueAsBytes(dto);

        Envelope envelope = new Envelope(
                1L,
                false,
                "order",
                "customer:order_creation"
        );
        Delivery delivery = mock(Delivery.class);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(delivery.getBody()).thenReturn(body);

        try (MockedConstruction<SQLConnector> sqlMock = mockConstruction(SQLConnector.class,
                (mock, context) -> {
                    // Simulate a real DB connection so try-with-resources doesn't NPE
                    Connection conn = mock(Connection.class);
                    when(mock.getConnection()).thenReturn(conn);
                    // createOrder: default (do nothing, no exception)
                });
             MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {

            DeliverCallback cb = getDeliverCallback();

            // Act
            cb.handle("ctag-1", delivery);

            // Assert: SQLConnector.createOrder was called
            assertFalse(sqlMock.constructed().isEmpty(), "SQLConnector was never constructed");
            SQLConnector connector = sqlMock.constructed().get(0);
            verify(connector).createOrder(any(), anyList(), any(Connection.class));

            // Assert: Producer.publishMessage was called with correct routing key
            producerMock.verify(() ->
                    Producer.publishMessage(eq("supplier:order_persisted"), anyString())
            );
        }
    }

    @Test
    void onOrderCreation_whenCreateOrderThrows_doesNotPublish() throws Exception {
        // Arrange: same valid JSON body as before
        OrderDetailsDTO dto = buildOrderDetailsDTO();
        ObjectMapper mapper = new ObjectMapper();
        byte[] body = mapper.writeValueAsBytes(dto);

        Envelope envelope = new Envelope(
                1L,
                false,
                "order",
                "customer:order_creation"
        );
        Delivery delivery = mock(Delivery.class);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(delivery.getBody()).thenReturn(body);

        try (MockedConstruction<SQLConnector> sqlMock = mockConstruction(SQLConnector.class,
                (mock, context) -> {
                    Connection conn = mock(Connection.class);
                    when(mock.getConnection()).thenReturn(conn);
                    // Force DB failure when createOrder is called
                    doThrow(new SQLException("DB failure"))
                            .when(mock).createOrder(any(), anyList(), any(Connection.class));
                });
             MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {

            DeliverCallback cb = getDeliverCallback();

            // Act
            cb.handle("ctag-2", delivery);

            // Assert: DB write attempted
            SQLConnector connector = sqlMock.constructed().get(0);
            verify(connector).createOrder(any(), anyList(), any(Connection.class));

            // Assert: NO publish on failure
            producerMock.verifyNoInteractions();
        }
    }
}
