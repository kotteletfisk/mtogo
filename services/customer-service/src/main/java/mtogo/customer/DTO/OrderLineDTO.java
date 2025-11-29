package mtogo.customer.DTO;

import java.util.UUID;

public class OrderLineDTO {
    private int orderLineId;
    private UUID orderId;
    private int itemId;
    private float priceSnapshot;
    private int amount;

    public OrderLineDTO() {
    }

    public OrderLineDTO(int orderLineId, UUID orderId, int itemId, float priceSnapshot, int amount) {
        this.orderLineId = orderLineId;
        this.orderId = orderId;
        this.itemId = itemId;
        this.priceSnapshot = priceSnapshot;
        this.amount = amount;
    }

    public int getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(int orderLineId) {
        this.orderLineId = orderLineId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public float getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(float price_snapshot) {
        this.priceSnapshot = price_snapshot;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
