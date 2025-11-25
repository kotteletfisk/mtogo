package mtogo.payment;

public class MobilePayStrategy implements PaymentStrategy {


    @Override
    public boolean pay(double amount) {
        System.out.println("Processing MobilePay payment of amount: " + amount);
        if (amount > 0) {
            System.out.println("MobilePay payment successful.");
            return true;
        }
        else {
            System.out.println("MobilePay payment failed. Invalid amount.");
            return false;
        }
    }
}
