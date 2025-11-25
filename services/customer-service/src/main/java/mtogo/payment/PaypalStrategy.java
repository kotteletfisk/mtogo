package mtogo.payment;

public class PaypalStrategy implements  PaymentStrategy{

    @Override
    public boolean pay(double amount) {
        System.out.println("Processing PayPal payment of amount: " + amount);
        if (amount > 0) {
            System.out.println("PayPal payment successful.");
            return true;
        }
        else {
            System.out.println("PayPal payment failed. Invalid amount.");
            return false;
        }
    }
}
