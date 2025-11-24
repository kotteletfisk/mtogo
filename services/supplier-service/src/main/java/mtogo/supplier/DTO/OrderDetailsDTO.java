package mtogo.supplier.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderDetailsDTO {
    private UUID orderId;
    private int customerId;
    private String customerPhone;

    public enum orderStatus{
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus status;
    private List<OrderLineDTO> orderLineDTOS = new ArrayList<>();
    
    public OrderDetailsDTO(UUID orderId, String customerPhone, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.status = status;
        this.customerPhone = customerPhone;
        this.orderLineDTOS = orderLineDTOS;
    }
}
