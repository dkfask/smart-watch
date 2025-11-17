package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * REST 风格的认证 API（用于前端 SPA 与后端整合）
 *
 * 说明（中文注释）：
 * - 原先该控制器的类级别映射为 `/api`，但项目中另有 `ApiAuthController` 也定义了 `/api/login`，
 *   导致在启动时出现 "Ambiguous mapping" 的错误（两个 bean 同时映射到 POST /api/login）。
 * - 为避免冲突并最小改动，我把本控制器的基路径改为 `/api/auth`，因此本控制器的端点为：
 *     POST /api/auth/login
 *     GET  /api/auth/me
 *     POST /api/auth/logout
 * - 如果你的前端/调用方期望仍使用 `/api/login`，请把调用方改为 `/api/auth/login`，或者选择合并两处逻辑。
 * - 保留了原方法行为（使用 AuthenticationManager 进行认证并把 SecurityContext 存入 HttpSession）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;

    public AuthApiController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // DTO 简单内部类：接收前端发送的登录 JSON
    public static class LoginRequest {
        public String username;
        public String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * REST 登录接口（适配前端 SPA）
     * - 请求：POST /api/auth/login
     * - Body: { "username": "...", "password": "..." }
     * - 返回：200 + { username: "..." }（认证成功），401（认证失败）
     */
    @PostMapping("/login")
    public ResponseEntity<?> apiLogin(@RequestBody LoginRequest body, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword());
            Authentication auth = authenticationManager.authenticate(token);

            // 将认证信息放入 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 把 SecurityContext 存入 HttpSession（创建 session）以维持后续请求的会话
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            Map<String, Object> resp = new HashMap<>();
            resp.put("username", auth.getName());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "认证失败：" + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
    }

    /**
     * 获取当前登录用户信息（简单示例）
     * - GET /api/auth/me
     * - 返回 200 + { username: '...' } 或 401
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "unauthenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("username", principal.getName());
        return ResponseEntity.ok(resp);
    }

    /**
     * SPA 友好的退出接口（会话失效并清理 SecurityContext）
     * - POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> apiLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        Map<String, Object> resp = new HashMap<>();
        resp.put("logout", true);
        return ResponseEntity.ok(resp);
    }
}
