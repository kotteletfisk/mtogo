package mtogo.redis.DTO;

import java.sql.Timestamp;

public class OrderDTO {
    private int order_id;
    private int customer_id;
    private Timestamp order_created;
    private Timestamp order_updated;
    public enum orderStatus{
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus orderStatus;

    public OrderDTO(int order_id, int customer_id) {
        this.order_id = order_id;
        this.customer_id = customer_id;
        this.order_created = new Timestamp(System.currentTimeMillis());
        this.order_updated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus.created;
    }
    // Used for consumer when order is created
    public OrderDTO(OrderDetailsDTO orderDetailsDTO) {
        this.order_id = orderDetailsDTO.getOrderId();
        this.customer_id = orderDetailsDTO.getCustomerId();
        this.order_created = new Timestamp(System.currentTimeMillis());
        this.order_updated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus.created;
    }

    public int getOrder_id() {
        return order_id;
    }

    public int getCustomer_id() {
        return customer_id;
    }

    public Timestamp getOrder_created() {
        return order_created;
    }

    public Timestamp getOrder_updated() {
        return order_updated;
    }

    public orderStatus getOrderStatus() {
        return orderStatus;
    }
}
