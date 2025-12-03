package mtogo.sql.DTO;


import java.sql.Timestamp;
import java.util.UUID;

import lombok.Getter;

@Getter
public class OrderDTO {
    private final UUID order_id;
    private final int customer_id;
    private final Timestamp order_created;
    private final Timestamp order_updated;
    private int supplierId;

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
        this.supplierId = orderDetailsDTO.getSupplierId();
    }
}
