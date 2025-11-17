package com.example.demo.repository;

import com.example.demo.model.HeartbeatRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeartbeatRecordRepository extends CrudRepository<HeartbeatRecord, Long> {
    List<HeartbeatRecord> findByImeiOrderByRecvTimeDesc(String imei);
}

