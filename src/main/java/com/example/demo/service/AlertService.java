package com.example.demo.service;

import com.example.demo.model.Alert;
import com.example.demo.model.Device;
import com.example.demo.model.Patient;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
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

/**
 * 统一报警服务类
 * 基于Alert实体提供报警的创建、查询、处理和统计功能
 */
@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AmapLocationService amapLocationService;
    private final DeviceRepository deviceRepository;
    private final PatientRepository patientRepository;

    /**
     * 构造器注入依赖
     * @param alertRepository 报警仓库
     * @param amapLocationService 地址逆解析服务
     * @param deviceRepository 设备仓库
     * @param patientRepository 病人仓库
     */
    public AlertService(AlertRepository alertRepository,
                        AmapLocationService amapLocationService,
                        DeviceRepository deviceRepository,
                        PatientRepository patientRepository) {
        this.alertRepository = alertRepository;
        this.amapLocationService = amapLocationService;
        this.deviceRepository = deviceRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * 创建报警（含地址逆解析 + WebSocket推送）
     * @param alert 报警对象
     * @return 保存后的报警
     */
    @Transactional
    public Alert createAlert(Alert alert) {
        if (alert.getLatitude() != null && alert.getLongitude() != null) {
            String address = amapLocationService.regeoAddress(alert.getLatitude(), alert.getLongitude());
            alert.setAddress(address);
        }
        Alert saved = alertRepository.save(alert);
        WebSocketHandler.pushAlarm(toWebSocketPayload(saved));
        return saved;
    }

    /**
     * 根据ID获取报警（含关联设备和病人信息）
     * @param id 报警ID
     * @return 填充关联信息后的报警
     */
    public Optional<Alert> getAlertWithRelations(Long id) {
        Optional<Alert> alertOpt = alertRepository.findById(id);
        alertOpt.ifPresent(this::populateRelations);
        return alertOpt;
    }

    /**
     * 根据查询条件获取报警列表（含关联信息）
     * @param condition 查询条件
     * @return 分页报警列表
     */
    public Page<Alert> getAlertsWithRelations(AlarmQueryCondition condition) {
        Page<Alert> page = queryAlerts(condition);
        page.getContent().forEach(this::populateRelations);
        return page;
    }

    /**
     * 通用报警查询（根据条件动态选择查询方法）
     * @param condition 查询条件
     * @return 分页结果
     */
    public Page<Alert> queryAlerts(AlarmQueryCondition condition) {
        Pageable pageable = PageRequest.of(condition.getPage(), condition.getSize());
        Long deviceId = condition.getDeviceId();
        Long patientId = condition.getPatientId();
        String status = condition.getStatus();
        Date startTime = condition.getStartTime();
        Date endTime = condition.getEndTime();

        if (deviceId != null && status != null) {
            return alertRepository.findByDeviceIdAndStatusOrderByAlertTimeDesc(deviceId, status, pageable);
        } else if (patientId != null && status != null) {
            return alertRepository.findByPatientIdAndStatusOrderByAlertTimeDesc(patientId, status, pageable);
        } else if (deviceId != null) {
            return alertRepository.findByDeviceIdOrderByAlertTimeDesc(deviceId, pageable);
        } else if (patientId != null) {
            return alertRepository.findByPatientIdOrderByAlertTimeDesc(patientId, pageable);
        } else if (status != null) {
            return alertRepository.findByStatusOrderByAlertTimeDesc(status, pageable);
        } else if (startTime != null && endTime != null) {
            return alertRepository.findByAlertTimeBetweenOrderByAlertTimeDesc(startTime, endTime, pageable);
        } else {
            return alertRepository.findAll(pageable);
        }
    }

    /**
     * 获取最近的报警记录（含关联信息）
     * @param limit 最大返回数量
     * @return 报警列表
     */
    public List<Alert> getRecentAlertsWithRelations(int limit) {
        List<Alert> allRecent = alertRepository.findTop100ByOrderByAlertTimeDesc();
        List<Alert> result = allRecent.stream().limit(limit).toList();
        result.forEach(this::populateRelations);
        return result;
    }

    /**
     * 获取未读报警数量
     * @return 未读报警数量
     */
    public long getUnreadAlertCount() {
        return alertRepository.countByIsReadNot(1);
    }

    /**
     * 获取指定设备的未读报警数量
     * @param deviceId 设备ID
     * @return 未读报警数量
     */
    public long getUnreadAlertCountByDeviceId(Long deviceId) {
        return alertRepository.countByDeviceIdAndIsReadNot(deviceId, 1);
    }

    /**
     * 获取指定病人的未读报警数量
     * @param patientId 病人ID
     * @return 未读报警数量
     */
    public long getUnreadAlertCountByPatientId(Long patientId) {
        return alertRepository.countByPatientIdAndIsReadNot(patientId, 1);
    }

    /**
     * 批量标记设备的报警为已读或未读
     * @param deviceId 设备ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 是否标记成功
     */
    @Transactional
    public boolean markDeviceAlertsAsRead(Long deviceId, Integer read) {
        return alertRepository.markReadByDeviceId(deviceId, read) > 0;
    }

    /**
     * 批量标记病人的报警为已读或未读
     * @param patientId 病人ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 是否标记成功
     */
    @Transactional
    public boolean markPatientAlertsAsRead(Long patientId, Integer read) {
        return alertRepository.markReadByPatientId(patientId, read) > 0;
    }

    /**
     * 标记报警为已读或未读
     * @param id 报警ID
     * @param read 已读标记值（0-未读，1-已读）
     * @return 是否标记成功
     */
    @Transactional
    public boolean markAlertAsRead(Long id, Integer read) {
        return alertRepository.markRead(id, read) > 0;
    }

    /**
     * 处理报警，更新状态、处理结果、备注和处理时间
     * @param id 报警ID
     * @param status 新状态
     * @param result 处理结果
     * @param remark 处理备注
     * @return 是否处理成功
     */
    @Transactional
    public boolean handleAlert(Long id, String status, String result, String remark) {
        Date handledTime = new Date();
        boolean success = alertRepository.handleAlert(id, status, result, remark, handledTime) > 0;
        if (success) {
            Optional<Alert> updatedAlert = alertRepository.findById(id);
            updatedAlert.ifPresent(a -> WebSocketHandler.pushAlarmUpdate(toWebSocketPayload(a)));
        }
        return success;
    }

    /**
     * 获取报警统计数据
     * @return 报警统计DTO
     */
    public AlarmStatsDto getAlertStats() {
        AlarmStatsDto stats = new AlarmStatsDto();
        stats.setTotal(alertRepository.count());
        stats.setPending(alertRepository.countByStatus("pending"));
        stats.setHandled(alertRepository.countByStatus("handled"));
        stats.setFalseAlarm(alertRepository.countByStatus("false_alarm"));
        stats.setUnread(alertRepository.countByIsReadNot(1));
        return stats;
    }

    /**
     * 填充报警的关联设备和病人信息
     * @param alert 报警对象
     */
    private void populateRelations(Alert alert) {
        deviceRepository.findById(alert.getDeviceId()).ifPresent(alert::setDevice);
        if (alert.getPatientId() != null) {
            patientRepository.findById(alert.getPatientId()).ifPresent(alert::setPatient);
        }
    }

    /**
     * 将Alert实体转换为前端兼容的WebSocket推送格式
     * 同时提供alarmType/triggeredTime（前端期望）和alertType/alertTime（Alert原始）字段名
     * @param alert 报警实体
     * @return 前端兼容的推送Map
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
}
