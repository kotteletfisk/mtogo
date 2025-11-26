package mtogo.supplier.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.supplier.DTO.LegacyOrder;
import mtogo.supplier.DTO.LegacyOrderDetailsDTO;
import mtogo.supplier.factory.OrderDTOFactory;
import mtogo.supplier.messaging.Producer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 *
 * @author kotteletfisk
 */
public class LegacyDBAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyDBAdapter.class);
    private static LegacyDBAdapter instance;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private ServerSocket serverSocket;

    private LegacyDBAdapter() {
        xmlMapper = new XmlMapper();
        objectMapper = new ObjectMapper();
    }

    ;

    public static LegacyDBAdapter getAdapter() {

        if (instance == null) {
            instance = new LegacyDBAdapter();
        }
        return instance;
    }

    public LegacyDBAdapter startListener(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        log.info("Adapter listener started on port " + port);

        Thread.ofPlatform().start(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread.ofVirtual().start(() -> handleConnection(socket));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
            }
        });
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

            log.info("Received connection from Legacy system");

            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }

            log.debug("Data from legacy system:\n" + sb.toString());

            LegacyOrderDetailsDTO dto = xmlToDTO(sb.toString());
            
            Producer.publishObject("supplier:order_creation", dto);

        } catch (IOException e) {
            log.error("Connection failed: ", e);
        }
    }

    private LegacyOrderDetailsDTO xmlToDTO(String xmlString) {

        try {
            LegacyOrder legacyOrder = xmlMapper.readValue(xmlString, LegacyOrder.class);
            log.debug("Mapped object:\n" + legacyOrder.toString());

            LegacyOrderDetailsDTO dto = OrderDTOFactory.createFromLegacy(legacyOrder);
            log.debug("Transformed DTO:\n" + dto.toString());

            return dto;

        } catch (JacksonException | IllegalArgumentException e) {
            log.error("Error while transforming: " + e.getMessage());
        }
        return null;
    }
}
