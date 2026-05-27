package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    @Bean
    public CommandLineRunner initDefaultUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            try {
                ensureUser(userRepository, encoder, "admin", "admin123", "admin@example.com", true);
                ensureUser(userRepository, encoder, "user", "user123", "user@example.com", false);
            } catch (Exception e) {
                log.warn("Failed to initialize default users, database connection might be unavailable: {}", e.getMessage());
                log.debug("Detailed error:", e);
            }
        };
    }

    /**
     * 确保默认用户存在，不存在则创建；已存在则更新角色
     * @param userRepository 用户仓库
     * @param encoder 密码编码器
     * @param username 用户名
     * @param rawPassword 原始密码
     * @param email 邮箱
     * @param mustChangePassword 是否必须修改密码
     */
    private void ensureUser(UserRepository userRepository, PasswordEncoder encoder,
                             String username, String rawPassword, String email,
                             boolean mustChangePassword) {
        try {
            var existing = userRepository.findByUsername(username);
            if (existing.isEmpty()) {
                User u = new User();
                u.setUsername(username);
                u.setEmail(email);
                u.setPassword(encoder.encode(rawPassword));
                u.setRole(username.equals("admin") ? "admin" : "user");
                u.setStatus("active");
                u.setMustChangePassword(mustChangePassword);
                User savedUser = userRepository.save(u);
                log.info("Initialized default user '{}' (id={})", username, savedUser.getId());
            } else {
                User u = existing.get();
                String expectedRole = username.equals("admin") ? "admin" : "user";
                if (!expectedRole.equals(u.getRole())) {
                    u.setRole(expectedRole);
                    userRepository.save(u);
                    log.info("Updated user '{}' role to '{}'", username, expectedRole);
                } else {
                    log.debug("Default user '{}' already exists", username);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to ensure user '{}': {}", username, e.getMessage());
        }
    }
}
