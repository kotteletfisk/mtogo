/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.supplier.handlers;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import lombok.AllArgsConstructor;
import mtogo.supplier.DTO.OrderDTO;
import mtogo.supplier.util.OrderRequester;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author kotteletfisk
 */
@AllArgsConstructor
public class SupplierOrderResponseHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;

    @Override
    public void handle(Delivery delivery, Channel channel) {

        log.info("Handling order response");

        String correlationId = delivery.getProperties().getCorrelationId();
        String body = new String(
                delivery.getBody(),
                java.nio.charset.StandardCharsets.UTF_8
        );

        log.debug("Received correlation ID: {}", correlationId);
        log.debug("Received message body: {}", body);

        List<OrderDTO> orders = objectMapper.readValue(
                body,
                objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, OrderDTO.class)
        );

        try {
            OrderRequester.getInstance().completeOrderRequest(correlationId, orders);
            log.info("Completed order request");
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

}
