package mtogo.courier.server;

import io.javalin.Javalin;
import mtogo.courier.exceptions.APIException;
import mtogo.courier.exceptions.ExceptionHandler;

import static io.javalin.apibuilder.ApiBuilder.*;

public class JavalinBuilder {

    public static void startServer(int port){
        var app = Javalin.create(config -> {
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.hostedPath = "/";
                        staticFiles.directory = "/app/public";
                        staticFiles.precompress = true;
                    });
                    config.router.apiBuilder(() -> {
                        path("/api", () -> {
                            get("/health", (ctx) -> ctx.status(200));


                        });
                    });
                })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler));

        app.error(404, ctx -> {
            if (!ctx.path().startsWith("/api")) {
                ctx.contentType("text/html");
                ctx.result(java.nio.file.Files.readString(java.nio.file.Path.of("/app/public/index.html")));
            }
        });

        app.start(port);
    }
}
