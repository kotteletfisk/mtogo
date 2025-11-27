package mtogo.customer.service;

import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;

import java.util.List;
import java.util.concurrent.*;

public class MenuService {

    private final BlockingQueue<List<menuItemDTO>> queue = new ArrayBlockingQueue<>(1);

    private static final MenuService INSTANCE = new MenuService();
    public static MenuService getInstance() { return INSTANCE; }

    private MenuService() {}

    public List<menuItemDTO> requestMenuBlocking(int supplierId) throws Exception {
        // Clear any leftover result
        queue.clear();

        // Send request to sql-driver
        Producer.publishMessage("customer:menu_request", String.valueOf(supplierId));

        // Wait max 2 seconds for the response
        List<menuItemDTO> items = queue.poll(2, TimeUnit.SECONDS);
        if (items == null) {
            throw new TimeoutException("No response from sql-driver");
        }
        return items;
    }

    public void completeMenuRequest(List<menuItemDTO> items) {
        queue.offer(items);
    }
}
