package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.util.concurrent.RateLimiter;

@Configuration
public class RateLimiterConfig {

    /**
     * 创建速率限制器，每秒允许10个请求
     * @return 速率限制器
     */
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(10.0); // 每秒10个请求
    }
}