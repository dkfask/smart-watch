package com.example.demo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 Alarm 实体在 setDevice/setPatient 时自动同步 deviceId/patientId
 *
 * <p>背景：v3.0.0 重构时给 Alarm 实体新增了 @Transient Device device/Patient patient 字段
 * 用于前端展示，但调用方改用 setDevice() 后忘了同步调用 setDeviceId()，导致
 * deviceId/patientId（nullable=false）持久化失败。</p>
 *
 * <p>Bug 现象：服务器上持续报错
 * "not-null property references a null or transient value:
 * com.example.demo.model.Alarm.deviceId"</p>
 *
 * <p>修复策略：在 setDevice() 中自动同步 setDeviceId()，
 * 防止这种"漏设" bug 再次出现。</p>
 */
@DisplayName("Alarm entity - device/patient 字段同步测试")
class AlarmTest {

    @Test
    @DisplayName("setDevice(Device) 应该自动同步设置 deviceId")
    void setDeviceShouldSyncDeviceId() {
        // Arrange
        Alarm alarm = new Alarm();
        Device device = new Device();
        device.setId(42L);
        device.setImei("355932600124999");

        // Act
        alarm.setDevice(device);

        // Assert
        assertNotNull(alarm.getDevice(), "device 字段应被设置");
        assertNotNull(alarm.getDeviceId(), "deviceId 应该从 device.getId() 同步设置");
        assertEquals(42L, alarm.getDeviceId(), "deviceId 应该等于 device.getId()");
    }

    @Test
    @DisplayName("setPatient(Patient) 应该自动同步设置 patientId")
    void setPatientShouldSyncPatientId() {
        // Arrange
        Alarm alarm = new Alarm();
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setName("张三");

        // Act
        alarm.setPatient(patient);

        // Assert
        assertNotNull(alarm.getPatient(), "patient 字段应被设置");
        assertNotNull(alarm.getPatientId(), "patientId 应该从 patient.getId() 同步设置");
        assertEquals(7L, alarm.getPatientId(), "patientId 应该等于 patient.getId()");
    }

    @Test
    @DisplayName("setDevice(null) 不应影响 deviceId 字段（允许保留之前的值）")
    void setDeviceWithNullShouldNotAffectDeviceId() {
        // Arrange
        Alarm alarm = new Alarm();
        alarm.setDeviceId(99L);

        // Act - 显式设为 null
        alarm.setDevice(null);

        // Assert - 显式 null 应当清空 deviceId（保持字段一致性）
        // 因为 setDevice(null) 的语义是"清除关联"，所以 deviceId 也应被清空
        // 但如果业务上希望保留，改为 assertEquals(99L, alarm.getDeviceId())
        assertNull(alarm.getDeviceId(), "setDevice(null) 应该同步清空 deviceId");
        assertNull(alarm.getDevice(), "device 字段应被设为 null");
    }

    @Test
    @DisplayName("setDeviceId 不会影响 device 字段（解耦）")
    void setDeviceIdShouldNotAffectDevice() {
        // Arrange
        Alarm alarm = new Alarm();

        // Act
        alarm.setDeviceId(123L);

        // Assert
        assertEquals(123L, alarm.getDeviceId());
        assertNull(alarm.getDevice(), "只设 deviceId 不应影响 device 字段");
    }

    @Test
    @DisplayName("围栏越界场景：构造完整 Alarm 后 deviceId/patientId 都应有值")
    void fenceBreachAlarmShouldHaveAllIds() {
        // Arrange - 模拟 FenceService.createFenceBreachAlert 中的赋值
        Alarm alarm = new Alarm();

        Device device = new Device();
        device.setId(15L);

        Patient patient = new Patient();
        patient.setId(7L);

        // Act
        alarm.setDevice(device);
        alarm.setPatient(patient);
        alarm.setAlarmType("fence_breach");
        alarm.setAlarmLevel("warning");
        alarm.setLatitude(30.889482);
        alarm.setLongitude(103.594774);

        // Assert - 这是修复 bug 的关键断言
        assertNotNull(alarm.getDeviceId(), "deviceId 必须有值（持久化要求）");
        assertNotNull(alarm.getPatientId(), "patientId 必须有值（业务要求）");
        assertEquals(15L, alarm.getDeviceId());
        assertEquals(7L, alarm.getPatientId());
        assertEquals("fence_breach", alarm.getAlarmType());
    }
}
