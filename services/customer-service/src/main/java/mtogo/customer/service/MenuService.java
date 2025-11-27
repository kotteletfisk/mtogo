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

    /**
     * Requests the menu for a given supplierId by blocking until a response is received or a timeout occurs.
     * @param supplierId the ID of the supplier whose menu is being requested.
     * @return a list of menuItemDTO representing the menu items.
     * @throws Exception if a timeout occurs or an error happens during the request.
     */
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

    /**
     * Completes the menu request by adding the received items to the queue.
     * @param items the list of menuItemDTO received in response to the menu request.
     */
    public void completeMenuRequest(List<menuItemDTO> items) {
        queue.offer(items);
    }
}
