package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserDetailsService.class);

    private final UserRepository userRepository;

    // 防止 loadUserByUsername 重入导致的递归/StackOverflow（线程本地）
    private static final ThreadLocal<Boolean> LOAD_IN_PROGRESS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public AuthUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 记录进入点与当前线程堆栈深度，帮助诊断递归或意外重入
        int depth = Thread.currentThread().getStackTrace().length;
        logger.debug("loadUserByUsername invoked for username={} (stack depth={})", username, depth);

        if (Boolean.TRUE.equals(LOAD_IN_PROGRESS.get())) {
            // 已经在执行 loadUserByUsername，防止递归导致 StackOverflow
            logger.error("Re-entrant call to loadUserByUsername detected for username={}; aborting to avoid StackOverflow", username);
            throw new UsernameNotFoundException("递归认证检测到，暂时无法加载用户: " + username);
        }

        try {
            LOAD_IN_PROGRESS.set(Boolean.TRUE);

            User u = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

            String passwordHash = u.getPassword_hash();
            if (passwordHash == null || passwordHash.isBlank()) {
                throw new UsernameNotFoundException("用户未设置密码: " + username);
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if ("admin".equalsIgnoreCase(u.getUsername())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return new org.springframework.security.core.userdetails.User(
                    u.getUsername(),
                    passwordHash,
                    true, true, true, true,
                    authorities
            );
        } catch (StackOverflowError soe) {
            // 对 StackOverflowError 做最小化、安全日志，避免在格式化日志时再次触发导致循环
            logger.error("StackOverflowError while loading user={}. Aborting authentication. (no further details to avoid recursion)", username);
            throw new UsernameNotFoundException("内部错误（StackOverflow）: " + username);
        } catch (UsernameNotFoundException unfe) {
            // 直接向上抛出用户名未找到的异常
            throw unfe;
        } catch (Throwable t) {
            // 捕获其他异常，记录简短消息并抛出 UsernameNotFoundException 以使认证失败而不是导致更深层次的错误
            logger.error("Unexpected error in loadUserByUsername for user={}: {}", username, t.getClass().getName());
            throw new UsernameNotFoundException("内部错误，无法加载用户: " + username);
        } finally {
            LOAD_IN_PROGRESS.remove();
        }
    }
}
