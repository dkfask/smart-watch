package com.example.demo.service;

import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeviceStatusService单元测试
 */
@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceTest {

    @Mock private DeviceStatusRepository statusRepo;
    @Mock private CacheService cacheService;

    private DeviceStatusService service;

    @BeforeEach
    void setUp() {
        service = new DeviceStatusService(statusRepo, cacheService);
    }

    /**
     * getDeviceStatus缓存命中返回缓存数据
     */
    @Test
    void getDeviceStatus_cacheHit_returnsCachedData() {
        DeviceStatus cached = new DeviceStatus();
        cached.setDeviceId(1L);
        cached.setIsOnline(true);
        when(cacheService.get(anyString(), eq(DeviceStatus.class))).thenReturn(cached);

        Optional<DeviceStatus> result = service.getDeviceStatus("1");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getDeviceId());
        verify(statusRepo, never()).findById(anyLong());
    }

    /**
     * getDeviceStatus缓存未命中查数据库
     */
    @Test
    void getDeviceStatus_cacheMiss_queriesDatabase() {
        when(cacheService.get(anyString(), eq(DeviceStatus.class))).thenReturn(null);
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(1L);
        status.setIsOnline(true);
        when(statusRepo.findById(1L)).thenReturn(Optional.of(status));

        Optional<DeviceStatus> result = service.getDeviceStatus("1");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getDeviceId());
        verify(cacheService).set(anyString(), eq(status), eq(30L), eq(TimeUnit.MINUTES));
    }

    /**
     * getDeviceStatus数据库不存在返回empty
     */
    @Test
    void getDeviceStatus_notFoundInDb_returnsEmpty() {
        when(cacheService.get(anyString(), eq(DeviceStatus.class))).thenReturn(null);
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        Optional<DeviceStatus> result = service.getDeviceStatus("1");

        assertTrue(result.isEmpty());
    }

    /**
     * getDeviceStatus无效设备ID格式返回empty
     */
    @Test
    void getDeviceStatus_invalidIdFormat_returnsEmpty() {
        when(cacheService.get(anyString(), eq(DeviceStatus.class))).thenReturn(null);

        Optional<DeviceStatus> result = service.getDeviceStatus("abc");

        assertTrue(result.isEmpty());
        verify(statusRepo, never()).findById(anyLong());
    }

    /**
     * getAllDeviceStatus缓存命中返回缓存数据
     */
    @Test
    void getAllDeviceStatus_cacheHit_returnsCachedData() {
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(1L);
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(List.of(s));

        List<DeviceStatus> result = service.getAllDeviceStatus();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(statusRepo, never()).findAll();
    }

    /**
     * getAllDeviceStatus缓存未命中查数据库
     */
    @Test
    void getAllDeviceStatus_cacheMiss_queriesDatabase() {
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(null);
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(1L);
        when(statusRepo.findAll()).thenReturn(List.of(s));

        List<DeviceStatus> result = service.getAllDeviceStatus();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(cacheService).set(anyString(), eq(List.of(s)), eq(5L), eq(TimeUnit.MINUTES));
    }

    /**
     * clearDeviceStatusCache清除缓存
     */
    @Test
    void clearDeviceStatusCache_clearsBothKeys() {
        service.clearDeviceStatusCache("1");

        verify(cacheService, times(2)).delete(anyString());
    }

    /**
     * clearAllDeviceStatusCache清除所有缓存
     */
    @Test
    void clearAllDeviceStatusCache_deletesAll() {
        service.clearAllDeviceStatusCache();

        verify(cacheService).delete(anyString());
    }

    /**
     * preheatDeviceStatusCache预热设备状态缓存
     */
    @Test
    void preheatDeviceStatusCache_foundsDevice_cachesIt() {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(1L);
        when(statusRepo.findById(1L)).thenReturn(Optional.of(status));

        service.preheatDeviceStatusCache("1");

        verify(cacheService).set(anyString(), eq(status), eq(30L), eq(TimeUnit.MINUTES));
    }

    /**
     * preheatDeviceStatusCache设备不存在不缓存
     */
    @Test
    void preheatDeviceStatusCache_deviceNotFound_noCache() {
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        service.preheatDeviceStatusCache("1");

        verify(cacheService, never()).set(anyString(), any(), anyLong(), any());
    }

    /**
     * preheatDeviceStatusCache无效ID格式不缓存
     */
    @Test
    void preheatDeviceStatusCache_invalidId_noCache() {
        service.preheatDeviceStatusCache("abc");

        verify(statusRepo, never()).findById(anyLong());
        verify(cacheService, never()).set(anyString(), any(), anyLong(), any());
    }

    /**
     * preheatAllDeviceStatusCache预热所有设备状态缓存
     */
    @Test
    void preheatAllDeviceStatusCache_cachesAll() {
        DeviceStatus s1 = new DeviceStatus();
        s1.setDeviceId(1L);
        DeviceStatus s2 = new DeviceStatus();
        s2.setDeviceId(2L);
        when(statusRepo.findAll()).thenReturn(List.of(s1, s2));

        service.preheatAllDeviceStatusCache();

        verify(cacheService, times(3)).set(anyString(), any(), anyLong(), any());
    }
}
