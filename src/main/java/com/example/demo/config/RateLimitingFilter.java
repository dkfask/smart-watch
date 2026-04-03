package com.example.demo.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiter rateLimiter;

    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只对API请求进行限流
        if (httpRequest.getRequestURI().startsWith("/api/")) {
            if (rateLimiter.tryAcquire()) {
                // 允许请求通过
                chain.doFilter(request, response);
            } else {
                // 限流，返回429状态码
                log.warn("Rate limit exceeded for request: {}", httpRequest.getRequestURI());
                httpResponse.setStatus(429); // SC_TOO_MANY_REQUESTS
                httpResponse.getWriter().write("Rate limit exceeded. Please try again later.");
                return;
            }
        } else {
            // 非API请求直接通过
            chain.doFilter(request, response);
        }
    }
}