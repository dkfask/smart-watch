package com.example.demo.repository;

import com.example.demo.model.Wearer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WearerRepository extends JpaRepository<Wearer, Long> {
    Optional<Wearer> findByDeviceId(Long deviceId);
    Page<Wearer> findAll(Pageable pageable);
}
