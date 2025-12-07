package mtogo.supplier.controller;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.Context;
import mtogo.supplier.exceptions.APIException;
import mtogo.supplier.handlers.OrderRPCHandler;

public class OrderController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final OrderRPCHandler orderRPCHandler;

    public OrderController(OrderRPCHandler orderRPCHandler) {
        this.orderRPCHandler = orderRPCHandler;
    }

    public void getOrders(Context ctx) throws APIException {
        int supplierId;
        try {
            supplierId = Integer.parseInt(ctx.queryParam("supplierId"));
        } catch (NumberFormatException e) {
            throw new APIException(400, "Supplier ID not valid");
        }

        log.debug("received order request for supplierId: {}", supplierId);

        // TODO: Should be validating jwt token and getting ID from there
        ctx.future(() -> {
            try {
                return orderRPCHandler.requestOrders(supplierId)
                        .thenApply(result -> {
                            ctx.status(200).json(result);
                            return result;
                        });
            } catch (IOException | InterruptedException | TimeoutException | ExecutionException e) {
                log.error(e.getMessage());
                throw new CompletionException(new APIException(500, e.getMessage()));
            }
        });
    }
}
