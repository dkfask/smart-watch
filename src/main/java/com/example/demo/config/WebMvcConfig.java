package com.example.demo.config;

import com.example.demo.model.ApiResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，用于配置 API 请求的错误处理
 */
@Controller
public class WebMvcConfig implements ErrorController {

    /**
     * 处理 API 请求的错误，返回 JSON 格式的错误响应
     */
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<?>> handleError(jakarta.servlet.http.HttpServletRequest request) {
        // 检查请求路径是否以 /api/ 开头
        String requestURI = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        
        if (requestURI != null && requestURI.startsWith("/api/")) {
            // 对于 API 请求，返回 JSON 格式的错误响应
            Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
            String message = (String) request.getAttribute("jakarta.servlet.error.message");
            
            if (statusCode == null) {
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            }
            
            if (message == null || message.isEmpty()) {
                message = "服务器内部错误";
            }
            
            return ResponseEntity
                    .status(statusCode)
                    .body(ApiResponse.error(statusCode, message));
        } else {
            // 对于非 API 请求，返回 500 错误
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误"));
        }
    }
    
    /**
     * 自定义异常处理器，处理所有 API 控制器的异常
     */
    @RestControllerAdvice("com.example.demo.controller.api")
    public static class ApiExceptionHandler {
        
        @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }
}
