package com.example.demo.repository;

import com.example.demo.model.LocationRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRecordRepository extends CrudRepository<LocationRecord, Long> {
    List<LocationRecord> findByImeiOrderByRecvTimeDesc(String imei);
}

