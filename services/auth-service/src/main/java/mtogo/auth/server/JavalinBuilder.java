package mtogo.auth.server;

import com.rabbitmq.client.ConnectionFactory;
import io.javalin.Javalin;
import mtogo.auth.controller.AuthController;
import mtogo.auth.exceptions.APIException;
import mtogo.auth.exceptions.ExceptionHandler;
import mtogo.auth.messaging.RabbitRpcClient;

import mtogo.auth.service.JwtService;
import mtogo.auth.util.KeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class JavalinBuilder {

    public static final Logger log = LoggerFactory.getLogger(JavalinBuilder.class);

    public static void startServer(int port) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("rabbitmq");
            factory.setPort(5672);
            factory.setUsername(System.getenv("RABBITMQ_USER"));
            factory.setPassword(System.getenv("RABBITMQ_PASS"));

            RabbitRpcClient rpcClient = new RabbitRpcClient(factory);

            String privateKey = KeyLoader.loadPrivateKey();
            JwtService jwtService = new JwtService(privateKey);


            var app = Javalin.create(config -> {
                        config.router.apiBuilder(() -> {
                            path("/api", () -> {
                                get("/health", (ctx) -> ctx.status(200));
                                post("/login", (ctx) -> AuthController.getInstance(rpcClient, jwtService).login(ctx));
                            });
                        });
                    })
                    .exception(APIException.class, (ExceptionHandler::apiExceptionHandler));

            app.start(port);
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.info("Server failed to start - see debug logs for details");
        }
    }
}
