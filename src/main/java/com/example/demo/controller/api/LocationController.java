package com.example.demo.controller.api;

import com.example.demo.model.DeviceLocation;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.service.TrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final TrackingService trackingService;
    private final DeviceLocationRepository locationRepo;

    public LocationController(TrackingService trackingService, DeviceLocationRepository locationRepo) {
        this.trackingService = trackingService;
        this.locationRepo = locationRepo;
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
        long id = trackingService.reportLocation(dl);
        return ResponseEntity.created(URI.create("/api/locations/" + id)).body(id);
    }

    @GetMapping("/device/{deviceId}")
    public List<DeviceLocation> recent(@PathVariable long deviceId,
                                       @RequestParam(defaultValue = "50") int limit,
                                       @RequestParam(defaultValue = "0") int offset) {
        return locationRepo.listRecent(deviceId, limit, offset);
    }

    @GetMapping("/device/{deviceId}/range")
    public List<DeviceLocation> range(@PathVariable long deviceId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                      @RequestParam(defaultValue = "200") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return locationRepo.listByRange(deviceId, start, end, limit, offset);
    }
}

