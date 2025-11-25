package mtogo.payment;

public class PaymentService {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }
    public boolean pay(double amount) {
        if (paymentStrategy != null) {
            return paymentStrategy.pay(amount);
        }
        else {
            System.out.println("Payment strategy not set.");
            return false;
        }
    }
}
