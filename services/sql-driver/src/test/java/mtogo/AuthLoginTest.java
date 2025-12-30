package mtogo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import mtogo.sql.adapter.handlers.AuthLoginHandler;
import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.core.AuthReceiverService;
import mtogo.sql.ports.out.IRpcResponder;
import mtogo.sql.ports.out.IRpcResponderFactory;

@ExtendWith(MockitoExtension.class)
public class AuthLoginTest {

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
    AuthReceiverService service;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void ackOnSuccesfullHandle() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "find_user_by_email",
                "email": "test@test.dk",
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void nackOnUnrecognizedAction() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "some_unknown_action",
                "email": "test@test.dk",
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
    }

    @Test
    void normalAckOnEmptyFields() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "find_user_by_email",
                "email": "",
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void nackOnMissingFields() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "find_user_by_email",
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
        verify(responder).replyError();
    }

    @Test
    void normalAckOnNullFields() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "find_user_by_email",
                "email": null,
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    void nackOnResponderFailure() throws IOException {

        long deliveryTag = 9L;
        String corrId = "id";
        String body = """
                {
                "action": "find_user_by_email",
                "email": "test@test.com",
                "service": "customer"
                }
                """;

        when(delivery.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(props.getCorrelationId()).thenReturn(corrId);
        when(delivery.getProperties()).thenReturn(props);
        when(factory.create(delivery)).thenReturn(responder);

        // Throw exception on response
        doThrow(IOException.class).when(responder).reply(any());;

        IMessageHandler handler = new AuthLoginHandler(mapper, service, factory);

        handler.handle(delivery, channel);

        verify(channel).basicNack(deliveryTag, false, false);
        verify(responder).replyError();
    }
}
