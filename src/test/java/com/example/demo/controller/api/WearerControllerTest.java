package com.example.demo.controller.api;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.model.dto.WearerDto;
import com.example.demo.service.WearerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = WearerController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class WearerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WearerService wearerService;
    @MockBean private com.google.common.util.concurrent.RateLimiter rateLimiter;

    @Test
    void getWearers_returns200() throws Exception {
        PageResponse<WearerDto> pr = new PageResponse<>();
        when(wearerService.getWearers(0, 20)).thenReturn(pr);
        mockMvc.perform(get("/api/wearers"))
                .andExpect(status().isOk());
    }

    @Test
    void getWearer_exists_returns200() throws Exception {
        WearerDto dto = new WearerDto(1L, "Alice", "123", null);
        when(wearerService.getWearer(1L)).thenReturn(dto);
        mockMvc.perform(get("/api/wearers/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getWearer_notFound_throwsException() throws Exception {
        when(wearerService.getWearer(999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/wearers/999"))
                .andExpect(status().is(org.hamcrest.Matchers.not(200)));
    }


    @Test
    void createWearer_returns200() throws Exception {
        WearerDto dto = new WearerDto(1L, "Bob", "456", null);
        when(wearerService.createWearer(any())).thenReturn(dto);
        mockMvc.perform(post("/api/wearers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"phone\":\"456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateWearer_returns200() throws Exception {
        WearerDto dto = new WearerDto(1L, "Bob Updated", "456", null);
        when(wearerService.updateWearer(eq(1L), any())).thenReturn(dto);
        mockMvc.perform(put("/api/wearers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWearer_returns204() throws Exception {
        mockMvc.perform(delete("/api/wearers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void assignDevice_returns200() throws Exception {
        WearerDto dto = new WearerDto(1L, "Alice", "123", 5L);
        when(wearerService.assignDevice(eq(1L), eq(5L))).thenReturn(dto);
        mockMvc.perform(post("/api/wearers/1/assign-device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void unassignDevice_returns200() throws Exception {
        WearerDto dto = new WearerDto(1L, "Alice", "123", null);
        when(wearerService.unassignDevice(1L)).thenReturn(dto);
        mockMvc.perform(post("/api/wearers/1/unassign-device"))
                .andExpect(status().isOk());
    }

    @Test
    void getWearerByDeviceId_returns200() throws Exception {
        WearerDto dto = new WearerDto(2L, "Carol", "789", 10L);
        when(wearerService.getWearerByDeviceId(10L)).thenReturn(dto);
        mockMvc.perform(get("/api/wearers/by-device/10"))
                .andExpect(status().isOk());
    }
}
