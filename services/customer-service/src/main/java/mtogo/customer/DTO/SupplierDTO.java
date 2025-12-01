package mtogo.customer.DTO;

public class SupplierDTO {
    private int supplierId;
    private String name;
    private String zipCode;

    public enum status{
        active,
        inactive
    }
    private status supplierStatus;

    public SupplierDTO(int supplierId, String name, String zipCode, status supplierStatus) {
        this.supplierId = supplierId;
        this.name = name;
        this.zipCode = zipCode;
        this.supplierStatus = supplierStatus;
    }

    public SupplierDTO() {}

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public status getSupplierStatus() {
        return supplierStatus;
    }

    public void setSupplierStatus(status supplierStatus) {
        this.supplierStatus = supplierStatus;
    }
}
