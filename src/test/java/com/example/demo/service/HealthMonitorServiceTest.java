package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.HeartbeatRecord;
import com.example.demo.model.Patient;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.HeartbeatRecordRepository;
import com.example.demo.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HealthMonitorService单元测试
 */
@ExtendWith(MockitoExtension.class)
class HealthMonitorServiceTest {

    @Mock private AlarmService alarmService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private HeartbeatRecordRepository heartbeatRecordRepository;

    private HealthMonitorService healthMonitorService;

    @BeforeEach
    void setUp() {
        healthMonitorService = new HealthMonitorService(alarmService, deviceRepository, patientRepository, heartbeatRecordRepository);
    }

    // === 血压测试 ===

    /**
     * 血压异常（高压>140）创建报警
     */
    @Test
    void checkHealthData_bloodPressure_systolicHigh_createsAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "85|160");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).createAlarm(captor.capture());
        assertEquals("blood_pressure_abnormal", captor.getValue().getAlarmType());
        assertEquals("critical", captor.getValue().getAlarmLevel());
        assertTrue(captor.getValue().getAlarmData().contains("160"));
    }

    /**
     * 血压异常（低压<60）创建报警
     */
    @Test
    void checkHealthData_bloodPressure_diastolicLow_createsAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "55|120");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getAlarmType().equals("blood_pressure_abnormal")));
    }

    /**
     * 血压异常（高压<90）创建报警
     */
    @Test
    void checkHealthData_bloodPressure_systolicLow_createsAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "70|85");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(any(Alarm.class));
    }

    /**
     * 血压异常（低压>90）创建报警
     */
    @Test
    void checkHealthData_bloodPressure_diastolicHigh_createsAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "95|130");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(any(Alarm.class));
    }

    /**
     * 血压正常不创建报警
     */
    @Test
    void checkHealthData_bloodPressure_normal_noAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "75|120");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 血压格式无效不创建报警
     */
    @Test
    void checkHealthData_bloodPressure_invalidFormat_noAlarm() {
        HealthRecord record = buildHealthRecord("blood_pressure", "120");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    // === 心率测试 ===

    /**
     * 心率过快（>100）创建报警
     */
    @Test
    void checkHealthData_heartRate_tooHigh_createsAlarm() {
        HealthRecord record = buildHealthRecord("heart_rate", "120");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).createAlarm(captor.capture());
        assertEquals("heart_rate_abnormal", captor.getValue().getAlarmType());
        assertEquals("critical", captor.getValue().getAlarmLevel());
    }

    /**
     * 心率过慢（<60）创建报警
     */
    @Test
    void checkHealthData_heartRate_tooLow_createsAlarm() {
        HealthRecord record = buildHealthRecord("heart_rate", "45");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getAlarmType().equals("heart_rate_abnormal")));
    }

    /**
     * 心率正常不创建报警
     */
    @Test
    void checkHealthData_heartRate_normal_noAlarm() {
        HealthRecord record = buildHealthRecord("heart_rate", "75");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    // === 血氧测试 ===

    /**
     * 血氧过低（<95%）创建报警
     */
    @Test
    void checkHealthData_bloodOxygen_tooLow_createsAlarm() {
        HealthRecord record = buildHealthRecord("blood_oxygen", "90");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).createAlarm(captor.capture());
        assertEquals("blood_oxygen_abnormal", captor.getValue().getAlarmType());
    }

    /**
     * 血氧正常不创建报警
     */
    @Test
    void checkHealthData_bloodOxygen_normal_noAlarm() {
        HealthRecord record = buildHealthRecord("blood_oxygen", "98");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    // === 体温测试 ===

    /**
     * 体温过高（>37.3°C）创建报警
     */
    @Test
    void checkHealthData_bodyTemperature_tooHigh_createsAlarm() {
        HealthRecord record = buildHealthRecord("body_temperature", "38.5");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).createAlarm(captor.capture());
        assertEquals("body_temperature_abnormal", captor.getValue().getAlarmType());
    }

    /**
     * 体温过低（<36°C）创建报警
     */
    @Test
    void checkHealthData_bodyTemperature_tooLow_createsAlarm() {
        HealthRecord record = buildHealthRecord("body_temperature", "35.5");
        setupDeviceAndPatient();
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getAlarmType().equals("body_temperature_abnormal")));
    }

    /**
     * 体温正常不创建报警
     */
    @Test
    void checkHealthData_bodyTemperature_normal_noAlarm() {
        HealthRecord record = buildHealthRecord("body_temperature", "36.8");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    @Test
    void checkHealthData_bodyTemperature_abnormalButNotWorn_noAlarm() {
        HealthRecord record = buildHealthRecord("body_temperature", "35.0");
        record.setImei("359999000000001");
        HeartbeatRecord wearStatus = new HeartbeatRecord();
        wearStatus.setImei("359999000000001");
        wearStatus.setRawPayload("{wear_flag=0, timestamp=2026-07-09 09:00:00}");
        when(heartbeatRecordRepository.findByImeiOrderByRecvTimeDesc("359999000000001"))
                .thenReturn(List.of(wearStatus));

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    // === 边界和异常测试 ===

    /**
     * 未知数据类型不创建报警
     */
    @Test
    void checkHealthData_unknownType_noAlarm() {
        HealthRecord record = buildHealthRecord("unknown_type", "100");

        healthMonitorService.checkHealthData(record);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * null记录不创建报警
     */
    @Test
    void checkHealthData_nullRecord_noAlarm() {
        healthMonitorService.checkHealthData(null);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 报警创建时正确填充设备和病人信息
     */
    @Test
    void checkHealthData_fillsDeviceAndPatientInfo() {
        HealthRecord record = buildHealthRecord("heart_rate", "120");
        Device device = new Device();
        device.setId(1L);
        Patient patient = new Patient();
        patient.setId(10L);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getDevice() == device && alarm.getPatient() == patient));
    }

    /**
     * 设备不存在时仍创建报警（device字段为null）
     */
    @Test
    void checkHealthData_deviceNotFound_stillCreatesAlarm() {
        HealthRecord record = buildHealthRecord("heart_rate", "120");
        when(deviceRepository.findById(1L)).thenReturn(Optional.empty());
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        healthMonitorService.checkHealthData(record);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getDevice() == null && alarm.getPatient() != null));
    }

    // === 辅助方法 ===

    private HealthRecord buildHealthRecord(String dataType, String value) {
        HealthRecord record = new HealthRecord();
        record.setDeviceId(1L);
        record.setPatientId(10L);
        record.setDataType(dataType);
        record.setValue(value);
        return record;
    }

    private void setupDeviceAndPatient() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));
    }
}
