package com.example.demo.controller.api;

import com.example.demo.model.PatientDevice;
import com.example.demo.repository.PatientDeviceRepository;
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
 * PatientDeviceController集成测试（MockMvc）
 */
@WebMvcTest(value = PatientDeviceController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class PatientDeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PatientDeviceRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/patient-devices 关联设备和病人
     */
    @Test
    void bind_newAssociation_returns200() throws Exception {
        when(repo.findByPatientIdAndDeviceId(1L, 1L)).thenReturn(null);
        when(repo.findByPatientId(1L)).thenReturn(Collections.emptyList());
        when(repo.findByDeviceId(1L)).thenReturn(Collections.emptyList());
        PatientDevice saved = new PatientDevice();
        saved.setPatientId(1L);
        saved.setDeviceId(1L);
        when(repo.save(any(PatientDevice.class))).thenReturn(saved);

        mockMvc.perform(post("/api/patient-devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"deviceId\":1}"))
                .andExpect(status().isOk());
    }

    /**
     * DELETE /api/patient-devices 解除关联
     */
    @Test
    void unbind_success_returns204() throws Exception {
        when(repo.unbindDeviceFromPatient(1L, 1L)).thenReturn(1);

        mockMvc.perform(delete("/api/patient-devices")
                        .param("patientId", "1")
                        .param("deviceId", "1"))
                .andExpect(status().isNoContent());
    }

    /**
     * GET /api/patient-devices/by-patient/{patientId} 获取病人关联设备
     */
    @Test
    void byPatient_returnsList() throws Exception {
        when(repo.findByPatientId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/patient-devices/by-patient/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/patient-devices/by-device/{deviceId} 获取设备关联病人
     */
    @Test
    void byDevice_returnsList() throws Exception {
        when(repo.findByDeviceId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/patient-devices/by-device/1"))
                .andExpect(status().isOk());
    }
}
