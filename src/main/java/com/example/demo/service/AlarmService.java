package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.repository.AlarmRepository;
import com.example.demo.service.AmapLocationService;
import com.example.demo.socket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AlarmService {

    @Autowired
    private AlarmRepository alarmRepository;
    
    @Autowired
    private AmapLocationService amapLocationService;
    
    // 创建报警
    @Transactional
    public Alarm createAlarm(Alarm alarm) {
        // 如果报警包含经纬度，调用高德地图API获取地址
        if (alarm.getLatitude() != null && alarm.getLongitude() != null) {
            String address = amapLocationService.regeoAddress(alarm.getLatitude(), alarm.getLongitude());
            alarm.setAddress(address);
        }
        Alarm saved = alarmRepository.save(alarm);
        // 推送报警消息给所有客户端
        WebSocketHandler.pushAlarm(saved);
        return saved;
    }
    
    // 根据ID获取报警
    public Optional<Alarm> getAlarmById(Long id) {
        return alarmRepository.findById(id);
    }
    
    // 获取报警列表
    public Page<Alarm> getAlarms(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findAll(pageable);
    }
    
    // 按设备ID获取报警列表
    public Page<Alarm> getAlarmsByDeviceId(Long deviceId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByDeviceIdOrderByTriggeredTimeDesc(deviceId, pageable);
    }
    
    // 按病人ID获取报警列表
    public Page<Alarm> getAlarmsByPatientId(Long patientId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByPatientIdOrderByTriggeredTimeDesc(patientId, pageable);
    }
    
    // 按状态获取报警列表
    public Page<Alarm> getAlarmsByStatus(String status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByStatusOrderByTriggeredTimeDesc(status, pageable);
    }
    
    // 按设备ID和状态获取报警列表
    public Page<Alarm> getAlarmsByDeviceIdAndStatus(Long deviceId, String status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByDeviceIdAndStatusOrderByTriggeredTimeDesc(deviceId, status, pageable);
    }
    
    // 按病人ID和状态获取报警列表
    public Page<Alarm> getAlarmsByPatientIdAndStatus(Long patientId, String status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByPatientIdAndStatusOrderByTriggeredTimeDesc(patientId, status, pageable);
    }
    
    // 按时间范围获取报警列表
    public Page<Alarm> getAlarmsByTimeRange(Date startTime, Date endTime, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return alarmRepository.findByTriggeredTimeBetweenOrderByTriggeredTimeDesc(startTime, endTime, pageable);
    }
    
    // 标记报警为已读
    @Transactional
    public boolean markAlarmAsRead(Long id, boolean read) {
        return alarmRepository.markRead(id, read) > 0;
    }
    
    // 批量标记设备的报警为已读
    @Transactional
    public boolean markDeviceAlarmsAsRead(Long deviceId, boolean read) {
        return alarmRepository.markReadByDeviceId(deviceId, read) > 0;
    }
    
    // 批量标记病人的报警为已读
    @Transactional
    public boolean markPatientAlarmsAsRead(Long patientId, boolean read) {
        return alarmRepository.markReadByPatientId(patientId, read) > 0;
    }
    
    // 处理报警
    @Transactional
    public boolean handleAlarm(Long id, String status, String result, String remark) {
        Date handledTime = new Date();
        boolean success = alarmRepository.handleAlarm(id, status, result, remark, handledTime) > 0;
        if (success) {
            // 获取更新后的报警信息并推送
            Optional<Alarm> updatedAlarm = alarmRepository.findById(id);
            updatedAlarm.ifPresent(WebSocketHandler::pushAlarmUpdate);
        }
        return success;
    }
    
    // 获取未读报警数量
    public long getUnreadAlarmCount() {
        return alarmRepository.countByIsReadFalse();
    }
    
    // 获取指定设备的未读报警数量
    public long getUnreadAlarmCountByDeviceId(Long deviceId) {
        return alarmRepository.countByDeviceIdAndIsReadFalse(deviceId);
    }
    
    // 获取指定病人的未读报警数量
    public long getUnreadAlarmCountByPatientId(Long patientId) {
        return alarmRepository.countByPatientIdAndIsReadFalse(patientId);
    }
    
    // 获取报警统计数据
    public Map<String, Object> getAlarmStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总报警数
        stats.put("total", alarmRepository.count());
        
        // 未处理报警数
        stats.put("pending", alarmRepository.findByStatusOrderByTriggeredTimeDesc("pending").size());
        
        // 已处理报警数
        stats.put("handled", alarmRepository.findByStatusOrderByTriggeredTimeDesc("handled").size());
        
        // 误报数
        stats.put("falseAlarm", alarmRepository.findByStatusOrderByTriggeredTimeDesc("false_alarm").size());
        
        // 未读报警数
        stats.put("unread", alarmRepository.countByIsReadFalse());
        
        return stats;
    }
    
    // 获取最近的报警记录
    public List<Alarm> getRecentAlarms(int limit) {
        List<Alarm> allRecent = alarmRepository.findTop100ByOrderByTriggeredTimeDesc();
        return allRecent.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
}