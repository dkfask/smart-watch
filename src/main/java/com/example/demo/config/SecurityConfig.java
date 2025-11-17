package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 对 /api/** 忽略 CSRF（REST API 使用）
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                // 放行页面与静态资源，避免在 forward 到 index.html 时被安全过滤器再次重定向到 /login
                // 注意：不能在 PathPatternParser 下使用像 "/**/*.js" 这样的模式（"**" 后不能接额外数据），
                // 否则运行时会抛出 PatternParseException: "No more pattern data allowed after {*...} or ** pattern element"。
                // 因此使用明确的目录通配路径（例如 /assets/** /css/** /js/**）来替代通配扩展名的写法。
                .requestMatchers(
                    "/login", "/register",
                    "/", "/index.html", "/favicon.ico",
                    "/css/**", "/js/**", "/images/**", "/assets/**", "/static/**",
                    "/webjars/**",
                    "/api/**"
                ).permitAll()
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
