package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final RateLimitingFilter rateLimitingFilter;
    private final InputValidationFilter inputValidationFilter;

    public SecurityConfig(PasswordEncoder passwordEncoder, RateLimitingFilter rateLimitingFilter, InputValidationFilter inputValidationFilter) {
        this.passwordEncoder = passwordEncoder;
        this.rateLimitingFilter = rateLimitingFilter;
        this.inputValidationFilter = inputValidationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 对 /api/** 忽略 CSRF（REST API 使用）
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            // 添加输入验证过滤器
            .addFilterBefore(inputValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // 添加速率限制过滤器
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 放行页面与静态资源，避免在 forward 到 index.html 时被安全过滤器再次重定向到 /login
                // 注意：不能在 PathPatternParser 下使用像 "/**/*.js" 这样的模式（"**" 后不能接额外数据），
                // 否则运行时会抛出 PatternParseException: "No more pattern data allowed after {*...} or ** pattern element"。
                // 因此使用明确的目录通配路径（例如 /assets/** /css/** /js/**）来替代通配扩展名的写法。
                .requestMatchers(
                    "/login", "/register",
                    "/", "/index.html", "/favicon.ico",
                    "/css/**", "/js/**", "/images/**", "/assets/**", "/static/**",
                    "/webjars/**"
                ).permitAll()
                // 允许无需认证的API
                .requestMatchers(
                    "/api/auth/login", "/api/auth/me", "/api/auth/logout"
                ).permitAll()
                // 需要ADMIN角色的API
                .requestMatchers(
                    "/api/admin/**",
                    "/api/users", "/api/users/**",
                    "/api/roles", "/api/roles/**"
                ).hasRole("ADMIN")
                // 需要USER角色的API
                .requestMatchers(
                    "/api/devices", "/api/devices/**",
                    "/api/patients", "/api/patients/**",
                    "/api/patient-devices", "/api/patient-devices/**",
                    "/api/fences", "/api/fences/**",
                    "/api/alarms", "/api/alarms/**",
                    "/api/alerts", "/api/alerts/**",
                    "/api/health-records", "/api/health-records/**",
                    "/api/wearers", "/api/wearers/**",
                    "/api/downlink", "/api/downlink/**",
                    "/api/locations", "/api/locations/**"
                ).hasRole("USER")
                // 测试API
                .requestMatchers("/db/test").permitAll()
                // 其他API需要认证
                .requestMatchers("/api/**").authenticated()
                // 对于非API的GET请求，由SpaFallbackController处理SPA路由
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/login", "/register", "/home", "/menu").permitAll()
                // 管理页面需要ADMIN角色
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                // 登录成功后重定向到主菜单页面 /menu，true 表示无论之前是否访问过受保护页面都强制跳转到该 URL
                .defaultSuccessUrl("/menu", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );
        return http.build();
    }

    // 使用 AuthenticationConfiguration 来构造 AuthenticationManager，以避免直接使用已弃用的 DaoAuthenticationProvider API
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
