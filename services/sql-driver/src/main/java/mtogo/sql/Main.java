package mtogo.sql;

import mtogo.sql.messaging.Consumer;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            String[] bindingKeys = {"customer:order_creation"};
            Consumer.consumeMessages(bindingKeys);
            System.out.println("SQL-driver started, listening for order events...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}