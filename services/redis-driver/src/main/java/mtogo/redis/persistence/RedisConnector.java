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
     * Creates an orderDTO in Redis
     * @param orderDTO the orderDTO to create
     */
    public void createOrder(OrderDTO orderDTO){
        try {
            String key = "orderDTO:" + orderDTO.getOrder_id();
            Map<String, String> orderMap = new HashMap<>();
            orderMap.put("order_id", String.valueOf(orderDTO.getOrder_id()));
            orderMap.put("customer_id", String.valueOf(orderDTO.getCustomer_id()));
            orderMap.put("order_created", String.valueOf(orderDTO.getOrder_created().getTime()));
            orderMap.put("order_updated", String.valueOf(orderDTO.getOrder_updated().getTime()));
            orderMap.put("order_status", orderDTO.getOrderStatus().name());
            jedis.hset(key, orderMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Creates order lines in Redis
     * @param orderLineDTOS the list of order lines to create
     */
    public void createOrderLines(List<OrderLineDTO> orderLineDTOS){
        try {
            for (OrderLineDTO orderLineDTO : orderLineDTOS) {
                String key = "orderline:" + orderLineDTO.getOrderLineId();
                Map<String, String> orderLineMap = new HashMap<>();
                orderLineMap.put("order_line_id", String.valueOf(orderLineDTO.getOrderLineId()));
                orderLineMap.put("order_id", String.valueOf(orderLineDTO.getOrderId()));
                orderLineMap.put("item_id",  String.valueOf(orderLineDTO.getItem_id()));
                orderLineMap.put("price_snapshot", String.valueOf(orderLineDTO.getPrice_snapshot()));
                orderLineMap.put("amount", String.valueOf(orderLineDTO.getAmount()));
                jedis.hset(key, orderLineMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
