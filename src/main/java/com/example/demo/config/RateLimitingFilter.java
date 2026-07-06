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

        // 鍙API璇锋眰杩涜闄愭祦
        if (httpRequest.getRequestURI().startsWith("/api/")) {
            if (rateLimiter.tryAcquire()) {
                // 鍏佽璇锋眰閫氳繃
                chain.doFilter(request, response);
            } else {
                // 闄愭祦锛岃繑鍥?29鐘舵€佺爜
                log.warn("Rate limit exceeded for request: {}", httpRequest.getRequestURI());
                httpResponse.setStatus(429); // SC_TOO_MANY_REQUESTS
                httpResponse.setHeader("Retry-After", "1");
                httpResponse.getWriter().write("Rate limit exceeded. Please try again later.");
                return;
            }
        } else {
            // 闈濧PI璇锋眰鐩存帴閫氳繃
            chain.doFilter(request, response);
        }
    }
}
