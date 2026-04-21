package com.example.demo.service;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.GeoFence;
import com.example.demo.model.PatientDevice;
import com.example.demo.model.Alert;
import com.example.demo.model.dto.FenceCreateRequest;
import com.example.demo.model.dto.FenceDto;
import com.example.demo.model.dto.FenceUpdateRequest;
import com.example.demo.repository.*;
import com.example.demo.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final FencePatientRepository fencePatientRepo;
    private final AlertRepository alertRepository;

    public FenceService(GeoFenceRepository fenceRepo,
                       FenceAlertRepository alertRepo,
                       SimpleFenceAlertRepository simpleAlertRepo,
                       PatientDeviceRepository patientDeviceRepo,
                       DeviceRepository deviceRepo,
                       AlarmService alarmService,
                       JdbcTemplate jdbc,
                       AlarmRepository alarmRepo,
                       DeviceStatusRepository statusRepo,
                       FencePatientRepository fencePatientRepo,
                       AlertRepository alertRepository) {
        this.fenceRepo = fenceRepo;
        this.alertRepo = alertRepo;
        this.simpleAlertRepo = simpleAlertRepo;
        this.patientDeviceRepo = patientDeviceRepo;
        this.deviceRepo = deviceRepo;
        this.alarmService = alarmService;
        this.jdbc = jdbc;
        this.alarmRepo = alarmRepo;
        this.statusRepo = statusRepo;
        this.fencePatientRepo = fencePatientRepo;
        this.alertRepository = alertRepository;
    }

    /**
     * 创建围栏（含病人关联）
     * @param request 围栏创建请求DTO
     * @return 新围栏ID
     */
    @Transactional
    public long createFence(FenceCreateRequest request) {
        GeoFence f = mapRequestToFence(request);
        long id = fenceRepo.create(f);
        bindPatients(id, request.getPatientIds());
        return id;
    }

    /**
     * 更新围栏（含病人关联）
     * @param id 围栏ID
     * @param request 围栏更新请求DTO
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateFence(long id, FenceUpdateRequest request) {
        GeoFence f = mapRequestToFence(request);
        f.setId(id);
        int n = fenceRepo.update(f);
        if (n <= 0) return false;
        fencePatientRepo.clearFencePatients(id);
        bindPatients(id, request.getPatientIds());
        return true;
    }

    /**
     * 获取围栏详情（含关联病人ID）
     * @param id 围栏ID
     * @return 围栏DTO
     */
    public FenceDto getFenceDetail(long id) {
        Optional<GeoFence> fenceOpt = fenceRepo.findById(id);
        if (fenceOpt.isEmpty()) return null;
        return buildFenceDto(fenceOpt.get());
    }

    /**
     * 获取围栏列表（含关联病人ID）
     * @param fences 围栏实体列表
     * @return 围栏DTO列表
     */
    public List<FenceDto> listFencesWithPatients(List<GeoFence> fences) {
        return fences.stream().map(this::buildFenceDto).collect(Collectors.toList());
    }

    /**
     * 删除围栏
     * @param id 围栏ID
     * @return 是否删除成功
     */
    public boolean deleteFence(long id) {
        return fenceRepo.delete(id) > 0;
    }

    /**
     * 检查围栏并触发报警
     * @param curr 当前设备位置
     * @param prevLat 上一次的纬度
     * @param prevLng 上一次的经度
     */
    public void checkFencesAndAlert(DeviceLocation curr, Double prevLat, Double prevLng) {
        log.debug("checkFencesAndAlert: device={}, imei={}, lat={}, lng={}",
                curr.getDeviceId(), curr.getImei(), curr.getLatitude(), curr.getLongitude());

        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(curr.getDeviceId());
        if (patientDevices.isEmpty()) return;

        var device = deviceRepo.findById(curr.getDeviceId()).orElse(null);
        if (device == null) return;

        for (PatientDevice pd : patientDevices) {
            Long patientId = pd.getPatientId();
            List<GeoFence> fences = getPatientFences(patientId);
            for (GeoFence fence : fences) {
                checkSingleFence(fence, curr, prevLat, prevLng, pd, device, patientId);
            }
        }
    }

    /**
     * 检查单个围栏
     */
    private void checkSingleFence(GeoFence fence, DeviceLocation curr, Double prevLat, Double prevLng,
                                   PatientDevice pd, com.example.demo.model.Device device, Long patientId) {
        if (!"active".equalsIgnoreCase(fence.getStatus())) return;
        if (!"circle".equalsIgnoreCase(fence.getType())) return;

        boolean nowInside = GeoUtils.inside(curr.getLatitude(), curr.getLongitude(), fence);
        if (nowInside) return;

        if (shouldCreateAlarm(patientId, "fence_breach")) {
            createFenceBreachAlert(device, pd.getPatient(), fence, curr);
        }
    }

    /**
     * 创建围栏越界报警（使用 AlarmService.createAlarm 统一处理双写）
     * @param device 设备对象
     * @param patient 病人对象
     * @param fence 围栏对象
     * @param curr 当前位置
     */
    private void createFenceBreachAlert(com.example.demo.model.Device device, com.example.demo.model.Patient patient,
                                         GeoFence fence, DeviceLocation curr) {
        // 保存 SimpleFenceAlert（旧表兼容）
        var alert = new com.example.demo.model.SimpleFenceAlert();
        alert.setDevice(device);
        alert.setPatient(patient);
        alert.setFence(fence);
        alert.setImei(curr.getImei());
        alert.setAlertType("fence_breach");
        alert.setLatitude(curr.getLatitude().doubleValue());
        alert.setLongitude(curr.getLongitude().doubleValue());
        simpleAlertRepo.save(alert);

        // 使用 AlarmService.createAlarm 统一处理 Alarm + Alert 双写（包含事务和 WebSocket 推送）
        var alarm = new com.example.demo.model.Alarm();
        alarm.setDevice(device);
        alarm.setPatient(patient);
        alarm.setAlarmType("fence_breach");
        alarm.setAlarmLevel("warning");
        alarm.setLatitude(curr.getLatitude().doubleValue());
        alarm.setLongitude(curr.getLongitude().doubleValue());
        alarmService.createAlarm(alarm);

        log.info("Fence breach alert: device={}, patient={}, fence={}", device.getId(), patient.getId(), fence.getId());
    }

    /**
     * 检查同一病人同一类型的报警间隔
     * @param patientId 病人ID
     * @param alarmType 报警类型
     * @return 是否可以创建新报警
     */
    private boolean shouldCreateAlarm(Long patientId, String alarmType) {
        long alarmIntervalMillis = 5 * 60 * 1000;
        Optional<com.example.demo.model.Alarm> lastAlarmOpt =
                alarmRepo.findTopByPatientIdAndAlarmTypeOrderByTriggeredTimeDesc(patientId, alarmType);
        if (lastAlarmOpt.isEmpty()) return true;
        long timeDiff = new Date().getTime() - lastAlarmOpt.get().getTriggeredTime().getTime();
        return timeDiff > alarmIntervalMillis;
    }

    /**
     * 获取病人关联的所有围栏
     * @param patientId 病人ID
     * @return 围栏列表
     */
    private List<GeoFence> getPatientFences(Long patientId) {
        List<GeoFence> patientFences = fenceRepo.listByPatient(patientId);
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
                f.setIsMultiPatient(rs.getObject("is_multi_patient", Boolean.class));
                f.setCreatedAt(rs.getTimestamp("created_at") != null ? new Date(rs.getTimestamp("created_at").getTime()) : null);
                f.setUpdatedAt(rs.getTimestamp("updated_at") != null ? new Date(rs.getTimestamp("updated_at").getTime()) : null);
                return f;
            },
            patientId
        );

        Set<Long> seen = new HashSet<>();
        List<GeoFence> uniqueFences = new ArrayList<>();
        for (GeoFence fence : patientFences) {
            if (seen.add(fence.getId())) uniqueFences.add(fence);
        }
        for (GeoFence fence : fencePatientsFences) {
            if (seen.add(fence.getId())) uniqueFences.add(fence);
        }
        return uniqueFences;
    }

    /**
     * 将请求DTO映射为GeoFence实体
     */
    private GeoFence mapRequestToFence(FenceCreateRequest request) {
        GeoFence f = new GeoFence();
        f.setName(request.getName());
        f.setType(request.getType() != null ? request.getType() : "circle");
        f.setRadius(request.getRadius());
        f.setCoordinates(request.getCoordinates());
        f.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        f.setDescription(request.getDescription());
        f.setCreatedBy(request.getCreatedBy());
        f.setPatientId(request.getPatientId());
        // 如果未指定 isMultiPatient，根据 patientIds 列表自动判断
        f.setIsMultiPatient(request.getIsMultiPatient() != null
                ? request.getIsMultiPatient()
                : (request.getPatientIds() != null && request.getPatientIds().size() > 1));
        f.setCenterLat(request.getCenterLat());
        f.setCenterLng(request.getCenterLng());
        return f;
    }

    /**
     * 将更新请求DTO映射为GeoFence实体
     */
    private GeoFence mapRequestToFence(FenceUpdateRequest request) {
        GeoFence f = new GeoFence();
        f.setName(request.getName());
        f.setType(request.getType() != null ? request.getType() : "circle");
        f.setRadius(request.getRadius());
        f.setCoordinates(request.getCoordinates());
        f.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        f.setDescription(request.getDescription());
        f.setCreatedBy(request.getCreatedBy());
        f.setPatientId(request.getPatientId());
        f.setIsMultiPatient(request.getIsMultiPatient() != null
                ? request.getIsMultiPatient()
                : (request.getPatientIds() != null && request.getPatientIds().size() > 1));
        f.setCenterLat(request.getCenterLat());
        f.setCenterLng(request.getCenterLng());
        return f;
    }

    /**
     * 绑定病人到围栏
     */
    private void bindPatients(long fenceId, List<Long> patientIds) {
        if (patientIds != null && !patientIds.isEmpty()) {
            fencePatientRepo.bindPatientsToFence(fenceId, patientIds);
        }
    }

    /**
     * 构建围栏DTO（含关联病人ID）
     */
    private FenceDto buildFenceDto(GeoFence fence) {
        FenceDto dto = FenceDto.fromFence(fence);
        List<Long> patientIds = new ArrayList<>();
        if (fence.getPatientId() != null) {
            patientIds.add(fence.getPatientId());
        }
        List<com.example.demo.model.FencePatient> fencePatients = fencePatientRepo.findByFenceId(fence.getId());
        for (com.example.demo.model.FencePatient fp : fencePatients) {
            if (fp.getPatient() != null && fp.getPatient().getId() != null && !patientIds.contains(fp.getPatient().getId())) {
                patientIds.add(fp.getPatient().getId());
            }
        }
        dto.setPatientIds(patientIds);
        return dto;
    }
}
