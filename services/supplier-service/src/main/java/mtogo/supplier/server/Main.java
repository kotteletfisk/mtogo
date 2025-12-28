package mtogo.supplier.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;

import mtogo.supplier.controller.OrderController;
import mtogo.supplier.handlers.IMessageHandler;
import mtogo.supplier.handlers.OrderRPCHandler;
import mtogo.supplier.messaging.ConnectionManager;
import mtogo.supplier.messaging.Consumer;
import mtogo.supplier.messaging.MessageRouter;
import tools.jackson.databind.ObjectMapper;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {

            Connection connection = ConnectionManager.getConnectionManager().getConnection();

            OrderRPCHandler orderRPCHandler = new OrderRPCHandler(new ObjectMapper());

            Map<String, IMessageHandler> map = Map.of(
                    orderRPCHandler.getReplyQueue(), orderRPCHandler
            );

            MessageRouter router = new MessageRouter(map);
            String[] bindingKeys = map.keySet().toArray(new String[map.size()]);

            Consumer consumer = new Consumer();
            consumer.consumeMessages(bindingKeys, connection, router);

            OrderController controller = new OrderController(orderRPCHandler);
            JavalinBuilder.startServer(7070, controller);
            LegacyDBAdapter.getAdapter().startListener(1984);

            log.info("Supplier service started");
            log.debug("Debug logging enabled");

        } catch (Exception e) {
            // A core compoenent is not working. Crash jvm and let orchestrator handle.
            log.error("CRITICAL: {}", e.getMessage());
            System.exit(1);
        }
    }
}
