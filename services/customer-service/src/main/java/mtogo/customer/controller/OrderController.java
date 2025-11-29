package mtogo.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.messaging.Producer;
import mtogo.customer.service.MenuService;
import mtogo.payment.MobilePayStrategy;
import mtogo.payment.PaymentService;
import mtogo.payment.PaypalStrategy;
import mtogo.payment.RevenueShareCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

                double total = orderDetailsDTO.getOrderLines().stream()
                        .mapToDouble(line -> line.getPriceSnapshot() * line.getAmount())
                        .sum();


                PaymentService ps = new PaymentService();

                switch (orderDetailsDTO.getPaymentMethod()){
                    case PAYPAL -> ps.setPaymentStrategy(new PaypalStrategy());
                    case MOBILEPAY -> ps.setPaymentStrategy(new MobilePayStrategy());
                }

                boolean pay = ps.pay(total);
                if (!pay){
                    ctx.status(402).result("Payment failed");
                    return;
                }
                LocalDateTime orderTime = LocalDateTime.now();

                RevenueShareCalculator.printBreakdown(total, orderTime);

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
     * Retrieves menu items for a given supplier ID. Calls the MenuService to fetch the items.
     * @param ctx the Javalin context
     */
    public void getItemsBySupplierId(Context ctx){
        String supplierId = ctx.pathParam("supplierId");
        try
        {
            int supplierIdInt = Integer.parseInt(supplierId);

            List<menuItemDTO> items = MenuService.getInstance().requestMenuBlocking(supplierIdInt);
            ctx.status(200).json(items);

        }
         catch (NumberFormatException e) {
            ctx.status(400).result("Invalid supplierId: " + supplierId);
        } catch (java.util.concurrent.TimeoutException e) {
            ctx.status(504).result("Timed out waiting for menu items");
        } catch (Exception e) {
            ctx.status(500).result("Failed to retrieve menu items");
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
                .check(r -> r.getPaymentMethod() != null, "Payment method must be provided")
                .get();
    }

}
