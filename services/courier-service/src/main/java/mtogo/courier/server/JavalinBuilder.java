package mtogo.courier.server;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import mtogo.courier.exceptions.APIException;
import mtogo.courier.exceptions.ExceptionHandler;

import static io.javalin.apibuilder.ApiBuilder.*;

public class JavalinBuilder {

    public static void startServer(int port){

        var app = Javalin.create(config -> {
                    config.staticFiles.add(staticFiles -> {
                        staticFiles.hostedPath = "/";
                        staticFiles.directory = "public";
                        staticFiles.precompress = true;
                        staticFiles.location = Location.CLASSPATH;
                    });
                    config.router.apiBuilder(() -> {
                        path("/api", () -> {
                            get("/health", (ctx) -> ctx.status(200));


                        });
                    });
                })
                .exception(APIException.class, (ExceptionHandler::apiExceptionHandler));

        app.before(ctx -> {
            System.out.println("Request: " + ctx.method() + " " + ctx.path());
        });

        app.error(404, ctx -> {
            if (!ctx.path().startsWith("/api")) {
                ctx.contentType("text/html");
                try (var stream = JavalinBuilder.class.getResourceAsStream("/public/index.html")) {
                    ctx.result(new String(stream.readAllBytes()));
                }
            }
        });


        app.start(port);
    }
}
