package com.example.demo.repository;

import com.example.demo.model.PatientDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientDeviceRepository extends JpaRepository<PatientDevice, Long> {
    
    // 获取病人关联的设备列表
    List<PatientDevice> findByPatientId(Long patientId);
    
    // 获取设备关联的病人列表
    List<PatientDevice> findByDeviceId(Long deviceId);
    
    // 根据病人ID和设备ID查找关联
    PatientDevice findByPatientIdAndDeviceId(Long patientId, Long deviceId);
    
    // 批量绑定设备和病人
    @Modifying
    @Query(value = "INSERT INTO patient_devices (patient_id, device_id, bind_time, relationship, is_active, created_at, updated_at) " +
            "VALUES (:patientId, :deviceId, NOW(), :relationship, true, NOW(), NOW())", nativeQuery = true)
    void bindDeviceToPatient(@Param("patientId") Long patientId, @Param("deviceId") Long deviceId, @Param("relationship") String relationship);
    
    // 解除设备和病人关联
    @Modifying
    @Query(value = "DELETE FROM patient_devices WHERE patient_id = :patientId AND device_id = :deviceId", nativeQuery = true)
    int unbindDeviceFromPatient(@Param("patientId") Long patientId, @Param("deviceId") Long deviceId);
    
    // 根据设备ID列表查询关联
    List<PatientDevice> findByDeviceIdIn(List<Long> deviceIds);
}
