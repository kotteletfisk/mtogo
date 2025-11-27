package mtogo.auth.server;

import io.javalin.Javalin;
import mtogo.auth.controller.AuthController;
import mtogo.auth.exceptions.APIException;
import mtogo.auth.exceptions.ExceptionHandler;

import static io.javalin.apibuilder.ApiBuilder.*;

public class JavalinBuilder {

    public static void startServer(int port){

        var app = Javalin.create(config -> {
                    config.router.apiBuilder(() -> {
                        path("/api", () -> {
                            get("/health", (ctx) -> ctx.status(200));
                            post("/login", (ctx) -> AuthController.getInstance().login(ctx));
                        });
                    });
                })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler));

        app.before(ctx -> {
            System.out.println("Request: " + ctx.method() + " " + ctx.path());
        });

        app.start(port);
    }
}
