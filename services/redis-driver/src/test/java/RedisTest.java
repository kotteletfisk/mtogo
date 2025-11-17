import mtogo.redis.DTO.Order;
import mtogo.redis.DTO.OrderLine;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisTest {


    @Mock
    UnifiedJedis jedis;

    @Mock
    Order order;

    @Mock
    OrderLine orderLine1;

    @Mock
    OrderLine orderLine2;


    @Test
    public void createOrder(){
        RedisConnector redisConnector = new RedisConnector(jedis);

        when(order.getOrder_id()).thenReturn(1);
        when(order.getCustomer_id()).thenReturn(123);
        Timestamp created = new Timestamp(System.currentTimeMillis());
        Timestamp updated = new Timestamp(System.currentTimeMillis());
        when(order.getOrder_created()).thenReturn(created);
        when(order.getOrder_updated()).thenReturn(updated);
        when(order.getOrderStatus()).thenReturn(Order.OrderStatus.Pending);

        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        redisConnector.createOrder(order);

        verify(jedis).hset(eq("order:1"), mapCaptor.capture());
        Map<String, String> map = mapCaptor.getValue();

        assertEquals("1", map.get("order_id"));
        assertEquals("123", map.get("customer_id"));
        assertEquals(String.valueOf(created.getTime()), map.get("order_created"));
        assertEquals(String.valueOf(updated.getTime()), map.get("order_updated"));
        assertEquals("Pending", map.get("order_status"));


    }
    @Test
    void createOrderLines_sendsEachOrderLineAsHash() {
        RedisConnector connector = new RedisConnector(jedis);

        when(orderLine1.getOrderLineId()).thenReturn(1);
        when(orderLine1.getOrderId()).thenReturn(42);
        when(orderLine1.getItem_id()).thenReturn(100);
        when(orderLine1.getPrice_snapshot()).thenReturn(50);
        when(orderLine1.getAmount()).thenReturn(2);

        when(orderLine2.getOrderLineId()).thenReturn(2);
        when(orderLine2.getOrderId()).thenReturn(42);
        when(orderLine2.getItem_id()).thenReturn(101);
        when(orderLine2.getPrice_snapshot()).thenReturn(75);
        when(orderLine2.getAmount()).thenReturn(1);

        List<OrderLine> orderLines = List.of(orderLine1, orderLine2);

        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        connector.createOrderLines(orderLines);

        verify(jedis, times(2)).hset(anyString(), anyMap()); // called twice total

        verify(jedis).hset(eq("orderline:1"), mapCaptor.capture());
        Map<String, String> first = mapCaptor.getValue();
        assertEquals("1", first.get("order_line_id"));
        assertEquals("42", first.get("order_id"));
        assertEquals("100", first.get("item_id"));
        assertEquals("50", first.get("price_snapshot"));
        assertEquals("2", first.get("amount"));

        mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jedis).hset(eq("orderline:2"), mapCaptor.capture());
        Map<String, String> second = mapCaptor.getValue();
        assertEquals("2", second.get("order_line_id"));
        assertEquals("42", second.get("order_id"));
        assertEquals("101", second.get("item_id"));
        assertEquals("75", second.get("price_snapshot"));
        assertEquals("1", second.get("amount"));
    }
    @Test
    void saveOrderAndOrderLines_savesAllDataToRedis() {

        RedisConnector connector = new RedisConnector(jedis);

        // Order
        Order order = mock(Order.class);
        when(order.getOrder_id()).thenReturn(100);
        when(order.getCustomer_id()).thenReturn(50);

        Timestamp created = new Timestamp(1_700_000_000_000L);
        Timestamp updated = new Timestamp(1_700_000_500_000L);

        when(order.getOrder_created()).thenReturn(created);
        when(order.getOrder_updated()).thenReturn(updated);
        when(order.getOrderStatus()).thenReturn(Order.OrderStatus.Pending);

        // Orderlines
        OrderLine line1 = mock(OrderLine.class);
        when(line1.getOrderLineId()).thenReturn(1);
        when(line1.getOrderId()).thenReturn(100);
        when(line1.getItem_id()).thenReturn(200);
        when(line1.getPrice_snapshot()).thenReturn(99);
        when(line1.getAmount()).thenReturn(2);

        OrderLine line2 = mock(OrderLine.class);
        when(line2.getOrderLineId()).thenReturn(2);
        when(line2.getOrderId()).thenReturn(100);
        when(line2.getItem_id()).thenReturn(300);
        when(line2.getPrice_snapshot()).thenReturn(149);
        when(line2.getAmount()).thenReturn(1);

        ArgumentCaptor<Map<String,String>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        connector.createOrder(order);
        connector.createOrderLines(List.of(line1, line2));


        // Order
        verify(jedis).hset(eq("order:100"), mapCaptor.capture());
        Map<String, String> orderMap = mapCaptor.getValue();

        assertEquals("100", orderMap.get("order_id"));
        assertEquals("50", orderMap.get("customer_id"));
        assertEquals(String.valueOf(created.getTime()), orderMap.get("order_created"));
        assertEquals(String.valueOf(updated.getTime()), orderMap.get("order_updated"));
        assertEquals("Pending", orderMap.get("order_status"));

        // Orderlines 1
        verify(jedis).hset(eq("orderline:1"), mapCaptor.capture());
        Map<String,String> l1 = mapCaptor.getValue();
        assertEquals("1", l1.get("order_line_id"));
        assertEquals("100", l1.get("order_id"));
        assertEquals("200", l1.get("item_id"));
        assertEquals("99", l1.get("price_snapshot"));
        assertEquals("2", l1.get("amount"));

        // Orderlines 2
        verify(jedis).hset(eq("orderline:2"), mapCaptor.capture());
        Map<String,String> l2 = mapCaptor.getValue();
        assertEquals("2", l2.get("order_line_id"));
        assertEquals("100", l2.get("order_id"));
        assertEquals("300", l2.get("item_id"));
        assertEquals("149", l2.get("price_snapshot"));
        assertEquals("1", l2.get("amount"));

        verifyNoMoreInteractions(jedis);
    }
}
