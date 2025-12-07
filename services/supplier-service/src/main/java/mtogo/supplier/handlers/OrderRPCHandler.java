package mtogo.supplier.handlers;

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
import com.rabbitmq.client.Connection;

import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.messaging.ConnectionManager;
import mtogo.supplier.messaging.Consumer;
import mtogo.supplier.messaging.Producer;
import tools.jackson.databind.ObjectMapper;

public class OrderRPCHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<String, CompletableFuture<List<OrderDTO>>> pendingRequests = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper;

    public OrderRPCHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<List<OrderDTO>> requestOrderBlocking(int supplierId)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {

        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<OrderDTO>> future = new CompletableFuture<>();

        log.info("Creating order request for supplier: {}", supplierId);
        log.debug("Correlation ID: {}", correlationId);

        pendingRequests.put(correlationId, future);

        // open exclusive response consumer:
        Connection conn = ConnectionManager.getConnectionManager().getConnection();
        String replyQueue =  new Consumer().consumeExclusiveResponse(conn, (consumerTag, delivery) -> {

            String responseCorrelationId = delivery.getProperties().getCorrelationId();

            if (responseCorrelationId.equals(correlationId)) {
                log.info("Handling order response");

                String body = new String(
                        delivery.getBody(),
                        java.nio.charset.StandardCharsets.UTF_8);

                log.debug("Received correlation ID: {}", correlationId);
                log.debug("Received message body: {}", body);

                List<OrderDTO> orders = objectMapper.readValue(
                        body,
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, OrderDTO.class));

                future.complete(orders);

                log.info("Completed future for correlation ID {}", correlationId);
                pendingRequests.remove(correlationId);
            }

        }, consumerTag -> {
            log.error("Cancel callback call on consumerTag: {}", consumerTag);
        });

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(replyQueue)
                .build();

        try {
            // Send request with correlation ID
            String payload = "supplierId:" + supplierId;
            Producer.publishMessage("supplier:order_request", payload, props);
            log.debug("publishing payload: {}", payload);

            return future.orTimeout(5, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            pendingRequests.remove(correlationId);
            log.error(e.getMessage());
            throw new TimeoutException("Publishing request timed out for supplier: " + supplierId);
        }
    }
}
