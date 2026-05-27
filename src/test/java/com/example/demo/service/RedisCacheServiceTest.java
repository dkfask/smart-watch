package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisCacheService单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisCacheService = new RedisCacheService(redisTemplate);
    }

    /**
     * set带过期时间调用RedisTemplate
     */
    @Test
    void set_withExpire_callsRedisTemplate() {
        redisCacheService.set("key1", "value1", 5, TimeUnit.MINUTES);

        verify(valueOperations).set("key1", "value1", 5, TimeUnit.MINUTES);
    }

    /**
     * set不带过期时间使用默认5分钟
     */
    @Test
    void set_withoutExpire_usesDefault() {
        redisCacheService.set("key1", "value1");

        verify(valueOperations).set("key1", "value1", 5, TimeUnit.MINUTES);
    }

    /**
     * get有值时返回缓存对象
     */
    @Test
    void get_existingKey_returnsValue() {
        when(valueOperations.get("key1")).thenReturn("cachedValue");

        String result = redisCacheService.get("key1", String.class);

        assertEquals("cachedValue", result);
    }

    /**
     * get无值时返回null
     */
    @Test
    void get_missingKey_returnsNull() {
        when(valueOperations.get("key1")).thenReturn(null);

        assertNull(redisCacheService.get("key1", String.class));
    }

    /**
     * get异常时返回null
     */
    @Test
    void get_exception_returnsNull() {
        when(valueOperations.get("key1")).thenThrow(new RuntimeException("Redis error"));

        assertNull(redisCacheService.get("key1", String.class));
    }

    /**
     * delete调用RedisTemplate
     */
    @Test
    void delete_callsRedisTemplate() {
        redisCacheService.delete("key1");

        verify(redisTemplate).delete("key1");
    }

    /**
     * deleteByPattern批量删除
     */
    @Test
    void deleteByPattern_batchDeletes() {
        when(redisTemplate.keys("pattern*")).thenReturn(Set.of("pattern1", "pattern2"));

        redisCacheService.deleteByPattern("pattern*");

        verify(redisTemplate).delete("pattern1");
        verify(redisTemplate).delete("pattern2");
    }

    /**
     * exists键存在返回true
     */
    @Test
    void exists_keyExists_returnsTrue() {
        when(redisTemplate.hasKey("key1")).thenReturn(true);

        assertTrue(redisCacheService.exists("key1"));
    }

    /**
     * exists键不存在返回false
     */
    @Test
    void exists_keyNotExists_returnsFalse() {
        when(redisTemplate.hasKey("key1")).thenReturn(false);

        assertFalse(redisCacheService.exists("key1"));
    }

    /**
     * exists返回null时返回false
     */
    @Test
    void exists_nullResult_returnsFalse() {
        when(redisTemplate.hasKey("key1")).thenReturn(null);

        assertFalse(redisCacheService.exists("key1"));
    }

    /**
     * expire设置过期时间
     */
    @Test
    void expire_callsRedisTemplate() {
        redisCacheService.expire("key1", 10, TimeUnit.MINUTES);

        verify(redisTemplate).expire("key1", 10, TimeUnit.MINUTES);
    }

    /**
     * getExpire返回剩余时间
     */
    @Test
    void getExpire_returnsExpireTime() {
        when(redisTemplate.getExpire("key1", TimeUnit.MINUTES)).thenReturn(8L);

        assertEquals(8L, redisCacheService.getExpire("key1", TimeUnit.MINUTES));
    }

    /**
     * getExpire返回null时返回-1
     */
    @Test
    void getExpire_nullResult_returnsMinus1() {
        when(redisTemplate.getExpire("key1", TimeUnit.MINUTES)).thenReturn(null);

        assertEquals(-1L, redisCacheService.getExpire("key1", TimeUnit.MINUTES));
    }

    /**
     * increment增加计数
     */
    @Test
    void increment_increasesValue() {
        when(valueOperations.increment("counter", 1)).thenReturn(5L);

        Long result = redisCacheService.increment("counter", 1);

        assertEquals(5L, result);
    }

    /**
     * setIfAbsent键不存在时设置成功
     */
    @Test
    void setIfAbsent_keyNotExists_setsValue() {
        when(valueOperations.setIfAbsent("key1", "value1", 5, TimeUnit.MINUTES)).thenReturn(true);

        assertTrue(redisCacheService.setIfAbsent("key1", "value1", 5, TimeUnit.MINUTES));
    }

    /**
     * setIfAbsent键已存在时返回false
     */
    @Test
    void setIfAbsent_keyExists_returnsFalse() {
        when(valueOperations.setIfAbsent("key1", "value1", 5, TimeUnit.MINUTES)).thenReturn(false);

        assertFalse(redisCacheService.setIfAbsent("key1", "value1", 5, TimeUnit.MINUTES));
    }

    /**
     * clear清除所有带前缀的缓存
     */
    @Test
    void clear_deletesAllWithPrefix() {
        when(redisTemplate.keys("smart:location:*")).thenReturn(Set.of("smart:location:key1"));

        redisCacheService.clear();

        verify(redisTemplate).delete("smart:location:key1");
    }

    /**
     * getKey生成带前缀的缓存键
     */
    @Test
    void getKey_generatesPrefixedKey() {
        String key = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "123");

        assertEquals("smart:location:device:status:123", key);
    }

    /**
     * getHitRate返回命中率
     */
    @Test
    void getHitRate_returnsRate() {
        assertEquals(0.85, redisCacheService.getHitRate(), 0.001);
    }

    /**
     * preheat预热缓存
     */
    @Test
    void preheat_setsValue() {
        redisCacheService.preheat("key1", "value1", 10, TimeUnit.MINUTES);

        verify(valueOperations).set("key1", "value1", 10, TimeUnit.MINUTES);
    }
}