package mtogo.customer.service;

import mtogo.customer.DTO.SupplierDTO;
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
public class SupplierServiceTest {

    @Test
    void requestSuppliersBlocking_successReturnsItems() throws Exception {
        SupplierService service = SupplierService.getInstance();

        String zipCode = "2200";
        List<SupplierDTO> fakeSuppliers = List.of(
                new SupplierDTO(1, "Supplier A", "2200", SupplierDTO.status.active),
                new SupplierDTO(2, "Supplier B", "2300", SupplierDTO.status.active)
        );

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Capture the correlation ID that gets sent
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            producerMock
                    .when(() -> Producer.publishMessage(
                            eq("customer:supplier_request"),
                            payloadCaptor.capture(),
                            any(AMQP.BasicProperties.class)
                    ))
                    .thenReturn(true);

            // Simulate async response from Rabbit consumer
            Thread responder = new Thread(() -> {
                try {
                    Thread.sleep(100);

                    String payload = payloadCaptor.getValue();
                    String correlationId = payload.split(":")[0];

                    service.completeSupplierRequest(correlationId, fakeSuppliers);

                } catch (InterruptedException ignored) {
                }
            });
            responder.start();

            List<SupplierDTO> result = service.requestSuppliersBlocking(zipCode);

            assertNotNull(result);
            assertEquals(fakeSuppliers, result);

            // Verify the payload format: "UUID:2200"
            String capturedPayload = payloadCaptor.getValue();
            assertTrue(capturedPayload.matches("^[a-f0-9-]{36}:2200$"),
                    "Payload should be in format 'correlationId:zipcode'");

            responder.join();
        }
    }

    @Test
    void requestSuppliersBlocking_timeoutThrowsException() throws Exception {
        SupplierService service = SupplierService.getInstance();

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            producerMock
                    .when(() -> Producer.publishMessage(anyString(), anyString()))
                    .thenReturn(true);

            // Don't send a response - let it timeout

            assertThrows(java.util.concurrent.TimeoutException.class, () -> {
                service.requestSuppliersBlocking("2200");
            });
        }
    }
}