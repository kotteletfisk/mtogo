package mtogo.supplier.DTO;

public class OrderLineDTO {
    private int orderLineId;
    private int orderId;
    private int item_id;
    private int price_snapshot;
    private int amount;


    public OrderLineDTO(int orderLineId, int orderId, int item_id, int price_snapshot, int amount) {
        this.orderLineId = orderLineId;
        this.orderId = orderId;
        this.item_id = item_id;
        this.price_snapshot = price_snapshot;
        this.amount = amount;
    }

    public int getOrderLineId() {
        return orderLineId;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getItem_id() {
        return item_id;
    }

    public int getPrice_snapshot() {
        return price_snapshot;
    }

    public int getAmount() {
        return amount;
    }
}
