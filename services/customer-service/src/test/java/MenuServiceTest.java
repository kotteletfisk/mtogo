
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;
import mtogo.customer.service.MenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Test
    void requestMenuBlocking_successReturnsItems() throws Exception {
        MenuService service = MenuService.getInstance();

        int supplierId = 1;
        List<menuItemDTO> fakeItems = List.of(
                new menuItemDTO(1, "Pizza", 85.0, 1, true),
                new menuItemDTO(2, "Burger", 60.0, 1, true)
        );

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Producer.publishMessage should be called once with correct routing key + payload
            producerMock
                    .when(() -> Producer.publishMessage("customer:menu_request", String.valueOf(supplierId)))
                    .thenReturn(true);

            // Simulate async response from Rabbit consumer:
            // after a short delay, it calls completeMenuRequest(...) which unblocks the waiting poll
            Thread responder = new Thread(() -> {
                try {
                    Thread.sleep(100); // small delay to let poll() start
                    service.completeMenuRequest(fakeItems);
                } catch (InterruptedException ignored) {
                }
            });
            responder.start();

            List<menuItemDTO> result = service.requestMenuBlocking(supplierId);

            assertNotNull(result);
            assertEquals(fakeItems, result);

            producerMock.verify(() ->
                    Producer.publishMessage("customer:menu_request", "1")
            );

            responder.join();
        }
    }
}