package com.example.demo.repository;

import com.example.demo.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 统一报警仓库接口
 * 合并 AlarmRepository 和 FenceAlertRepository 的所有查询方法
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 按设备ID获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @return 报警列表
     */
    List<Alert> findByDeviceIdOrderByAlertTimeDesc(Long deviceId);

    /**
     * 按设备ID分页获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByDeviceIdOrderByAlertTimeDesc(Long deviceId, Pageable pageable);

    /**
     * 按病人ID获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @return 报警列表
     */
    List<Alert> findByPatientIdOrderByAlertTimeDesc(Long patientId);

    /**
     * 按病人ID分页获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByPatientIdOrderByAlertTimeDesc(Long patientId, Pageable pageable);

    /**
     * 按状态获取报警列表，按报警时间倒序
     * @param status 报警状态
     * @return 报警列表
     */
    List<Alert> findByStatusOrderByAlertTimeDesc(String status);

    /**
     * 按状态分页获取报警列表，按报警时间倒序
     * @param status 报警状态
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByStatusOrderByAlertTimeDesc(String status, Pageable pageable);

    /**
     * 按设备ID和状态获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @param status 报警状态
     * @return 报警列表
     */
    List<Alert> findByDeviceIdAndStatusOrderByAlertTimeDesc(Long deviceId, String status);

    /**
     * 按设备ID和状态分页获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @param status 报警状态
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByDeviceIdAndStatusOrderByAlertTimeDesc(Long deviceId, String status, Pageable pageable);

    /**
     * 按病人ID和状态获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @param status 报警状态
     * @return 报警列表
     */
    List<Alert> findByPatientIdAndStatusOrderByAlertTimeDesc(Long patientId, String status);

    /**
     * 按病人ID和状态分页获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @param status 报警状态
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByPatientIdAndStatusOrderByAlertTimeDesc(Long patientId, String status, Pageable pageable);

    /**
     * 按报警类型获取报警列表，按报警时间倒序
     * @param alertType 报警类型
     * @return 报警列表
     */
    List<Alert> findByAlertTypeOrderByAlertTimeDesc(String alertType);

    /**
     * 按报警类型分页获取报警列表，按报警时间倒序
     * @param alertType 报警类型
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByAlertTypeOrderByAlertTimeDesc(String alertType, Pageable pageable);

    /**
     * 按报警级别获取报警列表，按报警时间倒序
     * @param alertLevel 报警级别
     * @return 报警列表
     */
    List<Alert> findByAlertLevelOrderByAlertTimeDesc(String alertLevel);

    /**
     * 按报警级别分页获取报警列表，按报警时间倒序
     * @param alertLevel 报警级别
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByAlertLevelOrderByAlertTimeDesc(String alertLevel, Pageable pageable);

    /**
     * 按时间范围获取报警列表，按报警时间倒序
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报警列表
     */
    List<Alert> findByAlertTimeBetweenOrderByAlertTimeDesc(Date startTime, Date endTime);

    /**
     * 按时间范围分页获取报警列表，按报警时间倒序
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByAlertTimeBetweenOrderByAlertTimeDesc(Date startTime, Date endTime, Pageable pageable);

    /**
     * 按设备ID和时间范围获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报警列表
     */
    List<Alert> findByDeviceIdAndAlertTimeBetweenOrderByAlertTimeDesc(Long deviceId, Date startTime, Date endTime);

    /**
     * 按设备ID和时间范围分页获取报警列表，按报警时间倒序
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByDeviceIdAndAlertTimeBetweenOrderByAlertTimeDesc(Long deviceId, Date startTime, Date endTime, Pageable pageable);

    /**
     * 按病人ID和时间范围获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报警列表
     */
    List<Alert> findByPatientIdAndAlertTimeBetweenOrderByAlertTimeDesc(Long patientId, Date startTime, Date endTime);

    /**
     * 按病人ID和时间范围分页获取报警列表，按报警时间倒序
     * @param patientId 病人ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 报警分页结果
     */
    Page<Alert> findByPatientIdAndAlertTimeBetweenOrderByAlertTimeDesc(Long patientId, Date startTime, Date endTime, Pageable pageable);

    /**
     * 按围栏ID获取报警列表，按报警时间倒序（来自FenceAlertRepository）
     * @param fenceId 围栏ID
     * @return 报警列表
     */
    List<Alert> findByFenceIdOrderByAlertTimeDesc(Long fenceId);

    /**
     * 按围栏ID获取报警列表（来自FenceAlertRepository）
     * @param fenceId 围栏ID
     * @return 报警列表
     */
    List<Alert> findByFenceId(Long fenceId);

    /**
     * 获取未读报警数量（isRead不等于指定值）
     * @param read 已读标记值
     * @return 未读报警数量
     */
    long countByIsReadNot(Integer read);

    /**
     * 获取指定设备的未读报警数量
     * @param deviceId 设备ID
     * @param read 已读标记值
     * @return 未读报警数量
     */
    long countByDeviceIdAndIsReadNot(Long deviceId, Integer read);

    /**
     * 获取指定病人的未读报警数量
     * @param patientId 病人ID
     * @param read 已读标记值
     * @return 未读报警数量
     */
    long countByPatientIdAndIsReadNot(Long patientId, Integer read);

    /**
     * 根据病人ID和报警类型获取最近的一条报警记录
     * @param patientId 病人ID
     * @param alertType 报警类型
     * @return 最近的报警记录
     */
    Optional<Alert> findTopByPatientIdAndAlertTypeOrderByAlertTimeDesc(Long patientId, String alertType);

    /**
     * 按状态统计报警数量
     * @param status 报警状态
     * @return 报警数量
     */
    long countByStatus(String status);

    /**
     * 标记报警为已读或未读
     * @param id 报警ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = :read WHERE a.id = :id")
    int markRead(@Param("id") Long id, @Param("read") Integer read);

    /**
     * 批量标记设备的报警为已读或未读
     * @param deviceId 设备ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = :read WHERE a.deviceId = :deviceId")
    int markReadByDeviceId(@Param("deviceId") Long deviceId, @Param("read") Integer read);

    /**
     * 批量标记病人的报警为已读或未读
     * @param patientId 病人ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = :read WHERE a.patientId = :patientId")
    int markReadByPatientId(@Param("patientId") Long patientId, @Param("read") Integer read);

    /**
     * 处理报警，更新状态、处理结果、备注和处理时间
     * @param id 报警ID
     * @param status 新状态
     * @param result 处理结果
     * @param remark 处理备注
     * @param handledTime 处理时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Alert a SET a.status = :status, a.handleResult = :result, a.handleRemark = :remark, a.handledTime = :handledTime WHERE a.id = :id")
    int handleAlert(@Param("id") Long id, @Param("status") String status, @Param("result") String result, @Param("remark") String remark, @Param("handledTime") Date handledTime);

    /**
     * 统计指定时间范围内的报警数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报警数量
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.alertTime BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计指定时间范围内按类型分组的报警数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 按类型分组的统计结果
     */
    @Query("SELECT a.alertType, COUNT(a) FROM Alert a WHERE a.alertTime BETWEEN :startTime AND :endTime GROUP BY a.alertType")
    List<Object[]> countByTypeAndTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 获取最近100条报警记录，按报警时间倒序
     * @return 报警列表
     */
    List<Alert> findTop100ByOrderByAlertTimeDesc();
}
