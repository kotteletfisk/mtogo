/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.core.AuthReceiverService;
import mtogo.sql.ports.out.IRpcResponder;
import mtogo.sql.ports.out.IRpcResponderFactory;

/**
 *
 * @author kotteletfisk
 */
public class AuthLoginHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;
    private final AuthReceiverService ar;
    private final IRpcResponderFactory factory;

    public AuthLoginHandler(ObjectMapper objectMapper, AuthReceiverService ar, IRpcResponderFactory factory) {
        this.ar = ar;
        this.objectMapper = objectMapper;
        this.factory = factory;
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {

        IRpcResponder responder = factory.create(delivery);

        try {
            
            log.info("Handling Auth login");
            log.info("Received correlationId '{}': '{}'",
                    delivery.getProperties().getCorrelationId(),
                    new String(delivery.getBody(), StandardCharsets.UTF_8));

            var body = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            var reqJson = objectMapper.readTree(body);
            String action = reqJson.get("action").asText();

            if ("find_user_by_email".equals(action)) {
                String email = reqJson.get("email").asText();
                String service = reqJson.get("service").asText();

                var resp = ar.handleAuthLookup(email, service);

                responder.reply(resp);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            else {
                log.error("Unrecognized action: {}", action);
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        } catch (Exception ex) {
            log.info("RPC handler error: {}", ex.getMessage());
            try {
                responder.replyError();
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            } catch (IOException ex1) {
                log.error("Error while replyError: {}", ex1.getLocalizedMessage());
            }
        }
    }

}
