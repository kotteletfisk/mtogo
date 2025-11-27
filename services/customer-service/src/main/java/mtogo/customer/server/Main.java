package mtogo.customer.server;


import mtogo.customer.messaging.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mtogo.customer.messaging.Consumer.log;


public class Main {
    public static void main(String[] args) {
        JavalinBuilder.startServer(7070);

        try {
            String[] bindingKeys = {"customer:menu_response"};
            Consumer.consumeMessages(bindingKeys);
            log.info("SQL-driver started, listening for order events...");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}