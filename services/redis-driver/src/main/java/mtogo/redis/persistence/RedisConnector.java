package mtogo.redis.persistence;

import mtogo.redis.DTO.OrderDTO;
import mtogo.redis.DTO.OrderLineDTO;
import redis.clients.jedis.UnifiedJedis;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class for connecting to Redis and performing operations
 */

public class RedisConnector {

    //Singleton
    private static RedisConnector instance;
    private final UnifiedJedis jedis;

    private RedisConnector() {
        this.jedis = new UnifiedJedis("redis://redis-active-db:6379");
    }
    // Constructor for injecting Jedis instance (for testing)
    public RedisConnector(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    public static synchronized RedisConnector getInstance() {
        if (instance == null) {
            instance = new RedisConnector();
        }
        return instance;
    }

    /**
     * Creates an order in Redis
     * @param order the order to create
     */
    public void createOrder(OrderDTO order) {
        Map<String, String> map = new HashMap<>();
        map.put("order_id", order.getOrder_id().toString());
        map.put("customer_id", String.valueOf(order.getCustomer_id()));
        map.put("order_created", String.valueOf(order.getOrder_created().getTime()));
        map.put("order_updated", String.valueOf(order.getOrder_updated().getTime()));
        map.put("order_status", order.getOrderStatus().name());

        jedis.hset("order:" + order.getOrder_id().toString(), map);
    }
    /**
     * Creates order lines in Redis
     * @param lines the list of order lines to create
     */
    public void createOrderLines(List<OrderLineDTO> lines) {
        for (OrderLineDTO l : lines) {
            Map<String, String> map = new HashMap<>();
            map.put("order_line_id", String.valueOf(l.getOrderLineId()));
            map.put("order_id", l.getOrderId().toString());
            map.put("item_id", String.valueOf(l.getItem_id()));
            map.put("price_snapshot", String.valueOf(l.getPrice_snapshot()));
            map.put("amount", String.valueOf(l.getAmount()));

            jedis.hset("orderline:" + l.getOrderLineId(), map);
        }
    }



}
