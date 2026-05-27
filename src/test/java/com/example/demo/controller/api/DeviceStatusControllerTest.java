package com.example.demo.controller.api;

import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceStatusRepository;
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

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DeviceStatusController集成测试（MockMvc）
 */
@WebMvcTest(value = DeviceStatusController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class DeviceStatusControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DeviceStatusRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * GET /api/status/{deviceId} 获取设备状态
     */
    @Test
    void get_exists_returns200() throws Exception {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(1L);
        status.setIsOnline(true);
        when(repo.findById(1L)).thenReturn(Optional.of(status));

        mockMvc.perform(get("/api/status/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/status/{deviceId} 不存在返回404
     */
    @Test
    void get_notExists_returns404() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/status/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /api/status/{deviceId}/online 设置在线状态
     */
    @Test
    void setOnline_success_returns200() throws Exception {
        when(repo.setOnline(1L, true)).thenReturn(1);

        mockMvc.perform(put("/api/status/1/online")
                        .param("online", "true"))
                .andExpect(status().isOk());
    }
}
