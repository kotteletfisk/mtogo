package mtogo.customer.server;

import io.javalin.http.staticfiles.Location;
import mtogo.customer.controller.OrderController;
import mtogo.customer.controller.ProductController;
import mtogo.customer.exceptions.APIException;
import mtogo.customer.exceptions.ExceptionHandler;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import io.javalin.validation.ValidationException;

public class JavalinBuilder {

    private static final OrderController orderController = OrderController.getInstance();
    private static final ProductController productController= ProductController.getInstance();

    public static void startServer(int port) {
        Javalin.create(config -> {
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.hostedPath = "/";
                        staticFiles.directory = "/public";
                        staticFiles.location = Location.CLASSPATH;
                    });

                    config.router.apiBuilder(() -> {
                        get("/", ctx -> ctx.redirect("/cs-order.html"));

                        path("/api", () -> {
                            get("/health", (ctx) -> ctx.status(200));
                            get("/", (ctx) -> ctx.status(418)); // Visit me in the browser ;)
                            post("/createorder", orderController::createOrder);
                            get("/{supplierId}/items", productController::getItemsBySupplierId);
                            get("/suppliers/{zipcode}", productController::getActiveSuppliers);

                        });
                    });
                    //Insert other configuration here if needed.
                })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler))
                .exception(ValidationException.class, (ExceptionHandler::validationExceptionHandler))
                .start(port);
    }
}
