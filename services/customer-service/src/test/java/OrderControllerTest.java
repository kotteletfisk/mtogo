
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import mtogo.customer.DTO.OrderDTO;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    Context ctx;

    @Mock
    BodyValidator<OrderDTO> bodyValidator;

    private OrderDTO buildValidOrder() {
        List<OrderLine> lines = List.of(
                new OrderLine(10, 100, 2)
        );
        return new OrderDTO(
                1,
                123,
                OrderDTO.orderStatus.Pending,
                lines
        );
    }

    @Test
    void createOrder_validOrder_publishesMessage_andReturns201() throws Exception {
        OrderController controller = OrderController.getInstance();
        OrderDTO orderDTO = buildValidOrder();
        when(ctx.status(anyInt())).thenReturn(ctx);


        when(ctx.bodyValidator(OrderDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator); // fluent API
        when(bodyValidator.get()).thenReturn(orderDTO);

        try (MockedStatic<Producer> producerMock = mockStatic(Producer.class)) {
            producerMock.when(() ->
                    Producer.publishMessage(anyString(), anyString())
            ).thenReturn(true);

            controller.createOrder(ctx);

            // message published with expected routing key + DTO string
            producerMock.verify(() ->
                    Producer.publishMessage("customer:order_creation", orderDTO.toString())
            );

            verify(ctx).status(201);
            verify(ctx).json(orderDTO);
        }
    }

    @Test
    void createOrder_producerThrows_returns500() throws Exception {
        OrderController controller = OrderController.getInstance();
        OrderDTO orderDTO = buildValidOrder();
        when(ctx.status(anyInt())).thenReturn(ctx);


        when(ctx.bodyValidator(OrderDTO.class)).thenReturn(bodyValidator);
        when(bodyValidator.check(any(), anyString())).thenReturn(bodyValidator);
        when(bodyValidator.get()).thenReturn(orderDTO);

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
}
