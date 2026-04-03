package com.example.demo.config;

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
import java.util.regex.Pattern;

@Component
public class InputValidationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(InputValidationFilter.class);

    // 危险字符正则表达式，用于检测SQL注入、XSS等攻击
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?i)(union|select|insert|update|delete|drop|alter|create|truncate|exec|xp_)|<script>|<iframe>|<img|javascript:|onerror=|onload=|onclick="
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只对API请求进行输入验证
        if (httpRequest.getRequestURI().startsWith("/api/")) {
            // 验证请求参数
            if (validateRequest(httpRequest)) {
                // 允许请求通过
                chain.doFilter(request, response);
            } else {
                // 输入验证失败，返回400状态码
                log.warn("Input validation failed for request: {}", httpRequest.getRequestURI());
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().write("Invalid input detected. Please check your request.");
                return;
            }
        } else {
            // 非API请求直接通过
            chain.doFilter(request, response);
        }
    }

    /**
     * 验证请求参数
     * @param request HTTP请求
     * @return 是否验证通过
     */
    private boolean validateRequest(HttpServletRequest request) {
        // 验证查询参数
        for (String paramName : request.getParameterMap().keySet()) {
            String[] paramValues = request.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                if (paramValue != null && DANGEROUS_PATTERN.matcher(paramValue).find()) {
                    log.warn("Dangerous input detected in parameter '{}': {}", paramName, paramValue);
                    return false;
                }
            }
        }

        // 验证请求头
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && DANGEROUS_PATTERN.matcher(headerValue).find()) {
                log.warn("Dangerous input detected in header '{}': {}", headerName, headerValue);
                return false;
            }
        }

        return true;
    }
}