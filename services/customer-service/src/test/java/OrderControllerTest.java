
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import mtogo.customer.DTO.OrderDetailsDTO;
import mtogo.customer.DTO.OrderLineDTO;
import mtogo.customer.DTO.menuItemDTO;
import mtogo.customer.controller.OrderController;
import mtogo.customer.exceptions.APIException;
import mtogo.customer.messaging.Producer;
import mtogo.customer.service.MenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
        List<OrderLineDTO> lines = List.of(
                new OrderLineDTO(
                        10,
                        null,
                        2,
                        500.0f,
                        2
                )
        );

        return new OrderDetailsDTO(
                123,
                OrderDetailsDTO.orderStatus.created,
                lines,
                OrderDetailsDTO.PaymentMethod.PAYPAL
        );
    }

    @Test
    void createOrder_validOrder() throws Exception {
        OrderDetailsDTO dto = buildValidOrder();

        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(dto);

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.json(any())).thenReturn(ctx);

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Mock successful publish
            producerMock.when(() ->
                    Producer.publishMessage(eq("customer:order_creation"), anyString())
            ).thenReturn(true);

            OrderController controller = OrderController.getInstance();
            controller.createOrder(ctx);

            // Verify publish was called
            producerMock.verify(() ->
                    Producer.publishMessage(eq("customer:order_creation"), anyString())
            );

            verify(ctx).status(201);
            verify(ctx).json(any(OrderDetailsDTO.class));
        }
    }

    @Test
    void createOrder_publishFails() throws Exception {
        OrderDetailsDTO dto = buildValidOrder();

        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(dto);
        when(ctx.status(anyInt())).thenReturn(ctx);

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            // Mock failed publish
            producerMock.when(() ->
                    Producer.publishMessage(anyString(), anyString())
            ).thenReturn(false);

            OrderController controller = OrderController.getInstance();
            controller.createOrder(ctx);

            verify(ctx).status(500);
            verify(ctx).result(contains("Failed to"));
        }
    }

    @Test
    void validateOrderDTO() {
        OrderController controller = OrderController.getInstance();
        OrderDetailsDTO expected = buildValidOrder();

        when(ctx.bodyValidator(OrderDetailsDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(expected);

        OrderDetailsDTO actual = controller.validateOrderDTO(ctx);

        assertNotNull(actual);
        assertEquals(expected.getOrderId(), actual.getOrderId());
    }


}
