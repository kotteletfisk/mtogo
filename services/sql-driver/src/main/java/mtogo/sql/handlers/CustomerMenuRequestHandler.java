package mtogo.sql.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.menuItemDTO;
import mtogo.sql.messaging.Producer;
import mtogo.sql.persistence.SQLConnector;

public class CustomerMenuRequestHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SQLConnector sqlConnector;
    private final ObjectMapper objectMapper;

    public CustomerMenuRequestHandler(SQLConnector sqlConnector, ObjectMapper objectMapper) {
        this.sqlConnector = sqlConnector;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();

        try {
            String body = new String(
                    delivery.getBody(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
            log.info(" [x] Received payload: {}", body);

            // Parse "correlationId:supplierId"
            int separatorIndex = body.indexOf(":");
            if (separatorIndex == -1) {
                log.error("Invalid menu request format - missing ':' separator");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            String correlationId = body.substring(0, separatorIndex);
            int supplierId = Integer.parseInt(body.substring(separatorIndex + 1).trim());

            log.info(" [x] Supplier ID: {}, Correlation: {}", supplierId, correlationId);

            List<menuItemDTO> items;

            try (java.sql.Connection conn = sqlConnector.getConnection()) {
                log.info(" [x] Fetching menu items from DB for supplier {}", supplierId);
                items = sqlConnector.getMenuItemsBySupplierId(supplierId, conn);
                log.info(" [x] Found {} menu items for supplier {}",
                        (items == null ? 0 : items.size()), supplierId);
            }

            if (items == null) {
                items = java.util.Collections.emptyList();
            }

            String itemsJson = objectMapper.writeValueAsString(items);
            // Format: "correlationId::[json]"
            String payload = correlationId + "::" + itemsJson;
            log.info(" [x] Sending menu response, length={} bytes", payload.length());

            boolean published = Producer.publishMessage("customer:menu_response", payload);

            if (published) {
                channel.basicAck(deliveryTag, false);
            } else {
                log.error("Failed to publish menu response");
                channel.basicNack(deliveryTag, false, true); // Requeue for retry
            }

        } catch (NumberFormatException e) {
            log.error("Invalid supplierId format in request", e);
            try {
                channel.basicNack(deliveryTag, false, false); // Don't requeue bad format
            } catch (Exception nackError) {
                log.error("Failed to NACK message", nackError);
            }
        } catch (Exception e) {
            log.error("Error handling customer:menu_request", e);
            try {
                channel.basicNack(deliveryTag, false, true); // Requeue for retry
            } catch (Exception nackError) {
                log.error("Failed to NACK message", nackError);
            }
        }
    }
}