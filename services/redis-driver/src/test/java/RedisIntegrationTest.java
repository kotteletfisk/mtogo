import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;

import mtogo.redis.DTO.OrderDTO;
import mtogo.redis.DTO.OrderLineDTO;
import mtogo.redis.exceptions.RedisException;
import mtogo.redis.persistence.RedisConnector;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

class RedisIntegrationTest {

    @Container
    RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8.2.3-alpine"));

    private RedisConnector redisConnector;
    private UnifiedJedis jedis;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        redis.start();
        jedis = new UnifiedJedis("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
        redisConnector = new RedisConnector(jedis);
    }

    @Test
    void connectionEstablishedTest() {
        String response = jedis.ping();

        assertEquals("PONG", response);
    }

    @Test
    void insertOrderWithLineTest() {

        UUID orderId = UUID.randomUUID();
        OrderDTO order = new OrderDTO(orderId, 1, 1, List.of(
                new OrderLineDTO(1, orderId, 1, 1.0f, 1),
                new OrderLineDTO(2, orderId, 2, 2.0f, 2)));

        try {
            redisConnector.createOrder(order);
        } catch (JsonProcessingException | RedisException e) {
            fail(e);
        }
    }

    @Test
    void fetchInsertedOrderTest() {

        UUID orderId = UUID.randomUUID();
        OrderDTO order = new OrderDTO(orderId, 1, 1, List.of(
                new OrderLineDTO(1, orderId, 1, 1.0f, 1),
                new OrderLineDTO(2, orderId, 2, 2.0f, 2)));

        try {
            redisConnector.createOrder(order);
        } catch (JsonProcessingException | RedisException e) {
            fail(e);
        }

        String key = "order:" + orderId;
        Object json = jedis.jsonGet(key);

        OrderDTO dto = mapper.convertValue(json, OrderDTO.class);
        assertNotNull(dto);
        assertEquals(orderId, dto.getOrderId());
        assertEquals(2, dto.getOrderlineDTOs().size());
    }

    @Test
    void fetchOrderBySupplierTest() {

        try {
            jedis.ftDropIndexDD("orders-idx");
        } catch (JedisDataException e) {
            System.out.println(e.getMessage());
        }
        redisConnector.ensureIndexForOrders();
        UUID order1Id = UUID.randomUUID();
        OrderDTO order = new OrderDTO(order1Id, 1, 1, List.of(
                new OrderLineDTO(1, order1Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order1Id, 2, 2.0f, 2)));
        UUID order2Id = UUID.randomUUID();
        OrderDTO order2 = new OrderDTO(order2Id, 1, 1, List.of(
                new OrderLineDTO(1, order2Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order2Id, 2, 2.0f, 2)));
        UUID order3Id = UUID.randomUUID();
        OrderDTO order3 = new OrderDTO(order3Id, 1, 2, List.of(
                new OrderLineDTO(1, order2Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order2Id, 2, 2.0f, 2)));

        try {
            redisConnector.createOrder(order);
            redisConnector.createOrder(order2);
            redisConnector.createOrder(order3);
        } catch (JsonProcessingException | RedisException e) {
            fail(e);
        }

        Query q = new Query().addFilter(new Query.NumericFilter("supplierId", 1, 1));

        SearchResult sr = jedis.ftSearch("orders-idx", q);
        assertEquals(sr.getDocuments().size(), 2);
    }

    @Test
    void correctObjectMappingFromRedisTest() {

        try {
            jedis.ftDropIndexDD("orders-idx");
        } catch (JedisDataException e) {
            System.out.println(e.getMessage());
        }
        redisConnector.ensureIndexForOrders();
        UUID order1Id = UUID.randomUUID();
        OrderDTO order = new OrderDTO(order1Id, 1, 1, List.of(
                new OrderLineDTO(1, order1Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order1Id, 2, 2.0f, 2)));
        UUID order2Id = UUID.randomUUID();
        OrderDTO order2 = new OrderDTO(order2Id, 1, 1, List.of(
                new OrderLineDTO(1, order2Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order2Id, 2, 2.0f, 2)));
        UUID order3Id = UUID.randomUUID();
        OrderDTO order3 = new OrderDTO(order3Id, 1, 2, List.of(
                new OrderLineDTO(1, order2Id, 1, 1.0f, 1),
                new OrderLineDTO(2, order2Id, 2, 2.0f, 2)));

        try {
            redisConnector.createOrder(order);
            redisConnector.createOrder(order2);
            redisConnector.createOrder(order3);
        } catch (JsonProcessingException | RedisException e) {
            fail(e);
        }

        Query q = new Query().addFilter(new Query.NumericFilter("supplierId", 2, 2));

        SearchResult sr = jedis.ftSearch("orders-idx", q);

        assertEquals(sr.getTotalResults(), 1);

        Document doc = sr.getDocuments().get(0);
            String json = (String) doc.get("$");
            try {
                OrderDTO dto = mapper.readValue(json, OrderDTO.class);

                assertEquals(order3.getOrderId(), dto.getOrderId());

            } catch (JsonProcessingException e) {
                fail(e);
            }
    }

    @Test
    void createIndexTest() {
        assertThrows(JedisDataException.class, () -> jedis.ftInfo("orders-idx"));

        redisConnector.ensureIndexForOrders();

        assertDoesNotThrow(() -> jedis.ftInfo("orders-idx"));
    }
}
