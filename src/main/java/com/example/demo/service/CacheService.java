package com.example.demo.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface CacheService {

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    void set(String key, Object value, long expire, TimeUnit timeUnit);

    /**
     * 设置缓存（默认过期时间：5分钟）
     * @param key 缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 获取缓存
     * @param key 缓存键
     * @param <T> 缓存值类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置缓存过期时间
     * @param key 缓存键
     * @param expire 过期时间
     * @param timeUnit 时间单位
     */
    void expire(String key, long expire, TimeUnit timeUnit);

    /**
     * 获取缓存过期时间
     * @param key 缓存键
     * @param timeUnit 时间单位
     * @return 过期时间
     */
    long getExpire(String key, TimeUnit timeUnit);

    /**
     * 清除所有缓存
     */
    void clear();

    /**
     * 缓存穿透模式：先查缓存，未命中则通过loader加载并写入缓存
     * @param key 缓存键
     * @param clazz 缓存值类型
     * @param loader 数据加载器（缓存未命中时调用）
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 缓存或加载的数据
     */
    default <T> T getOrSet(String key, Class<T> clazz, Supplier<T> loader, long timeout, TimeUnit unit) {
        T cached = get(key, clazz);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        if (value != null) {
            set(key, value, timeout, unit);
        }
        return value;
    }
}