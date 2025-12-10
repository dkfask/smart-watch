package com.example.demo.repository;

import com.example.demo.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    // 根据姓名、身份证号或病房搜索病人
    List<Patient> findByNameContainingOrIdCardContainingOrWardContaining(String name, String idCard, String ward);
    
    // 分页搜索病人
    Page<Patient> findByNameContainingOrIdCardContainingOrWardContaining(String name, String idCard, String ward, Pageable pageable);
}
