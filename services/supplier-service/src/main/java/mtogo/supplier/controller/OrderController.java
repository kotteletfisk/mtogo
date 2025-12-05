package mtogo.supplier.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.javalin.http.Context;
import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.exceptions.APIException;
import mtogo.supplier.util.OrderRequester;

public class OrderController {

    public void getOrders(Context ctx) throws APIException {
        int supplierId;
        try {
            supplierId = Integer.parseInt(ctx.queryParam("supplier"));
        } catch (NumberFormatException e) {
            throw new APIException(400, "Supplier ID not valid");
        }

        // TODO: Should be validating jwt token and getting ID from there

        ctx.future(() -> {
            try {
                return OrderRequester.requestOrderBlocking(supplierId);
            } catch (IOException | InterruptedException | TimeoutException | ExecutionException e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }
}
