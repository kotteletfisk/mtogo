package mtogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import mtogo.sql.adapter.out.RabbitMQEventProducer;
import mtogo.sql.adapter.out.RabbitMQOrderCreationEventProducer;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.model.event.CustomerOrderCreationEvent;
import mtogo.sql.ports.out.IOrderCreationEventProducer;

@ExtendWith(MockitoExtension.class)
public class EventProducerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private Connection mqConnection;

    @Mock
    private Channel channel;

    @Test
    void throwOnNullObjectPublish() throws IOException, TimeoutException, InterruptedException {

        IOrderCreationEventProducer producer = new RabbitMQOrderCreationEventProducer(
                new RabbitMQEventProducer(mapper, mqConnection));

        // No DTO payload present
        CustomerOrderCreationEvent event = new CustomerOrderCreationEvent(null);

        assertThrows(IllegalArgumentException.class, () -> producer.orderCreation(event));
    }    
    
    @Test
    void falseOnNonValidConnection() throws IOException, TimeoutException, InterruptedException {

        doThrow(IOException.class).when(mqConnection).createChannel();

        IOrderCreationEventProducer producer = new RabbitMQOrderCreationEventProducer(
                new RabbitMQEventProducer(mapper, mqConnection));

        OrderDetailsDTO dto = new OrderDetailsDTO();
        
        CustomerOrderCreationEvent event = new CustomerOrderCreationEvent(dto);

        // Payload present, but connection invalid
        assertFalse(producer.orderCreation(event));
    }    
    
    @Test
    void trueOnSuccesfullPublish() throws IOException, TimeoutException, InterruptedException {

        when(channel.waitForConfirms(anyLong())).thenReturn(true);
        when(mqConnection.createChannel()).thenReturn(channel);

        IOrderCreationEventProducer producer = new RabbitMQOrderCreationEventProducer(
                new RabbitMQEventProducer(mapper, mqConnection));

        OrderDetailsDTO dto = new OrderDetailsDTO();
        
        CustomerOrderCreationEvent event = new CustomerOrderCreationEvent(dto);

        assertTrue(producer.orderCreation(event));
    }

}
