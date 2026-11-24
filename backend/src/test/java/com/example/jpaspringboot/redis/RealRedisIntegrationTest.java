package com.example.jpaspringboot.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in smoke and semantics test against the isolated RC Redis server. */
@EnabledIfSystemProperty(named = "hnblue.redis.it", matches = "true")
class RealRedisIntegrationTest {
    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private String prefix;

    @BeforeEach
    void connect() {
        String host = System.getProperty("hnblue.redis.host", "127.0.0.1");
        int port = Integer.getInteger("hnblue.redis.port", 36379);
        String password = System.getProperty("hnblue.redis.password", "rc3-redis-local-20261019");
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        standalone.setPassword(RedisPassword.of(password));
        factory = new LettuceConnectionFactory(standalone);
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        prefix = "hnblue:rc3:it:" + UUID.randomUUID();
    }

    @AfterEach
    void close() {
        if (redis != null) {
            redis.delete(redis.keys(prefix + "*"));
        }
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void executesTtlAtomicRateLimitAndConsumerGroupStream() {
        assertEquals("PONG", factory.getConnection().ping());

        String valueKey = prefix + ":value";
        redis.opsForValue().set(valueKey, "ready", Duration.ofSeconds(30));
        assertEquals("ready", redis.opsForValue().get(valueKey));
        assertTrue(redis.getExpire(valueKey) > 0);

        DefaultRedisScript<Long> limiter = new DefaultRedisScript<>("""
                local count = redis.call('INCR', KEYS[1])
                if count == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end
                if count > tonumber(ARGV[2]) then
                  redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[3]))
                  return -1
                end
                return count
                """, Long.class);
        List<String> limitKeys = List.of(prefix + ":rate", prefix + ":blocked");
        assertEquals(1L, redis.execute(limiter, limitKeys, "30", "2", "30"));
        assertEquals(2L, redis.execute(limiter, limitKeys, "30", "2", "30"));
        assertEquals(-1L, redis.execute(limiter, limitKeys, "30", "2", "30"));

        String stream = prefix + ":tasks";
        String recordId = redis.execute((RedisCallback<String>) connection -> {
            Object result = connection.execute("XADD", bytes(stream), bytes("*"),
                    bytes("taskId"), bytes("task-1"), bytes("attempt"), bytes("1"));
            return result == null ? null : new String((byte[]) result, StandardCharsets.UTF_8);
        });
        assertNotNull(recordId);

        redis.execute((RedisCallback<Object>) connection -> connection.execute(
                "XGROUP", bytes("CREATE"), bytes(stream), bytes("workers"), bytes("0")));
        Object claimed = redis.execute((RedisCallback<Object>) connection -> connection.execute(
                "XREADGROUP", bytes("GROUP"), bytes("workers"), bytes("worker-1"),
                bytes("COUNT"), bytes("1"), bytes("STREAMS"), bytes(stream), bytes(">")));
        assertNotNull(claimed);

        var pending = redis.opsForStream().pending(stream, "workers");
        assertNotNull(pending);
        assertEquals(1L, pending.getTotalPendingMessages());

        Long length = redis.opsForStream().size(stream);
        assertEquals(1L, length);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
