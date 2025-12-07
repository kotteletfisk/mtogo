package mtogo.supplier.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.Context;
import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.exceptions.APIException;
import mtogo.supplier.util.OrderRequester;

public class OrderController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public void getOrders(Context ctx) throws APIException {
        int supplierId;
        try {
            supplierId = Integer.parseInt(ctx.queryParam("supplierId"));
        } catch (NumberFormatException e) {
            throw new APIException(400, "Supplier ID not valid");
        }

        log.debug("received order request for supplierId: {}", supplierId);

        // TODO: Should be validating jwt token and getting ID from there
        ctx.future(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return OrderRequester.getInstance()
                        .requestOrderBlocking(supplierId)
                        .get();
            } catch (TimeoutException e) {
                throw new CompletionException(new APIException(504, "Timed out waiting for response: " + e.getMessage()));
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new CompletionException(new APIException(500, e.getMessage()));
            }
        }).thenApply(result -> {
            ctx.status(200);
            ctx.json(result);
            return result;
        }));
    }
}
