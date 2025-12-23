package mtogo.sql.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.core.SupplierOrderCreationService;
import mtogo.sql.ports.out.IMessageProducer;

public class SupplierOrderCreationHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper;
    private final SupplierOrderCreationService service;
    private final IMessageProducer publisher;

    public SupplierOrderCreationHandler(ObjectMapper mapper, SupplierOrderCreationService service, IMessageProducer publisher) {
        this.objectMapper = mapper;
        this.service = service;
        this.publisher = publisher;
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {
        log.info("Handling legacy order message");
        long tag = delivery.getEnvelope().getDeliveryTag();

        try {
            LegacyOrderDetailsDTO legacyOrderDetailsDTO = objectMapper.readValue(delivery.getBody(),
                    LegacyOrderDetailsDTO.class);

            OrderDetailsDTO enriched = service.call(legacyOrderDetailsDTO);

            if (publisher.publishObject("customer:order_creation", enriched)) {
                log.debug("Published:\n" + enriched.toString());
            }

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                channel.basicNack(tag, false, false);
            } catch (IOException io) {
                log.error("Failed to NACK message", io);
            }
        }
    }
}
