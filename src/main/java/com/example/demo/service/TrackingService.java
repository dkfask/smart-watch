package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.FenceAlert;
import com.example.demo.model.GeoFence;
import com.example.demo.model.LocationRecord;
import com.example.demo.model.Patient;
import com.example.demo.model.PatientDevice;
import com.example.demo.model.SimpleFenceAlert;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrackingService {
    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final DeviceLocationRepository locationRepo;
    private final DeviceStatusRepository statusRepo;
    private final GeoFenceRepository fenceRepo;
    private final FenceAlertRepository alertRepo;
    private final SimpleFenceAlertRepository simpleAlertRepo;
    private final PatientDeviceRepository patientDeviceRepo;
    private final DeviceRepository deviceRepo;
    private final AlarmService alarmService;
    private final JdbcTemplate jdbc;
    private final AlarmRepository alarmRepo;

    public TrackingService(DeviceLocationRepository locationRepo,
                           DeviceStatusRepository statusRepo,
                           GeoFenceRepository fenceRepo,
                           FenceAlertRepository alertRepo,
                           SimpleFenceAlertRepository simpleAlertRepo,
                           PatientDeviceRepository patientDeviceRepo,
                           DeviceRepository deviceRepo,
                           AlarmService alarmService,
                           JdbcTemplate jdbc,
                           AlarmRepository alarmRepo) {
        this.locationRepo = locationRepo;
        this.statusRepo = statusRepo;
        this.fenceRepo = fenceRepo;
        this.alertRepo = alertRepo;
        this.simpleAlertRepo = simpleAlertRepo;
        this.patientDeviceRepo = patientDeviceRepo;
        this.deviceRepo = deviceRepo;
        this.alarmService = alarmService;
        this.jdbc = jdbc;
        this.alarmRepo = alarmRepo;
    }

    public long reportLocation(DeviceLocation dl) {
        // 1) 写入历史位置
        long id = locationRepo.insert(dl);

        // 2) 获取当前设备的位置信息，用于围栏判断
        Double currLat = dl.getLatitude() != null ? dl.getLatitude().doubleValue() : null;
        Double currLng = dl.getLongitude() != null ? dl.getLongitude().doubleValue() : null;
        
        // 3) 在更新设备状态之前，先获取上一次的位置信息
        Double prevLat = null;
        Double prevLng = null;
        Optional<DeviceStatus> prevStatusOpt = statusRepo.findById(dl.getDeviceId());
        if (prevStatusOpt.isPresent()) {
            DeviceStatus prevStatus = prevStatusOpt.get();
            prevLat = prevStatus.getLastLatitude();
            prevLng = prevStatus.getLastLongitude();
        }
        
        log.info("Device: {} previous location - lat: {}, lng: {}", dl.getDeviceId(), prevLat, prevLng);
        
        // 4) 更新实时状态
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(dl.getDeviceId());
        s.setBatteryLevel(dl.getBatteryLevel());
        s.setIsOnline(true);
        // 设置设备经纬度信息，用于围栏判断
        if (currLat != null) {
            s.setLastLatitude(currLat);
        }
        if (currLng != null) {
            s.setLastLongitude(currLng);
        }
        s.setLastLocationTime(dl.getTime() != null ? new Date(dl.getTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000) : new Date());
        // 同步写入 imei 到 DeviceStatus，便于在设备状态接口回显
        s.setImei(dl.getImei());
        statusRepo.upsert(s);

        // 5) 围栏判断：
        //   - 如果有上一点坐标，判断进出状态变化
        //   - 如果没有上一点坐标，判断当前是否在围栏外，若是则触发报警
        if (currLat != null && currLng != null) {
            checkFencesAndAlert(dl, prevLat, prevLng);
        }
        return id;
    }
    
    /**
     * 处理LocationRecord，用于围栏判断
     * @param locationRecord 位置记录
     */
    public void processLocationRecord(LocationRecord locationRecord) {
        // 1) 获取当前设备的位置信息
        Double currLat = locationRecord.getLatitude();
        Double currLng = locationRecord.getLongitude();
        
        // 2) 在更新设备状态之前，先获取上一次的位置信息
        Double prevLat = null;
        Double prevLng = null;
        Optional<DeviceStatus> prevStatusOpt = statusRepo.findById(locationRecord.getDevice().getId());
        if (prevStatusOpt.isPresent()) {
            DeviceStatus prevStatus = prevStatusOpt.get();
            prevLat = prevStatus.getLastLatitude();
            prevLng = prevStatus.getLastLongitude();
        }
        
        log.info("Device: {} previous location - lat: {}, lng: {}", locationRecord.getDevice().getId(), prevLat, prevLng);
        
        // 3) 更新实时状态
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(locationRecord.getDevice().getId());
        s.setBatteryLevel(null); // LocationRecord没有电池信息
        s.setIsOnline(true);
        // 设置设备经纬度信息，用于围栏判断
        s.setLastLatitude(currLat);
        s.setLastLongitude(currLng);
        s.setLastLocationTime(new Date());
        // 同步写入 imei 到 DeviceStatus
        s.setImei(locationRecord.getImei());
        statusRepo.upsert(s);
        
        // 4) 围栏判断：
        //   - 如果有上一点坐标，判断进出状态变化
        //   - 如果没有上一点坐标，判断当前是否在围栏外，若是则触发报警
        if (currLat != null && currLng != null) {
            // 创建DeviceLocation对象用于围栏判断
            DeviceLocation dl = new DeviceLocation();
            dl.setDeviceId(locationRecord.getDevice().getId());
            dl.setLatitude(BigDecimal.valueOf(currLat));
            dl.setLongitude(BigDecimal.valueOf(currLng));
            dl.setImei(locationRecord.getImei());
            
            checkFencesAndAlert(dl, prevLat, prevLng);
        }
    }

    private void checkFencesAndAlert(DeviceLocation curr, Double prevLat, Double prevLng) {
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
        Device device = deviceRepo.findById(curr.getDeviceId()).orElse(null);
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
            log.info("Total unique fences for patient: {} is {}", patientId, uniqueFences.size());
            
            // 5. 遍历每个围栏进行判断
            for (GeoFence fence : uniqueFences) {
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
                Patient patient = pd.getPatient();
                
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
                        SimpleFenceAlert alert = new SimpleFenceAlert();
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
                        Alarm alarm = new Alarm();
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

    private boolean inside(BigDecimal lat, BigDecimal lng, GeoFence f) {
        if (lat == null || lng == null || f.getCenterLat() == null || f.getCenterLng() == null || f.getRadius() == null) return false;
        double d = distanceMeters(lat.doubleValue(), lng.doubleValue(), f.getCenterLat(), f.getCenterLng());
        return d <= f.getRadius();
    }

    private boolean inside(Double lat, Double lng, GeoFence f) {
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
        Optional<Alarm> lastAlarmOpt = alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(patientId, alarmType);
        
        if (lastAlarmOpt.isPresent()) {
            Alarm lastAlarm = lastAlarmOpt.get();
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
    
    // Haversine distance in meters
    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
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
