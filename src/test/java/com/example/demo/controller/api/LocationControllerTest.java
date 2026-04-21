package com.example.demo.controller.api;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.service.AmapLocationService;
import com.example.demo.service.TrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LocationController集成测试（MockMvc）
 */
@WebMvcTest(value = LocationController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TrackingService trackingService;
    @MockBean private DeviceLocationRepository locationRepo;
    @MockBean private AmapLocationService amapLocationService;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/locations/report 上报位置
     */
    @Test
    void report_returns201() throws Exception {
        when(trackingService.reportLocation(any(DeviceLocation.class))).thenReturn(1L);

        mockMvc.perform(post("/api/locations/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":1,\"latitude\":39.9,\"longitude\":116.4,\"imei\":\"IMEI001\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * GET /api/locations/device/{deviceId} 获取最近位置
     */
    @Test
    void recent_returnsPagedResult() throws Exception {
        when(locationRepo.listRecent(eq(1L), anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(locationRepo.countByDeviceId(1L)).thenReturn(0L);

        mockMvc.perform(get("/api/locations/device/1")
                        .param("limit", "50")
                        .param("offset", "0"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/device/{deviceId}/history 获取历史位置
     */
    @Test
    void history_returnsPagedResult() throws Exception {
        when(locationRepo.listByRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(locationRepo.countByDeviceIdAndRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        mockMvc.perform(get("/api/locations/device/1/history")
                        .param("start", "2025-01-01T00:00:00")
                        .param("end", "2025-12-31T23:59:59"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest 获取最新位置
     */
    @Test
    void latest_exists_returns200() throws Exception {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setLatitude(BigDecimal.valueOf(39.9));
        dl.setLongitude(BigDecimal.valueOf(116.4));
        when(locationRepo.listRecent(1L, 1, 0)).thenReturn(List.of(dl));

        mockMvc.perform(get("/api/locations/device/1/latest"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest 无位置返回404
     */
    @Test
    void latest_notFound_returns404() throws Exception {
        when(locationRepo.listRecent(1L, 1, 0)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/locations/device/1/latest"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest-with-address 获取最新位置含地址
     */
    @Test
    void latestWithAddress_exists_returns200() throws Exception {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(BigDecimal.valueOf(39.9));
        dl.setLongitude(BigDecimal.valueOf(116.4));
        when(locationRepo.listRecent(1L, 1, 0)).thenReturn(List.of(dl));
        when(amapLocationService.regeoAddress(39.9, 116.4)).thenReturn("北京市东城区");

        mockMvc.perform(get("/api/locations/device/1/latest-with-address"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/search 地址搜索
     */
    @Test
    void search_returnsResult() throws Exception {
        when(amapLocationService.addressToLocation("北京")).thenReturn(Map.of("lat", 39.9, "lng", 116.4));

        mockMvc.perform(get("/api/locations/search")
                        .param("address", "北京"))
                .andExpect(status().isOk());
    }
}
