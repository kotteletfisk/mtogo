/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.out;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import mtogo.sql.ports.out.IRpcResponder;

/**
 *
 * @author kotteletfisk
 */
public class RabbitMQRpcResponder implements IRpcResponder {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper mapper;
    private final Delivery delivery;
    private final Connection connection;

    public RabbitMQRpcResponder(ObjectMapper mapper, Delivery delivery, Connection connection) {
        this.mapper = mapper;
        this.delivery = delivery;
        this.connection = connection;
    }

    @Override
    public void reply(Object response) throws JsonProcessingException, IOException {

        log.info("Replying to message");
        String respStr = mapper.writeValueAsString(response);

        log.debug("ReplyQueue: {}", delivery.getProperties().getReplyTo());
        log.debug("CorrelationId: {}", delivery.getProperties().getCorrelationId());
        log.debug("Replying with payload: {}", respStr);

        var props = new AMQP.BasicProperties.Builder()
                .correlationId(delivery.getProperties().getCorrelationId())
                .contentType("application/json")
                .build();

        try (Channel channel = connection.createChannel()) {

            channel.basicPublish(
                    "order",
                    delivery.getProperties().getReplyTo(),
                    props,
                    respStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        } catch (TimeoutException e) {
            log.error("Timeout on opening reply channel: {}", e.getLocalizedMessage());
            throw new IOException(e);
        }
    }

    @Override
    public void replyError() throws IOException {

        log.info("Replying with error");
        var props = new AMQP.BasicProperties.Builder()
                .correlationId(delivery.getProperties().getCorrelationId())
                .contentType("application/json")
                .build();
        try (Channel channel = connection.createChannel()) {

            channel.basicPublish(
                    "",
                    delivery.getProperties().getReplyTo(),
                    props,
                    "{\"status\":\"error\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (TimeoutException e) {
            log.error("Timeout on opening reply channel: {}", e.getLocalizedMessage());
            throw new IOException(e);
        }
    }
}
