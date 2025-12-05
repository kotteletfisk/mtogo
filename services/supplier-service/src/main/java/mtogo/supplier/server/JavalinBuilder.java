package mtogo.supplier.server;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import io.javalin.http.staticfiles.Location;
import io.javalin.validation.ValidationException;
import mtogo.supplier.controller.OrderController;
import mtogo.supplier.exceptions.APIException;
import mtogo.supplier.exceptions.ExceptionHandler;
import mtogo.supplier.server.security.AppRole;
import mtogo.supplier.server.security.Middleware;

public class JavalinBuilder {

    private static final OrderController orderController = new OrderController();

    public static void startServer(int port) {
        Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "public";
                staticFiles.precompress = true;
                staticFiles.location = Location.CLASSPATH;
            });

            config.router.apiBuilder(() -> {

                path("/api", () -> {
                    get("/health", (ctx) -> ctx.status(200));
                    get("/", (ctx) -> ctx.status(418)); // Visit me in the browser ;)
                    get("/access-test",  (ctx) -> ctx.status(200), AppRole.MANAGER);
                    get("/orders", orderController::getOrders);
                });
            });
            //Insert other configuration here if needed.
        })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler))
                .exception(ValidationException.class, (ExceptionHandler::validationExceptionHandler))

        .error(404, ctx -> {
            if (!ctx.path().startsWith("/api")) {
                ctx.contentType("text/html");
                ctx.result(JavalinBuilder.class
                        .getResourceAsStream("/public/200.html"));
            }
        })
                .beforeMatched("/api/*", ctx -> Middleware.registerAuth(ctx))
        .start(port);
    }
}
