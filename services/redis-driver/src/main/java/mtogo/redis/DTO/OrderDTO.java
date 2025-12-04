package mtogo.redis.DTO;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class OrderDTO {
    private UUID order_id;
    private int customer_id;
    private int supplierId;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp order_created;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp order_updated;
    private List<OrderLineDTO> orderlineDTOs;
    public enum orderStatus{
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus orderStatus;

    public OrderDTO(UUID order_id, int customer_id, int supplierId, List<OrderLineDTO> orderlineDTOs) {
        this.order_id = order_id;
        this.customer_id = customer_id;
        this.order_created = new Timestamp(System.currentTimeMillis());
        this.order_updated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus.created;
        this.supplierId = supplierId;
        this.orderlineDTOs = orderlineDTOs;
    }
    // Used for consumer when order is created
    public OrderDTO(OrderDetailsDTO orderDetailsDTO) {
        this.order_id = orderDetailsDTO.getOrderId();
        this.customer_id = orderDetailsDTO.getCustomerId();
        this.order_created = new Timestamp(System.currentTimeMillis());
        this.order_updated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus.created;
        this.supplierId = orderDetailsDTO.getSupplierId();
        this.orderlineDTOs = orderDetailsDTO.getOrderLineDTOS();
    }
}
