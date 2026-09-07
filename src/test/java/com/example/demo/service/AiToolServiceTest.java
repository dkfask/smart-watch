package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.Patient;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.model.dto.DeviceInfoDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.repository.LocationRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiToolService 单元测试：工具定义、字段白名单、数据聚合
 */
class AiToolServiceTest {

    private DeviceService deviceService;
    private AlarmService alarmService;
    private com.example.demo.repository.PatientRepository patientRepository;
    private HealthRecordRepository healthRecordRepository;
    private DeviceStatusRepository deviceStatusRepository;
    private DeviceRepository deviceRepository;
    private LocationRecordRepository locationRecordRepository;
    private TiandituLocationService tiandituLocationService;
    private AiToolService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        alarmService = mock(AlarmService.class);
        patientRepository = mock(com.example.demo.repository.PatientRepository.class);
        healthRecordRepository = mock(HealthRecordRepository.class);
        deviceStatusRepository = mock(DeviceStatusRepository.class);
        deviceRepository = mock(DeviceRepository.class);
        locationRecordRepository = mock(LocationRecordRepository.class);
        tiandituLocationService = mock(TiandituLocationService.class);
        objectMapper = new ObjectMapper();
        service = new AiToolService(deviceService, alarmService, patientRepository,
                healthRecordRepository, deviceStatusRepository, deviceRepository,
                locationRecordRepository, tiandituLocationService, objectMapper);
    }

    private static JsonNode parse(String json) throws Exception {
        return new ObjectMapper().readTree(json);
    }

    // === 工具定义 ===

    @Test
    void toolDefinitions_containsSixTools() throws Exception {
        List<java.util.Map<String, Object>> tools = service.toolDefinitions();
        assertEquals(6, tools.size());
        List<String> names = tools.stream()
                .map(t -> ((java.util.Map<?, ?>) t.get("function")).get("name").toString())
                .toList();
        assertTrue(names.containsAll(List.of("list_devices", "list_patients", "get_latest_health",
                "list_alarms", "get_alarm_stats", "get_device_location")));
    }

    // === list_devices ===

    @Test
    void listDevices_returnsWhitelistedFields() throws Exception {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.setId(1L);
        dto.setImei("860000000000001");
        dto.setIsOnline(true);
        dto.setBatteryLevel(88);
        dto.setLastLocationAddress("北京市朝阳区1号");
        DeviceInfoDto.PatientInfo patient = new DeviceInfoDto.PatientInfo();
        patient.setId(7L);
        patient.setName("张三");
        dto.setPatient(patient);
        when(deviceService.listDevices(0, 10, null))
                .thenReturn(new PageResponse<>(List.of(dto), 1, 0, 10));

        String json = service.executeTool("list_devices", "{}");
        JsonNode node = parse(json);

        assertEquals(1, node.get("total").asLong());
        assertEquals("860000000000001", node.get("devices").get(0).get("imei").asText());
        assertTrue(node.get("devices").get(0).get("online").asBoolean());
        assertEquals("张三", node.get("devices").get(0).get("patientName").asText());
    }

    // === list_patients（隐私字段白名单）===

    @Test
    void listPatients_excludesIdCardAndPhone() throws Exception {
        Patient p = new Patient();
        p.setId(7L);
        p.setName("张三");
        p.setGender("男");
        p.setAge(72);
        p.setWard("3楼养老区");
        p.setBed("301");
        p.setDiagnosis("高血压");
        p.setStatus("admitted");
        p.setIdCard("110101194001011234");
        p.setPhone("13800000000");
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(p)));

        String json = service.executeTool("list_patients", "{}");
        JsonNode node = parse(json);

        assertEquals("张三", node.get("patients").get(0).get("name").asText());
        assertFalse(json.contains("110101194001011234"), "身份证号不得进入模型上下文");
        assertFalse(json.contains("13800000000"), "电话不得进入模型上下文");
    }

    @Test
    void listPatients_statusFilter() throws Exception {
        Patient admitted = new Patient();
        admitted.setId(1L);
        admitted.setName("张三");
        admitted.setStatus("admitted");
        Patient discharged = new Patient();
        discharged.setId(2L);
        discharged.setName("李四");
        discharged.setStatus("discharged");
        when(patientRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admitted, discharged)));

        String json = service.executeTool("list_patients", "{\"status\":\"admitted\"}");
        JsonNode node = parse(json);

        assertEquals(1, node.get("patients").size());
        assertEquals("张三", node.get("patients").get(0).get("name").asText());
    }

    // === get_latest_health ===

    @Test
    void latestHealth_deduplicatesByDataTypeKeepingLatest() throws Exception {
        HealthRecord oldHeartRate = new HealthRecord();
        oldHeartRate.setDataType("heart_rate");
        oldHeartRate.setValue("90");
        oldHeartRate.setRecvTime(new Date(1000L));
        HealthRecord latestHeartRate = new HealthRecord();
        latestHeartRate.setDataType("heart_rate");
        latestHeartRate.setValue("76");
        latestHeartRate.setRecvTime(new Date(3000L));
        HealthRecord temperature = new HealthRecord();
        temperature.setDataType("temperature");
        temperature.setValue("36.5");
        temperature.setRecvTime(new Date(2000L));
        when(healthRecordRepository.findByPatientIdOrderByRecvTimeDesc(7L))
                .thenReturn(List.of(latestHeartRate, temperature, oldHeartRate));

        String json = service.executeTool("get_latest_health", "{\"patientId\":7}");
        JsonNode node = parse(json);

        assertEquals(2, node.get("records").size());
        assertEquals("heart_rate", node.get("records").get(0).get("dataType").asText());
        assertEquals("76", node.get("records").get(0).get("value").asText());
        assertEquals("temperature", node.get("records").get(1).get("dataType").asText());
    }

    @Test
    void latestHealth_requiresPatientOrDevice() throws Exception {
        String json = service.executeTool("get_latest_health", "{}");
        assertTrue(parse(json).has("error"));
    }

    // === list_alarms ===

    @Test
    void listAlarms_includesRelationsAndCondition() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setId(11L);
        alarm.setAlarmType("sos");
        alarm.setAlarmLevel("critical");
        alarm.setStatus("pending");
        Device device = new Device();
        device.setId(1L);
        device.setImei("860000000000001");
        alarm.setDevice(device);
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setName("张三");
        alarm.setPatient(patient);
        when(alarmService.getAlarmsWithRelations(any(AlarmQueryCondition.class)))
                .thenReturn(new PageImpl<>(List.of(alarm)));

        String json = service.executeTool("list_alarms", "{\"status\":\"pending\",\"limit\":5}");
        JsonNode node = parse(json);

        assertEquals("sos", node.get("alarms").get(0).get("alarmType").asText());
        assertEquals("860000000000001", node.get("alarms").get(0).get("imei").asText());
        assertEquals("张三", node.get("alarms").get(0).get("patientName").asText());

        ArgumentCaptor<AlarmQueryCondition> captor = ArgumentCaptor.forClass(AlarmQueryCondition.class);
        verify(alarmService).getAlarmsWithRelations(captor.capture());
        assertEquals("pending", captor.getValue().getStatus());
        assertEquals(5, captor.getValue().getSize());
    }

    // === get_alarm_stats ===

    @Test
    void alarmStats_mapsAllFields() throws Exception {
        AlarmStatsDto stats = new AlarmStatsDto();
        stats.setTotal(100);
        stats.setPending(3);
        stats.setHandled(95);
        stats.setFalseAlarm(2);
        stats.setUnread(4);
        when(alarmService.getAlarmStats()).thenReturn(stats);

        String json = service.executeTool("get_alarm_stats", "{}");
        JsonNode node = parse(json);

        assertEquals(100, node.get("total").asLong());
        assertEquals(3, node.get("pending").asLong());
        assertEquals(4, node.get("unread").asLong());
    }

    // === get_device_location ===

    @Test
    void deviceLocation_byDeviceId_withTiandituAddress() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setImei("860000000000001");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(1L);
        status.setImei("860000000000001");
        status.setIsOnline(true);
        status.setLastLatitude(39.9);
        status.setLastLongitude(116.4);
        when(deviceStatusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(locationRecordRepository.findByImeiOrderByRecvTimeDesc("860000000000001"))
                .thenReturn(List.of());
        when(tiandituLocationService.regeoAddress(39.9, 116.4)).thenReturn("北京市朝阳区1号");

        String json = service.executeTool("get_device_location", "{\"deviceId\":1}");
        JsonNode node = parse(json);

        assertTrue(node.get("online").asBoolean());
        assertEquals(39.9, node.get("latitude").asDouble(), 1e-6);
        assertEquals("北京市朝阳区1号", node.get("address").asText());
    }

    @Test
    void deviceLocation_deviceNotFound_returnsError() throws Exception {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());
        String json = service.executeTool("get_device_location", "{\"deviceId\":99}");
        assertTrue(parse(json).has("error"));
    }

    // === 异常兜底 ===

    @Test
    void unknownTool_returnsError() throws Exception {
        String json = service.executeTool("no_such_tool", "{}");
        assertTrue(parse(json).get("error").asText().contains("未知工具"));
    }

    @Test
    void toolFailure_returnsErrorJson() throws Exception {
        when(deviceService.listDevices(anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("db down"));
        String json = service.executeTool("list_devices", "{}");
        assertTrue(parse(json).get("error").asText().contains("db down"));
    }
}
