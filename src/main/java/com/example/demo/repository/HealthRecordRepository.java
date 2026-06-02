package com.example.demo.repository;

import com.example.demo.model.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 健康记录数据访问层
 */
@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByImeiOrderByRecvTimeDesc(String imei);
    List<HealthRecord> findByPatientIdOrderByRecvTimeDesc(Long patientId);
    List<HealthRecord> findByPatientIdAndDataTypeOrderByRecvTimeDesc(Long patientId, String dataType);
    Optional<HealthRecord> findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(Long patientId, String dataType);
    List<HealthRecord> findByPatientIdAndRecvTimeBetweenOrderByRecvTimeDesc(Long patientId, java.util.Date start, java.util.Date end);

    Page<HealthRecord> findByDeviceId(Long deviceId, Pageable pageable);
    List<HealthRecord> findByDeviceIdOrderByRecvTimeDesc(Long deviceId);
}
