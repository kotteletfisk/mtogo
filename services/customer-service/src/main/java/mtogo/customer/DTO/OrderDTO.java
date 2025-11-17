package mtogo.customer.DTO;

import java.util.List;

public class OrderDTO {
    private int orderId;
    private int customerId;
    public enum orderStatus{
        Pending,
        Accepted,
        PickedUp,
        Completed
    }
    private orderStatus status;
    private List<OrderLine>  orderLines;

    public OrderDTO() {}

    public OrderDTO(int orderId, int customerId, orderStatus status, List<OrderLine> orderLines) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.orderLines = orderLines;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public orderStatus getStatus() {
        return status;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getOrderId() {
        return orderId;
    }


}
