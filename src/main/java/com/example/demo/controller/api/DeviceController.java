package com.example.demo.controller.api;

import com.example.demo.model.ApiResponse;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRepository repo;
    private final DeviceStatusRepository statusRepo;

    public DeviceController(DeviceRepository repo, DeviceStatusRepository statusRepo) {
        this.repo = repo;
        this.statusRepo = statusRepo;
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
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Optional<Device> deviceOpt = repo.findById(id);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Device device = deviceOpt.get();
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
        
        // 获取设备在线状态
        Optional<DeviceStatus> status = statusRepo.findById(device.getId());
        deviceInfo.put("isOnline", status.map(DeviceStatus::getIsOnline).orElse(false));
        deviceInfo.put("lastLocationTime", status.map(DeviceStatus::getLastLocationTime).orElse(null));
        deviceInfo.put("lastLatitude", status.map(DeviceStatus::getLastLatitude).orElse(null));
        deviceInfo.put("lastLongitude", status.map(DeviceStatus::getLastLongitude).orElse(null));
        deviceInfo.put("batteryLevel", status.map(DeviceStatus::getBatteryLevel).orElse(null));
        
        return ResponseEntity.ok(deviceInfo);
    }

    @GetMapping("/by-imei/{imei}")
    public ResponseEntity<?> getByImei(@PathVariable("imei") String imei) {
        Optional<Device> deviceOpt = repo.findByImei(imei);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Device device = deviceOpt.get();
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
        
        // 获取设备在线状态
        Optional<DeviceStatus> status = statusRepo.findById(device.getId());
        deviceInfo.put("isOnline", status.map(DeviceStatus::getIsOnline).orElse(false));
        deviceInfo.put("lastLocationTime", status.map(DeviceStatus::getLastLocationTime).orElse(null));
        deviceInfo.put("lastLatitude", status.map(DeviceStatus::getLastLatitude).orElse(null));
        deviceInfo.put("lastLongitude", status.map(DeviceStatus::getLastLongitude).orElse(null));
        deviceInfo.put("batteryLevel", status.map(DeviceStatus::getBatteryLevel).orElse(null));
        
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
            Iterable<Device> allIter = repo.findAll();
            List<Device> all = new ArrayList<>();
            for (Device d : allIter) {
                if (d.getImei() != null && d.getImei().contains(search) ||
                    d.getIccid() != null && d.getIccid().contains(search) ||
                    d.getImsi() != null && d.getImsi().contains(search)) {
                    all.add(d);
                }
            }
            // 手动分页
            int from = Math.min(page * size, all.size());
            int to = Math.min(from + size, all.size());
            List<Device> pageContent = all.subList(from, to);
            devicePage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, all.size());
        } else {
            // 无搜索条件，直接查询所有
            Iterable<Device> allIter = repo.findAll();
            List<Device> all = new ArrayList<>();
            for (Device d : allIter) all.add(d);
            // 手动分页
            int from = Math.min(page * size, all.size());
            int to = Math.min(from + size, all.size());
            List<Device> pageContent = all.subList(from, to);
            devicePage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, all.size());
        }
        
        // 返回包含在线状态的设备信息
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
            
            // 获取设备在线状态
            Optional<DeviceStatus> status = statusRepo.findById(device.getId());
            deviceInfo.put("isOnline", status.map(DeviceStatus::getIsOnline).orElse(false));
            deviceInfo.put("lastLocationTime", status.map(DeviceStatus::getLastLocationTime).orElse(null));
            deviceInfo.put("batteryLevel", status.map(DeviceStatus::getBatteryLevel).orElse(null));
            
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
        return ResponseEntity.noContent().build();
    }
    
    // 获取未关联设备列表
    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableDevices() {
        List<Device> devices = repo.findAvailableDevices();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Device device : devices) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("id", device.getId());
            deviceInfo.put("imei", device.getImei());
            
            // 获取设备在线状态
            Optional<DeviceStatus> status = statusRepo.findById(device.getId());
            deviceInfo.put("isOnline", status.map(DeviceStatus::getIsOnline).orElse(false));
            deviceInfo.put("batteryLevel", status.map(DeviceStatus::getBatteryLevel).orElse(null));
            
            result.add(deviceInfo);
        }
        
        return result;
    }
}
