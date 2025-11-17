package mtogo.customer.server;

import mtogo.customer.controller.OrderController;
import mtogo.customer.exceptions.APIException;
import mtogo.customer.exceptions.ExceptionHandler;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import io.javalin.validation.ValidationException;

public class JavalinBuilder {

    private static final OrderController orderController = OrderController.getInstance();

    public static void startServer(int port) {
        Javalin.create(config -> {
                    config.router.apiBuilder(() -> {
                        path("/api", () -> {

                            get("/", (ctx) -> ctx.status(418)); // Visit me in the browser ;)
                            post("/createorder", orderController::createOrder);

                        });
                    });
                    //Insert other configuration here if needed.
                })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler))
                .exception(ValidationException.class, (ExceptionHandler::validationExceptionHandler))
                .start(port);
    }
}
