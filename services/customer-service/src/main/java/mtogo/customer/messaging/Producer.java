package mtogo.customer.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import mtogo.customer.exceptions.APIException;



public class Producer {

    // Routing Key
    private static final String EXCHANGE_NAME = "order";
    static ConnectionFactory connectionFactory = createDefaultFactory();

    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitMQ");
        return factory;
    }
    // Used to overwrite connectionfactory (For testing)
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }
    /**
     * publishes a message to RabbitMQ using the specified routing key and waits for RabbitMQ to confirm the message.
     *
     * @param routingKey the RabbitMQ routing key used for the message
     * @param message the message body to publish
     * @return true if the message was successfully confirmed by RabbitMQ
     * @throws APIException if connection fails or if RabbitMQ does not confirm the message.
     *
     */

    public static boolean publishMessage(String routingKey, String message) throws APIException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitMQ");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            channel.confirmSelect();

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            // Maybe log the message that is being sent?

            if(!channel.waitForConfirms(5000)) {
                // Maybe log this error too?
                throw new APIException(500, "Could not process your request due to an internal error.");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(); //Maybe we should log this to keep track of it?
            throw new APIException(500, "Could not process your request due to an internal error, connection failed.");
        }
    }
}