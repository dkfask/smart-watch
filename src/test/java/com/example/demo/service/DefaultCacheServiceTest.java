package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultCacheService单元测试
 */
class DefaultCacheServiceTest {

    private DefaultCacheService defaultCacheService;

    @BeforeEach
    void setUp() {
        defaultCacheService = new DefaultCacheService();
    }

    /**
     * set带过期时间不做任何操作
     */
    @Test
    void set_withExpire_noOp() {
        assertDoesNotThrow(() -> defaultCacheService.set("key1", "value1", 5, TimeUnit.MINUTES));
    }

    /**
     * set不带过期时间不做任何操作
     */
    @Test
    void set_withoutExpire_noOp() {
        assertDoesNotThrow(() -> defaultCacheService.set("key1", "value1"));
    }

    /**
     * get始终返回null
     */
    @Test
    void get_returnsNull() {
        assertNull(defaultCacheService.get("key1", String.class));
    }

    /**
     * delete不做任何操作
     */
    @Test
    void delete_noOp() {
        assertDoesNotThrow(() -> defaultCacheService.delete("key1"));
    }

    /**
     * exists始终返回false
     */
    @Test
    void exists_returnsFalse() {
        assertFalse(defaultCacheService.exists("key1"));
    }

    /**
     * expire不做任何操作
     */
    @Test
    void expire_noOp() {
        assertDoesNotThrow(() -> defaultCacheService.expire("key1", 5, TimeUnit.MINUTES));
    }

    /**
     * getExpire始终返回0
     */
    @Test
    void getExpire_returnsZero() {
        assertEquals(0, defaultCacheService.getExpire("key1", TimeUnit.MINUTES));
    }

    /**
     * clear不做任何操作
     */
    @Test
    void clear_noOp() {
        assertDoesNotThrow(() -> defaultCacheService.clear());
    }
}