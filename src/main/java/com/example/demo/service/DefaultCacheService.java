package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 默认缓存服务实现，不做任何实际缓存操作
 * 用于预留缓存接口，后续可替换为Redis等实际缓存实现
 */
@Service
public class DefaultCacheService implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCacheService.class);

    @Override
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        log.debug("CacheService (default): set key={}, value={}, timeout={}, unit={}", key, value, timeout, unit);
        // 默认实现：不做任何操作
    }

    @Override
    public <T> void set(String key, T value) {
        log.debug("CacheService (default): set key={}, value={}", key, value);
        // 默认实现：不做任何操作
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        log.debug("CacheService (default): get key={}, type={}", key, type.getName());
        // 默认实现：返回空
        return Optional.empty();
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
    public void clear() {
        log.debug("CacheService (default): clear");
        // 默认实现：不做任何操作
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        log.debug("CacheService (default): expire key={}, timeout={}, unit={}", key, timeout, unit);
        // 默认实现：返回false
        return false;
    }
}
