package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthService {

    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 默认构造函数，用于Spring创建实例
    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // Setter注入，允许userRepository为null
    @Autowired(required = false)
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public long registerNewUser(String username, String rawPassword, String email) {
        if (userRepository == null) {
            throw new IllegalStateException("用户仓库未初始化，无法注册新用户");
        }
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
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole("user");
        u.setStatus("active");
        User saved = userRepository.save(u);
        return saved.getId();
    }
}
