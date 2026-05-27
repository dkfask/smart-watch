package com.example.demo.repository;

import com.example.demo.model.HealthAlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthAlertThresholdRepository extends JpaRepository<HealthAlertThreshold, Long> {
    Optional<HealthAlertThreshold> findByDataType(String dataType);
}
