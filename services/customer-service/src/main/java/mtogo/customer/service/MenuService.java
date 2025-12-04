package mtogo.customer.service;

import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class MenuService {

    private final Map<String, CompletableFuture<List<menuItemDTO>>> pendingRequests =
            new ConcurrentHashMap<>();

    private static final MenuService INSTANCE = new MenuService();
    public static MenuService getInstance() { return INSTANCE; }

    private MenuService() {}

    /**
     * Requests the menu for a given supplierId by blocking until a response is received.
     * @param supplierId the ID of the supplier whose menu is being requested.
     * @return a list of menuItemDTO representing the menu items.
     * @throws Exception if a timeout occurs or an error happens during the request.
     */
    public List<menuItemDTO> requestMenuBlocking(int supplierId) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<menuItemDTO>> future = new CompletableFuture<>();

        pendingRequests.put(correlationId, future);

        try {
            // Send request with correlation ID
            String payload = correlationId + ":" + supplierId;
            Producer.publishMessage("customer:menu_request", payload);

            List<menuItemDTO> items = future.get(2, TimeUnit.SECONDS);
            return items;

        } catch (TimeoutException e) {
            throw new TimeoutException("No response from sql-driver for supplier " + supplierId);
        } finally {
            pendingRequests.remove(correlationId);
        }
    }

    /**
     * Completes the menu request by resolving the appropriate future.
     * @param correlationId the ID correlating this response to a request
     * @param items the list of menuItemDTO received in response to the menu request.
     */
    public void completeMenuRequest(String correlationId, List<menuItemDTO> items) {
        CompletableFuture<List<menuItemDTO>> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.complete(items);
        } else {
            System.err.println("Received menu response for unknown correlation ID: " + correlationId);
        }
    }
}