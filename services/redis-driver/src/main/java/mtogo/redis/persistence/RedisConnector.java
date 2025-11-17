package mtogo.redis.persistence;

import mtogo.redis.DTO.Order;
import mtogo.redis.DTO.OrderLine;
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
        this.jedis = new UnifiedJedis("redis://localhost:6379");
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
    public void createOrder(Order order){
        try {
            String key = "order:" + order.getOrder_id();
            Map<String, String> orderMap = new HashMap<>();
            orderMap.put("order_id", String.valueOf(order.getOrder_id()));
            orderMap.put("customer_id", String.valueOf(order.getCustomer_id()));
            orderMap.put("order_created", String.valueOf(order.getOrder_created().getTime()));
            orderMap.put("order_updated", String.valueOf(order.getOrder_updated().getTime()));
            orderMap.put("order_status", order.getOrderStatus().name());
            jedis.hset(key, orderMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Creates order lines in Redis
     * @param orderLines the list of order lines to create
     */
    public void createOrderLines(List<OrderLine> orderLines){
        try {
            for (OrderLine orderLine : orderLines) {
                String key = "orderline:" + orderLine.getOrderLineId();
                Map<String, String> orderLineMap = new HashMap<>();
                orderLineMap.put("order_line_id", String.valueOf(orderLine.getOrderLineId()));
                orderLineMap.put("order_id", String.valueOf(orderLine.getOrderId()));
                orderLineMap.put("item_id",  String.valueOf(orderLine.getItem_id()));
                orderLineMap.put("price_snapshot", String.valueOf(orderLine.getPrice_snapshot()));
                orderLineMap.put("amount", String.valueOf(orderLine.getAmount()));
                jedis.hset(key, orderLineMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
