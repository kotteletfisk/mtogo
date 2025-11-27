package mtogo.sql.DTO;

public class menuItemDTO {
    private int id;
    private String name;
    private double price;
    private int supplierId;
    private boolean isActive;


    public menuItemDTO() {
    }

    public menuItemDTO(int id, String name, double price, int supplierId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.supplierId = supplierId;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}