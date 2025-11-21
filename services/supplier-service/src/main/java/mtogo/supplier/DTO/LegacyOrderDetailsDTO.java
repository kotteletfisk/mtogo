package mtogo.supplier.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LegacyOrderDetailsDTO {
    private UUID orderId;
    private String customerPhone;

    public enum orderStatus{
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus status;
    private List<OrderLineDTO> orderLineDTOS;

    public LegacyOrderDetailsDTO() {
        orderLineDTOS = new ArrayList<>();
    }

    public LegacyOrderDetailsDTO(String customerPhone, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.customerPhone = customerPhone;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }

    public LegacyOrderDetailsDTO(UUID orderId, String customerPhone, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.customerPhone = customerPhone;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }
    public void setOrderLines(List<OrderLineDTO> orderLines) {
        this.orderLineDTOS = (orderLines != null) ? orderLines : new ArrayList<>();
    }

    public List<OrderLineDTO> getOrderLines() {
        return orderLineDTOS;
    }

    public orderStatus getStatus() {
        return status;
    }

    public String getCustomerId() {
        return customerPhone;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<OrderLineDTO> getOrderLineDTOS() {
        return orderLineDTOS;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setCustomerId(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public void setStatus(orderStatus status) {
        this.status = status;
    }

    public void setOrderLineDTOS(List<OrderLineDTO> orderLineDTOS) {
        this.orderLineDTOS = orderLineDTOS;
    }

}
