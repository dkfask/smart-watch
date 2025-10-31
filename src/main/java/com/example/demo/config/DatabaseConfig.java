package com.example.demo.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DatabaseConfig: 如果在 application.properties 中配置了 spring.datasource.*，
 * 则通过 DataSourceProperties 初始化并创建一个 DataSource bean，保证可以自动装配。
 */
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        // 使用 Spring Boot 的 DataSourceProperties 来创建 DataSource（会使用默认连接池 HikariCP）
        return properties.initializeDataSourceBuilder().build();
    }
}
