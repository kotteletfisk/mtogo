package mtogo.redis;


import io.javalin.Javalin;
import mtogo.redis.messaging.Consumer;

public class Main {
    public static void main(String[] args) {

        try {
            String[] bindingKeys = { "customer:order_creation", "customer:supplier_request" };
            Consumer.consumeMessages(bindingKeys);
            System.out.println("redis-driver started, listening for order events...");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}