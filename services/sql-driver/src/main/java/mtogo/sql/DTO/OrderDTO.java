package mtogo.sql.DTO;


import java.sql.Timestamp;
import java.util.UUID;

public class OrderDTO {
    private UUID order_id;
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

    public OrderDTO(UUID order_id, int customer_id) {
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

    public UUID getOrder_id() {
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
