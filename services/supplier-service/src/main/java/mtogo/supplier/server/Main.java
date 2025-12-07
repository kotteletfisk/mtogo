package mtogo.supplier.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            JavalinBuilder.startServer(7070);
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
