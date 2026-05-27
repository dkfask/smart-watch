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

    /**
     * 构造函数注入，确保所有依赖在构造时可用
     * @param userRepository 用户仓库
     * @param passwordEncoder 密码编码器
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册新用户
     * @param username 用户名
     * @param rawPassword 原始密码
     * @param email 邮箱
     * @return 新用户ID
     */
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
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole("user");
        u.setStatus("active");
        User saved = userRepository.save(u);
        return saved.getId();
    }
}
