package mtogo.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.redis.messaging.Consumer;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        try {
            String[] bindingKeys = { "customer:order_creation", "customer:supplier_request", "supplier:order_request" };
            Consumer.consumeMessages(bindingKeys);
            log.info("redis-driver started, listening for order events...");
            log.debug("Debug logging enabled");
        } catch (Exception e) {
            // A core compoenent is not working. Crash jvm and let orchestrator handle.
            log.error("CRITICAL: {}", e.getMessage());
            System.exit(1);
        }

    }

}