import mtogo.redis.DTO.OrderDTO;
import mtogo.redis.DTO.OrderLineDTO;
import mtogo.redis.persistence.RedisConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.UnifiedJedis;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisTest {

    @Mock
    UnifiedJedis jedis;

    @Mock
    OrderDTO orderDTO;

    @Mock
    OrderLineDTO orderLineDTO1;

    @Mock
    OrderLineDTO orderLineDTO2;

    @Test
    public void createOrder() {
        RedisConnector redisConnector = new RedisConnector(jedis);

        UUID orderId = UUID.randomUUID();

        when(orderDTO.getOrder_id()).thenReturn(orderId);
        when(orderDTO.getCustomer_id()).thenReturn(123);

        Timestamp created = new Timestamp(System.currentTimeMillis());
        Timestamp updated = new Timestamp(System.currentTimeMillis());
        when(orderDTO.getOrder_created()).thenReturn(created);
        when(orderDTO.getOrder_updated()).thenReturn(updated);
        when(orderDTO.getOrderStatus()).thenReturn(OrderDTO.orderStatus.created);
        when(orderDTO.getSupplierId()).thenReturn(1);

        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        redisConnector.createOrder(orderDTO);

        verify(jedis).hset(eq("order:" + orderId.toString()), mapCaptor.capture());
        Map<String, String> map = mapCaptor.getValue();

        assertEquals(orderId.toString(), map.get("order_id"));
        assertEquals("123", map.get("customer_id"));
        assertEquals(String.valueOf(created.getTime()), map.get("order_created"));
        assertEquals(String.valueOf(updated.getTime()), map.get("order_updated"));
        assertEquals("created", map.get("order_status"));
        assertEquals("1", map.get("supplier_id"));
    }

    @Test
    void createOrderLines_sendsEachOrderLineAsHash() {
        RedisConnector connector = new RedisConnector(jedis);

        UUID orderId = UUID.randomUUID();

        when(orderLineDTO1.getOrderLineId()).thenReturn(1);
        when(orderLineDTO1.getOrderId()).thenReturn(orderId);
        when(orderLineDTO1.getItemId()).thenReturn(100);
        when(orderLineDTO1.getPriceSnapshot()).thenReturn(50F);
        when(orderLineDTO1.getAmount()).thenReturn(2);

        when(orderLineDTO2.getOrderLineId()).thenReturn(2);
        when(orderLineDTO2.getOrderId()).thenReturn(orderId);
        when(orderLineDTO2.getItemId()).thenReturn(101);
        when(orderLineDTO2.getPriceSnapshot()).thenReturn(75F);
        when(orderLineDTO2.getAmount()).thenReturn(1);

        List<OrderLineDTO> orderLineDTOS = List.of(orderLineDTO1, orderLineDTO2);

        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        connector.createOrderLines(orderLineDTOS);

        verify(jedis, times(2)).hset(anyString(), anyMap());

        verify(jedis).hset(eq("orderline:1"), mapCaptor.capture());
        Map<String, String> first = mapCaptor.getValue();
        assertEquals("1", first.get("order_line_id"));
        assertEquals(orderId.toString(), first.get("order_id"));
        assertEquals("100", first.get("item_id"));
        assertEquals("50.0", first.get("price_snapshot"));
        assertEquals("2", first.get("amount"));

        mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jedis).hset(eq("orderline:2"), mapCaptor.capture());
        Map<String, String> second = mapCaptor.getValue();
        assertEquals("2", second.get("order_line_id"));
        assertEquals(orderId.toString(), second.get("order_id"));
        assertEquals("101", second.get("item_id"));
        assertEquals("75.0", second.get("price_snapshot"));
        assertEquals("1", second.get("amount"));
    }

    @Test
    void saveOrderAndOrderLines_savesAllDataToRedis() {

        RedisConnector connector = new RedisConnector(jedis);

        UUID orderId = UUID.randomUUID();

        OrderDTO orderDTO = mock(OrderDTO.class);
        when(orderDTO.getOrder_id()).thenReturn(orderId);
        when(orderDTO.getCustomer_id()).thenReturn(50);
        when(orderDTO.getSupplierId()).thenReturn(1);

        Timestamp created = new Timestamp(1_700_000_000_000L);
        Timestamp updated = new Timestamp(1_700_000_500_000L);

        when(orderDTO.getOrder_created()).thenReturn(created);
        when(orderDTO.getOrder_updated()).thenReturn(updated);
        when(orderDTO.getOrderStatus()).thenReturn(OrderDTO.orderStatus.created);

        OrderLineDTO line1 = mock(OrderLineDTO.class);
        when(line1.getOrderLineId()).thenReturn(1);
        when(line1.getOrderId()).thenReturn(orderId);
        when(line1.getItemId()).thenReturn(200);
        when(line1.getPriceSnapshot()).thenReturn(99F);
        when(line1.getAmount()).thenReturn(2);

        OrderLineDTO line2 = mock(OrderLineDTO.class);
        when(line2.getOrderLineId()).thenReturn(2);
        when(line2.getOrderId()).thenReturn(orderId);
        when(line2.getItemId()).thenReturn(300);
        when(line2.getPriceSnapshot()).thenReturn(149F);
        when(line2.getAmount()).thenReturn(1);

        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        connector.createOrder(orderDTO);
        connector.createOrderLines(List.of(line1, line2));

        // OrderDTO hash
        verify(jedis).hset(eq("order:" + orderId.toString()), mapCaptor.capture());
        Map<String, String> orderMap = mapCaptor.getValue();

        assertEquals(orderId.toString(), orderMap.get("order_id"));
        assertEquals("50", orderMap.get("customer_id"));
        assertEquals(String.valueOf(created.getTime()), orderMap.get("order_created"));
        assertEquals(String.valueOf(updated.getTime()), orderMap.get("order_updated"));
        assertEquals("created", orderMap.get("order_status"));
        assertEquals("1", orderMap.get("supplier_id"));

        // Orderline 1
        verify(jedis).hset(eq("orderline:1"), mapCaptor.capture());
        Map<String, String> l1 = mapCaptor.getValue();
        assertEquals("1", l1.get("order_line_id"));
        assertEquals(orderId.toString(), l1.get("order_id"));
        assertEquals("200", l1.get("item_id"));
        assertEquals("99.0", l1.get("price_snapshot"));
        assertEquals("2", l1.get("amount"));

        // Orderline 2
        verify(jedis).hset(eq("orderline:2"), mapCaptor.capture());
        Map<String, String> l2 = mapCaptor.getValue();
        assertEquals("2", l2.get("order_line_id"));
        assertEquals(orderId.toString(), l2.get("order_id"));
        assertEquals("300", l2.get("item_id"));
        assertEquals("149.0", l2.get("price_snapshot"));
        assertEquals("1", l2.get("amount"));

        verifyNoMoreInteractions(jedis);
    }
}
