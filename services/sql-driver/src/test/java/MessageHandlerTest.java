import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.DatabindException;
import com.rabbitmq.client.Delivery;

import mtogo.sql.messaging.MessageHandler;

@ExtendWith(MockitoExtension.class)
public class MessageHandlerTest {
    
    @Mock
    Delivery delivery;

    @Test
    void failOnEmptyDeliveryBody() {

        when(delivery.getBody()).thenReturn(null);

        MessageHandler mh = new MessageHandler();

        assertThrows(IllegalArgumentException.class, () -> mh.handleLegacyOrder(delivery));
    }
}
