/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import mtogo.sql.messaging.AuthReceiver;

/**
 *
 * @author kotteletfisk
 */
public class AuthLoginHandler implements IMessageHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;
    private final AuthReceiver ar;

    public AuthLoginHandler(AuthReceiver ar, ObjectMapper objectMapper) {
        this.ar = ar;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(Delivery delivery, Channel channel) {
        try {
            log.info(" [x] Received correlationId '{}': '{}'",
                    delivery.getProperties().getCorrelationId(),
                    new String(delivery.getBody(), StandardCharsets.UTF_8));
            var body = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            var reqJson = objectMapper.readTree(body);
            String action = reqJson.get("action").asText();

            if ("find_user_by_email".equals(action)) {
                String email = reqJson.get("email").asText();
                String resp = ar.handleAuthLookup(email);
                var props = new AMQP.BasicProperties.Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .contentType("application/json")
                        .build();

                channel.basicPublish(
                        "",
                        delivery.getProperties().getReplyTo(),
                        props,
                        resp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        } catch (Exception ex) {
            log.info("RPC handler error: {}", ex.getMessage());
            try {
                var props = new AMQP.BasicProperties.Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .contentType("application/json")
                        .build();
                channel.basicPublish(
                        "",
                        delivery.getProperties().getReplyTo(),
                        props,
                        "{\"status\":\"error\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                log.warn("IGNORED: {}", ignored.getMessage());
            }
            try {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

}
