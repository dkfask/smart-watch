package com.example.demo.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter rateLimiter() {
        // Dashboard and realtime pages intentionally load several API resources together.
        // Keep a global safety cap, but avoid rejecting normal monitoring refresh bursts.
        return RateLimiter.create(60.0);
    }
}
