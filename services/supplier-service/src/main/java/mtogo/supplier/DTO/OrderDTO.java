package mtogo.supplier.DTO;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
public class OrderDTO {
    private UUID orderId;
    private int customerId;
    private int supplierId;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp orderCreated;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp orderUpdated;
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

    public OrderDTO(UUID orderId, int customerId, int supplierId, List<OrderLineDTO> orderlineDTOs) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderCreated = new Timestamp(System.currentTimeMillis());
        this.orderUpdated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus.created;
        this.supplierId = supplierId;
        this.orderlineDTOs = orderlineDTOs;
    }    
    public OrderDTO(UUID orderId, int customerId, int supplierId, orderStatus orderStatus, List<OrderLineDTO> orderlineDTOs) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderCreated = new Timestamp(System.currentTimeMillis());
        this.orderUpdated = new Timestamp(System.currentTimeMillis());
        this.orderStatus = orderStatus;
        this.supplierId = supplierId;
        this.orderlineDTOs = orderlineDTOs;
    }
}
