package com.example.demo.repository;

import com.example.demo.model.SimpleFenceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 简化版围栏报警数据访问接口
 */
public interface SimpleFenceAlertRepository extends JpaRepository<SimpleFenceAlert, Long> {
    // 可以根据需要添加自定义查询方法
}