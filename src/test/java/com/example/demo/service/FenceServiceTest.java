package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.dto.FenceCreateRequest;
import com.example.demo.model.dto.FenceDto;
import com.example.demo.model.dto.FenceUpdateRequest;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FenceService单元测试
 */
@ExtendWith(MockitoExtension.class)
class FenceServiceTest {

    @Mock private GeoFenceRepository fenceRepo;
    @Mock private FenceAlertRepository alertRepo;
    @Mock private SimpleFenceAlertRepository simpleAlertRepo;
    @Mock private PatientDeviceRepository patientDeviceRepo;
    @Mock private DeviceRepository deviceRepo;
    @Mock private AlarmService alarmService;
    @Mock private JdbcTemplate jdbc;
    @Mock private AlarmRepository alarmRepo;
    @Mock private DeviceStatusRepository statusRepo;
    @Mock private FencePatientRepository fencePatientRepo;
    @Mock private AlertRepository alertRepository;

    private FenceService fenceService;

    @BeforeEach
    void setUp() {
        fenceService = new FenceService(fenceRepo, alertRepo, simpleAlertRepo, patientDeviceRepo, deviceRepo, alarmService, jdbc, alarmRepo, statusRepo, fencePatientRepo, alertRepository);
    }

    /**
     * 创建围栏并绑定病人
     */
    @Test
    void createFence_createsAndBindsPatients() {
        FenceCreateRequest req = new FenceCreateRequest();
        req.setName("测试围栏");
        req.setType("circle");
        req.setCenterLat(39.9087);
        req.setCenterLng(116.3975);
        req.setRadius(500);
        req.setStatus("active");
        req.setPatientIds(List.of(1L, 2L));
        when(fenceRepo.create(any(GeoFence.class))).thenReturn(1L);

        long id = fenceService.createFence(req);

        assertEquals(1L, id);
        verify(fencePatientRepo).bindPatientsToFence(eq(1L), eq(List.of(1L, 2L)));
    }

    /**
     * 更新围栏并重新绑定病人
     */
    @Test
    void updateFence_updatesAndRebindsPatients() {
        FenceUpdateRequest req = new FenceUpdateRequest();
        req.setName("更新围栏");
        req.setType("circle");
        req.setCenterLat(39.9087);
        req.setCenterLng(116.3975);
        req.setRadius(600);
        req.setStatus("active");
        req.setPatientIds(List.of(3L));
        when(fenceRepo.update(any(GeoFence.class))).thenReturn(1);

        boolean result = fenceService.updateFence(1L, req);

        assertTrue(result);
        verify(fencePatientRepo).clearFencePatients(1L);
        verify(fencePatientRepo).bindPatientsToFence(eq(1L), eq(List.of(3L)));
    }

    /**
     * 更新围栏不存在返回false
     */
    @Test
    void updateFence_notExists_returnsFalse() {
        FenceUpdateRequest req = new FenceUpdateRequest();
        req.setName("不存在");
        when(fenceRepo.update(any(GeoFence.class))).thenReturn(0);

        boolean result = fenceService.updateFence(999L, req);

        assertFalse(result);
    }

    /**
     * getFenceDetail围栏不存在返回null
     */
    @Test
    void getFenceDetail_notExists_returnsNull() {
        when(fenceRepo.findById(999L)).thenReturn(Optional.empty());
        assertNull(fenceService.getFenceDetail(999L));
    }

