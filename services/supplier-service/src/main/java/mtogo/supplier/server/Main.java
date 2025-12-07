package mtogo.supplier.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.supplier.handlers.IMessageHandler;
import mtogo.supplier.handlers.SupplierOrderResponseHandler;
import mtogo.supplier.messaging.Consumer;
import mtogo.supplier.messaging.MessageRouter;
import tools.jackson.databind.ObjectMapper;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            JavalinBuilder.startServer(7070);
            LegacyDBAdapter.getAdapter().startListener(1984);

            // TODO: add consumer start with handler
            Map<String, IMessageHandler> map = Map.of(
                    "supplier:order_response", new SupplierOrderResponseHandler(new ObjectMapper())
                    // "auth:login", new AuthLoginHandler(authReceiver, mapper)
            );

            MessageRouter router = new MessageRouter(map);

            String[] bindingKeys = map.keySet().toArray(new String[map.size()]);

            Consumer.consumeMessages(bindingKeys, router);
            log.info("Supplier service started");
            log.debug("Debug logging enabled");

        } catch (Exception e) {
            // A core compoenent is not working. Crash jvm and let orchestrator handle.
            log.error("CRITICAL: {}", e.getMessage());
            System.exit(1);
        }
    }
}
