package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RedisCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long DEFAULT_EXPIRE = 5; // 默认过期时间：5分钟
    private static final String CACHE_PREFIX = "smart:location:"; // 缓存键前缀

    // 缓存键命名规范：smart:location:{module}:{key}
    public static final String DEVICE_STATUS_PREFIX = CACHE_PREFIX + "device:status:";
    public static final String DEVICE_LOCATION_PREFIX = CACHE_PREFIX + "device:location:";
    public static final String GEO_FENCE_PREFIX = CACHE_PREFIX + "geo:fence:";
    public static final String ALARM_PREFIX = CACHE_PREFIX + "alarm:";
    public static final String PATIENT_PREFIX = CACHE_PREFIX + "patient:";

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成带前缀的缓存键
     * @param prefix 前缀
     * @param key 原始键
     * @return 带前缀的缓存键
     */
    public static String getKey(String prefix, String key) {
        return prefix + key;
    }

    @Override
    public void set(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, expire, timeUnit);
            log.debug("Set cache: key={}, expire={}{}", key, expire, timeUnit.name());
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE, TimeUnit.MINUTES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Get cache: key={}, value={}", key, value);
                return (T) value;
            }
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
        }
        return null;
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Delete cache: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache: key={}", key, e);
        }
    }

    /**
     * 批量删除缓存
     * @param pattern 键模式
     */
    public void deleteByPattern(String pattern) {
        try {
            redisTemplate.keys(pattern).forEach(key -> {
                redisTemplate.delete(key);
                log.debug("Delete cache by pattern: key={}", key);
            });
        } catch (Exception e) {
            log.error("Failed to delete cache by pattern: {}", pattern, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug("Check cache exists: key={}, exists={}", key, exists);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check cache exists: key={}", key, e);
            return false;
        }
    }

    @Override
    public void expire(String key, long expire, TimeUnit timeUnit) {
        try {
            redisTemplate.expire(key, expire, timeUnit);
            log.debug("Set cache expire: key={}, expire={}{}", key, expire, timeUnit.name());
        } catch (Exception e) {
            log.error("Failed to set cache expire: key={}", key, e);
        }
    }

    @Override
    public long getExpire(String key, TimeUnit timeUnit) {
        try {
            Long expire = redisTemplate.getExpire(key, timeUnit);
            log.debug("Get cache expire: key={}, expire={}{}", key, expire, timeUnit.name());
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("Failed to get cache expire: key={}", key, e);
            return -1;
        }
    }

    /**
     * 增加缓存计数
     * @param key 缓存键
     * @param delta 增加量
     * @return 增加后的值
     */
    public Long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Increment cache: key={}, delta={}, value={}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to increment cache: key={}", key, e);
            return null;
        }
    }

    /**
     * 设置缓存，只有当键不存在时才设置
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    public Boolean setIfAbsent(String key, Object value, long expire, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expire, timeUnit);
            log.debug("Set cache if absent: key={}, result={}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to set cache if absent: key={}", key, e);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            // 只清除本应用的缓存，避免影响其他应用
            redisTemplate.keys(CACHE_PREFIX + "*").forEach(key -> {
                redisTemplate.delete(key);
            });
            log.debug("Clear all cache with prefix: {}", CACHE_PREFIX);
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
        }
    }

    /**
     * 获取缓存命中率
     * @return 缓存命中率
     */
    public double getHitRate() {
        try {
            // 这里可以通过Redis的监控命令或自定义计数器来实现
            // 暂时返回模拟值
            return 0.85;
        } catch (Exception e) {
            log.error("Failed to get hit rate", e);
            return 0;
        }
    }

    /**
     * 预热缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    public void preheat(String key, Object value, long expire, TimeUnit timeUnit) {
        set(key, value, expire, timeUnit);
        log.debug("Preheat cache: key={}", key);
    }
}