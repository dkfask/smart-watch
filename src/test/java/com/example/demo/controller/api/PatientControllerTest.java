package com.example.demo.controller.api;

import com.example.demo.model.Patient;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.PatientRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PatientController集成测试（MockMvc）
 */
@WebMvcTest(value = PatientController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PatientRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/patients 创建病人
     */
    @Test
    void create_returns201() throws Exception {
        Patient saved = new Patient();
        saved.setId(1L);
        saved.setName("张三");
        when(repo.save(any(Patient.class))).thenReturn(saved);

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * GET /api/patients 分页查询
     */
    @Test
    void list_returnsPagedResult() throws Exception {
        Patient p = new Patient();
        p.setId(1L);
        p.setName("张三");
        Page<Patient> page = new PageImpl<>(List.of(p));
        when(repo.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/patients")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/patients/{id} 获取病人详情
     */
    @Test
    void get_exists_returns200() throws Exception {
        Patient p = new Patient();
        p.setId(1L);
        p.setName("张三");
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/patients/{id} 不存在返回404
     */
    @Test
    void get_notExists_returns404() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /api/patients/{id} 更新病人
     */
    @Test
    void update_exists_returns200() throws Exception {
        Patient p = new Patient();
        p.setId(1L);
        p.setName("李四");
        when(repo.existsById(1L)).thenReturn(true);
        when(repo.save(any(Patient.class))).thenReturn(p);

        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"李四\"}"))
                .andExpect(status().isOk());
    }

    /**
     * DELETE /api/patients/{id} 删除病人
     */
    @Test
    void delete_exists_returns204() throws Exception {
        when(repo.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/patients/1"))
                .andExpect(status().isNoContent());
    }
}
