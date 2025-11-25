package mtogo.payment;
public interface PaymentStrategy {

    // Strategy pattern for different payment methods

    boolean pay(double amount);
}
