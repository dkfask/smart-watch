package com.example.demo.controller.api;

import com.example.demo.model.ApiResponse;
import com.example.demo.model.DeviceLocation;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.service.TrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.service.AmapLocationService;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final TrackingService trackingService;
    private final DeviceLocationRepository locationRepo;
    private final AmapLocationService amapLocationService;

    public LocationController(TrackingService trackingService, DeviceLocationRepository locationRepo, AmapLocationService amapLocationService) {
        this.trackingService = trackingService;
        this.locationRepo = locationRepo;
        this.amapLocationService = amapLocationService;
    }

    public static class ReportReq {
        public Long deviceId;
        public LocalDateTime time; // 可选，默认当前时间
        public BigDecimal latitude;
        public BigDecimal longitude;
        public Integer accuracy;
        public BigDecimal altitude;
        public Integer batteryLevel;
        public String source; // gps/wifi/cell/bluetooth
        // 新增：imei 字段，客户端或设备连接时应传入设备 IMEI
        public String imei;
    }

    @PostMapping("/report")
    public ResponseEntity<?> report(@RequestBody ReportReq req) {
        DeviceLocation dl = new DeviceLocation();
        dl.setDeviceId(req.deviceId);
        dl.setTime(req.time);
        dl.setLatitude(req.latitude);
        dl.setLongitude(req.longitude);
        dl.setAccuracy(req.accuracy);
        dl.setAltitude(req.altitude);
        dl.setBatteryLevel(req.batteryLevel);
        dl.setSource(req.source);
        // 将 imei 一并保存到定位记录，便于按 imei 或 device_id 查询
        dl.setImei(req.imei);
        long id = trackingService.reportLocation(dl);
        return ResponseEntity.created(URI.create("/api/locations/" + id)).body(id);
    }

    @GetMapping("/device/{deviceId}")
    public Map<String, Object> recent(@PathVariable long deviceId,
                                       @RequestParam(defaultValue = "50") int limit,
                                       @RequestParam(defaultValue = "0") int offset) {
        List<DeviceLocation> locations = locationRepo.listRecent(deviceId, limit, offset);
        
        // 封装分页响应
        Map<String, Object> result = new HashMap<>();
        result.put("list", locations);
        result.put("total", locations.size());
        result.put("page", offset / limit);
        result.put("size", limit);
        
        return result;
    }

    @GetMapping("/device/{deviceId}/history")
    public Map<String, Object> history(@PathVariable long deviceId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                      @RequestParam(defaultValue = "50") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        List<DeviceLocation> locations = locationRepo.listByRange(deviceId, start, end, limit, offset);
        
        // 封装分页响应
        Map<String, Object> result = new HashMap<>();
        result.put("list", locations);
        result.put("total", locations.size());
        result.put("page", offset / limit);
        result.put("size", limit);
        
        return result;
    }

    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<?> getLatestLocation(@PathVariable long deviceId) {
        List<DeviceLocation> list = locationRepo.listRecent(deviceId, 1, 0);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(list.get(0));
    }

    @GetMapping("/device/{deviceId}/latest-with-address")
    public ResponseEntity<DeviceLocationDto> getLatestLocationWithAddress(@PathVariable long deviceId) {
        List<DeviceLocation> list = locationRepo.listRecent(deviceId, 1, 0);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DeviceLocation dl = list.get(0);
        if (dl.getLatitude() == null || dl.getLongitude() == null) {
            return ResponseEntity.notFound().build();
        }
        double lat = dl.getLatitude().doubleValue();
        double lng = dl.getLongitude().doubleValue();
        String address = amapLocationService.regeoAddress(lat, lng);

        DeviceLocationDto dto = new DeviceLocationDto();
        dto.setDeviceId(dl.getDeviceId());
        dto.setImei(dl.getImei());
        dto.setLatitude(lat);
        dto.setLongitude(lng);
        dto.setAddress(address);
        dto.setTime(dl.getTime());
        dto.setSource(dl.getSource());
        dto.setBatteryLevel(dl.getBatteryLevel());
        dto.setAccuracy(dl.getAccuracy());

        return ResponseEntity.ok(dto);
    }

    // 保留原有range接口，确保向后兼容
    @GetMapping("/device/{deviceId}/range")
    public List<DeviceLocation> range(@PathVariable long deviceId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                      @RequestParam(defaultValue = "200") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return locationRepo.listByRange(deviceId, start, end, limit, offset);
    }

    // 保留原有latest-with-amap接口，确保向后兼容
    @GetMapping("/device/{deviceId}/latest-with-amap")
    public ResponseEntity<DeviceLocationDto> latestWithAmap(@PathVariable long deviceId) {
        return getLatestLocationWithAddress(deviceId);
    }
    
    /**
     * 根据地址搜索位置（地理编码）
     * @param address 地址字符串
     * @return 包含经纬度的位置信息
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchLocation(@RequestParam String address) {
        Map<String, Double> location = amapLocationService.addressToLocation(address);
        if (location != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("location", location);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据关键词搜索POI（兴趣点）
     * @param keyword 搜索关键词
     * @param city 城市，可选
     * @param pageSize 每页结果数，默认20
     * @param page 当前页码，默认1
     * @return POI搜索结果列表
     */
    @GetMapping("/search/poi")
    public ResponseEntity<?> searchPoi(@RequestParam String keyword,
                                      @RequestParam(required = false) String city,
                                      @RequestParam(required = false) Integer pageSize,
                                      @RequestParam(required = false) Integer page) {
        List<Map<String, Object>> poiList = amapLocationService.searchPoi(keyword, city, pageSize, page);
        if (poiList != null) {
            return ResponseEntity.ok(poiList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
