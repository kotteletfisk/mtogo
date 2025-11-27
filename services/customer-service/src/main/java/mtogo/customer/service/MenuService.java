package mtogo.customer.service;

import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MenuService {
    private final Map<Integer, CompletableFuture<List<menuItemDTO>>> pendingRequests = new ConcurrentHashMap<>();


    private static final MenuService instance = new MenuService();

    public static MenuService getInstance() {
        return instance;
    }

    private MenuService() {}

    public List<menuItemDTO> requestMenuBlocking(int supplierId) throws Exception {
        CompletableFuture<List<menuItemDTO>> future = new CompletableFuture<>();
        pendingRequests.put(supplierId, future);

        // Send request to sql-driver
        Producer.publishMessage("customer:menu_request", String.valueOf(supplierId));

        try {
            // Wait max 2 seconds (tune as you like)
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(supplierId);
            throw e;
        }
    }

    public void completeMenuRequest(int supplierId, List<menuItemDTO> items) {
        CompletableFuture<List<menuItemDTO>> future = pendingRequests.remove(supplierId);
        if (future != null) {
            future.complete(items);
        }
    }
}
