package com.example.demo.controller.api;

import com.example.demo.model.FenceAlert;
import com.example.demo.repository.FenceAlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FenceAlertController集成测试（MockMvc）
 */
@WebMvcTest(value = FenceAlertController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class FenceAlertControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private FenceAlertRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * GET /api/alerts/by-device/{deviceId} 获取设备报警
     */
    @Test
    void byDevice_returnsAlertList() throws Exception {
        when(repo.findByDeviceId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/alerts/by-device/1"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alerts/{alertId}/read 标记已读
     */
    @Test
    void markRead_exists_returns200() throws Exception {
        FenceAlert alert = new FenceAlert();
        alert.setId(1L);
        alert.setStatus("pending");
        when(repo.findById(1L)).thenReturn(Optional.of(alert));
        when(repo.save(any(FenceAlert.class))).thenReturn(alert);

        mockMvc.perform(put("/api/alerts/1/read")
                        .param("read", "true"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/alerts/{alertId}/read 不存在返回404
     */
    @Test
    void markRead_notExists_returns404() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/alerts/999/read")
                        .param("read", "true"))
                .andExpect(status().isNotFound());
    }
}
