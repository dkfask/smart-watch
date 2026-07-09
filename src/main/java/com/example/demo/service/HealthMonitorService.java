package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.HeartbeatRecord;
import com.example.demo.model.Patient;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
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
    private final HeartbeatRecordRepository heartbeatRecordRepository;
    
    public HealthMonitorService(AlarmService alarmService,
                                DeviceRepository deviceRepository,
                                PatientRepository patientRepository,
                                HeartbeatRecordRepository heartbeatRecordRepository) {
        this.alarmService = alarmService;
        this.deviceRepository = deviceRepository;
        this.patientRepository = patientRepository;
        this.heartbeatRecordRepository = heartbeatRecordRepository;
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
                if (isDeviceKnownNotWorn(record)) {
                    log.info("Skip body temperature alarm because bracelet is not worn. device={}, imei={}, temperature={}",
                            record.getDeviceId(), record.getImei(), temperature);
                    return;
                }
                String alarmData = "{\"body_temperature\": " + temperature + "}";
                createHealthAlarm(record, "body_temperature_abnormal", "critical", alarmData);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid body temperature value: {}", value);
        }
    }

    private boolean isDeviceKnownNotWorn(HealthRecord record) {
        String imei = resolveImei(record);
        if (imei == null || imei.isBlank()) {
            return false;
        }

        try {
            List<HeartbeatRecord> records = heartbeatRecordRepository.findByImeiOrderByRecvTimeDesc(imei);
            if (records == null) {
                return false;
            }
            for (HeartbeatRecord heartbeat : records) {
                Optional<Boolean> worn = parseWearFlag(heartbeat);
                if (worn.isPresent()) {
                    return !worn.get();
                }
            }
        } catch (Exception e) {
            log.warn("Unable to read bracelet wear status for imei={}: {}", imei, e.getMessage());
        }
        return false;
    }

    private String resolveImei(HealthRecord record) {
        if (record.getImei() != null && !record.getImei().isBlank()) {
            return record.getImei();
        }
        Long deviceId = record.getDeviceId();
        if (deviceId == null) {
            return null;
        }
        try {
            return deviceRepository.findById(deviceId)
                    .map(Device::getImei)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Unable to resolve imei for device={}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    private Optional<Boolean> parseWearFlag(HeartbeatRecord heartbeat) {
        if (heartbeat == null) {
            return Optional.empty();
        }
        String text = firstNonBlank(heartbeat.getRawPayload(), heartbeat.getStatusBlock());
        if (text == null) {
            return Optional.empty();
        }
        String lower = text.toLowerCase();
        String flag = extractValue(lower, "wear_flag");
        if (flag == null) flag = extractValue(lower, "wear");
        if (flag == null) flag = extractValue(lower, "worn");
        if (flag == null) return Optional.empty();

        flag = flag.trim();
        if (flag.equals("1") || flag.equals("true") || flag.equals("yes")
                || flag.equals("on") || flag.equals("worn") || flag.equals("wearing")) {
            return Optional.of(true);
        }
        if (flag.equals("0") || flag.equals("false") || flag.equals("no")
                || flag.equals("off") || flag.equals("not_worn") || flag.equals("not-worn")
                || flag.equals("unworn")) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private String extractValue(String text, String key) {
        int idx = text.indexOf(key + "=");
        int offset = 1;
        if (idx < 0) {
            idx = text.indexOf(key + ":");
            offset = 1;
        }
        if (idx < 0) {
            return null;
        }
        int start = idx + key.length() + offset;
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c == ',' || c == '}' || c == ';' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        return text.substring(start, end).replace("\"", "").replace("'", "");
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