    /**
     * getFenceDetail返回围栏详情含patientIds
     */
    @Test
    void getFenceDetail_exists_returnsDtoWithPatientIds() {
        GeoFence fence = new GeoFence();
        fence.setId(1L);
        fence.setName("围栏1");
        fence.setPatientId(10L);
        when(fenceRepo.findById(1L)).thenReturn(Optional.of(fence));
        when(fencePatientRepo.findByFenceId(1L)).thenReturn(Collections.emptyList());

        FenceDto dto = fenceService.getFenceDetail(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("围栏1", dto.getName());
        assertNotNull(dto.getPatientIds());
        assertTrue(dto.getPatientIds().contains(10L));
    }

    /**
     * listFencesWithPatients批量构建DTO
     */
    @Test
    void listFencesWithPatients_buildsDtos() {
        GeoFence f1 = new GeoFence();
        f1.setId(1L);
        f1.setName("围栏1");
        GeoFence f2 = new GeoFence();
        f2.setId(2L);
        f2.setName("围栏2");
        when(fencePatientRepo.findByFenceId(anyLong())).thenReturn(Collections.emptyList());

        List<FenceDto> dtos = fenceService.listFencesWithPatients(List.of(f1, f2));

        assertEquals(2, dtos.size());
        assertEquals("围栏1", dtos.get(0).getName());
        assertEquals("围栏2", dtos.get(1).getName());
    }

    /**
     * deleteFence成功
     */
    @Test
    void deleteFence_success() {
        when(fenceRepo.delete(1L)).thenReturn(1);
        assertTrue(fenceService.deleteFence(1L));
    }

    /**
     * deleteFence失败
     */
    @Test
    void deleteFence_notExists() {
        when(fenceRepo.delete(999L)).thenReturn(0);
        assertFalse(fenceService.deleteFence(999L));
    }

    /**
     * 设备没有关联病人时不创建报警
     */
    @Test
    void checkFencesAndAlert_noPatientDevices_doesNotCreateAlarm() {
        DeviceLocation curr = buildDeviceLocation(1L, 39.0, 116.0);
        when(patientDeviceRepo.findByDeviceId(1L)).thenReturn(Collections.emptyList());

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
        verify(simpleAlertRepo, never()).save(any());
    }

    /**
     * 设备不存在时不创建报警
     */
    @Test
    void checkFencesAndAlert_deviceNotFound_doesNotCreateAlarm() {
        DeviceLocation curr = buildDeviceLocation(1L, 39.0, 116.0);
        PatientDevice pd = new PatientDevice();
        pd.setPatientId(10L);
        when(patientDeviceRepo.findByDeviceId(1L)).thenReturn(List.of(pd));
        when(deviceRepo.findById(1L)).thenReturn(Optional.empty());

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 围栏状态为inactive时跳过不报警
     */
    @Test
    void checkFencesAndAlert_inactiveFence_skips() {
        DeviceLocation curr = buildDeviceLocation(1L, 39.0, 116.0);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence = buildFence(1L, "inactive", "circle", 39.9087, 116.3975, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 围栏类型非circle时跳过不报警
     */
    @Test
    void checkFencesAndAlert_nonCircleFence_skips() {
        DeviceLocation curr = buildDeviceLocation(1L, 39.0, 116.0);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence = buildFence(1L, "active", "polygon", 39.9087, 116.3975, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 设备在围栏内时不创建报警
     */
    @Test
    void checkFencesAndAlert_insideFence_doesNotCreateAlarm() {
        DeviceLocation curr = buildDeviceLocation(1L, 39.9087, 116.3975);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence = buildFence(1L, "active", "circle", 39.9087, 116.3975, 5000);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
    }

    /**
     * 设备在围栏外且5分钟内无同类型报警时创建报警
     */
    @Test
    void checkFencesAndAlert_outsideFence_noRecentAlarm_createsAlarm() {
        DeviceLocation curr = buildDeviceLocation(1L, 35.0, 110.0);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());
        when(alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(10L, "fence_breach"))
                .thenReturn(Optional.empty());
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(simpleAlertRepo).save(any(SimpleFenceAlert.class));
        verify(alarmService).createAlarm(any(Alarm.class));
    }

    /**
     * 设备在围栏外但5分钟内已有同类型报警时不创建报警
     */
    @Test
    void checkFencesAndAlert_outsideFence_recentAlarmExists_doesNotCreateAlarm() {
        DeviceLocation curr = buildDeviceLocation(1L, 35.0, 110.0);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());
        Alarm recentAlarm = new Alarm();
        recentAlarm.setTriggeredTime(new Date());
        when(alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(10L, "fence_breach"))
                .thenReturn(Optional.of(recentAlarm));

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService, never()).createAlarm(any());
        verify(simpleAlertRepo, never()).save(any());
    }

    /**
     * 多病人多围栏组合时遍历所有
     */
    @Test
    void checkFencesAndAlert_multiplePatientsAndFences_iteratesAll() {
        DeviceLocation curr = buildDeviceLocation(1L, 35.0, 110.0);
        Device device = new Device();
        device.setId(1L);
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device));

        PatientDevice pd1 = new PatientDevice();
        pd1.setPatientId(10L);
        Patient patient1 = new Patient();
        patient1.setId(10L);
        pd1.setPatient(patient1);

        PatientDevice pd2 = new PatientDevice();
        pd2.setPatientId(20L);
        Patient patient2 = new Patient();
        patient2.setId(20L);
        pd2.setPatient(patient2);

        when(patientDeviceRepo.findByDeviceId(1L)).thenReturn(List.of(pd1, pd2));

        GeoFence fence1 = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        GeoFence fence2 = buildFence(2L, "active", "circle", 40.0, 117.0, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence1));
        when(fenceRepo.listByPatient(20L)).thenReturn(List.of(fence2));
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(Collections.emptyList());
        when(alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(anyLong(), eq("fence_breach")))
                .thenReturn(Optional.empty());
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(simpleAlertRepo, times(2)).save(any(SimpleFenceAlert.class));
        verify(alarmService, times(2)).createAlarm(any(Alarm.class));
    }

