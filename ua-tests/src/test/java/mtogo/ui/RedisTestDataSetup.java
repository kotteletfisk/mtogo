package mtogo.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple utility to flush Redis and add test suppliers.
 * Menu items and orders are stored in the database.
 */
public class RedisTestDataSetup {

    private final JedisPool jedisPool;

    public RedisTestDataSetup(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }

    public RedisTestDataSetup() {
        this(
                System.getenv().getOrDefault("REDIS_HOST", "localhost"),
                Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6380"))
        );
    }

    /**
     * Flush Redis and add test supplier
     */
    public void setupTestData() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Flush Redis
            System.out.println(">>> Flushing Redis database...");
            jedis.flushDB();
            System.out.println(">>> Redis flushed");

            // Add test supplier
            System.out.println(">>> Adding test supplier...");
            Map<String, String> supplier = new HashMap<>();
            supplier.put("supplier_id", "1");
            supplier.put("name", "Test Supplier");
            supplier.put("zip_code", "2200");
            supplier.put("status", "active");
            jedis.hset("supplier:2200:active:1", supplier);
            System.out.println(">>> Test supplier added: HSET supplier:2200:active:1");

        } catch (Exception e) {
            System.err.println("ERROR setting up Redis: " + e.getMessage());
            throw new RuntimeException("Failed to setup Redis", e);
        }
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}