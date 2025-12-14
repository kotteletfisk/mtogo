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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.messaging.Producer;
import tools.jackson.databind.ObjectMapper;

public class OrderRPCHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String replyQueue = "supplier:order_response";

    private final ObjectMapper objectMapper;
    private final Map<String, CompletableFuture<List<OrderDTO>>> pendingRequests = new ConcurrentHashMap<>();

    public OrderRPCHandler(ObjectMapper objectMapper) throws IOException, InterruptedException {
        this.objectMapper = objectMapper;
    }

    public String getReplyQueue() {
        return this.replyQueue;
    }

    public CompletableFuture<List<OrderDTO>> requestOrders(int supplierId)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {

        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<OrderDTO>> future = new CompletableFuture<>();

        pendingRequests.put(correlationId, future);

        log.info("Creating order request for supplier: {}", supplierId);
        log.debug("Correlation ID: {}", correlationId);

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
            log.error(e.getMessage());
            throw new TimeoutException("Publishing request timed out for supplier: " + supplierId);
        }
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {

        log.info("Handling order response");
        String responseCorrelationId = delivery.getProperties().getCorrelationId();
        log.debug("Received correlation ID: {}", responseCorrelationId);

        CompletableFuture<List<OrderDTO>> future = pendingRequests.remove(responseCorrelationId);

        if (future != null) {

            String body = new String(
                    delivery.getBody(),
                    java.nio.charset.StandardCharsets.UTF_8);

            log.debug("Received message body: {}", body);

            List<OrderDTO> orders = objectMapper.readValue(
                    body,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, OrderDTO.class));

            future.complete(orders);
            try {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (IOException ex) {
                try {
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                } catch (IOException ex1) {
                    log.error("Failed to Nack: {}", ex1.getMessage());
                }
                log.error(ex.getMessage());
            }

            log.info("Completed future for correlation ID {}", responseCorrelationId);
        } else {
            log.warn("No requests found with correlation id: {}", responseCorrelationId);
            try {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            } catch (IOException ex) {
                log.error("Failed to Nack: {}", ex.getMessage());
            }
        }
    }
}
