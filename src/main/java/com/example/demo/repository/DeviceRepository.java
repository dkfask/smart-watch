package com.example.demo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Device;

import java.util.Optional;

@Repository
public interface DeviceRepository extends CrudRepository<Device, Long> {
    Optional<Device> findByImei(String imei);
}
