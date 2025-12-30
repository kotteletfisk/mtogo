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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import mtogo.sql.adapter.handlers.CustomerMenuRequestHandler;
import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.core.CustomerMenuRequestService;
import mtogo.sql.ports.out.IRpcResponder;
import mtogo.sql.ports.out.IRpcResponderFactory;

@ExtendWith(MockitoExtension.class)
public class MenuRequestTest {

    @Mock
    Channel channel;

    @Mock
    Delivery delivery;

    @Mock
    Envelope envelope;

    @Mock
    AMQP.BasicProperties props;

    @Mock
    IRpcResponderFactory factory;

    @Mock
    IRpcResponder responder;

    @Mock
    CustomerMenuRequestService service;

    @Test
    void nackOnMissingSeperator() throws IOException {

        when(delivery.getBody()).thenReturn(new byte[0]); // Empty body: no seperator
        when(envelope.getDeliveryTag()).thenReturn(9L);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new CustomerMenuRequestHandler(service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicNack(9L, false, false);
    }

    @Test
    void ackOnSuccessfullHandle() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";

        when(delivery.getBody()).thenReturn("corrId:1".getBytes(StandardCharsets.UTF_8)); // valid body
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new CustomerMenuRequestHandler(service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void nackOnInvalidSupplierID() throws IOException {

        long deliveryTag = 9L;
        String body = "corrid:?"; // Invalid supplier id "?"

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);

        IMessageHandler handler = new CustomerMenuRequestHandler(service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }
}
