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
        
        // 缓存设备状态，过期时间30分钟
        String cacheKey = "device_status_" + dl.getDeviceId();
        cacheService.set(cacheKey, s, 30, TimeUnit.MINUTES);
        log.debug("Cached device status: {}", cacheKey);

        // 5) 围栏判断：
        //   - 如果有上一点坐标，判断进出状态变化
        //   - 如果没有上一点坐标，判断当前是否在围栏外，若是则触发报警
        if (currLat != null && currLng != null) {
            fenceService.checkFencesAndAlert(dl, prevLat, prevLng);
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
        
        // 缓存设备状态，过期时间30分钟
        String cacheKey = "device_status_" + locationRecord.getDevice().getId();
        cacheService.set(cacheKey, s, 30, TimeUnit.MINUTES);
        log.debug("Cached device status: {}", cacheKey);
        
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
            
            fenceService.checkFencesAndAlert(dl, prevLat, prevLng);
        }
    }
}
