package mtogo.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.messaging.Producer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for handling order-related operations.
 * Uses singleton Pattern.
 */
public class OrderController {
    private static final ObjectMapper objectMapper = new ObjectMapper();


    private static OrderController instance;
    public static OrderController getInstance() {
        if (instance == null) {
            instance = new OrderController();
        }
        return instance;
    }
    private OrderController() {}

    /**
     * Creates a new order. Publishes an order creation message to the message broker with the OrderDetailsDTO.
     * The orderId is generated server-side.
     * @param ctx the Javalin context.
     */
    public void createOrder(Context ctx){

        OrderDetailsDTO orderDetailsDTO = validateOrderDTO(ctx);
        if (orderDetailsDTO != null) {
            try {
                // Generate the orderId
                UUID orderId = UUID.randomUUID();
                orderDetailsDTO.setOrderId(orderId);

                AtomicInteger counter = new AtomicInteger(1);
                if (orderDetailsDTO.getOrderLines() != null) {
                    orderDetailsDTO.getOrderLines().forEach(line -> {
                        line.setOrderId(orderId);
                        line.setOrderLineId(counter.getAndIncrement());
                    });
                }

                String payload = objectMapper.writeValueAsString(orderDetailsDTO);

                Producer.publishMessage("customer:order_creation", payload);
                ctx.status(201).json(orderDetailsDTO);

            } catch (Exception e) {
                ctx.status(500).result("Failed to publish order creation message");
            }
        } else {
            ctx.status(400).result("Invalid order data");
        }
    }

    /**
     * Validates the OrderDetailsDTO from the request body.
     * @param ctx the Javalin context
     * @return the validated OrderDetailsDTO
     */
    public OrderDetailsDTO validateOrderDTO(Context ctx){
        return ctx.bodyValidator(OrderDetailsDTO.class)
                .check(r -> r != null, "Object is null")
                .check(r-> r.getCustomerId() > 0, "Customer ID must be greater than 0")
                .check(r-> r.getStatus() == OrderDetailsDTO.orderStatus.created, "Order status must be provided")
                .check(r-> r.getOrderLines() != null && !r.getOrderLines().isEmpty(), "Order must contain at least one order line")
                .get();
    }

}
