package mtogo.customer.service;

import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rabbitmq.client.AMQP;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuServiceTest {

    @Test
    void requestMenuBlocking_successReturnsItems() throws Exception {
        MenuService service = MenuService.getInstance();

        int supplierId = 1;
        List<menuItemDTO> fakeItems = List.of(
                new menuItemDTO(1, "Pizza", 85.0, 1, true),
                new menuItemDTO(2, "Burger", 60.0, 1, true)
        );

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Capture the payload that gets sent
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            producerMock
                    .when(() -> Producer.publishMessage(
                            eq("customer:menu_request"),
                            payloadCaptor.capture(),
                            any(AMQP.BasicProperties.class)
                    ))
                    .thenReturn(true);

            Thread responder = new Thread(() -> {
                try {
                    Thread.sleep(100);

                    // Extract correlation ID from the captured payload
                    // Format is "correlationId:supplierId"
                    String payload = payloadCaptor.getValue();
                    String correlationId = payload.substring(0, payload.indexOf(":"));

                    service.completeMenuRequest(correlationId, fakeItems);

                } catch (InterruptedException ignored) {
                }
            });
            responder.start();

            List<menuItemDTO> result = service.requestMenuBlocking(supplierId);

            assertNotNull(result);
            assertEquals(fakeItems, result);

            // Verify the payload format: "UUID:1"
            String capturedPayload = payloadCaptor.getValue();
            assertTrue(capturedPayload.matches("^[a-f0-9-]{36}:1$"),
                    "Payload should be in format 'correlationId:supplierId'");

            responder.join();
        }
    }

    @Test
    void requestMenuBlocking_timeoutThrowsException() throws Exception {
        MenuService service = MenuService.getInstance();

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            producerMock
                    .when(() -> Producer.publishMessage(anyString(), anyString()))
                    .thenReturn(true);

            // Don't send a response - let it timeout after 2 seconds

            assertThrows(java.util.concurrent.TimeoutException.class, () -> {
                service.requestMenuBlocking(1);
            });
        }
    }

    @Test
    void requestMenuBlocking_wrongCorrelationIdIgnored() throws Exception {
        MenuService service = MenuService.getInstance();

        int supplierId = 1;
        List<menuItemDTO> wrongItems = List.of(
                new menuItemDTO(99, "Wrong Item", 1.0, 99, true)
        );

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            producerMock
                    .when(() -> Producer.publishMessage(anyString(), anyString()))
                    .thenReturn(true);

            Thread responder = new Thread(() -> {
                try {
                    Thread.sleep(100);

                    // Send response with wrong correlation ID
                    service.completeMenuRequest("wrong-correlation-id", wrongItems);

                } catch (InterruptedException ignored) {
                }
            });
            responder.start();

            // Should timeout because the correlation ID doesn't match
            assertThrows(java.util.concurrent.TimeoutException.class, () -> {
                service.requestMenuBlocking(supplierId);
            });

            responder.join();
        }
    }
}