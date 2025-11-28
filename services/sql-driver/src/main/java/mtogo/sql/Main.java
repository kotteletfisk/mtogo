package mtogo.sql;

import mtogo.sql.messaging.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        try {
            String[] bindingKeys = {"customer:order_creation", "supplier:order_creation", "customer:menu_request", "auth:login"};
            Consumer.consumeMessages(bindingKeys);
            log.info("SQL-driver started, listening for order events...");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}