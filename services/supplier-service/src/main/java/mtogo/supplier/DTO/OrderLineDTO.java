package mtogo.supplier.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class OrderLineDTO {
    private int orderLineId;
    private UUID orderId;
    private int itemId;
    private float priceSnapshot;
    private int amount;

    // initial creation
    public OrderLineDTO(UUID orderId, int itemId, float priceSnapshot, int amount) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.priceSnapshot = priceSnapshot;
        this.amount = amount;
    }
}
