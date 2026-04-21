package com.example.demo.controller.api;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController集成测试（MockMvc）
 */
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserRepository repo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * POST /api/users 创建用户
     */
    @Test
    void create_returns201() throws Exception {
        User saved = new User();
        saved.setId(1L);
        saved.setUsername("testuser");
        when(repo.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * GET /api/users/{id} 获取用户
     */
    @Test
    void get_exists_returns200() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setUsername("testuser");
        when(repo.findById(1L)).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk());
    }

    /**
     * GET /api/users 列表查询
     */
    @Test
    void list_returnsUserList() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setUsername("testuser");
        when(repo.findAll()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users")
                        .param("limit", "20")
                        .param("offset", "0"))
                .andExpect(status().isOk());
    }

    /**
     * PUT /api/users/{id} 更新用户
     */
    @Test
    void update_exists_returns200() throws Exception {
        when(repo.existsById(1L)).thenReturn(true);
        User saved = new User();
        saved.setId(1L);
        when(repo.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"updated\"}"))
                .andExpect(status().isOk());
    }

    /**
     * DELETE /api/users/{id} 删除用户
     */
    @Test
    void delete_exists_returns204() throws Exception {
        when(repo.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}
