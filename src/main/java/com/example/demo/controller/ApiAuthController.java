package com.example.demo.controller;

import com.example.demo.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供简单的 REST 登录/注册接口，供前端 SPA 使用。
 * 注意：此控制器使用 Session-based 认证（与 Spring Security 默认机制兼容），
 * 在前端请求时需设置 credentials: 'include' 以携带/接收 session cookie（JSESSIONID）。
 */
@RestController
@RequestMapping("/api")
public class ApiAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ApiAuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    // 防止同一线程内递归调用 authenticate 导致 StackOverflow 的线程本地标志
    private static final ThreadLocal<Boolean> AUTH_IN_PROGRESS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ApiAuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户名和密码不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        try {
            long id = authService.registerNewUser(username.trim(), password, email);
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("id", id);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        } catch (Exception ex) {
            logger.error("注册失败", ex);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "注册失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户名或密码为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        // 先检查当前 SecurityContext，避免重复/递归认证导致 StackOverflow
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated() && !(currentAuth instanceof AnonymousAuthenticationToken)) {
            Object principal = currentAuth.getPrincipal();
            // 安全日志：避免直接记录可能触发递归 toString 的对象
            String principalClass = principal == null ? "null" : principal.getClass().getName();
            String principalName = null;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                principalName = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                principalName = (String) principal;
            } else if (principal != null) {
                // 避免调用可能有副作用的 toString()，只截取类名作为标识
                principalName = "<non-string-principal>";
            }
            logger.info("Current thread already has authenticated principalClass={} principalName={}", principalClass, principalName);
            if (principalName != null && principalName.equals(username)) {
                Map<String, Object> ok = new HashMap<>();
                ok.put("success", true);
                ok.put("username", username);
                return ResponseEntity.ok(ok);
            }
            // 如果已认证但与请求用户名不一致，继续往下认证（或视业务返回冲突），此处选择继续尝试认证新用户名
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        // 检查线程本地标志，防止递归认证
        if (Boolean.TRUE.equals(AUTH_IN_PROGRESS.get())) {
            logger.error("Re-entrant authentication detected for user={}; aborting to avoid StackOverflow", username);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "认证过程中检测到递归调用，已中止（请查看服务端日志以获取详细信息）");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        try {
            AUTH_IN_PROGRESS.set(Boolean.TRUE);
            logger.debug("Attempting authenticate for user={}", username);
            // 记录当前线程堆栈深度，帮助诊断潜在的递归/重入导致的 StackOverflowError
            int stackDepth = Thread.currentThread().getStackTrace().length;
            logger.debug("Current thread stack depth before authenticate: {}", stackDepth);
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            // 确保创建 session 以便存储 SecurityContext（Spring Security 默认行为通常会在过滤器链完成时存入）
            request.getSession(true);

            // 根据请求类型决定返回重定向还是 JSON：
            // - 浏览器请求（Accept 包含 text/html，或非 AJAX） => 重定向到欢迎页 /home
            // - API / AJAX 请求 => 返回 JSON
            String accept = request.getHeader("Accept");
            String xRequestedWith = request.getHeader("X-Requested-With");
            boolean isAjax = xRequestedWith != null && "XMLHttpRequest".equalsIgnoreCase(xRequestedWith);
            boolean wantsHtml = accept != null && accept.contains("text/html");

            if (wantsHtml && !isAjax) {
                // 使用 303 See Other，让客户端对 GET /home 发起请求（遵循 POST-Redirect-GET）
                return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/home").build();
            }

            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("username", username);
            logger.debug("Authentication succeeded for user={}", username);
            return ResponseEntity.ok(ok);
        } catch (BadCredentialsException ex) {
            logger.info("Authentication failed for user={}: bad credentials", username);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        } catch (Throwable t) {
            // 捕获 Throwable（包括 Error）以便记录完整堆栈，帮助诊断 StackOverflowError 的根因。
            // 注意：一般不建议吞掉 Error，但在这里我们记录并返回 500 以避免 servlet 层面未记录的崩溃。
            logger.error("Unexpected error during authentication for user={}", username, t);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "认证过程中发生内部错误，请检查服务器日志");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        } finally {
            AUTH_IN_PROGRESS.remove();
        }
    }
}
