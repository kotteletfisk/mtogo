package mtogo.sql.handlers;

import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.messaging.Producer;
import mtogo.sql.persistence.SQLConnector;

public class SupplierOrderCreationHandler implements IMessageHandler {
    private final Logger log = LoggerFactory.getLogger(SupplierOrderCreationHandler.class);
    private final ObjectMapper objectMapper;
    private final SQLConnector sqlConnector;

    public SupplierOrderCreationHandler(SQLConnector sqlConnector, ObjectMapper mapper) {
        this.sqlConnector = sqlConnector;
        this.objectMapper = mapper;
    }

    @Override
    public void handle(Delivery delivery) {
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
