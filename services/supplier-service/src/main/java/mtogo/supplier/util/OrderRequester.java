package mtogo.supplier.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;

import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.messaging.Producer;

public class OrderRequester {

    private static final Logger log = LoggerFactory.getLogger(OrderRequester.class);

    private final Map<String, CompletableFuture<List<OrderDTO>>> pendingRequests = new ConcurrentHashMap<>();

    private static OrderRequester instance;

    private OrderRequester() {}

    public static OrderRequester getInstance() {

        if (instance == null) {
            instance = new OrderRequester();
        }

        return instance;
    }

    public CompletableFuture<List<OrderDTO>> requestOrderBlocking(int supplierId) throws IOException, InterruptedException, TimeoutException, ExecutionException {

        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<OrderDTO>> future = new CompletableFuture<>();

        log.info("Creating order request for supplier: {}", supplierId);
        log.debug("Correlation ID: {}", correlationId);

        pendingRequests.put(correlationId, future);

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo("supplier:order_response")
                .build();

        try {
            // Send request with correlation ID
            String payload = "supplierId:" + supplierId;
            Producer.publishMessage("supplier:order_request", payload, props);
            log.debug("publishing payload: {}", payload);

            return future.orTimeout(2, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            pendingRequests.remove(correlationId);
            log.error(e.getMessage());
            throw new TimeoutException("Publishing request timed out for supplier: " + supplierId);
        }
    }

    public void completeOrderRequest(String correlationId, List<OrderDTO> items) throws IOException {
        CompletableFuture<List<OrderDTO>> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.complete(items);
            pendingRequests.remove(correlationId);
            log.debug("Completed future for correlationId: {}", correlationId);
        } else {
            log.error("Received order response for unknown correlation ID: " + correlationId);
            throw new IOException("Received menu response for unknown correlation ID: " + correlationId);
        }
    }

}
