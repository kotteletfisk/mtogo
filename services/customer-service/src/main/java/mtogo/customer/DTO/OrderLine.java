package mtogo.customer.DTO;

public class OrderLine {
    private int menuItemId;
    private int price;
    private int amount;


    public OrderLine(int menuItemId, int price, int amount) {
        this.menuItemId = menuItemId;
        this.price = price;
        this.amount = amount;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public int getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }
}
