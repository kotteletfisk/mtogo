package mtogo.supplier.server;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import io.javalin.http.staticfiles.Location;
import io.javalin.validation.ValidationException;
import mtogo.supplier.exceptions.APIException;
import mtogo.supplier.exceptions.ExceptionHandler;

public class JavalinBuilder {

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
                });
            });
            //Insert other configuration here if needed.
        })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler))
                .exception(ValidationException.class, (ExceptionHandler::validationExceptionHandler))
                .start(port);
    }
}
