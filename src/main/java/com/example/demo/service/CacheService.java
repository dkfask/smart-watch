package com.example.demo.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口，定义了缓存操作的基本方法
 * 预留接口，用于后续集成Redis等缓存服务
 */
public interface CacheService {
    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @param <T> 值类型
     */
    <T> void set(String key, T value, long timeout, TimeUnit unit);

    /**
     * 设置缓存，使用默认过期时间
     * @param key 缓存键
     * @param value 缓存值
     * @param <T> 值类型
     */
    <T> void set(String key, T value);

    /**
     * 获取缓存
     * @param key 缓存键
     * @param type 值类型
     * @param <T> 值类型
     * @return 缓存值，若不存在则返回Optional.empty()
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return 存在返回true，否则返回false
     */
    boolean exists(String key);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 设置缓存过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 设置成功返回true，否则返回false
     */
    boolean expire(String key, long timeout, TimeUnit unit);
}
