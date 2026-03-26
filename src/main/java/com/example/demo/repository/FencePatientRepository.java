package com.example.demo.repository;

import com.example.demo.model.FencePatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 围栏病人关联仓库接口
 */
public interface FencePatientRepository extends JpaRepository<FencePatient, Long> {
    
    /**
     * 根据围栏ID获取关联的病人列表
     * @param fenceId 围栏ID
     * @return 围栏病人关联列表
     */
    List<FencePatient> findByFenceId(Long fenceId);
    
    /**
     * 根据病人ID获取关联的围栏列表
     * @param patientId 病人ID
     * @return 围栏病人关联列表
     */
    List<FencePatient> findByPatientId(Long patientId);
    
    /**
     * 根据围栏ID和病人ID查找关联
     * @param fenceId 围栏ID
     * @param patientId 病人ID
     * @return 围栏病人关联对象
     */
    FencePatient findByFenceIdAndPatientId(Long fenceId, Long patientId);
    
    /**
     * 根据围栏ID删除所有关联
     * @param fenceId 围栏ID
     */
    void deleteByFenceId(Long fenceId);
    
    /**
     * 根据病人ID删除所有关联
     * @param patientId 病人ID
     */
    void deleteByPatientId(Long patientId);
    
    /**
     * 批量绑定病人到围栏
     * @param fenceId 围栏ID
     * @param patientIds 病人ID列表
     */
    @Modifying
    @Query(value = "INSERT INTO fence_patients (fence_id, patient_id, created_at, updated_at) " +
            "SELECT :fenceId, p.id, NOW(), NOW() " +
            "FROM patients p WHERE p.id IN :patientIds " +
            "AND NOT EXISTS (SELECT 1 FROM fence_patients fp WHERE fp.fence_id = :fenceId AND fp.patient_id = p.id)", 
            nativeQuery = true)
    void bindPatientsToFence(@Param("fenceId") Long fenceId, @Param("patientIds") List<Long> patientIds);
    
    /**
     * 批量解绑病人与围栏
     * @param fenceId 围栏ID
     * @param patientIds 病人ID列表
     */
    @Modifying
    @Query(value = "DELETE FROM fence_patients WHERE fence_id = :fenceId AND patient_id IN :patientIds", nativeQuery = true)
    void unbindPatientsFromFence(@Param("fenceId") Long fenceId, @Param("patientIds") List<Long> patientIds);
    
    /**
     * 清空围栏的所有病人关联
     * @param fenceId 围栏ID
     */
    @Modifying
    @Query(value = "DELETE FROM fence_patients WHERE fence_id = :fenceId", nativeQuery = true)
    void clearFencePatients(@Param("fenceId") Long fenceId);
}