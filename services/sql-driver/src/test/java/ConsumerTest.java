
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.handlers.CustomerOrderCreationHandler;
import mtogo.sql.messaging.Consumer;
import mtogo.sql.messaging.MessageRouter;
import mtogo.sql.messaging.Producer;
import mtogo.sql.persistence.SQLConnector;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

    // Helper: get the private deliverCallback() method
    private DeliverCallback getDeliverCallback() throws Exception {
        Method m = Consumer.class.getDeclaredMethod("deliverCallback", Channel.class);
        m.setAccessible(true);
        return (DeliverCallback) m.invoke(null, channel);
    }

    @Mock
    ConnectionFactory factory;

    @Mock
    com.rabbitmq.client.Connection connection;

    @Mock
    Channel channel;

    @Mock
    AMQP.Queue.DeclareOk declareOk;

    @Mock
    MessageRouter router;

    @Mock
    SQLConnector sqlConnector;

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
        Consumer.setMessageRouter(router);

        Consumer.consumeMessages(bindingKeys, router);

        verify(channel).exchangeDeclare("order", "topic", true);
        verify(channel).queueDeclare();
        verify(declareOk).getQueue();

        for (String bindingKey : bindingKeys) {
            verify(channel).queueBind(queueName, "order", bindingKey);
        }

        verify(channel).basicConsume(
                eq(queueName),
                eq(false),
                any(DeliverCallback.class),
                any(CancelCallback.class)
        );

        verifyNoMoreInteractions(channel);
    }

    // Helper: build a real OrderDetailsDTO with a non-null orderLines list.
    private OrderDetailsDTO buildOrderDetailsDTO() {
        UUID orderId = UUID.randomUUID();

        OrderLineDTO line = new OrderLineDTO(
                1, // orderLineId
                orderId, // orderId (UUID)
                10, // item_id
                50.0f, // price_snapshot
                2 // amount
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
    void customerOrderCreationHandler_persistsAndPublishes() throws Exception {
        // Arrange
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
        when(delivery.getBody()).thenReturn(body);

        // Mock SQLConnector
        try (MockedConstruction<SQLConnector> sqlMock = mockConstruction(SQLConnector.class,
                (mock, context) -> {
                    Connection conn = mock(Connection.class);
                    when(mock.getConnection()).thenReturn(conn);
                }); MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {

            // Instantiate the handler directly
            CustomerOrderCreationHandler handler = new CustomerOrderCreationHandler(
                    new SQLConnector(), mapper
            );

            // Act
            handler.handle(delivery);

            // Assert: SQLConnector.createOrder was called
            assertFalse(sqlMock.constructed().isEmpty(), "SQLConnector was never constructed");
            SQLConnector connector = sqlMock.constructed().get(0);
            verify(connector).createOrder(any(), anyList(), any(Connection.class));

            // Assert: Producer.publishMessage was called
            producerMock.verify(()
                    -> Producer.publishMessage(eq("supplier:order_persisted"), anyString())
            );
        }
    }

    @Test
    void customerOrderCreationHandler_whenCreateOrderThrows_doesNotPublish() throws Exception {
        // Arrange: valid DTO → JSON body
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
                    // Force DB failure
                    doThrow(new SQLException("DB failure"))
                            .when(mock).createOrder(any(), anyList(), any(Connection.class));
                }); MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {

            // Instantiate the handler
            CustomerOrderCreationHandler handler = new CustomerOrderCreationHandler(
                    new SQLConnector(), mapper
            );

            // Act
            handler.handle(delivery);

            // Assert: SQLConnector.createOrder was called
            SQLConnector connector = sqlMock.constructed().get(0);
            verify(connector).createOrder(any(), anyList(), any(Connection.class));

            // Assert: Producer.publishMessage was NOT called
            producerMock.verifyNoInteractions();
        }
    }
}
