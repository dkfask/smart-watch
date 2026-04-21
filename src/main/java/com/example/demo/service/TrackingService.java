package com.example.demo.service;

import com.example.demo.model.DeviceLocation;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.LocationRecord;
import com.example.demo.repository.DeviceLocationRepository;
import com.example.demo.repository.DeviceStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TrackingService {
    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final DeviceLocationRepository locationRepo;
    private final DeviceStatusRepository statusRepo;
    private final FenceService fenceService;
    private final CacheService cacheService;

    public TrackingService(DeviceLocationRepository locationRepo,
                           DeviceStatusRepository statusRepo,
                           FenceService fenceService,
                           CacheService cacheService) {
        this.locationRepo = locationRepo;
        this.statusRepo = statusRepo;
        this.fenceService = fenceService;
        this.cacheService = cacheService;
    }

    /**
     * 报告设备位置
     * @param dl 设备位置信息
     * @return 位置记录ID
     */
    public long reportLocation(DeviceLocation dl) {
        long id = locationRepo.insert(dl);

        Double currLat = dl.getLatitude() != null ? dl.getLatitude().doubleValue() : null;
        Double currLng = dl.getLongitude() != null ? dl.getLongitude().doubleValue() : null;

        Double[] prevLocation = getPreviousLocation(dl.getDeviceId());
        updateDeviceStatusAndCheckFence(
                dl.getDeviceId(), currLat, currLng, dl.getImei(),
                dl.getBatteryLevel(), dl.getTime(), prevLocation[0], prevLocation[1]);

        return id;
    }

    /**
     * 处理LocationRecord，用于围栏判断
     * @param locationRecord 位置记录
     */
    public void processLocationRecord(LocationRecord locationRecord) {
        Double currLat = locationRecord.getLatitude();
        Double currLng = locationRecord.getLongitude();

        Double[] prevLocation = getPreviousLocation(locationRecord.getDevice().getId());
        updateDeviceStatusAndCheckFence(
                locationRecord.getDevice().getId(), currLat, currLng, locationRecord.getImei(),
                null, null, prevLocation[0], prevLocation[1]);
    }

    /**
     * 获取设备上一次的位置信息
     * @param deviceId 设备ID
     * @return [prevLat, prevLng]
     */
    private Double[] getPreviousLocation(Long deviceId) {
        Optional<DeviceStatus> prevStatusOpt = statusRepo.findById(deviceId);
        if (prevStatusOpt.isPresent()) {
            DeviceStatus prevStatus = prevStatusOpt.get();
            return new Double[]{prevStatus.getLastLatitude(), prevStatus.getLastLongitude()};
        }
        return new Double[]{null, null};
    }

    /**
     * 更新设备状态并检查围栏（公共逻辑提取）
     * @param deviceId 设备ID
     * @param currLat 当前纬度
     * @param currLng 当前经度
     * @param imei 设备IMEI
     * @param batteryLevel 电池电量
     * @param locationTime 定位时间
     * @param prevLat 上一次纬度
     * @param prevLng 上一次经度
     */
    private void updateDeviceStatusAndCheckFence(Long deviceId, Double currLat, Double currLng,
                                                  String imei, Integer batteryLevel,
                                                  java.time.LocalDateTime locationTime,
                                                  Double prevLat, Double prevLng) {
        log.debug("Device: {} previous location - lat: {}, lng: {}", deviceId, prevLat, prevLng);

        DeviceStatus s = new DeviceStatus();
        s.setDeviceId(deviceId);
        s.setBatteryLevel(batteryLevel);
        s.setIsOnline(true);
        if (currLat != null) s.setLastLatitude(currLat);
        if (currLng != null) s.setLastLongitude(currLng);
        s.setLastLocationTime(locationTime != null
                ? new Date(locationTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                : new Date());
        s.setImei(imei);
        statusRepo.upsert(s);

        String cacheKey = "device_status_" + deviceId;
        cacheService.set(cacheKey, s, 30, TimeUnit.MINUTES);

        if (currLat != null && currLng != null) {
            DeviceLocation dl = new DeviceLocation();
            dl.setDeviceId(deviceId);
            dl.setLatitude(BigDecimal.valueOf(currLat));
            dl.setLongitude(BigDecimal.valueOf(currLng));
            dl.setImei(imei);
            fenceService.checkFencesAndAlert(dl, prevLat, prevLng);
        }
    }
}
