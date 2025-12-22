/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.core.CustomerOrderCreationService;
import mtogo.sql.messaging.Producer;

/**
 *
 * @author kotteletfisk
 */
public class CustomerOrderCreationHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomerOrderCreationHandler.class);

    private final ObjectMapper objectMapper;
    private final CustomerOrderCreationService service;

    public CustomerOrderCreationHandler(ObjectMapper objectMapper, CustomerOrderCreationService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }


    @Override
    public void handle(Delivery delivery, Channel channel) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();

        try {
            OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                    OrderDetailsDTO.class);

            service.call(orderDetailsDTO);

            String payload = objectMapper.writeValueAsString(orderDetailsDTO);
            Producer.publishMessage("supplier:order_persisted", payload);

            channel.basicAck(deliveryTag, false);
            log.debug("Order {} acknowledged", orderDetailsDTO.getOrderId());

        }
        catch (Exception e) {
            log.error("Error handling order: {}", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackError) {
                log.error("Failed to NACK message", nackError);
            }
        }
    }

}
