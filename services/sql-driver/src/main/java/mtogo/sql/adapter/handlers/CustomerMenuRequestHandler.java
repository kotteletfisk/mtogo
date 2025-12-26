package mtogo.sql.adapter.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.core.CustomerMenuRequestService;
import mtogo.sql.model.DTO.menuItemDTO;
import mtogo.sql.ports.out.IRpcResponder;
import mtogo.sql.ports.out.IRpcResponderFactory;

public class CustomerMenuRequestHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final CustomerMenuRequestService service;
    private final IRpcResponderFactory factory;

    public CustomerMenuRequestHandler(CustomerMenuRequestService service, IRpcResponderFactory factory) {
        this.service = service;
        this.factory = factory;
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();

        try {
            String body = new String(
                    delivery.getBody(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
            log.info("Received payload: {}", body);

            // Parse "correlationId:supplierId"
            int separatorIndex = body.indexOf(":");
            if (separatorIndex == -1) {
                log.error("Invalid menu request format - missing ':' separator");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // String correlationId = body.substring(0, separatorIndex);
            int supplierId = Integer.parseInt(body.substring(separatorIndex + 1).trim());

            log.info("Supplier ID: {}, Correlation: {}", supplierId, delivery.getProperties().getCorrelationId());

            List<menuItemDTO> items = service.call(supplierId);

            /*             String itemsJson = objectMapper.writeValueAsString(items);
            // Format: "correlationId::[json]"
            String payload = correlationId + "::" + itemsJson;
            log.info("Sending menu response, length={} bytes", payload.length()); */

            IRpcResponder responder = factory.create(delivery);
            responder.reply(items);

            /*             boolean published = Producer.publishMessage("customer:menu_response", payload); */
            channel.basicAck(deliveryTag, false);

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
