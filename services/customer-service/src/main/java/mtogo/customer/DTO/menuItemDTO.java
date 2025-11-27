package mtogo.customer.DTO;

public class menuItemDTO {
    private int id;
    private String name;
    private double price;
    private int supplerId;
    private boolean isActive;


    public menuItemDTO() {
    }


    public menuItemDTO(int id, String name, double price, int supplerId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.supplerId = supplerId;
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

    public int getSupplerId() {
        return supplerId;
    }

    public void setSupplerId(int supplerId) {
        this.supplerId = supplerId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}