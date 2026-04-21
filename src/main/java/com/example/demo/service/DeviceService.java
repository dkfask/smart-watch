package com.example.demo.service;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.Patient;
import com.example.demo.model.PatientDevice;
import com.example.demo.model.dto.DeviceInfoDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.socket.downlink.DownlinkManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepo;
    private final DeviceStatusRepository statusRepo;
    private final PatientDeviceRepository patientDeviceRepo;
    private final PatientRepository patientRepo;
    private final DownlinkManager downlinkManager;
    private final CacheService cacheService;

    public DeviceService(DeviceRepository deviceRepo,
                         DeviceStatusRepository statusRepo,
                         PatientDeviceRepository patientDeviceRepo,
                         PatientRepository patientRepo,
                         DownlinkManager downlinkManager,
                         CacheService cacheService) {
        this.deviceRepo = deviceRepo;
        this.statusRepo = statusRepo;
        this.patientDeviceRepo = patientDeviceRepo;
        this.patientRepo = patientRepo;
        this.downlinkManager = downlinkManager;
        this.cacheService = cacheService;
    }

    /**
     * 创建设备
     * @param device 设备对象
     * @return 保存后的设备ID
     */
    public Long createDevice(Device device) {
        Device saved = deviceRepo.save(device);
        return saved.getId();
    }

    /**
     * 检查设备是否存在
     * @param id 设备ID
     * @return 是否存在
     */
    public boolean existsById(long id) {
        return deviceRepo.existsById(id);
    }

    /**
     * 更新设备
     * @param id 设备ID
     * @param device 设备对象
     */
    public void updateDevice(long id, Device device) {
        device.setId(id);
        deviceRepo.save(device);
        clearDeviceCache(String.valueOf(id));
    }

    /**
     * 根据ID获取设备详情（含缓存）
     * @param id 设备ID
     * @return 设备信息DTO
     */
    public DeviceInfoDto getDeviceById(long id) {
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, String.valueOf(id));
        return cacheService.getOrSet(cacheKey, DeviceInfoDto.class, () -> {
            Optional<Device> deviceOpt = deviceRepo.findById(id);
            return deviceOpt.map(this::buildDeviceInfo).orElse(null);
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * 根据IMEI获取设备详情（含缓存）
     * @param imei 设备IMEI
     * @return 设备信息DTO
     */
    public DeviceInfoDto getDeviceByImei(String imei) {
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, imei);
        return cacheService.getOrSet(cacheKey, DeviceInfoDto.class, () -> {
            Optional<Device> deviceOpt = deviceRepo.findByImei(imei);
            return deviceOpt.map(this::buildDeviceInfo).orElse(null);
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * 分页查询设备列表
     * @param page 页码
     * @param size 每页大小
     * @param search 搜索关键字
     * @return 分页响应
     */
    public PageResponse<DeviceInfoDto> listDevices(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Device> devicePage;

        if (search != null && !search.isEmpty()) {
            devicePage = deviceRepo.findByImeiContainingOrIccidContainingOrImsiContaining(search, search, search, pageable);
        } else {
            devicePage = deviceRepo.findAll(pageable);
        }

        List<Long> deviceIds = devicePage.getContent().stream().map(Device::getId).toList();
        Map<Long, DeviceStatus> statusMap = batchLoadDeviceStatus(deviceIds);
        Map<Long, Patient> devicePatientMap = batchLoadDevicePatientMap(deviceIds);
        java.util.Set<String> onlineImeis = downlinkManager.getOnlineImeis();

        List<DeviceInfoDto> deviceList = devicePage.getContent().stream().map(device -> {
            DeviceInfoDto dto = DeviceInfoDto.fromDevice(device);
            DeviceStatus status = statusMap.get(device.getId());
            boolean realtimeOnline = onlineImeis.contains(device.getImei());
            dto.fillStatus(status, realtimeOnline);
            Patient patient = devicePatientMap.get(device.getId());
            dto.fillPatient(patient);
            return dto;
        }).toList();

        return new PageResponse<>(deviceList, devicePage.getTotalElements(), devicePage.getNumber(), devicePage.getSize());
    }

    /**
     * 删除设备
     * @param id 设备ID
     */
    public void deleteDevice(long id) {
        deviceRepo.deleteById(id);
        clearDeviceCache(String.valueOf(id));
    }

    /**
     * 获取未关联设备列表（含缓存）
     * @return 设备信息DTO列表
     */
    public List<DeviceInfoDto> getAvailableDevices() {
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "available");
        List<DeviceInfoDto> cached = cacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        List<Device> devices = deviceRepo.findAvailableDevices();
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Long, DeviceStatus> statusMap = batchLoadDeviceStatus(deviceIds);
        java.util.Set<String> onlineImeis = downlinkManager.getOnlineImeis();

        List<DeviceInfoDto> result = devices.stream().map(device -> {
            DeviceInfoDto dto = DeviceInfoDto.fromDevice(device);
            DeviceStatus status = statusMap.get(device.getId());
            boolean realtimeOnline = onlineImeis.contains(device.getImei());
            dto.fillStatus(status, realtimeOnline);
            return dto;
        }).toList();

        cacheService.set(cacheKey, result, 2, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 构建单个设备的完整信息DTO
     * @param device 设备实体
     * @return 设备信息DTO
     */
    private DeviceInfoDto buildDeviceInfo(Device device) {
        DeviceInfoDto dto = DeviceInfoDto.fromDevice(device);
        Optional<DeviceStatus> status = statusRepo.findById(device.getId());
        boolean realtimeOnline = downlinkManager.getOnlineImeis().contains(device.getImei());
        dto.fillStatus(status.orElse(null), realtimeOnline);

        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(device.getId());
        if (!patientDevices.isEmpty()) {
            Long patientId = patientDevices.get(0).getPatientId();
            patientRepo.findById(patientId).ifPresent(dto::fillPatient);
        }
        return dto;
    }

    /**
     * 批量加载设备状态
     * @param deviceIds 设备ID列表
     * @return 设备ID到状态的映射
     */
    private Map<Long, DeviceStatus> batchLoadDeviceStatus(List<Long> deviceIds) {
        Map<Long, DeviceStatus> map = new HashMap<>();
        List<DeviceStatus> statuses = statusRepo.findAllById(deviceIds);
        for (DeviceStatus status : statuses) {
            map.put(status.getDeviceId(), status);
        }
        return map;
    }

    /**
     * 批量加载设备关联病人映射
     * @param deviceIds 设备ID列表
     * @return 设备ID到病人的映射
     */
    private Map<Long, Patient> batchLoadDevicePatientMap(List<Long> deviceIds) {
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceIdIn(deviceIds);
        List<Long> patientIds = patientDevices.stream().map(PatientDevice::getPatientId).distinct().toList();
        List<Patient> patients = patientRepo.findAllById(patientIds);

        Map<Long, Patient> patientMap = new HashMap<>();
        for (Patient patient : patients) {
            patientMap.put(patient.getId(), patient);
        }

        Map<Long, Patient> devicePatientMap = new HashMap<>();
        for (PatientDevice pd : patientDevices) {
            devicePatientMap.put(pd.getDeviceId(), patientMap.get(pd.getPatientId()));
        }
        return devicePatientMap;
    }

    /**
     * 清除设备缓存
     * @param deviceId 设备ID
     */
    private void clearDeviceCache(String deviceId) {
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, deviceId);
        cacheService.delete(cacheKey);
    }
}
