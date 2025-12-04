import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
import redis.clients.jedis.json.Path2;

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
        assertEquals(orderId, dto.getOrder_id());
        assertEquals(2, dto.getOrderlineDTOs().size());
    }

}
