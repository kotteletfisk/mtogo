package mtogo.sql;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import mtogo.sql.handlers.AuthLoginHandler;
import mtogo.sql.handlers.CustomerMenuRequestHandler;
import mtogo.sql.handlers.CustomerOrderCreationHandler;
import mtogo.sql.handlers.IMessageHandler;
import mtogo.sql.handlers.SupplierOrderCreationHandler;
import mtogo.sql.messaging.AuthReceiver;
import mtogo.sql.messaging.Consumer;
import mtogo.sql.messaging.MessageRouter;
import mtogo.sql.persistence.SQLConnector;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Starting SQL Driver ===");

        try {
            SQLConnector connector = new SQLConnector();
            ObjectMapper mapper = new ObjectMapper();
            AuthReceiver authReceiver = new AuthReceiver();

            Map<String, IMessageHandler> map = Map.of(
                    "customer:order_creation", new CustomerOrderCreationHandler(connector, mapper),
                    "supplier:order_creation", new SupplierOrderCreationHandler(connector, mapper),
                    "customer:menu_request", new CustomerMenuRequestHandler(connector, mapper),
                    "auth:login", new AuthLoginHandler(authReceiver, mapper)
            );

            MessageRouter router = new MessageRouter(map);

            String[] bindingKeys = map.keySet().toArray(new String[0]);

            log.info("Starting consumer with binding keys: {}", String.join(", ", bindingKeys));
            Consumer.consumeMessages(bindingKeys, router);

            log.info("SQL-driver started successfully, listening for messages...");
            log.debug("Debug logging is enabled");

            log.info("Application running. Press Ctrl+C to stop.");
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