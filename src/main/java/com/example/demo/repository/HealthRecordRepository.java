package com.example.demo.repository;

import com.example.demo.model.HealthRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthRecordRepository extends CrudRepository<HealthRecord, Long> {
    List<HealthRecord> findByImeiOrderByRecvTimeDesc(String imei);
}

