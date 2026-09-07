package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.HealthRecord;
import com.example.demo.model.LocationRecord;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手的数据查询工具：定义 OpenAI function calling 的工具列表，
 * 并把模型发起的工具调用分发到现有业务层。
 *
 * 隐私边界：工具输出为白名单字段，患者身份证、电话等敏感信息不进入模型上下文。
 */
@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);

    private static final int MAX_ROWS = 20;

    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final com.example.demo.repository.PatientRepository patientRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final DeviceRepository deviceRepository;
    private final LocationRecordRepository locationRecordRepository;
    private final TiandituLocationService tiandituLocationService;
    private final ObjectMapper objectMapper;

    public AiToolService(DeviceService deviceService,
                         AlarmService alarmService,
                         com.example.demo.repository.PatientRepository patientRepository,
                         HealthRecordRepository healthRecordRepository,
                         DeviceStatusRepository deviceStatusRepository,
                         DeviceRepository deviceRepository,
                         LocationRecordRepository locationRecordRepository,
                         TiandituLocationService tiandituLocationService,
                         ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.alarmService = alarmService;
        this.patientRepository = patientRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.deviceRepository = deviceRepository;
        this.locationRecordRepository = locationRecordRepository;
        this.tiandituLocationService = tiandituLocationService;
        this.objectMapper = objectMapper;
    }

    /**
     * 工具的中文名称，用于前端「正在查询xx」提示
     */
    public String toolLabel(String name) {
        return switch (name) {
            case "list_devices" -> "设备列表";
            case "list_patients" -> "患者列表";
            case "get_latest_health" -> "健康数据";
            case "list_alarms" -> "告警记录";
            case "get_alarm_stats" -> "告警统计";
            case "get_device_location" -> "设备定位";
            default -> name;
        };
    }

    /**
     * OpenAI function calling 工具定义
     */
    public List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(functionTool("list_devices", "查询智能手环设备列表，含在线状态、电量和最后位置。可按 IMEI/ICCID/IMSI 关键字搜索。",
                mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "search", mapOf("type", "string", "description", "可选，按 IMEI/ICCID/IMSI 模糊搜索"),
                                "page", mapOf("type", "integer", "description", "页码，从 1 开始，默认 1"),
                                "pageSize", mapOf("type", "integer", "description", "每页条数，默认 10，最大 " + MAX_ROWS)),
                        "required", List.of())));
        tools.add(functionTool("list_patients", "查询患者（被监护老人）列表，含病房床位与诊断。可按姓名/病房/身份证号关键字搜索。",
                mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "search", mapOf("type", "string", "description", "可选，按姓名、病房或身份证号模糊搜索"),
                                "status", mapOf("type", "string", "description", "可选，在院状态过滤：admitted=在院，discharged=出院"),
                                "limit", mapOf("type", "integer", "description", "返回条数上限，默认 " + MAX_ROWS)),
                        "required", List.of())));
        tools.add(functionTool("get_latest_health", "查询患者或设备最新的健康数据（心率、血压、血氧、体温等）。",
                mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "patientId", mapOf("type", "integer", "description", "患者 ID，与 deviceId 二选一"),
                                "deviceId", mapOf("type", "integer", "description", "设备 ID，与 patientId 二选一"),
                                "dataType", mapOf("type", "string", "description", "可选，数据类型过滤：heart_rate/blood_pressure/blood_oxygen/temperature"),
                                "limit", mapOf("type", "integer", "description", "不指定 dataType 时返回最近 N 条不同类型记录，默认 8")),
                        "required", List.of())));
        tools.add(functionTool("list_alarms", "查询告警记录（SOS、跌倒、心率异常、低电量、围栏越界等），可按状态/设备/患者过滤。",
                mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "status", mapOf("type", "string", "description", "可选，告警状态：pending=待处理，handled=已处理，false_alarm=误报"),
                                "deviceId", mapOf("type", "integer", "description", "可选，按设备过滤"),
                                "patientId", mapOf("type", "integer", "description", "可选，按患者过滤"),
                                "limit", mapOf("type", "integer", "description", "返回条数上限，默认 10，最大 " + MAX_ROWS)),
                        "required", List.of())));
        tools.add(functionTool("get_alarm_stats", "查询告警统计数据：总数、待处理、已处理、误报、未读数量。",
                mapOf("type", "object", "properties", mapOf(), "required", List.of())));
        tools.add(functionTool("get_device_location", "查询设备最新定位（经纬度、地址、在线状态）。",
                mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "deviceId", mapOf("type", "integer", "description", "设备 ID，与 imei 二选一"),
                                "imei", mapOf("type", "string", "description", "设备 IMEI，与 deviceId 二选一")),
                        "required", List.of())));
        return tools;
    }

    private Map<String, Object> functionTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> fn = new HashMap<>();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", parameters);
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    /**
     * 执行一次工具调用，返回喂给模型的紧凑 JSON 字符串
     */
    public String executeTool(String name, String argumentsJson) {
        try {
            JsonNode args = (argumentsJson == null || argumentsJson.isBlank())
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(argumentsJson);
            Object result = switch (name) {
                case "list_devices" -> listDevices(args);
                case "list_patients" -> listPatients(args);
                case "get_latest_health" -> latestHealth(args);
                case "list_alarms" -> listAlarms(args);
                case "get_alarm_stats" -> alarmStats();
                case "get_device_location" -> deviceLocation(args);
                default -> Map.of("error", "未知工具: " + name);
            };
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("AI 工具 {} 执行失败: {}", name, e.getMessage());
            try {
                return objectMapper.writeValueAsString(Map.of("error", "查询失败: " + e.getMessage()));
            } catch (Exception ignored) {
                return "{\"error\":\"查询失败\"}";
            }
        }
    }

    private Object listDevices(JsonNode args) {
        int page = clampInt(args.path("page").asInt(1), 1, 100);
        int pageSize = clampInt(args.path("pageSize").asInt(10), 1, MAX_ROWS);
        String search = args.path("search").asText("");
        PageResponse<DeviceInfoDto> resp = deviceService.listDevices(page - 1, pageSize,
                search.isBlank() ? null : search.trim());
        List<Map<String, Object>> devices = new ArrayList<>();
        for (DeviceInfoDto d : resp.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", d.getId());
            item.put("imei", d.getImei());
            item.put("online", Boolean.TRUE.equals(d.getIsOnline()));
            item.put("batteryLevel", d.getBatteryLevel());
            item.put("lastLocationTime", d.getLastLocationTime());
            item.put("lastLocationAddress", d.getLastLocationAddress());
            if (d.getPatient() != null) {
                item.put("patientId", d.getPatient().getId());
                item.put("patientName", d.getPatient().getName());
            }
            devices.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", resp.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("devices", devices);
        return result;
    }

    private Object listPatients(JsonNode args) {
        String search = args.path("search").asText("");
        String status = args.path("status").asText("");
        int limit = clampInt(args.path("limit").asInt(MAX_ROWS), 1, MAX_ROWS);
        Pageable pageable = PageRequest.of(0, MAX_ROWS * 5);
        Page<Patient> page = search.isBlank()
                ? patientRepository.findAll(pageable)
                : patientRepository.findByNameContainingOrIdCardContainingOrWardContaining(
                        search.trim(), search.trim(), search.trim(), pageable);
        List<Map<String, Object>> patients = new ArrayList<>();
        for (Patient p : page.getContent()) {
            if (!status.isBlank() && !status.equals(p.getStatus())) {
                continue;
            }
            if (patients.size() >= limit) {
                break;
            }
            patients.add(patientMap(p));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchedTotal", page.getTotalElements());
        result.put("returned", patients.size());
        result.put("patients", patients);
        return result;
    }

    private Map<String, Object> patientMap(Patient p) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", p.getId());
        item.put("name", p.getName());
        item.put("gender", p.getGender());
        item.put("age", p.getAge());
        item.put("ward", p.getWard());
        item.put("bed", p.getBed());
        item.put("diagnosis", p.getDiagnosis());
        item.put("status", p.getStatus());
        return item;
    }

    private Object latestHealth(JsonNode args) {
        Long patientId = longValue(args, "patientId");
        Long deviceId = longValue(args, "deviceId");
        String dataType = args.path("dataType").asText("");
        int limit = clampInt(args.path("limit").asInt(8), 1, MAX_ROWS);
        if (patientId == null && deviceId == null) {
            return Map.of("error", "必须提供 patientId 或 deviceId");
        }

        List<HealthRecord> records;
        if (patientId != null) {
            records = (dataType.isBlank()
                    ? healthRecordRepository.findByPatientIdOrderByRecvTimeDesc(patientId)
                    : healthRecordRepository.findByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, dataType));
        } else {
            records = healthRecordRepository.findByDeviceIdOrderByRecvTimeDesc(deviceId);
            if (!dataType.isBlank()) {
                records = records.stream().filter(r -> dataType.equals(r.getDataType())).toList();
            }
        }

        // 不指定类型时，每个数据类型只保留最新一条，避免长列表冲刷上下文
        List<HealthRecord> selected = new ArrayList<>();
        if (dataType.isBlank()) {
            var seen = new java.util.HashSet<String>();
            for (HealthRecord r : records) {
                if (r.getDataType() != null && seen.add(r.getDataType())) {
                    selected.add(r);
                    if (selected.size() >= limit) {
                        break;
                    }
                }
            }
        } else {
            selected = records.subList(0, Math.min(limit, records.size()));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (HealthRecord r : selected) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dataType", r.getDataType());
            item.put("value", r.getValue());
            item.put("recvTime", r.getRecvTime());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("deviceId", deviceId);
        result.put("records", items);
        return result;
    }

    private Object listAlarms(JsonNode args) {
        AlarmQueryCondition condition = new AlarmQueryCondition();
        condition.setPage(0);
        condition.setSize(clampInt(args.path("limit").asInt(10), 1, MAX_ROWS));
        condition.setStatus(textOrNull(args, "status"));
        condition.setDeviceId(longValue(args, "deviceId"));
        condition.setPatientId(longValue(args, "patientId"));
        Page<Alarm> page = alarmService.getAlarmsWithRelations(condition);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Alarm a : page.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("alarmType", a.getAlarmType());
            item.put("alarmLevel", a.getAlarmLevel());
            item.put("status", a.getStatus());
            item.put("triggeredTime", a.getTriggeredTime());
            item.put("address", a.getAddress());
            if (a.getDevice() != null) {
                item.put("imei", a.getDevice().getImei());
            }
            if (a.getPatient() != null) {
                item.put("patientName", a.getPatient().getName());
            }
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", page.getTotalElements());
        result.put("alarms", items);
        return result;
    }

    private Object alarmStats() {
        AlarmStatsDto stats = alarmService.getAlarmStats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", stats.getTotal());
        result.put("pending", stats.getPending());
        result.put("handled", stats.getHandled());
        result.put("falseAlarm", stats.getFalseAlarm());
        result.put("unread", stats.getUnread());
        return result;
    }

    private Object deviceLocation(JsonNode args) {
        Long deviceId = longValue(args, "deviceId");
        String imei = args.path("imei").asText("");
        Device device = null;
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId).orElse(null);
        } else if (!imei.isBlank()) {
            device = deviceRepository.findByImei(imei.trim()).orElse(null);
        }
        if (device == null) {
            return Map.of("error", "未找到对应设备");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", device.getId());
        result.put("imei", device.getImei());

        DeviceStatus status = deviceStatusRepository.findById(device.getId()).orElse(null);
        if (status != null) {
            result.put("online", Boolean.TRUE.equals(status.getIsOnline()));
            result.put("lastLocationTime", status.getLastLocationTime());
            result.put("latitude", status.getLastLatitude());
            result.put("longitude", status.getLastLongitude());
            result.put("batteryLevel", status.getBatteryLevel());
        }

        List<LocationRecord> records = locationRecordRepository.findByImeiOrderByRecvTimeDesc(device.getImei());
        if (!records.isEmpty()) {
            LocationRecord latest = records.get(0);
            if (latest.getLatitude() != null && latest.getLongitude() != null) {
                result.put("latitude", latest.getLatitude());
                result.put("longitude", latest.getLongitude());
                result.put("locationTime", latest.getRecvTime());
                result.put("source", latest.getSource());
            }
        }

        Object lat = result.get("latitude");
        Object lng = result.get("longitude");
        if (lat instanceof Number latNum && lng instanceof Number lngNum) {
            String address = tiandituLocationService.regeoAddress(latNum.doubleValue(), lngNum.doubleValue());
            if (address != null && !address.isBlank()) {
                result.put("address", address);
            }
        }
        if (!result.containsKey("latitude")) {
            result.put("note", "该设备暂无定位记录");
        }
        return result;
    }

    private static Long longValue(JsonNode args, String field) {
        return args.path(field).isNumber() ? args.get(field).asLong() : null;
    }

    private static String textOrNull(JsonNode args, String field) {
        String value = args.path(field).asText("");
        return value.isBlank() ? null : value;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
