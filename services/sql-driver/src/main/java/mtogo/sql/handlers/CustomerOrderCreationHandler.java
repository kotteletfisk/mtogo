/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.messaging.Producer;
import mtogo.sql.persistence.SQLConnector;

/**
 *
 * @author kotteletfisk
 */
public class CustomerOrderCreationHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomerOrderCreationHandler.class);

    private final SQLConnector sqlConnector;
    private final ObjectMapper objectMapper;

    public CustomerOrderCreationHandler(SQLConnector sqlConnector, ObjectMapper objectMapper) {
        this.sqlConnector = sqlConnector;
        this.objectMapper = objectMapper;
    }


    @Override
    public void handle(Delivery delivery, Channel channel) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();

        try {
            OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                    OrderDetailsDTO.class);

            OrderDTO order = new OrderDTO(orderDetailsDTO);

            List<OrderLineDTO> orderLines = new ArrayList<>();
            for (OrderLineDTO line : orderDetailsDTO.getOrderLineDTOS()) {
                orderLines.add(
                        new OrderLineDTO(
                                line.getOrderLineId(),
                                line.getOrderId(),
                                line.getItemId(),
                                line.getPriceSnapshot(),
                                line.getAmount()));
            }

            log.info("'" + orderDetailsDTO + "'");
            String bodyStr = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            log.info("Raw message body: " + bodyStr);

            try (java.sql.Connection conn = sqlConnector.getConnection()) {
                sqlConnector.createOrder(order, orderLines, conn);
            }

            String payload = objectMapper.writeValueAsString(orderDetailsDTO);
            Producer.publishMessage("supplier:order_persisted", payload);

            channel.basicAck(deliveryTag, false);
            log.debug("Order {} acknowledged", orderDetailsDTO.getOrderId());

        } catch (org.postgresql.util.PSQLException e) {

            if (e.getMessage().contains("duplicate key")) {
                log.error("DB failure - duplicate order, discarding: {}", e.getMessage());
                try {
                    channel.basicNack(deliveryTag, false, false); // Don't requeue
                } catch (IOException nackError) {
                    log.error("Failed to NACK duplicate order", nackError);
                }
            } else {
                log.error("DB failure: {}", e.getMessage());
                try {
                    channel.basicNack(deliveryTag, false, true); // Requeue other DB errors
                } catch (IOException nackError) {
                    log.error("Failed to NACK message", nackError);
                }
            }
        } catch (IOException | SQLException e) {
            log.error("Error handling order: {}", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true); // Requeue for retry
            } catch (IOException nackError) {
                log.error("Failed to NACK message", nackError);
            }
        }
    }

}
