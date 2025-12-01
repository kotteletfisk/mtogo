package mtogo.supplier.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        try {
            JavalinBuilder.startServer(7070);
            LegacyDBAdapter.getAdapter().startListener(1984);
            
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }
}