package com.example.demo.service;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.GeoFence;
import com.example.demo.model.UserDevice;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final UserDeviceRepository userDeviceRepo;

    public TrackingService(DeviceLocationRepository locationRepo,
                           DeviceStatusRepository statusRepo,
                           GeoFenceRepository fenceRepo,
                           FenceAlertRepository alertRepo,
                           UserDeviceRepository userDeviceRepo) {
        this.locationRepo = locationRepo;
        this.statusRepo = statusRepo;
        this.fenceRepo = fenceRepo;
        this.alertRepo = alertRepo;
        this.userDeviceRepo = userDeviceRepo;
    }

    public long reportLocation(DeviceLocation dl) {
        // 1) 写入历史位置
        long id = locationRepo.insert(dl);

        // 2) 更新实时状态
        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(dl.getDeviceId());
        s.setBatteryLevel(dl.getBatteryLevel());
        s.setIsOnline(true);
        // 同步写入 imei 到 DeviceStatus，便于在设备状态接口回显
        s.setImei(dl.getImei());
        statusRepo.upsert(s);

        // 3) 围栏判断：仅当有上一点坐标时再判断进出
        statusRepo.findById(dl.getDeviceId()).ifPresent(prev -> {
            if (prev.getLastLatitude() != null && prev.getLastLongitude() != null) {
                checkFencesAndAlert(dl, prev.getLastLatitude(), prev.getLastLongitude());
            }
        });
        return id;
    }

    private void checkFencesAndAlert(DeviceLocation curr, Double prevLat, Double prevLng) {
        // 查询设备所有者的围栏集合
        List<UserDevice> rels = userDeviceRepo.findByDevice(curr.getDeviceId());
        List<Long> ownerUserIds = rels.stream()
                .filter(r -> "owner".equalsIgnoreCase(r.getRelationship()))
                .map(UserDevice::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (ownerUserIds.isEmpty()) return;

        for (Long uid : ownerUserIds) {
            List<GeoFence> fences = fenceRepo.listActiveByUser(uid);
            for (GeoFence f : fences) {
                // 只处理圆形围栏
                if (!"circle".equalsIgnoreCase(f.getType())) continue;
                
                boolean wasInside = inside(prevLat, prevLng, f);
                boolean nowInside = inside(curr.getLatitude(), curr.getLongitude(), f);
                if (wasInside == nowInside) continue; // 未发生状态变化
                
                // 简化处理：默认支持所有围栏类型的进出告警
                String alertType = nowInside ? "enter" : "exit";
                alertRepo.create(f.getId(), curr.getDeviceId(), alertType);
                log.info("Fence {} alert device={} fence={}", alertType, curr.getDeviceId(), f.getId());
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
