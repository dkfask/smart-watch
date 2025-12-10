package com.example.demo.config;

import com.example.demo.model.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应拦截器，统一封装API响应格式
 */
@RestControllerAdvice("com.example.demo.controller.api")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 支持所有API控制器的响应处理
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, 
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType, 
                                 ServerHttpRequest request, ServerHttpResponse response) {
        
        // 如果已经是ApiResponse类型，直接返回
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // 如果是分页响应类型，直接返回（PatientController中的PageResponse）
        if (body != null && body.getClass().getSimpleName().equals("PageResponse")) {
            return ApiResponse.success(body);
        }
        
        // 如果是String类型，直接返回，避免双重序列化问题
        if (body instanceof String) {
            return body;
        }
        
        // 否则封装成ApiResponse
        return ApiResponse.success(body);
    }
}