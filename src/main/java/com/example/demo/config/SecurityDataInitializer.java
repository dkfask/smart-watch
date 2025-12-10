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
                ensureUser(userRepository, encoder, "admin", "admin123", "admin@example.com");
                ensureUser(userRepository, encoder, "user", "user123", "user@example.com");
            } catch (Exception e) {
                log.warn("Failed to initialize default users, database connection might be unavailable: {}", e.getMessage());
                log.debug("Detailed error:", e);
            }
        };
    }

    private void ensureUser(UserRepository userRepository, PasswordEncoder encoder,
                             String username, String rawPassword, String email) {
        try {
            if (userRepository.findByUsername(username).isEmpty()) {
                User u = new User();
                u.setUsername(username);
                u.setEmail(email);
                u.setPassword(encoder.encode(rawPassword));
                u.setRole("user");
                u.setStatus("active");
                User savedUser = userRepository.save(u);
                log.info("Initialized default user '{}' (id={})");
            } else {
                log.debug("Default user '{}' already exists");
            }
        } catch (Exception e) {
            log.warn("Failed to ensure user '{}': {}");
        }
    }
}
