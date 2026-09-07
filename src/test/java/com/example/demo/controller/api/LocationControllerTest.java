package com.example.demo.controller.api;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.service.TiandituLocationService;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LocationController MockMvc tests.
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
    @MockBean private DeviceStatusRepository deviceStatusRepo;
    @MockBean private TiandituLocationService tiandituLocationService;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/locations/report accepts a device location report.
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
     * GET /api/locations/device/{deviceId} returns recent locations.
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
     * GET /api/locations/device/{deviceId}/history returns a paged history range.
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
     * GET /api/locations/device/{deviceId}/latest returns the latest location.
     */
    @Test
    void latest_exists_returns200() throws Exception {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setLatitude(BigDecimal.valueOf(39.9));
        dl.setLongitude(BigDecimal.valueOf(116.4));
        when(locationRepo.listRecentValid(1L, 1)).thenReturn(List.of(dl));

        mockMvc.perform(get("/api/locations/device/1/latest"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest returns 404 when no location exists.
     */
    @Test
    void latest_notFound_returns404() throws Exception {
        when(locationRepo.listRecentValid(1L, 1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/locations/device/1/latest"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest-with-address returns the latest location with address.
     */
    @Test
    void latestWithAddress_exists_returns200() throws Exception {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(1L);
        dl.setImei("IMEI001");
        dl.setLatitude(BigDecimal.valueOf(39.9));
        dl.setLongitude(BigDecimal.valueOf(116.4));
        when(locationRepo.listRecentValid(1L, 1)).thenReturn(List.of(dl));
        when(tiandituLocationService.regeoAddress(39.9, 116.4)).thenReturn("\u5317\u4eac\u5e02\u4e1c\u57ce\u533a");

        mockMvc.perform(get("/api/locations/device/1/latest-with-address"))
                .andExpect(status().isOk());
    }

    @Test
    void range_acceptsSpaceSeparatedDateTimeFromDatePicker() throws Exception {
        when(locationRepo.listByRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/locations/device/1/range")
                        .param("start", "2026-06-03 00:00:00")
                        .param("end", "2026-06-03 23:59:59")
                        .param("limit", "20")
                        .param("offset", "0"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/locations/device/{deviceId}/latest-with-address skips invalid 0,0 coordinates.
     */
    @Test
    void latestWithAddress_skipsZeroZeroLocation() throws Exception {
        DeviceLocation invalid = new DeviceLocation();
        invalid.setDeviceId(1L);
        invalid.setLatitude(BigDecimal.ZERO);
        invalid.setLongitude(BigDecimal.ZERO);
        invalid.setAddress("[]");

        DeviceLocation valid = new DeviceLocation();
        valid.setDeviceId(1L);
        valid.setImei("IMEI001");
        valid.setLatitude(BigDecimal.valueOf(30.886866));
        valid.setLongitude(BigDecimal.valueOf(103.594415));
        valid.setAddress("\u56db\u5ddd\u7701\u6210\u90fd\u5e02\u90fd\u6c5f\u5830\u5e02\u9752\u57ce\u5c71\u9547\u6210\u90fd\u4e1c\u8f6f\u5b66\u9662C5\u5ea7");

        when(locationRepo.listRecentValid(1L, 1)).thenReturn(List.of(valid));

        mockMvc.perform(get("/api/locations/device/1/latest-with-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(30.886866))
                .andExpect(jsonPath("$.data.longitude").value(103.594415))
                .andExpect(jsonPath("$.data.address").value("\u56db\u5ddd\u7701\u6210\u90fd\u5e02\u90fd\u6c5f\u5830\u5e02\u9752\u57ce\u5c71\u9547\u6210\u90fd\u4e1c\u8f6f\u5b66\u9662C5\u5ea7"));
    }

    @Test
    void latestWithAddress_returnsLatestRecordWhenOnlyInvalidCoordinateExists() throws Exception {
        DeviceLocation invalid = new DeviceLocation();
        invalid.setDeviceId(15L);
        invalid.setImei("355932600124999");
        invalid.setLatitude(BigDecimal.ZERO);
        invalid.setLongitude(BigDecimal.ZERO);
        invalid.setBatteryLevel(53);
        invalid.setSource("lbs");

        when(locationRepo.listRecent(15L, 20, 0)).thenReturn(List.of(invalid));
        when(locationRepo.listRecent(15L, 1, 0)).thenReturn(List.of(invalid));

        mockMvc.perform(get("/api/locations/device/15/latest-with-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryLevel").value(53))
                .andExpect(jsonPath("$.data.latitude").value(0))
                .andExpect(jsonPath("$.data.longitude").value(0));
    }

    @Test
    void latestWithAddress_usesDeviceStatusWhenLocationRecordsAreMissing() throws Exception {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(15L);
        status.setImei("355932600124999");
        status.setLastLatitude(30.886817);
        status.setLastLongitude(103.594445);
        status.setLastLocationTime(new Date());
        status.setBatteryLevel(61);

        when(locationRepo.listRecentValid(15L, 1)).thenReturn(Collections.emptyList());
        when(locationRepo.listRecent(15L, 1, 0)).thenReturn(Collections.emptyList());
        when(deviceStatusRepo.findById(15L)).thenReturn(Optional.of(status));
        when(tiandituLocationService.regeoAddress(30.886817, 103.594445))
                .thenReturn("四川省成都市都江堰市青城山镇成都东软学院");

        mockMvc.perform(get("/api/locations/device/15/latest-with-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(30.886817))
                .andExpect(jsonPath("$.data.longitude").value(103.594445))
                .andExpect(jsonPath("$.data.batteryLevel").value(61))
                .andExpect(jsonPath("$.data.source").value("device-status"));
    }

    /**
     * GET /api/locations/search resolves an address.
     */
    @Test
    void search_returnsResult() throws Exception {
        when(tiandituLocationService.addressToLocation("\u5317\u4eac")).thenReturn(Map.of("lat", 39.9, "lng", 116.4));

        mockMvc.perform(get("/api/locations/search")
                        .param("address", "\u5317\u4eac"))
                .andExpect(status().isOk());
    }
}
