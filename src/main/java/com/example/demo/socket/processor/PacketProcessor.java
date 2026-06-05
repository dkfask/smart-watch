package com.example.demo.socket.processor;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AmapLocationService;
import com.example.demo.service.HealthMonitorService;
import com.example.demo.service.TrackingService;
import com.example.demo.socket.downlink.DownlinkManager;
import com.example.demo.socket.protocol.BraceletPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据包处理器，负责处理各种协议包
 */
public class PacketProcessor {
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);

    private final DeviceRepository deviceRepository;
    private final DownlinkManager downlinkManager;
    private final LocationRecordRepository locationRecordRepository;
    private final HeartbeatRecordRepository heartbeatRecordRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final PatientDeviceRepository patientDeviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final AmapLocationService amapLocationService;
    private final HealthMonitorService healthMonitorService;
    private final TrackingService trackingService;

    public PacketProcessor(DeviceRepository deviceRepository,
                          DownlinkManager downlinkManager,
                          LocationRecordRepository locationRecordRepository,
                          HeartbeatRecordRepository heartbeatRecordRepository,
                          HealthRecordRepository healthRecordRepository,
                          PatientDeviceRepository patientDeviceRepository,
                          DeviceStatusRepository deviceStatusRepository,
                          AmapLocationService amapLocationService,
                          HealthMonitorService healthMonitorService,
                          TrackingService trackingService) {
        this.deviceRepository = deviceRepository;
        this.downlinkManager = downlinkManager;
        this.locationRecordRepository = locationRecordRepository;
        this.heartbeatRecordRepository = heartbeatRecordRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.patientDeviceRepository = patientDeviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.amapLocationService = amapLocationService;
        this.healthMonitorService = healthMonitorService;
        this.trackingService = trackingService;
    }

    /**
     * 处理数据包
     */
    public void processPacket(BraceletPacket packet, String raw, String clientInfo, Socket clientSocket, String imei) {
        String proto = packet.getProtocol();
        // 解析统一从 raw 中截取 payload（header+proto 长度为 2 + 4 = 6）
        String payload = "";
        if (raw != null && raw.length() > 6) {
            // 去掉开头 IWAPxx 并去掉尾部的 '#'
            int end = raw.endsWith("#") ? raw.length() - 1 : raw.length();
            payload = raw.substring(6, end);
        }

        try {
            switch (proto) {
                case "AP00":
                    handleAp00(payload, clientSocket, clientInfo);
                    break;
                case "AP01":
                    // 使用 packet 对象中的参数，而不是重新解析 raw 数据
                    Map<String, String> paramsToSave = new LinkedHashMap<>();
                    if (packet.getParams() != null) {
                        paramsToSave.putAll(packet.getParams());
                    }
                    handleAp01(paramsToSave, imei);
                    log.debug("✅ AP01 处理完成, IMEI={}", imei);
                    break;
                case "AP02":
                    // 健康包（旧版 AP02 可能另用，此处如无明确需求可当作健康数据）
                    saveHealthData(parseKeyValueParams(payload), imei);
                    log.debug("✅ AP02 处理完成, IMEI={}", imei);
                    break;
                case "AP03":
                    // 使用 packet 对象中的参数，而不是重新解析 raw 数据
                    Map<String, String> ap03Params = new LinkedHashMap<>();
                    if (packet.getParams() != null) {
                        ap03Params.putAll(packet.getParams());
                    }
                    handleAp03(ap03Params, clientInfo, imei);
                    log.debug("✅ AP03 处理完成, IMEI={}", imei);
                    break;
                case "AP04":
                    handleAp04(payload, clientInfo, imei);
                    log.debug("✅ AP04 处理完成, IMEI={}", imei);
                    break;
                case "AP10":
                    handleAp10(payload, clientInfo, imei);
                    log.debug("✅ AP10 处理完成, IMEI={}", imei);
                    break;
                case "AP42":
                    handleAp42(payload, clientInfo, imei);
                    log.debug("✅ AP42 处理完成, IMEI={}", imei);
                    break;
                case "APBL":
                    handleApBl(payload, clientInfo, imei);
                    log.debug("✅ APBL 处理完成, IMEI={}", imei);
                    break;
                case "APJK":
                    handleApJk(payload, clientInfo, imei);
                    log.debug("✅ APJK 处理完成, IMEI={}", imei);
                    break;
                case "APTP":
                    handleApTp(payload, clientInfo, imei);
                    log.debug("✅ APTP 处理完成, IMEI={}", imei);
                    break;
                case "APVR":
                    handleApVr(payload, clientInfo, imei);
                    log.debug("✅ APVR 处理完成, IMEI={}", imei);
                    break;
                case "APWR":
                    handleApWr(payload, clientInfo, imei);
                    log.debug("✅ APWR 处理完成, IMEI={}", imei);
                    break;
                case "AP16":
                    log.debug("✅ AP16 处理完成, IMEI={}", imei);
                    break;
                default:
                    log.warn("❓ 未知协议类型: {} raw={}", proto, raw);
            }
        } catch (Exception e) {
            log.error("💥 处理数据包失败: 协议={}, raw={}, IMEI={}, 错误={}", proto, raw, imei, e.getMessage(), e);
        }
    }

    // --------------------------- 各协议处理函数（签名加入 imei 参数） ---------------------------

    /** 处理 AP00 登录包 */
    private void handleAp00(String payload, Socket clientSocket, String clientInfo) {
        if (payload == null || payload.isEmpty()) return;
        String[] parts = payload.split(",");
        String imei = parts[0].trim();
        if (imei.length() != 15 || !imei.chars().allMatch(Character::isDigit)) {
            return;
        }

        // 注册下行连接
        try {
            downlinkManager.register(imei, clientSocket);
        } catch (Exception e) {
            log.warn("注册下行连接失败 IMEI={}");
        }

        // 保存设备信息（若不存在）
        try {
            Optional<Device> opt = deviceRepository.findByImei(imei);
            if (opt.isPresent()) {
                return;
            }

            Device device = new Device();
            device.setImei(imei);
            device.setCreatedAt(new java.util.Date());

            if (parts.length >= 2) {
                String second = parts[1];
                if (second.contains("|")) {
                    String[] net = second.split("\\|", 3);
                    if (net.length >= 1) device.setMcc(net[0]);
                    if (net.length >= 2) device.setMnc(net[1]);
                    if (net.length >= 3) device.setApn(net[2]);
                } else if (parts.length >= 3) {
                    device.setIccid(parts[1].trim());
                    device.setImsi(parts[2].trim());
                }
            }
            deviceRepository.save(device);
        } catch (Exception e) {
            log.error("保存设备失败 IMEI={}");
        }
    }

    /** 处理 AP01 定位包 */
    private void handleAp01(Map<String, String> paramsToSave, String imei) {
        saveLocationData(paramsToSave, imei);
    }

    /** 处理 AP03 心跳包 */
    private void handleAp03(Map<String, String> params, String clientInfo, String imei) {
        if (params == null || params.isEmpty()) return;

        // 对于AP03心跳包，只有当IMEI有效时才保存关联数据
        // 确保不会使用无效的IMEI创建设备
        final String validImei;
        if (imei != null && imei.length() == 15 && imei.chars().allMatch(Character::isDigit)) {
            validImei = imei;
        } else {
            validImei = null;
        }

        saveHeartbeatData(params, validImei);
        // 更新设备在线状态：收到AP03心跳包即表示设备在线
        if (validImei != null) {
            deviceRepository.findByImei(validImei).ifPresent(device -> {
                DeviceStatus status = new DeviceStatus();
                status.setDeviceId(device.getId());
                status.setImei(validImei);
                status.setIsOnline(true);
                // 设置电池电量
                if (params.containsKey("battery_level")) {
                    try {
                        status.setBatteryLevel(Integer.parseInt(params.get("battery_level")));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid battery_level value: {}", params.get("battery_level"));
                    }
                } else if (params.containsKey("battery")) {
                    try {
                        status.setBatteryLevel(Integer.parseInt(params.get("battery")));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid battery value: {}", params.get("battery"));
                    }
                }
                status.setUpdatedAt(new java.util.Date());
                deviceStatusRepository.upsert(status);
                log.debug("✅ 更新设备在线状态: IMEI={}, 在线状态={}, 电池电量={}", validImei, true, status.getBatteryLevel());
            });
        }
    }

    /** 处理 AP04 低电报警 */
    private void handleAp04(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;
        String[] parts = payload.split(",");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("battery", parts.length > 0 ? parts[0] : "");
        saveHeartbeatData(params, imei);
    }

    /** 处理 AP10 报警上报 */
    private void handleAp10(String payload, String clientInfo, String imei) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        saveLocationData(params, imei);
    }

    /** 处理 AP42 图片包 */
    private void handleAp42(String payload, String clientInfo, String imei) {
        String[] p = payload.split(",", 5);
        // reference imei to avoid unused parameter warning and help tracing
        if (imei != null && !imei.isEmpty()) {
            log.debug("AP42 associated imei={}");
        }
        String time = p.length > 0 ? p[0] : "";
        String total = p.length > 1 ? p[1] : "";
        String seq = p.length > 2 ? p[2] : "";
    }

    /** 处理 APBL 蓝牙数据 */
    private void handleApBl(String payload, String clientInfo, String imei) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("raw", payload);
        if (imei != null && !imei.isEmpty()) params.put("imei", imei);
    }

    /** 处理 APJK 健康数据 */
    private void handleApJk(String payload, String clientInfo, String imei) {
        if (payload == null || payload.isEmpty()) return;

        // 期望形式为：timestamp,type,value  （value 可能含有 '|' 分隔多个子值）
        // 例如: 2021-05-29 13:00:00,1,69|120
        String[] parts = payload.split(",", 3);
        Map<String, String> params = new LinkedHashMap<>();
        if (parts.length >= 3) {
            String timeStr = parts[0].trim();
            String typeStr = parts[1].trim();
            String valStr = parts[2].trim();
            params.put("timestamp", timeStr);
            params.put("type", typeStr);
            params.put("raw_value", valStr);

            // 解析类型并把值拆解为更具体字段
            switch (typeStr) {
                case "1": // 血压: diastolic|systolic
                    params.put("data_type", "blood_pressure");
                    if (valStr.contains("|")) {
                        String[] vs = valStr.split("\\|", 2);
                        params.put("bp_diastolic", vs[0]);
                        params.put("bp_systolic", vs[1]);
                        params.put("value", vs[0] + "|" + vs[1]);
                    } else {
                        params.put("value", valStr);
                    }
                    break;
                case "2": // 心率
                    params.put("data_type", "heart_rate");
                    params.put("value", valStr);
                    break;
                case "3": // 体温
                    params.put("data_type", "body_temperature");
                    params.put("value", valStr);
                    break;
                case "4": // 血氧
                    params.put("data_type", "blood_oxygen");
                    params.put("value", valStr);
                    break;
                default:
                    // 未知类型，回退为 raw
                    params.put("data_type", "unknown");
                    params.put("value", valStr);
            }
            // 将 imei 从 payload 或 raw 中尝试提取（若这个 payload 中包含 imei，虽然协议通常不在此处带 imei）
            if (imei != null) params.put("imei", imei);

            saveHealthData(params, imei);
            return;
        }

        // 回退：如果不符合新的三段式格式，则尝试解析为 key=value 键值对（向后兼容）
        Map<String, String> kv = parseKeyValueParams(payload);
        if (imei != null) kv.put("imei", imei);
        saveHealthData(kv, imei);
    }

    /** 处理 APTP 体温数据 */
    private void handleApTp(String payload, String clientInfo, String imei) {
        Map<String, String> params = parseKeyValueParams(payload);
        if (params.isEmpty() && payload != null && !payload.isBlank()) {
            String[] parts = payload.split(",", 2);
            if (parts.length >= 1 && !parts[0].isBlank()) {
                params.put("data_type", "body_temperature");
                params.put("value", parts[0].trim());
            }
            if (parts.length >= 2 && !parts[1].isBlank()) {
                params.put("wrist_temp", parts[1].trim());
            }
        }
        if (imei != null) params.put("imei", imei);
        saveHealthData(params, imei);
    }

    /** 处理 APVR 版本信息 */
    private void handleApVr(String payload, String clientInfo, String imei) {
        Map<String, String> params = parseKeyValueParams(payload);
        if (imei != null) params.put("imei", imei);
    }

    /** 处理 APWR 佩戴状态 */
    private void handleApWr(String payload, String clientInfo, String imei) {
        Map<String, String> params = parseKeyValueParams(payload);
        if (imei != null) params.put("imei", imei);
    }

    // --------------------------- 数据保存方法 ---------------------------

    /** 保存定位数据到数据库 */
    private void saveLocationData(Map<String, String> params, String imei) {
        try {
            LocationRecord rec = new LocationRecord();
            // 解析常用字段
            // 优先使用显式的 lat/lon 字段，如果不存在则回退到 wifiGeoLat/wifiGeoLon
            Double latVal = null;
            Double lonVal = null;
            if (params.containsKey("lat")) latVal = parseDoubleSafely(params.get("lat"));
            else if (params.containsKey("wifiGeoLat")) latVal = parseDoubleSafely(params.get("wifiGeoLat"));
            if (params.containsKey("lon")) lonVal = parseDoubleSafely(params.get("lon"));
            else if (params.containsKey("wifiGeoLon")) lonVal = parseDoubleSafely(params.get("wifiGeoLon"));
            if (latVal != null) rec.setLatitude(latVal);
            if (lonVal != null) rec.setLongitude(lonVal);

            // 若 GPS 坐标为占位 (0 或 null) 且存在 wifiGeoLat/wifiGeoLon，则覆盖
            boolean gpsPlaceholder = (latVal == null || lonVal == null || (latVal != null && latVal.doubleValue() == 0.0) || (lonVal != null && lonVal.doubleValue() == 0.0));
            if (gpsPlaceholder && params.containsKey("wifiGeoLat") && params.containsKey("wifiGeoLon")) {
                Double wlat = parseDoubleSafely(params.get("wifiGeoLat"));
                Double wlon = parseDoubleSafely(params.get("wifiGeoLon"));
                if (wlat != null && wlon != null) {
                    rec.setLatitude(wlat);
                    rec.setLongitude(wlon);
                    params.put("locationSource", "wifi");
                }
            }

            if (!hasValidCoordinate(rec.getLatitude(), rec.getLongitude())
                    && params.containsKey("mcc")
                    && params.containsKey("mnc")
                    && params.containsKey("lac")
                    && params.containsKey("cid")) {
                Map<String, Double> lbsLocation = amapLocationService.locateByCell(
                        imei,
                        params.get("mcc"),
                        params.get("mnc"),
                        params.get("lac"),
                        params.get("cid"),
                        params.get("gsm"));
                if (lbsLocation != null) {
                    rec.setLatitude(lbsLocation.get("lat"));
                    rec.setLongitude(lbsLocation.get("lng"));
                    params.put("locationSource", "lbs");
                }
            }

            // 保存速度和方向
            if (params.containsKey("speed")) {
                try { rec.setSpeed(Double.parseDouble(params.get("speed"))); } catch (Exception ignored) {}
            }
            if (params.containsKey("direction")) {
                try { rec.setDirection(Double.parseDouble(params.get("direction"))); } catch (Exception ignored) {}
            }
            rec.setBatteryLevel(parseIntegerParam(params, "battery_level", "batteryLevel", "battery"));

            // 获取地址信息
            if (rec.getLatitude() != null && rec.getLongitude() != null) {
                String address = amapLocationService.regeoAddress(rec.getLatitude(), rec.getLongitude());
                if (address != null) {
                    rec.setAddress(address);
                }
            }

            // 保存地址信息（如果参数中已有则优先使用）
            if (params.containsKey("address")) rec.setAddress(params.get("address"));

            // 保存定位源
            if (params.containsKey("locationSource")) rec.setSource(params.get("locationSource"));
            else if (params.containsKey("source")) rec.setSource(params.get("source"));

            // 保存原始GPS数据
            if (params.containsKey("gps_raw")) {
                rec.setGpsRaw(params.get("gps_raw"));
            } else if (params.containsKey("rawGpsPart")) {
                rec.setGpsRaw(params.get("rawGpsPart"));
            }

            // 保存额外原始数据
            if (params.containsKey("extra_raw")) {
                rec.setExtraRaw(params.get("extra_raw"));
            } else if (params.containsKey("rawExtraPart")) {
                rec.setExtraRaw(params.get("rawExtraPart"));
            } else {
                rec.setExtraRaw(params.toString());
            }

            // 试图关联 imei
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) {
                    rec.setDevice(d);
                    // 保存位置数据后，调用跟踪服务进行围栏检查
                    trackingService.processLocationRecord(rec);
                }
            }

            locationRecordRepository.save(rec);
            log.debug("✅ 成功保存定位数据: IMEI={}", imei);
        } catch (Exception e) {
            log.error("❌ 保存定位数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    /** 保存健康数据到数据库 */
    private void saveHealthData(Map<String, String> params, String imei) {
        try {
            HealthRecord rec = new HealthRecord();
            // 保存数据类型和值
            if (params.containsKey("temp")) {
                rec.setDataType("body_temperature");
                rec.setValue(params.get("temp"));
            } else if (params.containsKey("data_type")) {
                rec.setDataType(params.get("data_type"));
                rec.setValue(params.getOrDefault("value", params.toString()));
            } else if (params.containsKey("wrist_temp")) {
                rec.setDataType("body_temperature");
                rec.setValue(params.get("wrist_temp"));
            } else if (params.containsKey("spo2")) {
                rec.setDataType("blood_oxygen");
                rec.setValue(params.get("spo2"));
            } else if (params.containsKey("hr")) {
                rec.setDataType("heart_rate");
                rec.setValue(params.get("hr"));
            } else {
                rec.setDataType("unknown");
                rec.setValue(params.toString());
            }

            // 保存时间戳
            if (params.containsKey("timestamp")) {
                try {
                    java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(params.get("timestamp"), df);
                    java.time.Instant inst = ldt.toInstant(java.time.ZoneOffset.UTC);
                    rec.setRecvTime(java.util.Date.from(inst));
                } catch (Exception ignored) {
                    // 尝试解析其他时间格式
                    try {
                        java.time.format.DateTimeFormatter df2 = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                        java.time.LocalDateTime ldt2 = java.time.LocalDateTime.parse(params.get("timestamp"), df2);
                        java.time.Instant inst2 = ldt2.toInstant(java.time.ZoneOffset.UTC);
                        rec.setRecvTime(java.util.Date.from(inst2));
                    } catch (Exception ignored2) {}
                }
            }

            // 关联设备
            if (imei != null && !imei.isEmpty()) {
                rec.setImei(imei);
                Device d = findOrCreateDeviceByImei(imei);
                if (d != null) {
                    rec.setDeviceId(d.getId());
                    patientDeviceRepository.findByDeviceId(d.getId()).stream()
                            .filter(pd -> pd.getIsActive() == null || pd.getIsActive())
                            .findFirst()
                            .ifPresent(pd -> rec.setPatientId(pd.getPatientId()));
                }
            }

            // 保存原始数据
            rec.setRawData(params.toString());
            
            // 保存健康数据到数据库
            healthRecordRepository.save(rec);
            log.debug("✅ 成功保存健康数据: IMEI={}, 类型={}", imei, rec.getDataType());
            
            // 调用健康监测服务检测是否异常
            healthMonitorService.checkHealthData(rec);
        } catch (Exception e) {
            log.error("❌ 保存健康数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    /** 保存心跳数据到数据库 */
    private void saveHeartbeatData(Map<String, String> params, String imei) {
        try {
            HeartbeatRecord rec = new HeartbeatRecord();
            // 保存状态块
            rec.setStatusBlock(params.getOrDefault("param_block", 
                              params.getOrDefault("status_block", 
                              params.getOrDefault("statusBlock", null))));
            // 保存步数/计数器
            rec.setCounter(params.getOrDefault("steps", 
                          params.getOrDefault("counter", null)));
            // 保存滚动计数
            rec.setRollCount(params.getOrDefault("roll_count", 
                           params.getOrDefault("rollCount", null)));
            // 保存工作模式
            rec.setWorkMode(params.getOrDefault("work_mode", 
                           params.getOrDefault("workMode", null)));
            // 保存间隔秒数
            if (params.containsKey("interval")) {
                try { rec.setIntervalSeconds(Integer.parseInt(params.get("interval"))); } catch (Exception ignored) {}
            }
            if (params.containsKey("interval_seconds")) {
                try { rec.setIntervalSeconds(Integer.parseInt(params.get("interval_seconds"))); } catch (Exception ignored) {}
            }
            // 保存原始负载
            rec.setRawPayload(params.toString());

            // 关联设备 - 只有当IMEI有效时才创建关联
            String validImei = null;
            if (imei != null && imei.length() == 15 && imei.chars().allMatch(Character::isDigit)) {
                validImei = imei;
                rec.setImei(validImei);
                // 只查找现有设备，不创建新设备
                deviceRepository.findByImei(validImei).ifPresent(rec::setDevice);
            }

            heartbeatRecordRepository.save(rec);
            log.debug("✅ 成功保存心跳数据: IMEI={}", validImei);
        } catch (Exception e) {
            log.error("❌ 保存心跳数据失败: IMEI={}, 错误={}", imei, e.getMessage());
        }
    }

    // --------------------------- 辅助方法 ---------------------------

    /** 将类似 key=value,key2=value2 的简单字符串解析为 map；如果不是该格式则保存为 raw */
    private Map<String, String> parseKeyValueParams(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return m;
        if (s.contains("=")) {
            String[] kvs = s.split(",");
            for (String kv : kvs) {
                String[] t = kv.split("=", 2);
                if (t.length == 2) m.put(t[0], t[1]);
            }
        } else {
            m.put("raw", s);
        }
        return m;
    }

    /** 将字符串安全转换为 Double（返回 null 表示转换失败或为空） */
    private Double parseDoubleSafely(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private Integer parseIntegerParam(Map<String, String> params, String... keys) {
        if (params == null || keys == null) return null;
        for (String key : keys) {
            String value = params.get(key);
            if (value == null || value.isBlank()) continue;
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                log.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        return null;
    }

    private boolean hasValidCoordinate(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(Double.compare(lat, 0.0) == 0 && Double.compare(lng, 0.0) == 0);
    }

    /** 查找或创建设备 */
    private Device findOrCreateDeviceByImei(String imei) {
        if (imei == null || imei.isEmpty()) return null;
        
        // 验证IMEI是否有效
        if (!isValidImei(imei)) {
            log.debug("📌 跳过创建无效IMEI设备: {}", imei);
            return null;
        }
        
        Optional<Device> opt = deviceRepository.findByImei(imei);
        if (opt.isPresent()) {
            return opt.get();
        }
        // 创建新设备
        Device d = new Device();
        d.setImei(imei);
        d.setCreatedAt(new java.util.Date());
        return deviceRepository.save(d);
    }
    
    /**
     * 验证IMEI是否有效
     * @param imei 要验证的IMEI
     * @return true if valid, false otherwise
     */
    private boolean isValidImei(String imei) {
        if (imei == null || imei.length() != 15) {
            return false;
        }
        
        // 排除已知的无效IMEI
        if ("05700008100008".equals(imei) || "000570001000000".equals(imei)) {
            return false;
        }
        
        // 排除全是0的IMEI
        if (imei.matches("^0+$")) {
            return false;
        }
        
        // 排除以000开头的IMEI，这些看起来像是无效的测试值
        if (imei.startsWith("000")) {
            return false;
        }
        
        // 可以添加Luhn算法验证，这里暂时省略
        return true;
    }
}
