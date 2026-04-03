package com.example.demo.controller.api;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.Patient;
import com.example.demo.model.PatientDevice;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.service.CacheService;
import com.example.demo.service.RedisCacheService;
import com.example.demo.socket.downlink.DownlinkManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRepository repo;
    private final DeviceStatusRepository statusRepo;
    private final PatientDeviceRepository patientDeviceRepo;
    private final PatientRepository patientRepo;
    private final DownlinkManager downlinkManager;
    private final CacheService cacheService;

    public DeviceController(DeviceRepository repo, DeviceStatusRepository statusRepo, PatientDeviceRepository patientDeviceRepo, PatientRepository patientRepo, DownlinkManager downlinkManager, CacheService cacheService) {
        this.repo = repo;
        this.statusRepo = statusRepo;
        this.patientDeviceRepo = patientDeviceRepo;
        this.patientRepo = patientRepo;
        this.downlinkManager = downlinkManager;
        this.cacheService = cacheService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Device d) {
        Device saved = repo.save(d);
        Long id = saved.getId();
        return ResponseEntity.created(URI.create("/api/devices/" + id)).body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Device d) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        d.setId(id);
        repo.save(d);
        // 清除缓存
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, String.valueOf(id));
        cacheService.delete(cacheKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        // 尝试从缓存获取
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, String.valueOf(id));
        Map<String, Object> cachedDeviceInfo = cacheService.get(cacheKey, Map.class);
        if (cachedDeviceInfo != null) {
            return ResponseEntity.ok(cachedDeviceInfo);
        }

        Optional<Device> deviceOpt = repo.findById(id);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> deviceInfo = buildDeviceInfo(deviceOpt.get());
        // 缓存结果，过期时间5分钟
        cacheService.set(cacheKey, deviceInfo, 5, TimeUnit.MINUTES);
        return ResponseEntity.ok(deviceInfo);
    }

    @GetMapping("/by-imei/{imei}")
    public ResponseEntity<?> getByImei(@PathVariable("imei") String imei) {
        // 尝试从缓存获取
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, imei);
        Map<String, Object> cachedDeviceInfo = cacheService.get(cacheKey, Map.class);
        if (cachedDeviceInfo != null) {
            return ResponseEntity.ok(cachedDeviceInfo);
        }

        Optional<Device> deviceOpt = repo.findByImei(imei);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> deviceInfo = buildDeviceInfo(deviceOpt.get());
        // 缓存结果，过期时间5分钟
        cacheService.set(cacheKey, deviceInfo, 5, TimeUnit.MINUTES);
        return ResponseEntity.ok(deviceInfo);
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String search) {
        // 使用Pageable实现数据库分页
        Pageable pageable = PageRequest.of(page, size);
        Page<Device> devicePage;
        
        // 搜索过滤
        if (search != null && !search.isEmpty()) {
            // 使用JPA Query Methods实现模糊搜索
            devicePage = repo.findByImeiContainingOrIccidContainingOrImsiContaining(search, search, search, pageable);
        } else {
            // 无搜索条件，直接查询所有
            devicePage = repo.findAll(pageable);
        }
        
        // 获取所有设备ID，批量查询设备状态和关联病人信息
        List<Long> deviceIds = devicePage.getContent().stream().map(Device::getId).toList();
        Map<Long, DeviceStatus> deviceStatusMap = new HashMap<>();
        Map<Long, Patient> patientMap = new HashMap<>();
        
        // 批量查询设备状态
        List<DeviceStatus> deviceStatuses = statusRepo.findAllById(deviceIds);
        for (DeviceStatus status : deviceStatuses) {
            deviceStatusMap.put(status.getDeviceId(), status);
        }
        
        // 批量查询设备关联的病人
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceIdIn(deviceIds);
        List<Long> patientIds = patientDevices.stream().map(PatientDevice::getPatientId).distinct().toList();
        List<Patient> patients = patientRepo.findAllById(patientIds);
        for (Patient patient : patients) {
            patientMap.put(patient.getId(), patient);
        }
        
        // 构建病人设备映射
        Map<Long, Patient> devicePatientMap = new HashMap<>();
        for (PatientDevice pd : patientDevices) {
            devicePatientMap.put(pd.getDeviceId(), patientMap.get(pd.getPatientId()));
        }
        
        // 获取在线设备列表
        java.util.Set<String> onlineImeis = downlinkManager.getOnlineImeis();
        
        // 返回包含在线状态和关联病人的设备信息
        List<Map<String, Object>> deviceList = new ArrayList<>();
        for (Device device : devicePage.getContent()) {
            Map<String, Object> deviceInfo = new HashMap<>();
            // 添加设备基本信息
            deviceInfo.put("id", device.getId());
            deviceInfo.put("imei", device.getImei());
            deviceInfo.put("mcc", device.getMcc());
            deviceInfo.put("mnc", device.getMnc());
            deviceInfo.put("iccid", device.getIccid());
            deviceInfo.put("imsi", device.getImsi());
            deviceInfo.put("createdAt", device.getCreatedAt());
            deviceInfo.put("updatedAt", device.getUpdatedAt());
            
            // 获取设备在线状态：优先使用实时连接状态，其次使用数据库状态
            DeviceStatus deviceStatus = deviceStatusMap.get(device.getId());
            boolean realtimeOnline = onlineImeis.contains(device.getImei());
            deviceInfo.put("isOnline", realtimeOnline);
            deviceInfo.put("lastLocationTime", deviceStatus != null ? deviceStatus.getLastLocationTime() : null);
            deviceInfo.put("batteryLevel", deviceStatus != null ? deviceStatus.getBatteryLevel() : null);
            
            // 获取设备关联的病人信息
            Patient patient = devicePatientMap.get(device.getId());
            if (patient != null) {
                Map<String, Object> patientInfo = new HashMap<>();
                patientInfo.put("id", patient.getId());
                patientInfo.put("name", patient.getName());
                patientInfo.put("gender", patient.getGender());
                patientInfo.put("age", patient.getAge());
                patientInfo.put("ward", patient.getWard());
                patientInfo.put("bed", patient.getBed());
                patientInfo.put("phone", patient.getPhone());
                deviceInfo.put("patient", patientInfo);
            }
            
            deviceList.add(deviceInfo);
        }
        
        // 封装分页响应
        Map<String, Object> result = new HashMap<>();
        result.put("list", deviceList);
        result.put("total", devicePage.getTotalElements());
        result.put("page", devicePage.getNumber());
        result.put("size", devicePage.getSize());
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        // 清除缓存
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, String.valueOf(id));
        cacheService.delete(cacheKey);
        return ResponseEntity.noContent().build();
    }
    
    // 获取未关联设备列表
    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableDevices() {
        // 尝试从缓存获取
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "available");
        List<Map<String, Object>> cachedResult = cacheService.get(cacheKey, List.class);
        if (cachedResult != null) {
            return cachedResult;
        }

        List<Device> devices = repo.findAvailableDevices();
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有设备ID，批量查询设备状态
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Long, DeviceStatus> deviceStatusMap = new HashMap<>();
        List<DeviceStatus> deviceStatuses = statusRepo.findAllById(deviceIds);
        for (DeviceStatus status : deviceStatuses) {
            deviceStatusMap.put(status.getDeviceId(), status);
        }
        
        // 获取在线设备列表
        java.util.Set<String> onlineImeis = downlinkManager.getOnlineImeis();
        
        for (Device device : devices) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("id", device.getId());
            deviceInfo.put("imei", device.getImei());
            
            // 获取设备在线状态：优先使用实时连接状态，其次使用数据库状态
            DeviceStatus deviceStatus = deviceStatusMap.get(device.getId());
            boolean realtimeOnline = onlineImeis.contains(device.getImei());
            deviceInfo.put("isOnline", realtimeOnline);
            deviceInfo.put("batteryLevel", deviceStatus != null ? deviceStatus.getBatteryLevel() : null);
            
            result.add(deviceInfo);
        }
        
        // 缓存结果，过期时间2分钟
        cacheService.set(cacheKey, result, 2, TimeUnit.MINUTES);
        return result;
    }
    
    /**
     * 构建设备信息
     * @param device 设备对象
     * @return 设备信息Map
     */
    private Map<String, Object> buildDeviceInfo(Device device) {
        Map<String, Object> deviceInfo = new HashMap<>();
        // 添加设备基本信息
        deviceInfo.put("id", device.getId());
        deviceInfo.put("imei", device.getImei());
        deviceInfo.put("mcc", device.getMcc());
        deviceInfo.put("mnc", device.getMnc());
        deviceInfo.put("apn", device.getApn());
        deviceInfo.put("iccid", device.getIccid());
        deviceInfo.put("imsi", device.getImsi());
        deviceInfo.put("createdAt", device.getCreatedAt());
        deviceInfo.put("updatedAt", device.getUpdatedAt());
        
        // 获取设备在线状态：优先使用实时连接状态，其次使用数据库状态
        Optional<DeviceStatus> status = statusRepo.findById(device.getId());
        // 检查DownlinkManager中的实时连接状态
        boolean realtimeOnline = downlinkManager.getOnlineImeis().contains(device.getImei());
        deviceInfo.put("isOnline", realtimeOnline);
        deviceInfo.put("lastLocationTime", status.map(DeviceStatus::getLastLocationTime).orElse(null));
        deviceInfo.put("lastLatitude", status.map(DeviceStatus::getLastLatitude).orElse(null));
        deviceInfo.put("lastLongitude", status.map(DeviceStatus::getLastLongitude).orElse(null));
        deviceInfo.put("batteryLevel", status.map(DeviceStatus::getBatteryLevel).orElse(null));
        
        // 获取设备关联的病人信息
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(device.getId());
        if (!patientDevices.isEmpty()) {
            // 取第一个关联的病人（一对一关系）
            PatientDevice patientDevice = patientDevices.get(0);
            Optional<Patient> patientOpt = patientRepo.findById(patientDevice.getPatientId());
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                Map<String, Object> patientInfo = new HashMap<>();
                patientInfo.put("id", patient.getId());
                patientInfo.put("name", patient.getName());
                patientInfo.put("gender", patient.getGender());
                patientInfo.put("age", patient.getAge());
                patientInfo.put("ward", patient.getWard());
                patientInfo.put("bed", patient.getBed());
                patientInfo.put("phone", patient.getPhone());
                deviceInfo.put("patient", patientInfo);
            }
        }
        
        return deviceInfo;
    }
}
