package com.example.demo.service;

import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceStatusService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusService.class);

    private final DeviceStatusRepository statusRepo;
    private final CacheService cacheService;

    public DeviceStatusService(DeviceStatusRepository statusRepo, CacheService cacheService) {
        this.statusRepo = statusRepo;
        this.cacheService = cacheService;
    }

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    public Optional<DeviceStatus> getDeviceStatus(String deviceId) {
        // 先从缓存获取
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, deviceId);
        DeviceStatus cachedStatus = cacheService.get(cacheKey, DeviceStatus.class);
        if (cachedStatus != null) {
            log.debug("Got device status from cache: {}", deviceId);
            return Optional.of(cachedStatus);
        }

        // 缓存未命中，从数据库获取
        try {
            long deviceIdLong = Long.parseLong(deviceId);
            Optional<DeviceStatus> statusOpt = statusRepo.findById(deviceIdLong);
            if (statusOpt.isPresent()) {
                // 缓存设备状态，过期时间30分钟
                cacheService.set(cacheKey, statusOpt.get(), 30, TimeUnit.MINUTES);
                log.debug("Cached device status: {}", deviceId);
            }

            return statusOpt;
        } catch (NumberFormatException e) {
            log.warn("Invalid device ID format: {}", deviceId);
            return Optional.empty();
        }
    }

    /**
     * 获取所有设备状态
     * @return 设备状态列表
     */
    public List<DeviceStatus> getAllDeviceStatus() {
        // 缓存键
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "all");
        List<DeviceStatus> cachedStatusList = cacheService.get(cacheKey, List.class);
        if (cachedStatusList != null) {
            log.debug("Got all device status from cache");
            return cachedStatusList;
        }

        // 缓存未命中，从数据库获取
        List<DeviceStatus> statusList = statusRepo.findAll();
        // 缓存设备状态列表，过期时间5分钟
        cacheService.set(cacheKey, statusList, 5, TimeUnit.MINUTES);
        log.debug("Cached all device status");

        return statusList;
    }

    /**
     * 清除设备状态缓存
     * @param deviceId 设备ID
     */
    public void clearDeviceStatusCache(String deviceId) {
        String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, deviceId);
        cacheService.delete(cacheKey);
        // 同时清除所有设备状态缓存
        String allCacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "all");
        cacheService.delete(allCacheKey);
        log.debug("Cleared device status cache: {}", deviceId);
    }

    /**
     * 清除所有设备状态缓存
     */
    public void clearAllDeviceStatusCache() {
        String allCacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "all");
        cacheService.delete(allCacheKey);
        // 清除所有设备状态缓存
        if (cacheService instanceof RedisCacheService) {
            ((RedisCacheService) cacheService).deleteByPattern(RedisCacheService.DEVICE_STATUS_PREFIX + "*");
        }
        log.debug("Cleared all device status cache");
    }

    /**
     * 预热设备状态缓存
     * @param deviceId 设备ID
     */
    public void preheatDeviceStatusCache(String deviceId) {
        try {
            long deviceIdLong = Long.parseLong(deviceId);
            Optional<DeviceStatus> statusOpt = statusRepo.findById(deviceIdLong);
            if (statusOpt.isPresent()) {
                String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, deviceId);
                cacheService.set(cacheKey, statusOpt.get(), 30, TimeUnit.MINUTES);
                log.debug("Preheated device status cache: {}", deviceId);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid device ID format: {}", deviceId);
        }
    }

    /**
     * 预热所有设备状态缓存
     */
    public void preheatAllDeviceStatusCache() {
        List<DeviceStatus> statusList = statusRepo.findAll();
        statusList.forEach(status -> {
            String cacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, String.valueOf(status.getDeviceId()));
            cacheService.set(cacheKey, status, 30, TimeUnit.MINUTES);
        });
        // 缓存所有设备状态列表
        String allCacheKey = RedisCacheService.getKey(RedisCacheService.DEVICE_STATUS_PREFIX, "all");
        cacheService.set(allCacheKey, statusList, 5, TimeUnit.MINUTES);
        log.debug("Preheated all device status cache");
    }
}