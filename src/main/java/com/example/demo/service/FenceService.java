package com.example.demo.service;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.GeoFence;
import com.example.demo.model.PatientDevice;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class FenceService {
    private static final Logger log = LoggerFactory.getLogger(FenceService.class);

    private final GeoFenceRepository fenceRepo;
    private final FenceAlertRepository alertRepo;
    private final SimpleFenceAlertRepository simpleAlertRepo;
    private final PatientDeviceRepository patientDeviceRepo;
    private final DeviceRepository deviceRepo;
    private final AlarmService alarmService;
    private final JdbcTemplate jdbc;
    private final AlarmRepository alarmRepo;
    private final DeviceStatusRepository statusRepo;

    public FenceService(GeoFenceRepository fenceRepo,
                       FenceAlertRepository alertRepo,
                       SimpleFenceAlertRepository simpleAlertRepo,
                       PatientDeviceRepository patientDeviceRepo,
                       DeviceRepository deviceRepo,
                       AlarmService alarmService,
                       JdbcTemplate jdbc,
                       AlarmRepository alarmRepo,
                       DeviceStatusRepository statusRepo) {
        this.fenceRepo = fenceRepo;
        this.alertRepo = alertRepo;
        this.simpleAlertRepo = simpleAlertRepo;
        this.patientDeviceRepo = patientDeviceRepo;
        this.deviceRepo = deviceRepo;
        this.alarmService = alarmService;
        this.jdbc = jdbc;
        this.alarmRepo = alarmRepo;
        this.statusRepo = statusRepo;
    }

    /**
     * 检查围栏并触发报警
     * @param curr 当前设备位置
     * @param prevLat 上一次的纬度
     * @param prevLng 上一次的经度
     */
    public void checkFencesAndAlert(DeviceLocation curr, Double prevLat, Double prevLng) {
        log.info("checkFencesAndAlert called for device: {}, IMEI: {}, currLat: {}, currLng: {}, prevLat: {}, prevLng: {}", 
                curr.getDeviceId(), curr.getImei(), curr.getLatitude(), curr.getLongitude(), prevLat, prevLng);
        
        // 1. 获取设备关联的病人列表
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(curr.getDeviceId());
        log.info("Found {} patients associated with device: {}", patientDevices.size(), curr.getDeviceId());
        
        // 调试设备关联的病人列表
        for (PatientDevice pd : patientDevices) {
            log.info("Device: {} associated with patient: {}", curr.getDeviceId(), pd.getPatientId());
        }
        
        if (patientDevices.isEmpty()) {
            log.info("No patients associated with device: {}", curr.getDeviceId());
            return;
        }
        
        // 2. 获取设备实体，用于创建报警
        var device = deviceRepo.findById(curr.getDeviceId()).orElse(null);
        if (device == null) {
            log.warn("Device not found for ID: {}", curr.getDeviceId());
            return;
        }
        log.info("Device found: {}, IMEI: {}", device.getId(), device.getImei());
        
        // 3. 遍历每个关联的病人
        for (PatientDevice pd : patientDevices) {
            Long patientId = pd.getPatientId();
            log.info("Processing patient: {} for device: {}", patientId, curr.getDeviceId());
            
            // 4. 获取该病人关联的围栏列表
            List<GeoFence> fences = getPatientFences(patientId);
            log.info("Total unique fences for patient: {} is {}", patientId, fences.size());
            
            // 5. 遍历每个围栏进行判断
            for (GeoFence fence : fences) {
                log.info("Checking fence: {} (type: {}, status: {}) for patient: {}", 
                        fence.getId(), fence.getType(), fence.getStatus(), patientId);
                
                // 检查围栏状态是否为active
                if (!"active".equalsIgnoreCase(fence.getStatus())) {
                    log.info("Skipping inactive fence: {}, status: {}", fence.getId(), fence.getStatus());
                    continue;
                }
                
                // 只处理圆形围栏
                if (!"circle".equalsIgnoreCase(fence.getType())) {
                    log.info("Skipping non-circle fence: {}", fence.getId());
                    continue;
                }
                
                // 计算设备到围栏中心的距离
                double distance = distanceMeters(
                        curr.getLatitude().doubleValue(), 
                        curr.getLongitude().doubleValue(), 
                        fence.getCenterLat(), 
                        fence.getCenterLng());
                
                boolean wasInside = inside(prevLat, prevLng, fence);
                boolean nowInside = inside(curr.getLatitude(), curr.getLongitude(), fence);
                
                log.info("Fence: {}, wasInside: {}, nowInside: {}, distance: {}/{}, prevLat: {}, prevLng: {}, fence center: {}, {}, radius: {}",
                        fence.getId(), wasInside, nowInside, distance, fence.getRadius(),
                        prevLat, prevLng, fence.getCenterLat(), fence.getCenterLng(), fence.getRadius());
                
                // 6. 获取病人信息
                var patient = pd.getPatient();
                
                            // 7. 当设备不在围栏中时，生成报警
                // 条件：设备现在不在围栏中
                boolean shouldAlarm = !nowInside;
                log.info("Should alarm: {} for device: {}, fence: {}, wasInside: {}, nowInside: {}, prevLat: {}, prevLng: {}",
                        shouldAlarm, curr.getDeviceId(), fence.getId(), wasInside, nowInside, prevLat, prevLng);
                
                if (shouldAlarm) {
                    // 检查报警间隔：同一病人同一类型的报警，默认5分钟间隔
                    boolean canCreateAlarm = checkAlarmInterval(patientId, "fence_breach");
                    log.info("Can create alarm: {} for patient: {}, alarmType: fence_breach", canCreateAlarm, patientId);
                    
                    if (canCreateAlarm) {
                        // 创建SimpleFenceAlert记录（简化版，仅包含数据库表中实际存在的字段）
                        var alert = new com.example.demo.model.SimpleFenceAlert();
                        alert.setDevice(device);
                        alert.setPatient(patient);
                        alert.setFence(fence);
                        alert.setImei(curr.getImei());
                        alert.setAlertType("fence_breach");
                        alert.setLatitude(curr.getLatitude().doubleValue());
                        alert.setLongitude(curr.getLongitude().doubleValue());
                        alert.setStatus("pending");
                        alert.setAlertTime(new Date());
                        alert.setTriggeredTime(new Date());
                        alert.setCreatedAt(new Date());
                        alert.setUpdatedAt(new Date());
                        alert.setIsRead(0);
                        simpleAlertRepo.save(alert);
                        log.info("Simple fence breach alert created: device={}, patient={}, fence={}, alertType={}", 
                                curr.getDeviceId(), patientId, fence.getId(), alert.getAlertType());
                        
                        // 创建系统报警记录
                        var alarm = new com.example.demo.model.Alarm();
                        alarm.setDevice(device);
                        alarm.setPatient(patient);
                        alarm.setAlarmType("fence_breach");
                        alarm.setAlarmLevel("warning");
                        alarm.setLatitude(curr.getLatitude().doubleValue());
                        alarm.setLongitude(curr.getLongitude().doubleValue());
                        alarm.setTriggeredTime(new Date());
                        alarm.setStatus("pending");
                        alarm.setIsRead(false);
                        alarm.setCreatedAt(new Date());
                        alarm.setUpdatedAt(new Date());
                        
                        // 调用报警服务创建报警
                        alarmService.createAlarm(alarm);
                        log.info("Created fence breach alarm: device={}, patient={}, alarmType={}, status={}", 
                                curr.getDeviceId(), patientId, alarm.getAlarmType(), alarm.getStatus());
                    } else {
                        log.info("Alarm skipped due to interval constraint: patient={}, alarmType: fence_breach", patientId);
                    }
                }
            }
        }
    }

    /**
     * 获取病人关联的所有围栏
     * @param patientId 病人ID
     * @return 围栏列表
     */
    private List<GeoFence> getPatientFences(Long patientId) {
        // 4.1 先获取旧格式的围栏（通过patient_id字段关联的围栏）
        List<GeoFence> patientFences = fenceRepo.listByPatient(patientId);
        log.info("Found {} old-format fences for patient: {}", patientFences.size(), patientId);
        
        // 4.2 再获取通过fence_patients表关联的围栏
        // 这里使用JdbcTemplate直接查询，因为FencePatientRepository没有提供相应的方法
        List<GeoFence> fencePatientsFences = jdbc.query(
            "SELECT gf.* FROM geo_fences gf INNER JOIN fence_patients fp ON gf.id = fp.fence_id WHERE fp.patient_id = ? AND gf.status = 'active' ORDER BY gf.id DESC",
            (rs, n) -> {
                GeoFence f = new GeoFence();
                f.setId(rs.getLong("id"));
                f.setName(rs.getString("name"));
                f.setType(rs.getString("type"));
                f.setCenterLat(rs.getObject("center_lat", Double.class));
                f.setCenterLng(rs.getObject("center_lng", Double.class));
                f.setRadius(rs.getObject("radius", Integer.class));
                f.setCoordinates(rs.getString("coordinates"));
                f.setStatus(rs.getString("status"));
                f.setDescription(rs.getString("description"));
                f.setCreatedBy(rs.getObject("created_by", Long.class));
                f.setPatientId(rs.getObject("patient_id", Long.class));
                f.setCreatedAt(rs.getTimestamp("created_at") != null ? new Date(rs.getTimestamp("created_at").getTime()) : null);
                f.setUpdatedAt(rs.getTimestamp("updated_at") != null ? new Date(rs.getTimestamp("updated_at").getTime()) : null);
                return f;
            },
            patientId
        );
        log.info("Found {} new-format fences for patient: {}", fencePatientsFences.size(), patientId);
        
        // 4.3 合并围栏列表，避免重复
        List<GeoFence> uniqueFences = new ArrayList<>();
        for (GeoFence fence : patientFences) {
            if (!uniqueFences.contains(fence)) {
                uniqueFences.add(fence);
            }
        }
        for (GeoFence fence : fencePatientsFences) {
            if (!uniqueFences.contains(fence)) {
                uniqueFences.add(fence);
            }
        }
        return uniqueFences;
    }

    /**
     * 检查位置是否在围栏内
     * @param lat 纬度
     * @param lng 经度
     * @param f 围栏
     * @return 是否在围栏内
     */
    public boolean inside(java.math.BigDecimal lat, java.math.BigDecimal lng, GeoFence f) {
        if (lat == null || lng == null || f.getCenterLat() == null || f.getCenterLng() == null || f.getRadius() == null) return false;
        double d = distanceMeters(lat.doubleValue(), lng.doubleValue(), f.getCenterLat(), f.getCenterLng());
        return d <= f.getRadius();
    }

    /**
     * 检查位置是否在围栏内
     * @param lat 纬度
     * @param lng 经度
     * @param f 围栏
     * @return 是否在围栏内
     */
    public boolean inside(Double lat, Double lng, GeoFence f) {
        if (lat == null || lng == null || f.getCenterLat() == null || f.getCenterLng() == null || f.getRadius() == null) return false;
        double d = distanceMeters(lat, lng, f.getCenterLat(), f.getCenterLng());
        return d <= f.getRadius();
    }

    /**
     * 检查同一病人同一类型的报警间隔
     * @param patientId 病人ID
     * @param alarmType 报警类型
     * @return 是否可以创建新报警
     */
    private boolean checkAlarmInterval(Long patientId, String alarmType) {
        // 默认报警间隔：5分钟
        long alarmIntervalMinutes = 5;
        long alarmIntervalMillis = alarmIntervalMinutes * 60 * 1000;
        
        // 获取最近的一条同一病人同一类型的报警
        Optional<com.example.demo.model.Alarm> lastAlarmOpt = alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(patientId, alarmType);
        
        if (lastAlarmOpt.isPresent()) {
            com.example.demo.model.Alarm lastAlarm = lastAlarmOpt.get();
            Date lastTriggeredTime = lastAlarm.getTriggeredTime();
            Date now = new Date();
            
            // 计算时间差
            long timeDiff = now.getTime() - lastTriggeredTime.getTime();
            log.info("Last alarm time: {}, now: {}, diff: {}ms, interval: {}ms", 
                    lastTriggeredTime, now, timeDiff, alarmIntervalMillis);
            
            // 如果时间差大于间隔，允许创建新报警
            return timeDiff > alarmIntervalMillis;
        } else {
            // 没有最近报警，允许创建新报警
            return true;
        }
    }
    
    /**
     * 计算两点之间的距离（米）
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0; // Earth radius meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}