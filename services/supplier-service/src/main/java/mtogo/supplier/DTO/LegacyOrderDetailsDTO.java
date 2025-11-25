package mtogo.supplier.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class LegacyOrderDetailsDTO {
    private UUID orderId;
    private String customerPhone;

    private List<OrderLineDTO> orderLineDTOS = new ArrayList<>();
    
    public LegacyOrderDetailsDTO(UUID orderId, String customerPhone, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.customerPhone = customerPhone;
        this.orderLineDTOS = orderLineDTOS;
    }
}
