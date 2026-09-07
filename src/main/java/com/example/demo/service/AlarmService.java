package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Alert;
import com.example.demo.model.Device;
import com.example.demo.model.Patient;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.repository.AlarmRepository;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.service.TiandituLocationService;
import com.example.demo.socket.WebSocketHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final TiandituLocationService tiandituLocationService;
    private final DeviceRepository deviceRepository;
    private final PatientRepository patientRepository;
    private final AlertRepository alertRepository;

    /**
     * 构造器注入依赖
     * @param alarmRepository 报警仓库
     * @param tiandituLocationService 地址逆解析服务
     * @param deviceRepository 设备仓库
     * @param patientRepository 病人仓库
     * @param alertRepository 统一报警仓库
     */
    public AlarmService(AlarmRepository alarmRepository,
                        TiandituLocationService tiandituLocationService,
                        DeviceRepository deviceRepository,
                        PatientRepository patientRepository,
                        AlertRepository alertRepository) {
        this.alarmRepository = alarmRepository;
        this.tiandituLocationService = tiandituLocationService;
        this.deviceRepository = deviceRepository;
        this.patientRepository = patientRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * 创建报警（双写到 Alert 表）
     * 使用 @Transactional 确保 Alarm 和 Alert 表的原子性，WebSocket 推送在事务提交后执行
     * @param alarm 报警对象
     * @return 保存后的报警
     */
    @Transactional
    public Alarm createAlarm(Alarm alarm) {
        if (alarm.getLatitude() != null && alarm.getLongitude() != null) {
            String address = tiandituLocationService.regeoAddress(alarm.getLatitude(), alarm.getLongitude());
            alarm.setAddress(address);
        }
        
        // 保存 Alarm 记录
        Alarm saved = alarmRepository.save(alarm);
        
        // 构建 Alert 对象（不推送，等待事务提交）
        Alert alert = new Alert();
        alert.setSource("alarm");
        alert.setSourceId(saved.getId());
        alert.setDeviceId(alarm.getDeviceId());
        alert.setPatientId(alarm.getPatientId());
        alert.setAlertType(alarm.getAlarmType());
        alert.setAlertLevel(alarm.getAlarmLevel());
        alert.setAlertData(alarm.getAlarmData());
        alert.setLatitude(alarm.getLatitude());
        alert.setLongitude(alarm.getLongitude());
        alert.setAddress(alarm.getAddress());
        alert.setAlertTime(alarm.getTriggeredTime());
        alert.setStatus(alarm.getStatus());
        alert.setIsRead(alarm.getIsRead() != null && alarm.getIsRead() ? 1 : 0);
        
        // 保存 Alert 记录（与 Alarm 在同一事务中）
        Alert savedAlert = alertRepository.save(alert);
        
        // 事务提交后推送 WebSocket（使用 ApplicationListener 或手动推送）
        // 这里使用事务后钩子确保推送时数据已提交
        pushAlarmAfterCommit(saved, savedAlert);
        
        return saved;
    }

    /**
     * 根据ID获取报警（含关联设备和病人信息）
     * @param id 报警ID
     * @return 填充关联信息后的报警
     */
    public Optional<Alarm> getAlarmWithRelations(Long id) {
        Optional<Alarm> alarmOpt = alarmRepository.findById(id);
        alarmOpt.ifPresent(this::populateRelations);
        return alarmOpt;
    }

    /**
     * 根据查询条件获取报警列表（含关联信息）
     * @param condition 查询条件
     * @return 分页报警列表
     */
    public Page<Alarm> getAlarmsWithRelations(AlarmQueryCondition condition) {
        Page<Alarm> page = queryAlarms(condition);
        page.getContent().forEach(this::populateRelations);
        return page;
    }

    /**
     * 通用报警查询（根据条件动态选择查询方法）
     * @param condition 查询条件
     * @return 分页结果
     */
    public Page<Alarm> queryAlarms(AlarmQueryCondition condition) {
        Pageable pageable = PageRequest.of(condition.getPage(), condition.getSize());
        Long deviceId = condition.getDeviceId();
        Long patientId = condition.getPatientId();
        String status = condition.getStatus();
        Date startTime = condition.getStartTime();
        Date endTime = condition.getEndTime();

        if (deviceId != null && status != null) {
            return alarmRepository.findByDeviceIdAndStatusOrderByTriggeredTimeDesc(deviceId, status, pageable);
        } else if (patientId != null && status != null) {
            return alarmRepository.findByPatientIdAndStatusOrderByTriggeredTimeDesc(patientId, status, pageable);
        } else if (deviceId != null) {
            return alarmRepository.findByDeviceIdOrderByTriggeredTimeDesc(deviceId, pageable);
        } else if (patientId != null) {
            return alarmRepository.findByPatientIdOrderByTriggeredTimeDesc(patientId, pageable);
        } else if (status != null) {
            return alarmRepository.findByStatusOrderByTriggeredTimeDesc(status, pageable);
        } else if (startTime != null && endTime != null) {
            return alarmRepository.findByTriggeredTimeBetweenOrderByTriggeredTimeDesc(startTime, endTime, pageable);
        } else {
            return alarmRepository.findAll(pageable);
        }
    }

    /**
     * 标记报警为已读
     */
    @Transactional
    public boolean markAlarmAsRead(Long id, boolean read) {
        return alarmRepository.markRead(id, read) > 0;
    }

    /**
     * 批量标记设备的报警为已读
     */
    @Transactional
    public boolean markDeviceAlarmsAsRead(Long deviceId, boolean read) {
        return alarmRepository.markReadByDeviceId(deviceId, read) > 0;
    }

    /**
     * 批量标记病人的报警为已读
     */
    @Transactional
    public boolean markPatientAlarmsAsRead(Long patientId, boolean read) {
        return alarmRepository.markReadByPatientId(patientId, read) > 0;
    }

    /**
     * 处理报警
     */
    @Transactional
    public boolean handleAlarm(Long id, String status, String result, String remark) {
        Date handledTime = new Date();
        boolean success = alarmRepository.handleAlarm(id, status, result, remark, handledTime) > 0;
        if (success) {
            Optional<Alarm> updatedAlarm = alarmRepository.findById(id);
            updatedAlarm.ifPresent(WebSocketHandler::pushAlarmUpdate);
        }
        return success;
    }

    /**
     * 获取未读报警数量
     */
    public long getUnreadAlarmCount() {
        return alarmRepository.countByIsReadFalse();
    }

    /**
     * 获取指定设备的未读报警数量
     */
    public long getUnreadAlarmCountByDeviceId(Long deviceId) {
        return alarmRepository.countByDeviceIdAndIsReadFalse(deviceId);
    }

    /**
     * 获取指定病人的未读报警数量
     */
    public long getUnreadAlarmCountByPatientId(Long patientId) {
        return alarmRepository.countByPatientIdAndIsReadFalse(patientId);
    }

    /**
     * 获取报警统计数据（使用COUNT查询替代全表扫描）
     * @return 报警统计DTO
     */
    public AlarmStatsDto getAlarmStats() {
        AlarmStatsDto stats = new AlarmStatsDto();
        stats.setTotal(alarmRepository.count());
        stats.setPending(alarmRepository.countByStatus("pending"));
        stats.setHandled(alarmRepository.countByStatus("handled"));
        stats.setFalseAlarm(alarmRepository.countByStatus("false_alarm"));
        stats.setUnread(alarmRepository.countByIsReadFalse());
        return stats;
    }

    /**
     * 获取最近的报警记录（含关联信息）
     */
    public List<Alarm> getRecentAlarmsWithRelations(int limit) {
        List<Alarm> allRecent = alarmRepository.findTop100ByOrderByTriggeredTimeDesc();
        List<Alarm> result = allRecent.stream().limit(limit).toList();
        result.forEach(this::populateRelations);
        return result;
    }

    /**
     * 填充报警的关联设备和病人信息
     * @param alarm 报警对象
     */
    private void populateRelations(Alarm alarm) {
        deviceRepository.findById(alarm.getDeviceId()).ifPresent(alarm::setDevice);
        if (alarm.getPatientId() != null) {
            patientRepository.findById(alarm.getPatientId()).ifPresent(alarm::setPatient);
        }
    }

    /**
     * 将 Alert 实体转换为前端兼容的 WebSocket 推送格式
     * 同时提供 alarmType/triggeredTime（前端期望）和 alertType/alertTime（Alert 原始）字段名
     * @param alert 报警实体
     * @return 前端兼容的推送 Map
     */
    private Map<String, Object> toWebSocketPayload(Alert alert) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", alert.getId());
        map.put("deviceId", alert.getDeviceId());
        map.put("patientId", alert.getPatientId());
        map.put("imei", alert.getImei());
        map.put("fenceId", alert.getFenceId());
        map.put("alarmType", alert.getAlertType());
        map.put("alertType", alert.getAlertType());
        map.put("alertLevel", alert.getAlertLevel());
        map.put("alarmLevel", alert.getAlertLevel());
        map.put("alarmData", alert.getAlertData());
        map.put("alertData", alert.getAlertData());
        map.put("latitude", alert.getLatitude());
        map.put("longitude", alert.getLongitude());
        map.put("address", alert.getAddress());
        map.put("triggeredTime", alert.getAlertTime());
        map.put("alertTime", alert.getAlertTime());
        map.put("status", alert.getStatus());
        map.put("isRead", alert.getIsRead() != null && alert.getIsRead() == 1);
        map.put("handleResult", alert.getHandleResult());
        map.put("handleRemark", alert.getHandleRemark());
        map.put("handledTime", alert.getHandledTime());
        map.put("createdAt", alert.getCreatedAt());
        map.put("updatedAt", alert.getUpdatedAt());
        return map;
    }

    /**
     * 事务提交后推送 WebSocket 更新
     * 使用 TransactionSynchronizationManager 确保在事务提交后才推送，避免数据不一致
     * 如果事务未激活（如测试环境），则直接推送
     * @param savedAlarm 保存后的 Alarm 对象
     * @param savedAlert 保存后的 Alert 对象
     */
    private void pushAlarmAfterCommit(Alarm savedAlarm, Alert savedAlert) {
        // 检查事务是否激活
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            // 事务激活，注册同步回调
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务提交后才推送 WebSocket，确保数据已持久化
                        // 清空 @Transient 关联字段（防止 Jackson 触发 HibernateProxy 懒加载）
                        clearTransientAssociations(savedAlarm);
                        WebSocketHandler.pushAlarm(savedAlarm);
                        WebSocketHandler.pushAlarm(toWebSocketPayload(savedAlert));
                    }
                }
            );
        } else {
            // 事务未激活（如测试环境），直接推送
            clearTransientAssociations(savedAlarm);
            WebSocketHandler.pushAlarm(savedAlarm);
            WebSocketHandler.pushAlarm(toWebSocketPayload(savedAlert));
        }
    }

    /**
     * 清空 Alarm 的 @Transient 关联字段（device, patient）。
     *
     * <p>背景：v3.0.0 重构时给 Alarm 加了 {@code @Transient Device device} 和
     * {@code @Transient Patient patient} 用于前端展示。{@link WebSocketHandler} 在
     * 事务结束后异步推送 Alarm（Jackson 序列化），如果这两个字段装的是
     * HibernateProxy（{@code PatientDevice.patient} 是 LAZY 关联 → FenceService
     * 调 pd.getPatient() → 返回 Proxy），序列化时访问字段会触发懒加载，session
     * 已关闭 → {@code "Could not initialize proxy ... - no session"} 错误。</p>
     *
     * <p>解决：在推送 WebSocket 前清空 @Transient 关联字段。patientId/deviceId
     * 是数据库字段，已持久化，detached 状态下修改对象不影响数据库；前端拿
     * patientId 仍能通过单独接口查病人详情。</p>
     *
     * <p>注意：此方法修改传入对象的 @Transient 字段，不应回写到数据库
     * （savedAlarm 在 afterCommit 回调里是 detached 状态，修改仅影响内存）。</p>
     *
     * @param alarm Alarm 实体（必须在事务结束后或 detached 状态调用）
     */
    static void clearTransientAssociations(Alarm alarm) {
        if (alarm == null) {
            return;
        }
        // 使用 Alarm.clearTransientFields()：只清空 @Transient 字段，保留 ID
        // 不能用 setPatient(null)/setDevice(null)，那俩会同步清空 patientId/deviceId
        alarm.clearTransientFields();
    }
}
