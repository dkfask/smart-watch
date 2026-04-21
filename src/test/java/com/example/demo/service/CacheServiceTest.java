package com.example.demo.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheService接口默认方法测试
 */
class CacheServiceTest {

    /**
     * getOrSet缓存命中返回缓存值
     */
    @Test
    void getOrSet_cacheHit_returnsCachedValue() {
        CacheService cacheService = mock(CacheService.class);
        String cachedValue = "cached_data";
        when(cacheService.get(anyString(), eq(String.class))).thenReturn(cachedValue);
        when(cacheService.getOrSet(anyString(), eq(String.class), any(), anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();

        String result = cacheService.getOrSet("test_key", String.class, () -> "new_data", 5, TimeUnit.MINUTES);

        assertEquals("cached_data", result);
        verify(cacheService, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    /**
     * getOrSet缓存未命中调用loader并写入缓存
     */
    @Test
    void getOrSet_cacheMiss_callsLoaderAndSetsCache() {
        CacheService cacheService = mock(CacheService.class);
        when(cacheService.get(anyString(), eq(String.class))).thenReturn(null);
        doNothing().when(cacheService).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(cacheService.getOrSet(anyString(), eq(String.class), any(), anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();

        String result = cacheService.getOrSet("test_key", String.class, () -> "loaded_data", 5, TimeUnit.MINUTES);

        assertEquals("loaded_data", result);
        verify(cacheService).set(eq("test_key"), eq("loaded_data"), eq(5L), eq(TimeUnit.MINUTES));
    }
}
