package com.example.demo.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 错误页面配置类，用于处理所有错误并转发到index.html
 */
@Configuration
public class ErrorPageConfig {

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        
        // 添加错误页面，将所有错误状态码转发到/index.html
        // Tomcat会自动处理错误页面的转发，位置必须以/开头
        ErrorPage errorPage = new ErrorPage("/index.html");
        factory.addErrorPages(errorPage);
        
        return factory;
    }
}
