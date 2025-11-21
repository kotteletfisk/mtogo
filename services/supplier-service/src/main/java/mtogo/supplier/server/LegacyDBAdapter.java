package mtogo.supplier.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kotteletfisk
 */
public class LegacyDBAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyDBAdapter.class);
    private ServerSocket serverSocket;
    private static LegacyDBAdapter instance;

    private LegacyDBAdapter() {
    };

    public static LegacyDBAdapter getAdapter() {

        if (instance == null) {
            instance = new LegacyDBAdapter();
        }
        return instance;
    }

    public LegacyDBAdapter startListener(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        log.info("Adapter listener started on port " + port);

        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread.ofVirtual().start(() -> handleConnection(socket));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
        }});
        return this;
    }

    // for connection testing
    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }

            log.debug("Received from legacy system:\n" + sb.toString());

        } catch (IOException e) {
            log.error("Connection failed", e);
        }
    }
}
