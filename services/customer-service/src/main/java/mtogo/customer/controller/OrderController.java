package mtogo.customer.controller;

import io.javalin.http.Context;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.messaging.Producer;

/**
 * Controller for handling order-related operations.
 * Uses singleton Pattern.
 */
public class OrderController {

    private static OrderController instance = null;
    public static OrderController getInstance() {
        if (instance == null) {
            instance = new OrderController();
        }
        return instance;
    }
    private OrderController() {}

    /**
     * Creates a new order. Publishes an order creation message to the message broker with the OrderDetailsDTO.
     * @param ctx the Javalin context.
     */
    public void createOrder(Context ctx){

        OrderDetailsDTO orderDetailsDTO = validateOrderDTO(ctx);
        if (orderDetailsDTO != null) {
            try {
                Producer.publishMessage("customer:order_creation", orderDetailsDTO.toString());
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
                .check(r-> r.getOrderId() >= 0, "Order ID must be non-negative")
                .check(r-> r.getCustomerId() > 0, "Customer ID must be greater than 0")
                .check(r-> r.getStatus() == OrderDetailsDTO.orderStatus.Pending, "Order status must be provided")
                .check(r-> r.getOrderLines() != null && !r.getOrderLines().isEmpty(), "Order must contain at least one order line")
                .get();
    }

}
