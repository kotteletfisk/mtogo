package mtogo.sql;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;

import mtogo.sql.adapter.handlers.AuthLoginHandler;
import mtogo.sql.adapter.handlers.CustomerMenuRequestHandler;
import mtogo.sql.adapter.handlers.CustomerOrderCreationHandler;
import mtogo.sql.adapter.handlers.IMessageHandler;
import mtogo.sql.adapter.handlers.SupplierOrderCreationHandler;
import mtogo.sql.adapter.in.RabbitMQEventConsumer;
import mtogo.sql.adapter.messaging.RetryingRabbitMQConnectionProvider;
import mtogo.sql.adapter.messaging.ConnectionManager;
import mtogo.sql.adapter.messaging.IRabbitMQConnectionProvider;
import mtogo.sql.adapter.messaging.MessageRouter;
import mtogo.sql.adapter.messaging.RabbitMQConfig;
import mtogo.sql.adapter.out.PostgresAuthRepository;
import mtogo.sql.adapter.out.PostgresModelRepository;
import mtogo.sql.adapter.out.RabbitMQEventProducer;
import mtogo.sql.adapter.out.RabbitMQOrderCreationEventProducer;
import mtogo.sql.adapter.out.RabbitMQOrderPersistedEventProducer;
import mtogo.sql.adapter.out.RabbitMQRpcResponderFactory;
import mtogo.sql.adapter.persistence.SQLConnector;
import mtogo.sql.core.AuthReceiverService;
import mtogo.sql.core.CustomerMenuRequestService;
import mtogo.sql.core.CustomerOrderCreationService;
import mtogo.sql.core.SupplierOrderCreationService;
import mtogo.sql.env.IEnvProvider;
import mtogo.sql.env.SystemEnvProvider;
import mtogo.sql.ports.in.IEventConsumer;
import mtogo.sql.ports.out.IAuthRepository;
import mtogo.sql.ports.out.IModelRepository;
import mtogo.sql.ports.out.IRpcResponderFactory;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Starting SQL Driver ===");

        try {
            ObjectMapper mapper = new ObjectMapper();
            IEnvProvider env = new SystemEnvProvider();

            SQLConnector sqlConnector = new SQLConnector(env);

            IModelRepository modelRepo = new PostgresModelRepository(sqlConnector::getConnection);
            IAuthRepository authRepo = new PostgresAuthRepository(sqlConnector::getConnection);

            // Check repo connection and panic on error
            modelRepo.healthCheck();
            authRepo.healthCheck();


            // Get connection to event broker and panic on error
            RabbitMQConfig config = new RabbitMQConfig(env);
            IRabbitMQConnectionProvider provider = new RetryingRabbitMQConnectionProvider(
                    config, 10, 2000);

            ConnectionManager connectionManager = new ConnectionManager(provider);
            Connection mqConnection = connectionManager.getConnection();


            // General Event publisher
            RabbitMQEventProducer eventProducer = new RabbitMQEventProducer(mapper, mqConnection);

            // Rpc request handler factory
            IRpcResponderFactory rpcFactory = new RabbitMQRpcResponderFactory(mapper, mqConnection);

            Map<String, IMessageHandler> map = Map.of(

                    "customer:order_creation",
                    new CustomerOrderCreationHandler(mapper,
                            new CustomerOrderCreationService(modelRepo),
                            new RabbitMQOrderPersistedEventProducer(eventProducer)),
                    "supplier:order_creation",
                    new SupplierOrderCreationHandler(mapper,
                            new SupplierOrderCreationService(modelRepo),
                            new RabbitMQOrderCreationEventProducer(eventProducer)),
                    "customer:menu_request",
                    new CustomerMenuRequestHandler(
                            new CustomerMenuRequestService(modelRepo),
                            rpcFactory),
                    "auth:login",
                    new AuthLoginHandler(mapper,
                            new AuthReceiverService(authRepo),
                            rpcFactory)
                    );

            IEventConsumer consumer = new RabbitMQEventConsumer(new MessageRouter(map), mqConnection);

            // Start event consumer and panic on error
            consumer.start();

            log.info("SQL-driver started successfully, listening for messages...");
            log.debug("Debug logging is enabled");

            Thread.currentThread().join();

        } catch (InterruptedException e) {
            log.info("Application interrupted, shutting down...");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("FATAL: Failed to start SQL Driver: {}", e.getMessage(), e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
