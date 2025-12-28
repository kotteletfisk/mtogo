import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import mtogo.sql.adapter.handlers.CustomerOrderCreationHandler;
import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.core.CustomerOrderCreationService;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.ports.out.IOrderPersistedEventProducer;

@ExtendWith(MockitoExtension.class)
public class CustomerOrderCreationTest {
    @Mock
    Channel channel;

    @Mock
    Delivery delivery;

    @Mock
    Envelope envelope;

    @Mock
    AMQP.BasicProperties props;

    @Mock
    CustomerOrderCreationService service;

    @Mock
    IOrderPersistedEventProducer producer;

    @Mock
    OrderDetailsDTO orderDetailsDTO;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void ackOnSuccesfullHandle() throws IOException {

        long deliveryTag = 9L;
        OrderDetailsDTO dto = new OrderDetailsDTO();
        String body = mapper.writeValueAsString(dto);

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new CustomerOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }    
    
    @Test
    void nackOnEmptyInput() throws IOException {

        long deliveryTag = 9L;

        when(delivery.getBody()).thenReturn(new byte[0]);
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new CustomerOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }    
    
    @Test
    void nackOnMangledInput() throws IOException {

        long deliveryTag = 9L;
        String body = "Grotesquely invalid";

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new CustomerOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }
}
