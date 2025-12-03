package mtogo.redis.persistence;

import mtogo.redis.DTO.OrderDTO;
import mtogo.redis.DTO.OrderLineDTO;
import mtogo.redis.DTO.SupplierDTO;
import redis.clients.jedis.UnifiedJedis;

import java.util.*;

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
        map.put("supplier_id", String.valueOf(order.getSupplierId())); // TODO: inserts 0 on uninitialized value

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
            map.put("item_id", String.valueOf(l.getItemId()));
            map.put("price_snapshot", String.valueOf(l.getPriceSnapshot()));
            map.put("amount", String.valueOf(l.getAmount()));

            jedis.hset("orderline:" + l.getOrderLineId(), map);
        }
    }

    public void saveSupplier(SupplierDTO supplier) {

        Map<String, String> map = new HashMap<>();
        map.put("supplier_id", String.valueOf(supplier.getSupplierId()));
        map.put("name", supplier.getName());
        map.put("zip_code", supplier.getZipCode());
        map.put("status", supplier.getSupplierStatus().name());

        String key = "supplier:"
                + supplier.getZipCode() + ":"
                + supplier.getSupplierStatus().name() + ":"
                + supplier.getSupplierId();

        jedis.hset(key, map);
    }

    public List<SupplierDTO> findSuppliersByZipAndStatus(
            String zip, SupplierDTO.status stat) {

        String pattern = "supplier:" + zip + ":" + stat.name() + ":*";
        Set<String> keys = jedis.keys(pattern);

        List<SupplierDTO> result = new ArrayList<>();

        for (String key : keys) {
            Map<String, String> data = jedis.hgetAll(key);

            if (data != null && !data.isEmpty()) {
                SupplierDTO dto = new SupplierDTO();
                dto.setSupplierId(Integer.parseInt(data.get("supplier_id")));
                dto.setName(data.get("name"));
                dto.setZipCode(data.get("zip_code"));
                dto.setSupplierStatus(SupplierDTO.status.valueOf(data.get("status")));
                result.add(dto);
            }
        }

        return result;
    }



}
