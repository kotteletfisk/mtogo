package mtogo.sql;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;

import mtogo.sql.adapter.in.RabbitMQEventConsumer;
import mtogo.sql.adapter.out.PostgresAuthRepository;
import mtogo.sql.adapter.out.PostgresModelRepository;
import mtogo.sql.adapter.out.RabbitMQEventProducer;
import mtogo.sql.adapter.out.RabbitMQOrderCreationEventProducer;
import mtogo.sql.adapter.out.RabbitMQOrderPersistedEventProducer;
import mtogo.sql.core.AuthReceiverService;
import mtogo.sql.core.CustomerMenuRequestService;
import mtogo.sql.core.CustomerOrderCreationService;
import mtogo.sql.core.SupplierOrderCreationService;
import mtogo.sql.handlers.AuthLoginHandler;
import mtogo.sql.handlers.CustomerMenuRequestHandler;
import mtogo.sql.handlers.CustomerOrderCreationHandler;
import mtogo.sql.handlers.IMessageHandler;
import mtogo.sql.handlers.SupplierOrderCreationHandler;
import mtogo.sql.messaging.ConnectionManager;
import mtogo.sql.messaging.MessageRouter;
import mtogo.sql.persistence.SQLConnector;
import mtogo.sql.ports.in.IEventConsumer;
import mtogo.sql.ports.out.IAuthRepository;
import mtogo.sql.ports.out.IModelRepository;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Starting SQL Driver ===");

        try {
            ObjectMapper mapper = new ObjectMapper();
            SQLConnector sqlConnector = new SQLConnector();

            IModelRepository modelRepo = new PostgresModelRepository(sqlConnector);
            IAuthRepository authRepo = new PostgresAuthRepository(sqlConnector);

            // Check repo connection and panic on error
            modelRepo.healthCheck();
            authRepo.healthCheck();

            // Get connection to event broker and panic on error
            Connection mqConnection = ConnectionManager.getConnectionManager().getConnection();

            RabbitMQEventProducer eventProducer = new RabbitMQEventProducer(mapper, mqConnection);
            
            Map<String, IMessageHandler> map = Map.of(
                    "customer:order_creation", 
                            new CustomerOrderCreationHandler(mapper,
                            new CustomerOrderCreationService(modelRepo),
                            new RabbitMQOrderPersistedEventProducer(eventProducer)),
                    "supplier:order_creation", 
                            new SupplierOrderCreationHandler(mapper,
                            new SupplierOrderCreationService(modelRepo),
                            new RabbitMQOrderCreationEventProducer(eventProducer)),
                    "customer:menu_request", new CustomerMenuRequestHandler(mapper, new CustomerMenuRequestService(modelRepo)),
                    "auth:login", new AuthLoginHandler(mapper, new AuthReceiverService(authRepo))
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
