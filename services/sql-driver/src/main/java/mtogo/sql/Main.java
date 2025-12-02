package mtogo.sql;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import mtogo.sql.handlers.CustomerOrderCreationHandler;
import mtogo.sql.handlers.IMessageHandler;
import mtogo.sql.handlers.SupplierOrderCreationHandler;
import mtogo.sql.messaging.Consumer;
import mtogo.sql.messaging.MessageRouter;
import mtogo.sql.persistence.SQLConnector;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {

            SQLConnector connector = new SQLConnector();
            ObjectMapper mapper = new ObjectMapper();

            Map<String, IMessageHandler> map = Map.of(
                    "customer:order_creation", new CustomerOrderCreationHandler(connector, mapper),
                    "supplier:order_creation", new SupplierOrderCreationHandler(connector, mapper)
            );

            MessageRouter router = new MessageRouter(map);

            // String[] k = map.keySet().toArray(new String[0]);

            String[] bindingKeys = {"customer:order_creation", "supplier:order_creation", "customer:menu_request", "auth:login"};
            Consumer.consumeMessages(bindingKeys, router);
            log.info("SQL-driver started, listening for order events...");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
