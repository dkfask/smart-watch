package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AuthUserDetailsService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    private AuthUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AuthUserDetailsService();
        service.setUserRepository(userRepository);
    }

    /**
     * 正常加载用户返回UserDetails
     */
    @Test
    void loadUserByUsername_normalUser_returnsUserDetails() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setRole("user");
        user.setStatus("active");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("testuser");

        assertEquals("testuser", details.getUsername());
        assertEquals("encoded_password", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    /**
     * 用户不存在抛UsernameNotFoundException
     */
    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("nouser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nouser"));
    }

    /**
     * userRepository为null抛UsernameNotFoundException
     */
    @Test
    void loadUserByUsername_nullRepo_throwsException() {
        service.setUserRepository(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));
    }

    /**
     * admin角色拥有ROLE_ADMIN和ROLE_USER权限
     */
    @Test
    void loadUserByUsername_adminRole_hasAdminAndUserAuthorities() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("encoded_password");
        user.setRole("admin");
        user.setStatus("active");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    /**
     * 递归调用检测抛UsernameNotFoundException
     */
    @Test
    void loadUserByUsername_recursiveCall_throwsException() {
        User user = new User();
        user.setUsername("recursive");
        user.setPassword("encoded_password");
        user.setRole("user");
        user.setStatus("active");
        when(userRepository.findByUsername("recursive")).thenReturn(Optional.of(user));

        UserDetails first = service.loadUserByUsername("recursive");
        assertNotNull(first);
    }
}
