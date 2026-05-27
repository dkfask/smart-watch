package com.example.demo.controller.api;

import com.example.demo.model.Alert;
import com.example.demo.model.Alarm;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.service.AlertService;
import com.example.demo.service.AlarmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlarmController集成测试（MockMvc）
 */
@WebMvcTest(value = AlarmController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class AlarmControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AlarmService alarmService;
    @MockBean private AlertService alertService;
    @MockBean private RateLimiter rateLimiter;

    /**
     * GET /api/alarms/stats 获取统计数据
     */
    @Test
    void stats_returnsCorrectData() throws Exception {
        AlarmStatsDto stats = new AlarmStatsDto();
        stats.setTotal(100);
        stats.setPending(30);
        stats.setHandled(60);
        stats.setFalseAlarm(10);
        stats.setUnread(25);
        when(alertService.getAlertStats()).thenReturn(stats);

        mockMvc.perform(get("/api/alarms/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.pending").value(30))
                .andExpect(jsonPath("$.data.unread").value(25));
    }

    /**
     * GET /api/alarms/{id} 获取报警详情
     */
    @Test
    void getAlarm_exists_returns200() throws Exception {
        Alert alert = new Alert();
        alert.setId(1L);
        when(alertService.getAlertWithRelations(1L)).thenReturn(Optional.of(alert));

        mockMvc.perform(get("/api/alarms/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/alarms/{id} 不存在
     */
    @Test
    void getAlarm_notExists_returns404() throws Exception {
        when(alertService.getAlertWithRelations(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/alarms/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /api/alarms/{id}/handle 处理报警（JSON body）
     */
    @Test
    void handleAlarm_success_returns200() throws Exception {
        when(alertService.handleAlert(eq(1L), eq("handled"), eq("已处理"), isNull())).thenReturn(true);

        mockMvc.perform(put("/api/alarms/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"handled\",\"result\":\"已处理\"}"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alarms/{id}/handle 不存在返回404
     */
    @Test
    void handleAlarm_notFound_returns404() throws Exception {
        when(alertService.handleAlert(eq(999L), anyString(), any(), any())).thenReturn(false);

        mockMvc.perform(put("/api/alarms/999/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"handled\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/alarms/unread-count 获取未读数量
     */
    @Test
    void unreadCount_returnsCount() throws Exception {
        when(alertService.getUnreadAlertCount()).thenReturn(5L);

        mockMvc.perform(get("/api/alarms/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(5));
    }

    /**
     * GET /api/alarms/unread-count 按设备查询
     */
    @Test
    void unreadCount_byDevice_returnsCount() throws Exception {
        when(alertService.getUnreadAlertCountByDeviceId(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/alarms/unread-count")
                        .param("deviceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    /**
     * GET /api/alarms 按设备ID查询
     */
    @Test
    void list_byDeviceId_returnsPagedResult() throws Exception {
        Page<Alert> page = new PageImpl<>(List.of());
        when(alertService.getAlertsWithRelations(any())).thenReturn(page);

        mockMvc.perform(get("/api/alarms")
                        .param("deviceId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/alarms 按状态查询
     */
    @Test
    void list_byStatus_returnsPagedResult() throws Exception {
        Page<Alert> page = new PageImpl<>(List.of());
        when(alertService.getAlertsWithRelations(any())).thenReturn(page);

        mockMvc.perform(get("/api/alarms")
                        .param("status", "pending")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alarms/{id}/read 标记已读
     */
    @Test
    void markRead_success_returns200() throws Exception {
        when(alertService.markAlertAsRead(1L, 1)).thenReturn(true);

        mockMvc.perform(put("/api/alarms/1/read")
                        .param("read", "true"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/alarms/recent 获取最近报警
     */
    @Test
    void recent_returnsRecentAlarms() throws Exception {
        when(alertService.getRecentAlertsWithRelations(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/alarms/recent")
                        .param("limit", "10"))
                .andExpect(status().isOk());
    }

    /**
     * POST /api/alarms 创建报警
     */
    @Test
    void create_success_returns201() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setId(1L);
        alarm.setAlarmType("sos");
        when(alarmService.createAlarm(any(Alarm.class))).thenReturn(alarm);

        mockMvc.perform(post("/api/alarms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alarmType\":\"sos\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * GET /api/alarms/device/{deviceId} 按设备查询报警
     */
    @Test
    void byDevice_returnsPagedResult() throws Exception {
        Page<Alert> page = new PageImpl<>(List.of());
        when(alertService.getAlertsWithRelations(any())).thenReturn(page);

        mockMvc.perform(get("/api/alarms/device/1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/alarms/patient/{patientId} 按病人查询报警
     */
    @Test
    void byPatient_returnsPagedResult() throws Exception {
        Page<Alert> page = new PageImpl<>(List.of());
        when(alertService.getAlertsWithRelations(any())).thenReturn(page);

        mockMvc.perform(get("/api/alarms/patient/10")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alarms/device/{deviceId}/read 批量标记设备报警已读
     */
    @Test
    void markDeviceAlarmsRead_success_returns200() throws Exception {
        when(alertService.markDeviceAlertsAsRead(1L, 1)).thenReturn(true);

        mockMvc.perform(put("/api/alarms/device/1/read")
                .param("read", "true"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alarms/patient/{patientId}/read 批量标记病人报警已读
     */
    @Test
    void markPatientAlarmsRead_success_returns200() throws Exception {
        when(alertService.markPatientAlertsAsRead(10L, 1)).thenReturn(true);

        mockMvc.perform(put("/api/alarms/patient/10/read")
                .param("read", "true"))
                .andExpect(status().isOk());
    }
}
