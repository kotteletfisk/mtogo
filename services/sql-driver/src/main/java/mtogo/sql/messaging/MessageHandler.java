package mtogo.sql.messaging;

import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.persistence.SQLConnector;

public class MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    SQLConnector sqlConnector = new SQLConnector();

    public void handleLegacyOrder(Delivery delivery) {
        log.info("Handling legay order message");
        try {
            LegacyOrderDetailsDTO legacyOrderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                    LegacyOrderDetailsDTO.class);
            log.debug("Received:\n" + legacyOrderDetailsDTO.toString());

            try (java.sql.Connection conn = sqlConnector.getConnection()) {
                OrderDetailsDTO enriched = sqlConnector.customerEnrichLegacyOrder(legacyOrderDetailsDTO, conn);
                Producer.publishObject("customer:order_creation", enriched);
                log.debug("Published:\n" + enriched.toString());
            }
        } catch (SQLException | IOException e) {
            log.error(e.getMessage());
        }
    }
}