    /**
     * 报警创建时正确填充信息
     */
    @Test
    void checkFencesAndAlert_createsAlarmWithCorrectInfo() {
        DeviceLocation curr = buildDeviceLocation(1L, 35.0, 110.0);
        Device device = new Device();
        device.setId(1L);
        PatientDevice pd = new PatientDevice();
        pd.setPatientId(10L);
        Patient patient = new Patient();
        patient.setId(10L);
        pd.setPatient(patient);
        when(patientDeviceRepo.findByDeviceId(1L)).thenReturn(List.of(pd));
        when(deviceRepo.findById(1L)).thenReturn(Optional.of(device));
        GeoFence fence = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(Collections.emptyList());
        when(alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(10L, "fence_breach"))
                .thenReturn(Optional.empty());
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(alarmService).createAlarm(argThat(alarm ->
                alarm.getAlarmType().equals("fence_breach") &&
                alarm.getAlarmLevel().equals("warning") &&
                alarm.getDevice() == device &&
                alarm.getPatient() == patient
        ));
        verify(simpleAlertRepo).save(argThat(alert ->
                alert.getAlertType().equals("fence_breach") &&
                alert.getDevice() == device &&
                alert.getPatient() == patient &&
                alert.getFence() == fence
        ));
    }

    /**
     * 病人通过fence_patients关联围栏时去重检测
     */
    @Test
    void checkFencesAndAlert_fencePatientsDeduplication() {
        DeviceLocation curr = buildDeviceLocation(1L, 35.0, 110.0);
        setupDeviceAndPatient(1L, 10L);
        GeoFence fence1 = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        GeoFence fence2 = buildFence(2L, "active", "circle", 40.0, 117.0, 500);
        when(fenceRepo.listByPatient(10L)).thenReturn(List.of(fence1));
        GeoFence fence1Dup = buildFence(1L, "active", "circle", 39.9087, 116.3975, 500);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(List.of(fence1Dup, fence2));
        when(alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(10L, "fence_breach"))
                .thenReturn(Optional.empty());
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        fenceService.checkFencesAndAlert(curr, null, null);

        verify(simpleAlertRepo, times(2)).save(any(SimpleFenceAlert.class));
    }

    /**
     * 构建DeviceLocation测试对象
     */
    private DeviceLocation buildDeviceLocation(long deviceId, double lat, double lng) {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(deviceId);
        dl.setImei("IMEI001");
        dl.setLatitude(BigDecimal.valueOf(lat));
        dl.setLongitude(BigDecimal.valueOf(lng));
        return dl;
    }

    /**
     * 构建GeoFence测试对象
     */
    private GeoFence buildFence(long id, String status, String type, double centerLat, double centerLng, int radius) {
        GeoFence fence = new GeoFence();
        fence.setId(id);
        fence.setStatus(status);
        fence.setType(type);
        fence.setCenterLat(centerLat);
        fence.setCenterLng(centerLng);
        fence.setRadius(radius);
        fence.setName("测试围栏" + id);
        return fence;
    }

    /**
     * 设置设备和病人关联的基础mock
     */
    private void setupDeviceAndPatient(long deviceId, long patientId) {
        Device device = new Device();
        device.setId(deviceId);
        when(deviceRepo.findById(deviceId)).thenReturn(Optional.of(device));

        PatientDevice pd = new PatientDevice();
        pd.setPatientId(patientId);
        Patient patient = new Patient();
        patient.setId(patientId);
        pd.setPatient(patient);
        when(patientDeviceRepo.findByDeviceId(deviceId)).thenReturn(List.of(pd));
    }
}
