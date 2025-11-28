package mtogo.auth.messaging;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

public class RabbitRpcClient implements AutoCloseable {
    private final Connection connection;
    private final Channel channel;
    private final String replyQueueName;

    public RabbitRpcClient(ConnectionFactory factory) throws IOException, TimeoutException {
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.replyQueueName = channel.queueDeclare("", false, true, true, null).getQueue();
    }

    public String rpcCall(String requestJson, String routingKey) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .contentType("application/json")
                .build();

        final ArrayBlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId() != null &&
                    delivery.getProperties().getCorrelationId().equals(corrId)) {
                String resp = new String(delivery.getBody(), StandardCharsets.UTF_8);
                response.offer(resp);
            }
        }, consumerTag -> {});

        channel.basicPublish("", routingKey, props, requestJson.getBytes(StandardCharsets.UTF_8));

        String result = response.poll(5, java.util.concurrent.TimeUnit.SECONDS); // timeout
        channel.basicCancel(ctag);
        if (result == null) throw new IOException("RPC timeout waiting for reply from " + routingKey);
        return result;
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }
}
