
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.adapter.handlers.CustomerOrderCreationHandler;
import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.adapter.in.RabbitMQEventConsumer;
import mtogo.sql.adapter.messaging.MessageRouter;
import mtogo.sql.core.CustomerOrderCreationService;
import mtogo.sql.event.OrderPersistedEvent;
import mtogo.sql.ports.in.IEventConsumer;
import mtogo.sql.ports.out.IModelRepository;
import mtogo.sql.ports.out.IOrderPersistedEventProducer;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

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
    IModelRepository modelRepo;

    @Test
    void consumeMessages_declaresExchange_bindsKeys_andStartsConsuming() throws Exception {
        // Arrange
        String[] bindingKeys = {
            "customer:order_creation",
            "something.else"
        };

        String generatedQueueName = "amq.gen-123";

        when(connection.createChannel()).thenReturn(channel);
        when(channel.queueDeclare())
                .thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn(generatedQueueName);

        IMessageHandler handler = mock(IMessageHandler.class);

        Map<String, IMessageHandler> handlers = Map.of(
                "customer:order_creation", handler,
                "something.else", handler
        );

        MessageRouter router = new MessageRouter(handlers);

        IEventConsumer consumer
                = new RabbitMQEventConsumer(router, connection);

        // Act
        consumer.start();

        // Assert
        verify(channel).exchangeDeclare("order", "topic", true);
        verify(channel).queueDeclare();

        for (String bindingKey : bindingKeys) {
            verify(channel).queueBind(
                    generatedQueueName,
                    "order",
                    bindingKey
            );
        }

        verify(channel).basicConsume(
                eq(generatedQueueName),
                eq(false),
                any(DeliverCallback.class),
                any(CancelCallback.class)
        );
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
                1,
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
        when(delivery.getEnvelope()).thenReturn(envelope);

        Channel channel = mock(Channel.class);

        // Mock ports
        IModelRepository modelRepo = mock(IModelRepository.class);
        IOrderPersistedEventProducer producer
                = mock(IOrderPersistedEventProducer.class);

        CustomerOrderCreationService service
                = new CustomerOrderCreationService(modelRepo);

        // Handler under test
        CustomerOrderCreationHandler handler
                = new CustomerOrderCreationHandler(
                        mapper,
                        service,
                        producer
                );

        // Act
        handler.handle(delivery, channel);

        // Assert – persistence was triggered
        verify(modelRepo).createOrder(
                any(OrderDTO.class),
                anyList()
        );

        // Assert – domain event was published
        verify(producer).orderPersisted(
                any(OrderPersistedEvent.class)
        );

        // Assert – message acknowledged
        verify(channel).basicAck(1L, false);
    }

    @Test
    void customerOrderCreationHandler_whenCreateOrderThrows_doesNotPublish() throws Exception {
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
        when(delivery.getEnvelope()).thenReturn(envelope);

        Channel channel = mock(Channel.class);

        IModelRepository modelRepo = mock(IModelRepository.class);
        IOrderPersistedEventProducer producer
                = mock(IOrderPersistedEventProducer.class);

        // Simulate persistence failure
        doThrow(new SQLException("DB failure"))
                .when(modelRepo)
                .createOrder(any(OrderDTO.class), anyList());

        CustomerOrderCreationService service
                = new CustomerOrderCreationService(modelRepo);

        CustomerOrderCreationHandler handler
                = new CustomerOrderCreationHandler(
                        mapper,
                        service,
                        producer
                );

        // Act
        handler.handle(delivery, channel);

        // Assert – message is rejected
        verify(channel).basicNack(1L, false, false);

        // Assert – no event is published
        verifyNoInteractions(producer);
    }
}
