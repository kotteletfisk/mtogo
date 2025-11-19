package mtogo.sql.DTO;

public class OrderLineDTO {
    private int orderLineId;
    private int orderId;
    private int item_id;
    private float price_snapshot;
    private int amount;

    public OrderLineDTO() {
    }

    public OrderLineDTO(int orderLineId, int orderId, int item_id, float price_snapshot, int amount) {
        this.orderLineId = orderLineId;
        this.orderId = orderId;
        this.item_id = item_id;
        this.price_snapshot = price_snapshot;
        this.amount = amount;
    }

    public int getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(int orderLineId) {
        this.orderLineId = orderLineId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getItem_id() {
        return item_id;
    }

    public void setItem_id(int item_id) {
        this.item_id = item_id;
    }

    public float getPrice_snapshot() {
        return price_snapshot;
    }

    public void setPrice_snapshot(float price_snapshot) {
        this.price_snapshot = price_snapshot;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
