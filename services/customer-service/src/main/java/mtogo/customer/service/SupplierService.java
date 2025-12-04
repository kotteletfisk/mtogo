package mtogo.customer.service;

import mtogo.customer.DTO.SupplierDTO;
import mtogo.customer.messaging.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    // Map of correlationId -> CompletableFuture
    private final Map<String, CompletableFuture<List<SupplierDTO>>> pendingRequests =
            new ConcurrentHashMap<>();

    private static final SupplierService INSTANCE = new SupplierService();
    public static SupplierService getInstance() { return INSTANCE; }

    private SupplierService() {}

    /**
     * Requests suppliers for a given zipcode by blocking until a response is received.
     * @param zipCode the zipcode to search for suppliers
     * @return a list of SupplierDTO representing active suppliers
     * @throws Exception if a timeout occurs or an error happens during the request
     */
    public List<SupplierDTO> requestSuppliersBlocking(String zipCode) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<SupplierDTO>> future = new CompletableFuture<>();

        pendingRequests.put(correlationId, future);

        try {
            // Send request with correlation ID
            String payload = correlationId + ":" + zipCode;
            Producer.publishMessage("customer:supplier_request", payload);

            log.debug("Sent supplier request for zipcode {} with correlation {}",
                    zipCode, correlationId);

            // Wait max 2 seconds for the response
            List<SupplierDTO> suppliers = future.get(2, TimeUnit.SECONDS);
            log.debug("Received {} suppliers for zipcode {}", suppliers.size(), zipCode);
            return suppliers;

        } catch (TimeoutException e) {
            log.error("Timeout waiting for suppliers for zipcode: {}", zipCode);
            throw new TimeoutException("No response from Redis driver for zipcode " + zipCode);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for suppliers for zipcode: {}", zipCode);
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            log.error("Error retrieving suppliers for zipcode: {}", zipCode, e);
            throw new Exception("Error retrieving suppliers", e.getCause());
        } finally {
            pendingRequests.remove(correlationId);
        }
    }

    /**
     * Completes the supplier request by resolving the appropriate future.
     * @param correlationId the ID correlating this response to a request
     * @param suppliers the list of SupplierDTO received in response to the request
     */
    public void completeSupplierRequest(String correlationId, List<SupplierDTO> suppliers) {
        CompletableFuture<List<SupplierDTO>> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.complete(suppliers);
            log.debug("Completed supplier request for correlation {}", correlationId);
        } else {
            log.warn("Received supplier response for unknown correlation ID: {}", correlationId);
        }
    }
}