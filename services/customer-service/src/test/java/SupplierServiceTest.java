
import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.messaging.Producer;
import mtogo.customer.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Test
    void requestMenuBlocking_successReturnsItems() throws Exception {
        SupplierService service = SupplierService.getInstance();

        String zipCode = "2200";
        List<SupplierDTO> fakeItems = List.of(
                new SupplierDTO(1, "Supplier A", "2200", SupplierDTO.status.active),
                new SupplierDTO(2, "Supplier B", "2300", SupplierDTO.status.active)
        );

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Producer.publishMessage should be called once with correct routing key + payload
            producerMock
                    .when(() -> Producer.publishMessage("customer:supplier_request", zipCode))
                    .thenReturn(true);

            // Simulate async response from Rabbit consumer:
            // after a short delay, it calls completeSupplierRequest(...) which unblocks the waiting poll
            Thread responder = new Thread(() -> {
                try {
                    Thread.sleep(100); // small delay to let poll() start
                    service.completeSupplierRequest(fakeItems);
                } catch (InterruptedException ignored) {
                }
            });
            responder.start();

            List<SupplierDTO> result = service.requestSuppliersBlocking(zipCode);

            assertNotNull(result);
            assertEquals(fakeItems, result);

            producerMock.verify(() ->
                    Producer.publishMessage("customer:supplier_request", "2200")
            );

            responder.join();
        }
    }
}