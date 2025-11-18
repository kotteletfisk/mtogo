package mtogo.redis;


import io.javalin.Javalin;
import mtogo.redis.messaging.Consumer;

public class Main {
    public static void main(String[] args) {

        try {
            // Which routing keys should this service listen to?
            String[] bindingKeys = { "customer:order_creation" };

            // This sets up the RabbitMQ connection, declares the queue,
            // binds it, and starts listening asynchronously.
            Consumer.consumeMessages(bindingKeys);

            // Nothing else needed here: RabbitMQ's consumer threads keep the JVM alive.
            // If you want logging:
            System.out.println("redis-driver started, listening for order events...");
        } catch (Exception e) {
            e.printStackTrace();
            // In a real app: log and maybe exit with non-zero code.
        }

    }

}