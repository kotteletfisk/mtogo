package mtogo.redis.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import mtogo.redis.DTO.Order;
import mtogo.redis.DTO.OrderDetailsDTO;

public class Consumer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";

    public static void consumeMessages(String[] bindingKeys) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
        String queueName = channel.queueDeclare().getQueue();

        for (String bindingKey : bindingKeys) {
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
        }

        channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> { });
    }
    private static DeliverCallback deliverCallback(){
        return (consumerTag, delivery) -> {
            if(delivery.getEnvelope().getRoutingKey().equals("customer:order_creation")) {
                objectMapper.registerModule(new JavaTimeModule());

                OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(), OrderDetailsDTO.class);
                Order order = new Order(orderDetailsDTO);

            }

        };
    }
}
