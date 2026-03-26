package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.Patient;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * 健康监测服务，用于检测健康数据是否异常，生成健康状态报警
 */
@Service
public class HealthMonitorService {
    private static final Logger log = LoggerFactory.getLogger(HealthMonitorService.class);
    
    private final AlarmService alarmService;
    private final DeviceRepository deviceRepository;
    private final PatientRepository patientRepository;
    
    public HealthMonitorService(AlarmService alarmService, DeviceRepository deviceRepository, PatientRepository patientRepository) {
        this.alarmService = alarmService;
        this.deviceRepository = deviceRepository;
        this.patientRepository = patientRepository;
    }
    
    /**
     * 检测健康数据是否异常，生成对应的报警
     * @param record 健康记录
     */
    public void checkHealthData(HealthRecord record) {
        if (record == null) return;
        
        String dataType = record.getDataType();
        String value = record.getValue();
        
        try {
            switch (dataType) {
                case "blood_pressure":
                    checkBloodPressure(record, value);
                    break;
                case "heart_rate":
                    checkHeartRate(record, value);
                    break;
                case "blood_oxygen":
                    checkBloodOxygen(record, value);
                    break;
                case "body_temperature":
                    checkBodyTemperature(record, value);
                    break;
                default:
                    log.debug("Unknown health data type: {}", dataType);
            }
        } catch (Exception e) {
            log.error("Failed to check health data: {}", e.getMessage());
        }
    }
    
    /**
     * 检测血压是否异常
     * 高压>140或<90，低压>90或<60
     */
    private void checkBloodPressure(HealthRecord record, String value) {
        // 血压值格式：低压|高压 mmHg
        String[] parts = value.split("\\|");
        if (parts.length != 2) return;
        
        try {
            int diastolic = Integer.parseInt(parts[0].trim());
            int systolic = Integer.parseInt(parts[1].trim());
            
            boolean isAbnormal = false;
            String alarmData = "{\"systolic\": " + systolic + ", \"diastolic\": " + diastolic + "}";
            
            if (systolic > 140 || systolic < 90 || diastolic > 90 || diastolic < 60) {
                isAbnormal = true;
            }
            
            if (isAbnormal) {
                createHealthAlarm(record, "blood_pressure_abnormal", "critical", alarmData);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid blood pressure value: {}", value);
        }
    }
    
    /**
     * 检测心率是否异常
     * 心率>100或<60
     */
    private void checkHeartRate(HealthRecord record, String value) {
        try {
            int heartRate = Integer.parseInt(value);
            
            if (heartRate > 100 || heartRate < 60) {
                String alarmData = "{\"heart_rate\": " + heartRate + "}";
                createHealthAlarm(record, "heart_rate_abnormal", "critical", alarmData);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid heart rate value: {}", value);
        }
    }
    
    /**
     * 检测血氧是否异常
     * 血氧<95%
     */
    private void checkBloodOxygen(HealthRecord record, String value) {
        try {
            int bloodOxygen = Integer.parseInt(value);
            
            if (bloodOxygen < 95) {
                String alarmData = "{\"blood_oxygen\": " + bloodOxygen + "}";
                createHealthAlarm(record, "blood_oxygen_abnormal", "critical", alarmData);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid blood oxygen value: {}", value);
        }
    }
    
    /**
     * 检测体温是否异常
     * 体温>37.3°C或<36°C
     */
    private void checkBodyTemperature(HealthRecord record, String value) {
        try {
            double temperature = Double.parseDouble(value);
            
            if (temperature > 37.3 || temperature < 36.0) {
                String alarmData = "{\"body_temperature\": " + temperature + "}";
                createHealthAlarm(record, "body_temperature_abnormal", "critical", alarmData);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid body temperature value: {}", value);
        }
    }
    
    /**
     * 创建健康状态报警
     */
    private void createHealthAlarm(HealthRecord record, String alarmType, String alarmLevel, String alarmData) {
        Alarm alarm = new Alarm();
        
        // 获取设备信息
        Optional<Device> deviceOptional = deviceRepository.findById(record.getDeviceId());
        if (deviceOptional.isPresent()) {
            alarm.setDevice(deviceOptional.get());
        }
        
        // 获取病人信息
        Optional<Patient> patientOptional = patientRepository.findById(record.getPatientId());
        if (patientOptional.isPresent()) {
            alarm.setPatient(patientOptional.get());
        }
        
        alarm.setAlarmType(alarmType);
        alarm.setAlarmLevel(alarmLevel);
        alarm.setAlarmData(alarmData);
        alarm.setTriggeredTime(new Date());
        
        alarmService.createAlarm(alarm);
        log.info("Created health alarm for device={}, type={}", record.getDeviceId(), alarmType);
    }
}