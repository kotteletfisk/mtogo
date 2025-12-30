package mtogo;

import static org.mockito.ArgumentMatchers.any;
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

import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.adapter.handlers.SupplierOrderCreationHandler;
import mtogo.sql.core.SupplierOrderCreationService;
import mtogo.sql.model.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.ports.out.IOrderCreationEventProducer;

@ExtendWith(MockitoExtension.class)
public class SupplierOrderCreationTest {
    @Mock
    Channel channel;

    @Mock
    Delivery delivery;

    @Mock
    Envelope envelope;

    @Mock
    AMQP.BasicProperties props;

    @Mock
    SupplierOrderCreationService service;

    @Mock
    IOrderCreationEventProducer producer;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void ackOnSuccesfullHandle() throws Exception {

        long deliveryTag = 9L;
        LegacyOrderDetailsDTO dto = new LegacyOrderDetailsDTO();
        String body = mapper.writeValueAsString(dto);

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(producer.orderCreation(any())).thenReturn(true);
        when(service.call(any())).thenReturn(new OrderDetailsDTO());

        IMessageHandler handler = new SupplierOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void nackOnFailedEventPublish() throws IOException {

        long deliveryTag = 9L;

        LegacyOrderDetailsDTO dto = new LegacyOrderDetailsDTO();
        String body = mapper.writeValueAsString(dto);

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        // event publish fail
        when(producer.orderCreation(any())).thenReturn(false);

        IMessageHandler handler = new SupplierOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }

    @Test
    void nackOnEmptyInput() throws IOException {

        long deliveryTag = 9L;

        when(delivery.getBody()).thenReturn(new byte[0]);
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new SupplierOrderCreationHandler(mapper, service, producer);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }
}
