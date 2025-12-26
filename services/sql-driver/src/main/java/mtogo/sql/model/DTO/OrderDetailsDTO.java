package mtogo.sql.model.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDetailsDTO {

    private UUID orderId;
    private int customerId;
    private int supplierId;

    public enum PaymentMethod {
        PAYPAL,
        MOBILEPAY,
        LEGACY
    }

    public enum orderStatus {
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus status;
    @Builder.Default
    private List<OrderLineDTO> orderLineDTOS = new ArrayList<>();
    private PaymentMethod paymentMethod;


    public OrderDetailsDTO(int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }

    public OrderDetailsDTO(int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS, PaymentMethod paymentMethod) {
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
        this.paymentMethod = paymentMethod;
    }

    public OrderDetailsDTO(UUID orderId, int customerId, orderStatus status, int supplierId, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.supplierId = supplierId;
        this.orderLineDTOS = orderLineDTOS;
    }
}
