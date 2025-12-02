/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

/**
 *
 * @author kotteletfisk
 */
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

        try {
            String body = new String(
                    delivery.getBody(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
            log.info(" [x] Received payload: {}", body);

            int supplierId = Integer.parseInt(body.trim());
            log.info(" [x] Supplier ID: {}", supplierId);

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

            String payload = objectMapper.writeValueAsString(items);
            log.info(" [x] Sending menu response, length={} bytes", payload.length());

            Producer.publishMessage("customer:menu_response", payload);

        } catch (Exception e) {
            log.error("Error handling customer:menu_request", e);
        }
    }

}
