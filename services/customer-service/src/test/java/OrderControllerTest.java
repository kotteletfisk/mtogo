
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.DTO.OrderLine;
import mtogo.customer.controller.OrderController;
import mtogo.customer.exceptions.APIException;
import mtogo.customer.messaging.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    Context ctx;

    @Mock
    BodyValidator<OrderDetailsDTO> bodyValidator;

    private OrderDetailsDTO buildValidOrder() {
        List<OrderLine> lines = List.of(
                new OrderLine(10, 1, 2, 500, 2)
        );
        return new OrderDetailsDTO(
                1,
                123,
                OrderDetailsDTO.orderStatus.Pending,
                lines
        );
    }

    @Test
    void createOrder_validOrder() throws Exception {
        OrderController controller = OrderController.getInstance();
        OrderDetailsDTO orderDetailsDTO = buildValidOrder();
        when(ctx.status(anyInt())).thenReturn(ctx);


        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator); // fluent API
        when(bodyValidator.get()).thenReturn(orderDetailsDTO);

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            producerMock.when(() ->
                    Producer.publishMessage(anyString(), anyString())
            ).thenReturn(true);

            controller.createOrder(ctx);

            // message published with expected routing key + DTO string
            producerMock.verify(() ->
                    Producer.publishMessage("customer:order_creation", orderDetailsDTO.toString())
            );

            verify(ctx).status(201);
            verify(ctx).json(orderDetailsDTO);
        }
    }

    @Test
    void createOrder_fail() throws Exception {
        OrderController controller = OrderController.getInstance();
        OrderDetailsDTO orderDetailsDTO = buildValidOrder();
        when(ctx.status(anyInt())).thenReturn(ctx);


        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(orderDetailsDTO);

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Simulate RabbitMQ failure
            producerMock.when(() ->
                    Producer.publishMessage(anyString(), anyString())
            ).thenThrow(new APIException(500, "RabbitMQ failure"));

            controller.createOrder(ctx);

            verify(ctx).status(500);
            verify(ctx).result("Failed to publish order creation message");
        }
    }

    @Test
    void validateOrderDTO() {
        OrderController controller = OrderController.getInstance();
        OrderDetailsDTO expected = new OrderDetailsDTO(1, 123, OrderDetailsDTO.orderStatus.Pending,
                List.of(new OrderLine(10, 1, 10, 500, 1)));

        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(expected);

        OrderDetailsDTO actual = controller.validateOrderDTO(ctx);

        assertNotNull(actual);
        assertEquals(expected.getOrderId(), actual.getOrderId());
    }

}
