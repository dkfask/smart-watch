package com.example.demo.repository;

import com.example.demo.model.FenceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * 围栏报警仓库接口
 */
public interface FenceAlertRepository extends JpaRepository<FenceAlert, Long> {
    
    /**
     * 根据设备ID获取报警列表
     * @param deviceId 设备ID
     * @return 围栏报警列表
     */
    List<FenceAlert> findByDeviceId(Long deviceId);
    
    /**
     * 根据病人ID获取报警列表
     * @param patientId 病人ID
     * @return 围栏报警列表
     */
    List<FenceAlert> findByPatientId(Long patientId);
    
    /**
     * 根据围栏ID获取报警列表
     * @param fenceId 围栏ID
     * @return 围栏报警列表
     */
    List<FenceAlert> findByFenceId(Long fenceId);
    
    /**
     * 根据状态获取报警列表
     * @param status 报警状态
     * @return 围栏报警列表
     */
    List<FenceAlert> findByStatus(String status);
    
    /**
     * 根据状态和时间范围获取报警列表
     * @param status 报警状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 围栏报警列表
     */
    List<FenceAlert> findByStatusAndAlertTimeBetween(String status, Date startTime, Date endTime);
    
    /**
     * 按状态分组统计报警数量
     * @return 状态统计结果，格式：[{status: 'pending', count: 5}, ...]
     */
    @Query(value = "SELECT status, COUNT(*) as count FROM fence_alerts GROUP BY status", nativeQuery = true)
    List<Object[]> countByStatus();
    
    /**
     * 更新报警状态
     * @param id 报警ID
     * @param status 新状态
     * @param handledBy 处理人ID
     * @param handledTime 处理时间
     * @param remark 备注
     * @return 更新的记录数
     */
    @Modifying
    @Query(value = "UPDATE fence_alerts SET status = :status, handled_by = :handledBy, handled_time = :handledTime, remark = :remark WHERE id = :id", nativeQuery = true)
    int updateAlertStatus(@Param("id") Long id, @Param("status") String status, 
                          @Param("handledBy") Long handledBy, @Param("handledTime") Date handledTime, 
                          @Param("remark") String remark);
    
    /**
     * 删除指定时间之前的报警记录
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query(value = "DELETE FROM fence_alerts WHERE alert_time < :beforeTime", nativeQuery = true)
    int deleteBeforeTime(@Param("beforeTime") Date beforeTime);
}