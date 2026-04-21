package com.example.demo.model.dto;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.GeoFence;
import com.example.demo.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO工厂方法单元测试
 */
class DtoTest {

    /**
     * 测试DeviceInfoDto.fromDevice()字段正确映射
     */
    @Test
    void fromDevice_mapsAllFields() {
        Device device = new Device();
        device.setId(1L);
        device.setImei("IMEI123");
        device.setMcc("460");
        device.setMnc("01");
        device.setApn("cmnet");
        device.setIccid("ICCID456");
        device.setImsi("IMSI789");
        device.setCreatedAt(new Date());
        device.setUpdatedAt(new Date());

        DeviceInfoDto dto = DeviceInfoDto.fromDevice(device);

        assertEquals(1L, dto.getId());
        assertEquals("IMEI123", dto.getImei());
        assertEquals("460", dto.getMcc());
        assertEquals("01", dto.getMnc());
        assertEquals("cmnet", dto.getApn());
        assertEquals("ICCID456", dto.getIccid());
        assertEquals("IMSI789", dto.getImsi());
        assertNull(dto.getIsOnline());
        assertNull(dto.getPatient());
    }

    /**
     * 测试DeviceInfoDto.fillStatus()状态填充
     */
    @Test
    void fillStatus_populatesOnlineAndLocation() {
        DeviceInfoDto dto = new DeviceInfoDto();
        DeviceStatus status = new DeviceStatus();
        status.setLastLatitude(39.9087);
        status.setLastLongitude(116.3975);
        status.setBatteryLevel(85);

        dto.fillStatus(status, true);

        assertTrue(dto.getIsOnline());
        assertEquals(39.9087, dto.getLastLatitude());
        assertEquals(116.3975, dto.getLastLongitude());
        assertEquals(85, dto.getBatteryLevel());
    }

    /**
     * 测试DeviceInfoDto.fillStatus() null状态时只设置在线
     */
    @Test
    void fillStatus_nullStatus_setsOnlineOnly() {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.fillStatus(null, false);

        assertFalse(dto.getIsOnline());
        assertNull(dto.getLastLatitude());
        assertNull(dto.getBatteryLevel());
    }

    /**
     * 测试DeviceInfoDto.fillPatient()病人信息填充
     */
    @Test
    void fillPatient_populatesPatientInfo() {
        DeviceInfoDto dto = new DeviceInfoDto();
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("张三");
        patient.setGender("男");
        patient.setAge(65);
        patient.setWard("A区");
        patient.setBed("101");
        patient.setPhone("13800138000");

        dto.fillPatient(patient);

        assertNotNull(dto.getPatient());
        assertEquals(10L, dto.getPatient().getId());
        assertEquals("张三", dto.getPatient().getName());
        assertEquals("男", dto.getPatient().getGender());
        assertEquals(65, dto.getPatient().getAge());
    }

    /**
     * 测试DeviceInfoDto.fillPatient() null时不设置
     */
    @Test
    void fillPatient_null_doesNotSet() {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.fillPatient(null);
        assertNull(dto.getPatient());
    }

    /**
     * 测试FenceDto.fromFence()字段正确映射
     */
    @Test
    void fromFence_mapsAllFields() {
        GeoFence fence = new GeoFence();
        fence.setId(1L);
        fence.setName("测试围栏");
        fence.setType("circle");
        fence.setCenterLat(39.9087);
        fence.setCenterLng(116.3975);
        fence.setRadius(500);
        fence.setCoordinates("[]");
        fence.setStatus("active");
        fence.setDescription("描述");
        fence.setCreatedBy(1L);
        fence.setPatientId(10L);
        fence.setIsMultiPatient(true);
        fence.setCreatedAt(new Date());
        fence.setUpdatedAt(new Date());

        FenceDto dto = FenceDto.fromFence(fence);

        assertEquals(1L, dto.getId());
        assertEquals("测试围栏", dto.getName());
        assertEquals("circle", dto.getType());
        assertEquals(39.9087, dto.getCenterLat());
        assertEquals(116.3975, dto.getCenterLng());
        assertEquals(500, dto.getRadius());
        assertEquals("active", dto.getStatus());
        assertEquals(10L, dto.getPatientId());
        assertTrue(dto.getIsMultiPatient());
        assertNull(dto.getPatientIds());
    }

    /**
     * 测试PageResponse.from()从Spring Data Page构建
     */
    @Test
    void from_springDataPage_mapsCorrectly() {
        List<String> items = List.of("a", "b", "c");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 25);

        PageResponse<String> response = PageResponse.from(page);

        assertEquals(3, response.getContent().size());
        assertEquals(25, response.getTotal());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(3, response.getTotalPages());
    }

    /**
     * 测试PageResponse构造函数计算totalPages
     */
    @Test
    void constructor_calculatesTotalPages() {
        PageResponse<String> response = new PageResponse<>(List.of("a", "b"), 25, 0, 10);
        assertEquals(3, response.getTotalPages());
    }

    /**
     * 测试PageResponse构造函数size为0时totalPages为0
     */
    @Test
    void constructor_zeroSize_totalPagesIsZero() {
        PageResponse<String> response = new PageResponse<>(List.of(), 0, 0, 0);
        assertEquals(0, response.getTotalPages());
    }

    /**
     * 测试AlarmQueryCondition.hasFilter()各种条件组合
     */
    @Test
    void hasFilter_withDeviceId_returnsTrue() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setDeviceId(1L);
        assertTrue(cond.hasFilter());
    }

    @Test
    void hasFilter_withPatientId_returnsTrue() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPatientId(1L);
        assertTrue(cond.hasFilter());
    }

    @Test
    void hasFilter_withStatus_returnsTrue() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setStatus("pending");
        assertTrue(cond.hasFilter());
    }

    @Test
    void hasFilter_withStartTime_returnsTrue() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setStartTime(new Date());
        assertTrue(cond.hasFilter());
    }

    @Test
    void hasFilter_noFilter_returnsFalse() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        assertFalse(cond.hasFilter());
    }
}
