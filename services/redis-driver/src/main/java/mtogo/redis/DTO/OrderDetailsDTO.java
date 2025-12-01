package mtogo.redis.DTO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.ToString;

@ToString
public class OrderDetailsDTO {
    private UUID orderId;
    private int customerId;

    public enum PaymentMethod {
        PAYPAL,
        MOBILEPAY,
        LEGACY
    }

    public enum orderStatus{
        created,
        rejected,
        accepted,
        waiting,
        delivering,
        delivered
    }
    private orderStatus status;
    private List<OrderLineDTO> orderLineDTOS = new ArrayList<>();
    private PaymentMethod paymentMethod;

    public OrderDetailsDTO() {}

    public OrderDetailsDTO(int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }

    public OrderDetailsDTO(int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS, PaymentMethod paymentMethod) {
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
        this.paymentMethod = paymentMethod;
    }


    public OrderDetailsDTO(UUID orderId, int customerId, orderStatus status, List<OrderLineDTO> orderLineDTOS) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.orderLineDTOS = orderLineDTOS;
    }
    public void setOrderLines(List<OrderLineDTO> orderLines) {
        this.orderLineDTOS = (orderLines != null) ? orderLines : new ArrayList<>();
    }
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public UUID getOrderId() {
        return orderId;
    }

    public List<OrderLineDTO> getOrderLineDTOS() {
        return orderLineDTOS;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setStatus(orderStatus status) {
        this.status = status;
    }

    public void setOrderLineDTOS(List<OrderLineDTO> orderLineDTOS) {
        this.orderLineDTOS = orderLineDTOS;
    }
}
