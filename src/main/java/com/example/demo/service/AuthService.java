package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public long registerNewUser(String username, String rawPassword, String email) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        String uname = username.trim();
        Optional<User> exists = userRepository.findByUsername(uname);
        if (exists.isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(uname);
        u.setEmail(StringUtils.hasText(email) ? email.trim() : null);
        u.setPassword_hash(passwordEncoder.encode(rawPassword));
        return userRepository.create(u);
    }
}
