package mtogo.redis.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import mtogo.redis.DTO.Order;
import mtogo.redis.DTO.OrderDetailsDTO;
import mtogo.redis.DTO.OrderLine;
import mtogo.redis.persistence.RedisConnector;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";

    static ConnectionFactory connectionFactory = createDefaultFactory();


    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        return factory;
    }
    // Injectable connectionfactory for testing
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * Consumes messages from RabbitMQ based on the provided binding keys.
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public static void consumeMessages(String[] bindingKeys) throws Exception{
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
        String queueName = channel.queueDeclare().getQueue();

        for (String bindingKey : bindingKeys) {
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
        }

        channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> { });
    }

    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks functionality can vary on keyword
     * @return the DeliverCallback function
     */
    private static DeliverCallback deliverCallback(){
        return (consumerTag, delivery) -> {
            if(delivery.getEnvelope().getRoutingKey().equals("customer:order_creation")) {
                objectMapper.registerModule(new JavaTimeModule());

                OrderDetailsDTO orderDetailsDTO = objectMapper.readValue(delivery.getBody(), OrderDetailsDTO.class);
                Order order = new Order(orderDetailsDTO);
                List<OrderLine> orderLines = new ArrayList<>();
                for (OrderLine line : orderDetailsDTO.getOrderLines()) {
                    orderLines.add(new OrderLine(line.getOrderLineId(), line.getOrderId(), line.getItem_id(), line.getPrice_snapshot(), line.getAmount()));
                }
                System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + orderDetailsDTO + "'");
                // Persist order and order lines to Redis
                RedisConnector redisConnector = RedisConnector.getInstance();
                redisConnector.createOrder(order);
                redisConnector.createOrderLines(orderLines);

            }

        };
    }
}
