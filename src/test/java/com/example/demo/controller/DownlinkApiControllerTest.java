package com.example.demo.controller;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.LocationRecord;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.LocationRecordRepository;
import com.example.demo.socket.downlink.DownlinkManager;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DownlinkApiController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class DownlinkApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DownlinkManager downlinkManager;

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private DeviceStatusRepository deviceStatusRepository;

    @MockBean
    private LocationRecordRepository locationRecordRepository;

    @MockBean
    private RateLimiter rateLimiter;

    @Test
    void batteryReport_returnsDeviceStatusAndLatestLocation() throws Exception {
        Device device = new Device();
        device.setId(15L);
        device.setImei("355932600124999");

        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(15L);
        status.setBatteryLevel(87);
        status.setLastLocationTime(new Date());

        LocationRecord record = new LocationRecord();
        record.setImei("355932600124999");
        record.setLatitude(30.886866);
        record.setLongitude(103.594415);
        record.setRecvTime(new Date());

        when(deviceRepository.findByImei("355932600124999")).thenReturn(Optional.of(device));
        when(deviceStatusRepository.findById(15L)).thenReturn(Optional.of(status));
        when(locationRecordRepository.findByImeiOrderByRecvTimeDesc("355932600124999")).thenReturn(List.of(record));
        when(downlinkManager.getOnlineImeis()).thenReturn(Set.of("355932600124999"));

        mockMvc.perform(get("/api/downlink/battery-report").param("imei", "355932600124999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batteryLevel").value(87))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.latestLocation.latitude").value(30.886866));
    }

    @Test
    void logs_returnsEmptyLogPayloadWhenNoFilesExist() throws Exception {
        mockMvc.perform(get("/api/downlink/logs")
                        .param("imei", "355932600124999")
                        .param("startTime", "2026-06-01T00:00:00Z")
                        .param("endTime", "2026-06-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void historyTrack_returnsLocationRecordsByImei() throws Exception {
        LocationRecord record = new LocationRecord();
        record.setImei("355932600124999");
        record.setLatitude(30.886866);
        record.setLongitude(103.594415);
        record.setRecvTime(Date.from(Instant.parse("2026-06-03T08:00:00Z")));
        when(locationRecordRepository.findByImeiOrderByRecvTimeDesc("355932600124999")).thenReturn(List.of(record));

        mockMvc.perform(get("/api/downlink/history-track")
                        .param("imei", "355932600124999")
                        .param("startTime", "2026-06-01T00:00:00Z")
                        .param("endTime", "2026-06-30T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[0].latitude").value(30.886866));
    }

    @Test
    void exportStatusHistory_returnsCsvAttachment() throws Exception {
        Device device = new Device();
        device.setId(15L);
        device.setImei("355932600124999");

        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(15L);
        status.setBatteryLevel(88);

        when(deviceRepository.findByImei("355932600124999")).thenReturn(Optional.of(device));
        when(deviceStatusRepository.findById(15L)).thenReturn(Optional.of(status));
        when(locationRecordRepository.findByImeiOrderByRecvTimeDesc("355932600124999")).thenReturn(List.of());

        mockMvc.perform(get("/api/downlink/export-status-history")
                        .param("imei", "355932600124999")
                        .param("startTime", "2026-06-01T00:00:00Z")
                        .param("endTime", "2026-06-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"device_355932600124999_status_history.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IMEI")));
    }
}
