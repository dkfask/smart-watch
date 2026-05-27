package com.example.demo.controller.api;

import com.example.demo.model.dto.FenceCreateRequest;
import com.example.demo.model.dto.FenceDto;
import com.example.demo.model.dto.FenceUpdateRequest;
import com.example.demo.repository.GeoFenceRepository;
import com.example.demo.service.FenceService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GeoFenceController集成测试（MockMvc）
 */
@WebMvcTest(value = GeoFenceController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class GeoFenceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private GeoFenceRepository fenceRepo;
    @MockBean private FenceService fenceService;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/fences 创建围栏
     */
    @Test
    void create_returns201() throws Exception {
        when(fenceService.createFence(any(FenceCreateRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/api/fences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"围栏1\",\"type\":\"circle\",\"centerLat\":39.9,\"centerLng\":116.4,\"radius\":500,\"status\":\"active\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * PUT /api/fences/{id} 更新围栏成功
     */
    @Test
    void update_success_returns200() throws Exception {
        when(fenceService.updateFence(eq(1L), any(FenceUpdateRequest.class))).thenReturn(true);

        mockMvc.perform(put("/api/fences/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新围栏\",\"type\":\"circle\"}"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/fences/{id} 更新不存在返回404
     */
    @Test
    void update_notFound_returns404() throws Exception {
        when(fenceService.updateFence(eq(999L), any(FenceUpdateRequest.class))).thenReturn(false);

        mockMvc.perform(put("/api/fences/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"不存在\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/fences/{id} 获取围栏详情
     */
    @Test
    void get_exists_returns200() throws Exception {
        FenceDto dto = new FenceDto();
        dto.setId(1L);
        dto.setName("围栏1");
        when(fenceService.getFenceDetail(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/fences/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/fences/{id} 不存在返回404
     */
    @Test
    void get_notExists_returns404() throws Exception {
        when(fenceService.getFenceDetail(999L)).thenReturn(null);

        mockMvc.perform(get("/api/fences/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/fences 获取围栏列表
     */
    @Test
    void list_returnsFenceList() throws Exception {
        FenceDto dto = new FenceDto();
        dto.setId(1L);
        dto.setName("围栏1");
        when(fenceRepo.listAll()).thenReturn(Collections.emptyList());
        when(fenceService.listFencesWithPatients(anyList())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/fences"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/fences/active 获取活跃围栏
     */
    @Test
    void active_returnsActiveFences() throws Exception {
        when(fenceRepo.listActive()).thenReturn(Collections.emptyList());
        when(fenceService.listFencesWithPatients(anyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/fences/active"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/fences/by-user/{userId} 按用户获取围栏
     */
    @Test
    void byUser_returnsUserFences() throws Exception {
        when(fenceRepo.listByUser(1L)).thenReturn(Collections.emptyList());
        when(fenceService.listFencesWithPatients(anyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/fences/by-user/1"))
                .andExpect(status().isOk());
    }

    /**
     * DELETE /api/fences/{id} 删除围栏
     */
    @Test
    void delete_success_returns204() throws Exception {
        when(fenceService.deleteFence(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/fences/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * DELETE /api/fences/{id} 删除不存在返回404
     */
    @Test
    void delete_notFound_returns404() throws Exception {
        when(fenceService.deleteFence(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/fences/999"))
                .andExpect(status().isNotFound());
    }
}
