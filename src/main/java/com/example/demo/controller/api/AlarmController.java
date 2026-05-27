package com.example.demo.controller.api;

import com.example.demo.model.Alert;
import com.example.demo.model.Alarm;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.service.AlertService;
import com.example.demo.service.AlarmService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;
    private final AlertService alertService;

    /**
     * 构造器注入依赖
     * @param alarmService 报警服务（保留用于双写创建）
     * @param alertService 统一报警服务（用于读取操作）
     */
    public AlarmController(AlarmService alarmService, AlertService alertService) {
        this.alarmService = alarmService;
        this.alertService = alertService;
    }

    /**
     * 创建报警（保留双写逻辑，同时写入Alarm和Alert表）
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Alarm alarm) {
        Alarm saved = alarmService.createAlarm(alarm);
        return ResponseEntity.status(201).body(saved);
    }

    /**
     * 获取报警详情（含关联设备和病人信息）
     * 从Alert表读取，字段映射为前端兼容格式
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Alert> alertOpt = alertService.getAlertWithRelations(id);
        return alertOpt.<ResponseEntity<?>>map(alert -> ResponseEntity.ok(toApiResponse(alert)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 获取报警列表（支持多条件过滤，统一返回PageResponse）
     * 从Alert表读取，字段映射为前端兼容格式
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status) {

        AlarmQueryCondition condition = new AlarmQueryCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setDeviceId(deviceId);
        condition.setPatientId(patientId);
        condition.setStatus(status);

        Page<Alert> alerts = alertService.getAlertsWithRelations(condition);
        List<Map<String, Object>> content = alerts.getContent().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
        PageResponse<Map<String, Object>> response = new PageResponse<>(
                content, alerts.getTotalElements(), alerts.getNumber(), alerts.getSize());
        return ResponseEntity.ok(response);
    }

    /**
     * 按设备ID获取报警列表
     * 从Alert表读取，字段映射为前端兼容格式
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> byDevice(
            @PathVariable long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        AlarmQueryCondition condition = new AlarmQueryCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setDeviceId(deviceId);
        condition.setStatus(status);

        Page<Alert> alerts = alertService.getAlertsWithRelations(condition);
        List<Map<String, Object>> content = alerts.getContent().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
        PageResponse<Map<String, Object>> response = new PageResponse<>(
                content, alerts.getTotalElements(), alerts.getNumber(), alerts.getSize());
        return ResponseEntity.ok(response);
    }

    /**
     * 按病人ID获取报警列表
     * 从Alert表读取，字段映射为前端兼容格式
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(
            @PathVariable long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        AlarmQueryCondition condition = new AlarmQueryCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setPatientId(patientId);
        condition.setStatus(status);

        Page<Alert> alerts = alertService.getAlertsWithRelations(condition);
        List<Map<String, Object>> content = alerts.getContent().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
        PageResponse<Map<String, Object>> response = new PageResponse<>(
                content, alerts.getTotalElements(), alerts.getNumber(), alerts.getSize());
        return ResponseEntity.ok(response);
    }

    /**
     * 标记报警为已读
     * 通过AlertService操作Alert表
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @PathVariable long id,
            @RequestParam(defaultValue = "true") boolean read) {
        Integer readValue = read ? 1 : 0;
        boolean success = alertService.markAlertAsRead(id, readValue);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 批量标记设备的报警为已读
     * 通过AlertService操作Alert表
     */
    @PutMapping("/device/{deviceId}/read")
    public ResponseEntity<?> markDeviceAlarmsRead(
            @PathVariable long deviceId,
            @RequestParam(defaultValue = "true") boolean read) {
        Integer readValue = read ? 1 : 0;
        boolean success = alertService.markDeviceAlertsAsRead(deviceId, readValue);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 批量标记病人的报警为已读
     * 通过AlertService操作Alert表
     */
    @PutMapping("/patient/{patientId}/read")
    public ResponseEntity<?> markPatientAlarmsRead(
            @PathVariable long patientId,
            @RequestParam(defaultValue = "true") boolean read) {
        Integer readValue = read ? 1 : 0;
        boolean success = alertService.markPatientAlertsAsRead(patientId, readValue);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 处理报警（接收JSON body，兼容前端请求方式）
     * 通过AlertService操作Alert表
     */
    @PutMapping("/{id}/handle")
    public ResponseEntity<?> handle(@PathVariable long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String result = body.get("result");
        String remark = body.get("remark");
        boolean success = alertService.handleAlert(id, status, result, remark);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 获取报警统计数据
     * 从Alert表读取统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<AlarmStatsDto> stats() {
        return ResponseEntity.ok(alertService.getAlertStats());
    }

    /**
     * 获取未读报警数量
     * 从Alert表读取未读数量，支持按设备ID或病人ID过滤
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long patientId) {
        long count;
        if (deviceId != null) {
            count = alertService.getUnreadAlertCountByDeviceId(deviceId);
        } else if (patientId != null) {
            count = alertService.getUnreadAlertCountByPatientId(patientId);
        } else {
            count = alertService.getUnreadAlertCount();
        }
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 获取最近的报警记录
     * 从Alert表读取，字段映射为前端兼容格式
     */
    @GetMapping("/recent")
    public ResponseEntity<?> recent(@RequestParam(defaultValue = "10") int limit) {
        List<Alert> alerts = alertService.getRecentAlertsWithRelations(limit);
        List<Map<String, Object>> result = alerts.stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 将Alert实体转换为前端兼容的响应格式
     * 同时提供Alarm风格和Alert风格的字段名，确保前端无需修改即可兼容
     * @param alert 报警实体
     * @return 前端兼容的响应Map
     */
    private Map<String, Object> toApiResponse(Alert alert) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", alert.getId());
        map.put("deviceId", alert.getDeviceId());
        map.put("patientId", alert.getPatientId());
        map.put("imei", alert.getImei());
        map.put("fenceId", alert.getFenceId());
        map.put("alarmType", alert.getAlertType());
        map.put("alertType", alert.getAlertType());
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
        map.put("device", alert.getDevice());
        map.put("patient", alert.getPatient());
        map.put("createdAt", alert.getCreatedAt());
        map.put("updatedAt", alert.getUpdatedAt());
        return map;
    }
}
