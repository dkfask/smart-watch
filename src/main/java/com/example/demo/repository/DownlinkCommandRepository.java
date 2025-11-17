package com.example.demo.repository;

import com.example.demo.model.DownlinkCommand;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownlinkCommandRepository extends CrudRepository<DownlinkCommand, Long> {
    List<DownlinkCommand> findByImeiOrderByCreatedAtDesc(String imei);
}

