package com.example.demo.config;

import com.example.demo.model.ApiResponse;
import com.example.demo.service.AiChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理所有API控制器的异常
 */
@RestControllerAdvice("com.example.demo.controller.api")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * AI 助手异常：消息已是面向用户的中文提示，直接透传
     */
    @ExceptionHandler(AiChatClient.AiChatException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiChatException(AiChatClient.AiChatException ex) {
        log.warn("AI chat error: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(503, ex.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.error(500, "Internal server error");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
