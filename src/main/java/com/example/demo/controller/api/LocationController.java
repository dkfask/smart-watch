package com.example.demo.controller.api;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.service.AmapLocationService;
import com.example.demo.service.TrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        public LocalDateTime time;
        public BigDecimal latitude;
        public BigDecimal longitude;
        public Integer accuracy;
        public BigDecimal altitude;
        public Integer batteryLevel;
        public String source;
        public String imei;
    }

    /**
     * 上报设备位置
     */
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
        dl.setImei(req.imei);
        long id = trackingService.reportLocation(dl);
        return ResponseEntity.created(URI.create("/api/locations/" + id)).body(id);
    }

    /**
     * 获取设备最近位置记录（分页）
     */
    @GetMapping("/device/{deviceId}")
    public PageResponse<DeviceLocation> recent(@PathVariable long deviceId,
                                                @RequestParam(defaultValue = "50") int limit,
                                                @RequestParam(defaultValue = "0") int offset) {
        List<DeviceLocation> locations = locationRepo.listRecent(deviceId, limit, offset);
        long total = locationRepo.countByDeviceId(deviceId);
        return new PageResponse<>(locations, total, offset / Math.max(limit, 1), limit);
    }

    /**
     * 获取设备历史位置记录（分页）
     */
    @GetMapping("/device/{deviceId}/history")
    public PageResponse<DeviceLocation> history(@PathVariable long deviceId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                                 @RequestParam(defaultValue = "50") int limit,
                                                 @RequestParam(defaultValue = "0") int offset) {
        List<DeviceLocation> locations = locationRepo.listByRange(deviceId, start, end, limit, offset);
        long total = locationRepo.countByDeviceIdAndRange(deviceId, start, end);
        return new PageResponse<>(locations, total, offset / Math.max(limit, 1), limit);
    }

    /**
     * 获取设备最新位置
     */
    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<?> getLatestLocation(@PathVariable long deviceId) {
        List<DeviceLocation> list = locationRepo.listRecent(deviceId, 1, 0);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(list.get(0));
    }

    /**
     * 获取设备最新位置（含地址）
     */
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
        String address = dl.getAddress();
        if (address == null || address.isBlank()) {
            address = amapLocationService.regeoAddress(lat, lng);
        }

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

    /**
     * 获取设备位置范围（兼容旧接口）
     */
    @GetMapping("/device/{deviceId}/range")
    public List<DeviceLocation> range(@PathVariable long deviceId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                      @RequestParam(defaultValue = "200") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return locationRepo.listByRange(deviceId, start, end, limit, offset);
    }

    /**
     * 获取设备最新位置含高德地址（兼容旧接口）
     */
    @GetMapping("/device/{deviceId}/latest-with-amap")
    public ResponseEntity<DeviceLocationDto> latestWithAmap(@PathVariable long deviceId) {
        return getLatestLocationWithAddress(deviceId);
    }

    /**
     * 根据地址搜索位置（地理编码）
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchLocation(@RequestParam String address) {
        Map<String, Double> location = amapLocationService.addressToLocation(address);
        if (location != null) {
            Map<String, Object> result = Map.of("address", address, "location", location);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据关键词搜索POI
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
