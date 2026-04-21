package com.example.demo.controller.api;

import com.example.demo.model.dto.DeviceInfoDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.service.DeviceService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DeviceController集成测试（MockMvc）
 */
@WebMvcTest(value = DeviceController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DeviceService deviceService;
    @MockBean private RateLimiter rateLimiter;

    /**
     * GET /api/devices/{id} 获取设备详情
     */
    @Test
    void getDevice_exists_returns200() throws Exception {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.setId(1L);
        dto.setImei("IMEI001");
        when(deviceService.getDeviceById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.imei").value("IMEI001"));
    }

    /**
     * GET /api/devices/{id} 不存在返回404
     */
    @Test
    void getDevice_notExists_returns404() throws Exception {
        when(deviceService.getDeviceById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/devices/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/devices 分页查询（page/size参数）
     */
    @Test
    void listDevices_pageSizeParams_returnsPagedResult() throws Exception {
        PageResponse<DeviceInfoDto> page = new PageResponse<>(List.of(), 0, 0, 20);
        when(deviceService.listDevices(eq(0), eq(20), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/devices")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    /**
     * GET /api/devices 分页查询（limit/offset参数兼容）
     */
    @Test
    void listDevices_limitOffsetParams_returnsPagedResult() throws Exception {
        PageResponse<DeviceInfoDto> page = new PageResponse<>(List.of(), 0, 0, 20);
        when(deviceService.listDevices(eq(0), eq(20), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/devices")
                        .param("limit", "20")
                        .param("offset", "0"))
                .andExpect(status().isOk());
    }

    /**
     * POST /api/devices 创建设备
     */
    @Test
    void createDevice_returns201() throws Exception {
        when(deviceService.createDevice(any())).thenReturn(1L);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imei\":\"IMEI001\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * PUT /api/devices/{id} 更新设备不存在返回404
     */
    @Test
    void updateDevice_notExists_returns404() throws Exception {
        when(deviceService.existsById(999L)).thenReturn(false);

        mockMvc.perform(put("/api/devices/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imei\":\"IMEI001\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * DELETE /api/devices/{id} 不存在返回404
     */
    @Test
    void deleteDevice_notExists_returns404() throws Exception {
        when(deviceService.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/devices/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /api/devices/{id} 更新设备成功
     */
    @Test
    void updateDevice_exists_returns200() throws Exception {
        when(deviceService.existsById(1L)).thenReturn(true);

        mockMvc.perform(put("/api/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imei\":\"IMEI001\"}"))
                .andExpect(status().isOk());
    }

    /**
     * DELETE /api/devices/{id} 删除设备成功
     */
    @Test
    void deleteDevice_exists_returns204() throws Exception {
        when(deviceService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/devices/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * GET /api/devices 搜索查询
     */
    @Test
    void listDevices_withSearchParam_returnsFilteredResult() throws Exception {
        PageResponse<DeviceInfoDto> page = new PageResponse<>(List.of(), 0, 0, 20);
        when(deviceService.listDevices(eq(0), eq(20), eq("IMEI"))).thenReturn(page);

        mockMvc.perform(get("/api/devices")
                        .param("page", "0")
                        .param("size", "20")
                        .param("search", "IMEI"))
                .andExpect(status().isOk());
    }
}
