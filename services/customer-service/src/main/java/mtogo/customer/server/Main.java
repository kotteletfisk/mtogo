package mtogo.customer.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.customer.messaging.Consumer;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Customer Service...");

        // Start HTTP server
        JavalinBuilder.startServer(7070);
        log.info("HTTP server started on port 7070");

        try {
            String[] bindingKeys = {"customer:menu_response", "customer:supplier_response"};

            log.info("Starting RabbitMQ consumer with keys: {}", String.join(", ", bindingKeys));
            Consumer.consumeMessages(bindingKeys);

            log.info("Customer-service started, listening for order events...");

            // CRITICAL: Keep application alive
            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("FATAL: Failed to start consumer: {}", e.getMessage(), e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}