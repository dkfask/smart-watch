package com.example.demo.repository;

import com.example.demo.model.HealthRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 健康记录数据访问层
 */
@Repository
public interface HealthRecordRepository extends CrudRepository<HealthRecord, Long> {
    List<HealthRecord> findByImeiOrderByRecvTimeDesc(String imei);
    List<HealthRecord> findByPatientIdOrderByRecvTimeDesc(Long patientId);
    List<HealthRecord> findByPatientIdAndDataTypeOrderByRecvTimeDesc(Long patientId, String dataType);
    Optional<HealthRecord> findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(Long patientId, String dataType);
    List<HealthRecord> findByPatientIdAndRecvTimeBetweenOrderByRecvTimeDesc(Long patientId, java.util.Date start, java.util.Date end);
}

