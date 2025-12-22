/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.handlers;

import java.io.IOException;
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
import mtogo.sql.ports.out.ModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class CustomerOrderCreationHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomerOrderCreationHandler.class);

    private final ObjectMapper objectMapper;
    private final ModelRepository repo;

    public CustomerOrderCreationHandler(ObjectMapper objectMapper, ModelRepository repo) {
        this.objectMapper = objectMapper;
        this.repo = repo;
    }


    @Override
    public void handle(Delivery delivery, Channel channel) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();

        try {
            OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                    OrderDetailsDTO.class);

            // TODO: use case

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

/*             try (java.sql.Connection conn = sqlConnector.getConnection()) {
                sqlConnector.createOrder(order, orderLines, conn);
            } */

           repo.createOrder(order, orderLines);

            String payload = objectMapper.writeValueAsString(orderDetailsDTO);
            Producer.publishMessage("supplier:order_persisted", payload);

            channel.basicAck(deliveryTag, false);
            log.debug("Order {} acknowledged", orderDetailsDTO.getOrderId());

        }catch (IOException e) {
            log.error("Error handling order: {}", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true); // Requeue for retry
            } catch (IOException nackError) {
                log.error("Failed to NACK message", nackError);
            }
        }
        
    }

}
