package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 默认缓存服务实现，不做任何实际缓存操作
 * 用于预留缓存接口，后续可替换为Redis等实际缓存实现
 */
@Service
@ConditionalOnMissingBean(RedisCacheService.class)
public class DefaultCacheService implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCacheService.class);

    @Override
    public void set(String key, Object value, long expire, TimeUnit timeUnit) {
        log.debug("CacheService (default): set key={}, value={}, expire={}, timeUnit={}", key, value, expire, timeUnit);
        // 默认实现：不做任何操作
    }

    @Override
    public void set(String key, Object value) {
        log.debug("CacheService (default): set key={}, value={}", key, value);
        // 默认实现：不做任何操作
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        log.debug("CacheService (default): get key={}, clazz={}", key, clazz.getName());
        // 默认实现：返回null
        return null;
    }

    @Override
    public void delete(String key) {
        log.debug("CacheService (default): delete key={}", key);
        // 默认实现：不做任何操作
    }

    @Override
    public boolean exists(String key) {
        log.debug("CacheService (default): exists key={}", key);
        // 默认实现：返回false
        return false;
    }

    @Override
    public void expire(String key, long expire, TimeUnit timeUnit) {
        log.debug("CacheService (default): expire key={}, expire={}, timeUnit={}", key, expire, timeUnit);
        // 默认实现：不做任何操作
    }

    @Override
    public long getExpire(String key, TimeUnit timeUnit) {
        log.debug("CacheService (default): getExpire key={}, timeUnit={}", key, timeUnit);
        // 默认实现：返回0
        return 0;
    }

    @Override
    public void clear() {
        log.debug("CacheService (default): clear");
        // 默认实现：不做任何操作
    }
}
