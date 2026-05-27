package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    /**
     * 注册成功返回用户ID
     */
    @Test
    void registerNewUser_success_returnsUserId() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        User savedUser = new User();
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        long id = authService.registerNewUser("testuser", "password123", "test@example.com");

        assertEquals(1L, id);
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("testuser") &&
                user.getPassword().equals("encoded_password") &&
                user.getEmail().equals("test@example.com") &&
                user.getRole().equals("user") &&
                user.getStatus().equals("active")
        ));
    }

    /**
     * 用户名已存在抛出异常
     */
    @Test
    void registerNewUser_usernameExists_throwsException() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () ->
                authService.registerNewUser("existing", "password", null));
    }

    /**
     * 空用户名抛出异常
     */
    @Test
    void registerNewUser_emptyUsername_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerNewUser("", "password", null));
    }

    /**
     * 空密码抛出异常
     */
    @Test
    void registerNewUser_emptyPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerNewUser("user", "", null));
    }

    /**
     * 邮箱为空时设为null
     */
    @Test
    void registerNewUser_nullEmail_setsEmailNull() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("enc");
        User saved = new User();
        saved.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.registerNewUser("user1", "pass", null);

        verify(userRepository).save(argThat(user -> user.getEmail() == null));
    }
}
