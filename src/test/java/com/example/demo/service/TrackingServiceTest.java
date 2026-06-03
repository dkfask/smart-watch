package com.example.demo.service;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.repository.DeviceStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TrackingService单元测试
 */
@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock private DeviceLocationRepository locationRepo;
    @Mock private DeviceStatusRepository statusRepo;
    @Mock private FenceService fenceService;
    @Mock private CacheService cacheService;

    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new TrackingService(locationRepo, statusRepo, fenceService, cacheService);
    }

    /**
     * reportLocation插入位置记录并更新状态
     */
    @Test
    void reportLocation_insertsAndUpdatesStatus() {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(new BigDecimal("39.9087"));
        dl.setLongitude(new BigDecimal("116.3975"));
        dl.setBatteryLevel(85);
        dl.setTime(LocalDateTime.now());
        when(locationRepo.insert(any(DeviceLocation.class))).thenReturn(1L);
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        long id = trackingService.reportLocation(dl);

        org.junit.jupiter.api.Assertions.assertEquals(1L, id);
        verify(locationRepo).insert(any(DeviceLocation.class));
        verify(statusRepo).upsert(any(DeviceStatus.class));
        verify(cacheService).set(anyString(), any(), anyLong(), any());
    }

    /**
     * reportLocation有坐标时检查围栏
     */
    @Test
    void reportLocation_withCoordinates_checksFence() {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(new BigDecimal("39.9087"));
        dl.setLongitude(new BigDecimal("116.3975"));
        dl.setTime(LocalDateTime.now());
        when(locationRepo.insert(any(DeviceLocation.class))).thenReturn(1L);
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        trackingService.reportLocation(dl);

        verify(fenceService).checkFencesAndAlert(any(DeviceLocation.class), any(), any());
    }

    /**
     * reportLocation无坐标时不检查围栏
     */
    @Test
    void reportLocation_withoutCoordinates_doesNotCheckFence() {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(null);
        dl.setLongitude(null);
        dl.setTime(LocalDateTime.now());
        when(locationRepo.insert(any(DeviceLocation.class))).thenReturn(1L);
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        trackingService.reportLocation(dl);

        verify(fenceService, never()).checkFencesAndAlert(any(), any(), any());
    }

    /**
     * processLocationRecord处理位置记录并检查围栏
     */
    @Test
    void processLocationRecord_withCoordinates_checksFence() {
        com.example.demo.model.LocationRecord record = new com.example.demo.model.LocationRecord();
        com.example.demo.model.Device device = new com.example.demo.model.Device();
        device.setId(1L);
        record.setDevice(device);
        record.setImei("IMEI001");
        record.setLatitude(39.9087);
        record.setLongitude(116.3975);
        record.setBatteryLevel(80);
        when(statusRepo.findById(1L)).thenReturn(Optional.empty());

        trackingService.processLocationRecord(record);

        verify(statusRepo).upsert(argThat(status -> Integer.valueOf(80).equals(status.getBatteryLevel())));
        verify(fenceService).checkFencesAndAlert(any(DeviceLocation.class), any(), any());
    }
}
