package com.example.demo.repository;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.service.AmapLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JdbcTemplate Repository单元测试
 */
@ExtendWith(MockitoExtension.class)
class JdbcRepositoryTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private AmapLocationService amapLocationService;

    private DeviceLocationRepository locationRepo;
    private DeviceStatusRepository statusRepo;

    @BeforeEach
    void setUp() {
        locationRepo = new DeviceLocationRepository(jdbc, amapLocationService);
        statusRepo = new DeviceStatusRepository(jdbc);
    }

    /**
     * DeviceLocationRepository insert插入位置记录
     */
    @Test
    void deviceLocation_insert_returnsId() {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(BigDecimal.valueOf(39.9));
        dl.setLongitude(BigDecimal.valueOf(116.4));
        when(amapLocationService.regeoAddress(39.9, 116.4)).thenReturn("北京市");
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenReturn(1);

        long id = locationRepo.insert(dl);

        verify(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }

    /**
     * DeviceLocationRepository listRecent查询最近位置
     */
    @Test
    void deviceLocation_listRecent_returnsList() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        List<DeviceLocation> result = locationRepo.listRecent(1L, 50, 0);

        assertNotNull(result);
        verify(jdbc).query(anyString(), any(RowMapper.class), eq(1L), eq(50), eq(0));
    }

    /**
     * DeviceLocationRepository countByDeviceId统计数量
     */
    @Test
    void deviceLocation_countByDeviceId_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(10L);

        long count = locationRepo.countByDeviceId(1L);

        assertEquals(10L, count);
    }

    /**
     * DeviceStatusRepository upsert更新或插入
     */
    @Test
    void deviceStatus_upsert_executesUpdate() {
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(1L);
        s.setIsOnline(true);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        int result = statusRepo.upsert(s);

        assertEquals(1, result);
    }

    /**
     * DeviceStatusRepository findById查询设备状态
     */
    @Test
    void deviceStatus_findById_returnsStatus() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(Collections.emptyList());

        var result = statusRepo.findById(1L);

        assertTrue(result.isEmpty());
    }

    /**
     * DeviceStatusRepository findAllById批量查询
     */
    @Test
    void deviceStatus_findAllById_emptyList_returnsEmpty() {
        List<DeviceStatus> result = statusRepo.findAllById(List.of());

        assertTrue(result.isEmpty());
    }
}
