package com.example.demo.controller.api;

import com.example.demo.model.HealthAlertThreshold;
import com.example.demo.model.dto.HealthAlertThresholdDto;
import com.example.demo.repository.HealthAlertThresholdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = HealthAlertThresholdController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class HealthAlertThresholdControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private HealthAlertThresholdRepository repo;
    @MockBean private com.google.common.util.concurrent.RateLimiter rateLimiter;

    @Test
    void getThresholds_returns200WithList() throws Exception {
        HealthAlertThreshold entity = new HealthAlertThreshold();
        entity.setDataType("heart_rate");
        entity.setMinValue(60.0);
        entity.setMaxValue(100.0);
        entity.setEnabled(true);
        when(repo.findAll()).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/health-alert-thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dataType").value("heart_rate"))
                .andExpect(jsonPath("$.data[0].minValue").value(60.0))
                .andExpect(jsonPath("$.data[0].maxValue").value(100.0));
    }

    @Test
    void getThresholds_empty_returnsEmptyList() throws Exception {
        when(repo.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/health-alert-thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void updateThresholds_upsertsAndReturns200() throws Exception {
        HealthAlertThreshold saved = new HealthAlertThreshold();
        saved.setDataType("temperature");
        saved.setMinValue(36.0);
        saved.setMaxValue(37.5);
        saved.setEnabled(true);
        when(repo.findByDataType("temperature")).thenReturn(java.util.Optional.of(new HealthAlertThreshold()));
        when(repo.save(any(HealthAlertThreshold.class))).thenReturn(saved);

        String body = "[{\"dataType\":\"temperature\",\"minValue\":36.0,\"maxValue\":37.5,\"enabled\":true}]";
        mockMvc.perform(put("/api/health-alert-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dataType").value("temperature"));
    }
}