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
            ensureUser(userRepository, encoder, "admin", "admin123", "admin@example.com");
            ensureUser(userRepository, encoder, "user", "user123", "user@example.com");
        };
    }

    private void ensureUser(UserRepository userRepository, PasswordEncoder encoder,
                             String username, String rawPassword, String email) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPassword_hash(encoder.encode(rawPassword));
            long id = userRepository.create(u);
            log.info("Initialized default user '{}' (id={})", username, id);
        } else {
            log.debug("Default user '{}' already exists", username);
        }
    }
}
