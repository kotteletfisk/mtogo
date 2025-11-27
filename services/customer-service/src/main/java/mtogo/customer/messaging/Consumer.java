package mtogo.customer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.DTO.OrderLineDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumes messages from RabbitMQ
 */
public class Consumer {

    public static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EXCHANGE_NAME = "order";
    private static StringWriter sw = new StringWriter();
    private static PrintWriter pw = new PrintWriter(sw);


    static ConnectionFactory connectionFactory = createDefaultFactory();

    private static ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setPort(5672);
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASS"));
        return factory;
    }

    // Injectable connectionfactory for testing
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * Consumes messages from RabbitMQ based on the provided binding keys.
     *
     * @param bindingKeys the routing keys to bind the queue to
     * @throws Exception if an error occurs while consuming messages
     */
    public static void consumeMessages(String[] bindingKeys) throws Exception {

        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            String queueName = channel.queueDeclare().getQueue();

            for (String bindingKey : bindingKeys) {
                channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            }

            channel.basicConsume(queueName, true, deliverCallback(), consumerTag -> {
            });
        } catch (Exception e) {
            log.error("Error consuming message:\n" + e.getMessage());
            e.printStackTrace(pw);
            log.error("Stacktrace:\n" + sw.toString());
        }

    }

    /**
     * Creates a DeliverCallback to handle incoming messages. The callbacks
     * functionality can vary on keyword
     *
     * @return the DeliverCallback function
     */
    private static DeliverCallback deliverCallback() {
        return (consumerTag, delivery) -> {
            String routingKey = delivery.getEnvelope().getRoutingKey();

            switch (routingKey) {


                case "customer:menu_response"->{
                    try {
                        String body = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);

                        String[] parts = body.split(";", 2);
                        int supplierId = Integer.parseInt(parts[parts.length-1]);

                        List<menuItemDTO> menuItems = objectMapper.readValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, menuItemDTO.class));

                        log.info("Received {} menu items for supplier ID: {}", menuItems.size(), supplierId);

                        MenuService.getInstance().completeMenuRequest(supplierId, menuItems);



                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            }
        };
    }

}
