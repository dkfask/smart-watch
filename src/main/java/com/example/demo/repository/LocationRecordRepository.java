package com.example.demo.repository;

import com.example.demo.model.LocationRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LocationRecordRepository extends CrudRepository<LocationRecord, Long> {
    List<LocationRecord> findByImeiOrderByRecvTimeDesc(String imei);

    // Count records older than given date
    long countByRecvTimeBefore(Date cutoff);

    // Delete records older than given date
    void deleteByRecvTimeBefore(Date cutoff);
}
