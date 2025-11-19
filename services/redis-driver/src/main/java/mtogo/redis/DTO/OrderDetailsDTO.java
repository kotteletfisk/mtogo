package mtogo.redis.DTO;

import java.util.List;

public class OrderDetailsDTO {
    private int orderId;
    private int customerId;

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

    public OrderDetailsDTO() {}

    public OrderDetailsDTO(int orderId, int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }

    public List<OrderLineDTO> getOrderLines() {
        return orderLineDTOS;
    }

    public orderStatus getStatus() {
        return status;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getOrderId() {
        return orderId;
    }


}
