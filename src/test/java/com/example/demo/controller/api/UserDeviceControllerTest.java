package com.example.demo.controller.api;

import com.example.demo.model.UserDevice;
import com.example.demo.repository.UserDeviceRepository;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserDeviceController集成测试（MockMvc）
 */
@WebMvcTest(value = UserDeviceController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class UserDeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserDeviceRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/user-devices 绑定
     */
    @Test
    void bind_returns201() throws Exception {
        when(repo.bind(1L, 1L, "owner")).thenReturn(1L);

        mockMvc.perform(post("/api/user-devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"deviceId\":1,\"relationship\":\"owner\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * DELETE /api/user-devices 解绑
     */
    @Test
    void unbind_success_returns204() throws Exception {
        when(repo.unbind(1L, 1L)).thenReturn(1);

        mockMvc.perform(delete("/api/user-devices")
                        .param("userId", "1")
                        .param("deviceId", "1"))
                .andExpect(status().isNoContent());
    }

    /**
     * GET /api/user-devices/by-user/{userId} 按用户查询
     */
    @Test
    void byUser_returnsList() throws Exception {
        when(repo.findByUser(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user-devices/by-user/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/user-devices/by-device/{deviceId} 按设备查询
     */
    @Test
    void byDevice_returnsList() throws Exception {
        when(repo.findByDevice(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user-devices/by-device/1"))
                .andExpect(status().isOk());
    }
}
