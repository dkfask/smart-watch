package com.example.demo.repository;

import com.example.demo.model.Alarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // 按设备ID获取报警列表
    List<Alarm> findByDeviceIdOrderByTriggeredTimeDesc(Long deviceId);
    
    // 按设备ID分页获取报警列表
    Page<Alarm> findByDeviceIdOrderByTriggeredTimeDesc(Long deviceId, Pageable pageable);
    
    // 按病人ID获取报警列表
    List<Alarm> findByPatientIdOrderByTriggeredTimeDesc(Long patientId);
    
    // 按病人ID分页获取报警列表
    Page<Alarm> findByPatientIdOrderByTriggeredTimeDesc(Long patientId, Pageable pageable);
    
    // 按状态获取报警列表
    List<Alarm> findByStatusOrderByTriggeredTimeDesc(String status);
    
    // 按状态分页获取报警列表
    Page<Alarm> findByStatusOrderByTriggeredTimeDesc(String status, Pageable pageable);
    
    // 按设备ID和状态获取报警列表
    List<Alarm> findByDeviceIdAndStatusOrderByTriggeredTimeDesc(Long deviceId, String status);
    
    // 按设备ID和状态分页获取报警列表
    Page<Alarm> findByDeviceIdAndStatusOrderByTriggeredTimeDesc(Long deviceId, String status, Pageable pageable);
    
    // 按病人ID和状态获取报警列表
    List<Alarm> findByPatientIdAndStatusOrderByTriggeredTimeDesc(Long patientId, String status);
    
    // 按病人ID和状态分页获取报警列表
    Page<Alarm> findByPatientIdAndStatusOrderByTriggeredTimeDesc(Long patientId, String status, Pageable pageable);
    
    // 按报警类型获取报警列表
    List<Alarm> findByAlarmTypeOrderByTriggeredTimeDesc(String alarmType);
    
    // 按报警类型分页获取报警列表
    Page<Alarm> findByAlarmTypeOrderByTriggeredTimeDesc(String alarmType, Pageable pageable);
    
    // 按报警级别获取报警列表
    List<Alarm> findByAlarmLevelOrderByTriggeredTimeDesc(String alarmLevel);
    
    // 按报警级别分页获取报警列表
    Page<Alarm> findByAlarmLevelOrderByTriggeredTimeDesc(String alarmLevel, Pageable pageable);
    
    // 按时间范围获取报警列表
    List<Alarm> findByTriggeredTimeBetweenOrderByTriggeredTimeDesc(Date startTime, Date endTime);
    
    // 按时间范围分页获取报警列表
    Page<Alarm> findByTriggeredTimeBetweenOrderByTriggeredTimeDesc(Date startTime, Date endTime, Pageable pageable);
    
    // 按设备ID和时间范围获取报警列表
    List<Alarm> findByDeviceIdAndTriggeredTimeBetweenOrderByTriggeredTimeDesc(Long deviceId, Date startTime, Date endTime);
    
    // 按设备ID和时间范围分页获取报警列表
    Page<Alarm> findByDeviceIdAndTriggeredTimeBetweenOrderByTriggeredTimeDesc(Long deviceId, Date startTime, Date endTime, Pageable pageable);
    
    // 按病人ID和时间范围获取报警列表
    List<Alarm> findByPatientIdAndTriggeredTimeBetweenOrderByTriggeredTimeDesc(Long patientId, Date startTime, Date endTime);
    
    // 按病人ID和时间范围分页获取报警列表
    Page<Alarm> findByPatientIdAndTriggeredTimeBetweenOrderByTriggeredTimeDesc(Long patientId, Date startTime, Date endTime, Pageable pageable);
    
    // 获取未读报警数量
    long countByIsReadFalse();
    
    // 获取指定设备的未读报警数量
    long countByDeviceIdAndIsReadFalse(Long deviceId);
    
    // 获取指定病人的未读报警数量
    long countByPatientIdAndIsReadFalse(Long patientId);
    
    // 标记报警为已读
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = :read WHERE a.id = :id")
    int markRead(@Param("id") Long id, @Param("read") boolean read);
    
    // 批量标记设备的报警为已读
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = :read WHERE a.deviceId = :deviceId")
    int markReadByDeviceId(@Param("deviceId") Long deviceId, @Param("read") boolean read);
    
    // 批量标记病人的报警为已读
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = :read WHERE a.patientId = :patientId")
    int markReadByPatientId(@Param("patientId") Long patientId, @Param("read") boolean read);
    
    // 处理报警
    @Modifying
    @Query("UPDATE Alarm a SET a.status = :status, a.handleResult = :result, a.handleRemark = :remark, a.handledTime = :handledTime WHERE a.id = :id")
    int handleAlarm(@Param("id") Long id, @Param("status") String status, @Param("result") String result, @Param("remark") String remark, @Param("handledTime") Date handledTime);
    
    // 统计指定时间范围内的报警数量
    @Query("SELECT COUNT(a) FROM Alarm a WHERE a.triggeredTime BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    // 统计指定时间范围内按类型分组的报警数量
    @Query("SELECT a.alarmType, COUNT(a) FROM Alarm a WHERE a.triggeredTime BETWEEN :startTime AND :endTime GROUP BY a.alarmType")
    List<Object[]> countByTypeAndTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    // 获取最近的报警记录
    List<Alarm> findTop100ByOrderByTriggeredTimeDesc();
    
    // 根据病人ID和报警类型获取最近的一条报警记录
    Optional<Alarm> findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(Long patientId, String alarmType);

    // 按状态统计报警数量
    long countByStatus(String status);
}