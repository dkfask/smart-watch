package com.example.demo.controller.api;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.Patient;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.service.AlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    @Autowired
    private AlarmService alarmService;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    // 创建报警
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Alarm alarm) {
        Alarm saved = alarmService.createAlarm(alarm);
        return ResponseEntity.status(201).body(saved);
    }
    
    // 获取报警详情
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Alarm> alarmOpt = alarmService.getAlarmById(id);
        if (alarmOpt.isPresent()) {
            Alarm alarm = alarmOpt.get();
            // 填充设备和病人信息
            populateAlarmRelations(alarm);
            return ResponseEntity.ok(alarm);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 获取报警列表
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status) {
        
        Page<Alarm> alarms;
        
        if (deviceId != null && status != null) {
            alarms = alarmService.getAlarmsByDeviceIdAndStatus(deviceId, status, page, size);
        } else if (patientId != null && status != null) {
            alarms = alarmService.getAlarmsByPatientIdAndStatus(patientId, status, page, size);
        } else if (deviceId != null) {
            alarms = alarmService.getAlarmsByDeviceId(deviceId, page, size);
        } else if (patientId != null) {
            alarms = alarmService.getAlarmsByPatientId(patientId, page, size);
        } else if (status != null) {
            alarms = alarmService.getAlarmsByStatus(status, page, size);
        } else {
            alarms = alarmService.getAlarms(page, size);
        }
        
        // 填充设备和病人信息
        populateAlarmRelations(alarms.getContent());
        
        return ResponseEntity.ok(alarms);
    }
    
    // 按设备ID获取报警列表
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> byDevice(
            @PathVariable long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        Page<Alarm> alarms;
        if (status != null) {
            alarms = alarmService.getAlarmsByDeviceIdAndStatus(deviceId, status, page, size);
        } else {
            alarms = alarmService.getAlarmsByDeviceId(deviceId, page, size);
        }
        
        // 填充设备和病人信息
        populateAlarmRelations(alarms.getContent());
        
        return ResponseEntity.ok(alarms);
    }
    
    // 按病人ID获取报警列表
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(
            @PathVariable long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        Page<Alarm> alarms;
        if (status != null) {
            alarms = alarmService.getAlarmsByPatientIdAndStatus(patientId, status, page, size);
        } else {
            alarms = alarmService.getAlarmsByPatientId(patientId, page, size);
        }
        
        // 填充设备和病人信息
        populateAlarmRelations(alarms.getContent());
        
        return ResponseEntity.ok(alarms);
    }
    
    // 标记报警为已读
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @PathVariable long id,
            @RequestParam(defaultValue = "true") boolean read) {
        
        boolean success = alarmService.markAlarmAsRead(id, read);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    // 批量标记设备的报警为已读
    @PutMapping("/device/{deviceId}/read")
    public ResponseEntity<?> markDeviceAlarmsRead(
            @PathVariable long deviceId,
            @RequestParam(defaultValue = "true") boolean read) {
        
        boolean success = alarmService.markDeviceAlarmsAsRead(deviceId, read);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    // 批量标记病人的报警为已读
    @PutMapping("/patient/{patientId}/read")
    public ResponseEntity<?> markPatientAlarmsRead(
            @PathVariable long patientId,
            @RequestParam(defaultValue = "true") boolean read) {
        
        boolean success = alarmService.markPatientAlarmsAsRead(patientId, read);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    // 处理报警
    @PutMapping("/{id}/handle")
    public ResponseEntity<?> handle(
            @PathVariable long id,
            @RequestParam String status,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String remark) {
        
        boolean success = alarmService.handleAlarm(id, status, result, remark);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    // 获取报警统计数据
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        Map<String, Object> stats = alarmService.getAlarmStats();
        return ResponseEntity.ok(stats);
    }
    
    // 获取未读报警数量
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long patientId) {
        
        long count;
        if (deviceId != null) {
            count = alarmService.getUnreadAlarmCountByDeviceId(deviceId);
        } else if (patientId != null) {
            count = alarmService.getUnreadAlarmCountByPatientId(patientId);
        } else {
            count = alarmService.getUnreadAlarmCount();
        }
        
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    // 获取最近的报警记录
    @GetMapping("/recent")
    public ResponseEntity<?> recent(@RequestParam(defaultValue = "10") int limit) {
        List<Alarm> alarms = alarmService.getRecentAlarms(limit);
        // 填充设备和病人信息
        populateAlarmRelations(alarms);
        return ResponseEntity.ok(alarms);
    }
    
    // 填充单个报警的设备和病人信息
    private void populateAlarmRelations(Alarm alarm) {
        // 获取设备信息
        Optional<Device> deviceOpt = deviceRepository.findById(alarm.getDeviceId());
        deviceOpt.ifPresent(alarm::setDevice);
        
        // 获取病人信息
        if (alarm.getPatientId() != null) {
            Optional<Patient> patientOpt = patientRepository.findById(alarm.getPatientId());
            patientOpt.ifPresent(alarm::setPatient);
        }
    }
    
    // 填充多个报警的设备和病人信息
    private void populateAlarmRelations(List<Alarm> alarms) {
        for (Alarm alarm : alarms) {
            populateAlarmRelations(alarm);
        }
    }
}