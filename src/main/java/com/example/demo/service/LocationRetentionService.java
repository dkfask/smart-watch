package com.example.demo.service;

import com.example.demo.repository.LocationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 定期清理 LocationRecord 的组件：保留最近 N 天的数据，删除更早的记录。
 * - 在应用启动时立即执行一次清理
 * - 此外每天在凌晨 02:00 执行一次
 */
@Component
public class LocationRetentionService {
    private static final Logger log = LoggerFactory.getLogger(LocationRetentionService.class);

    private final LocationRecordRepository repo;
    private final int retentionDays;

    public LocationRetentionService(LocationRecordRepository repo,
                                    @Value("${app.location.retentionDays:30}") int retentionDays) {
        this.repo = repo;
        this.retentionDays = Math.max(0, retentionDays);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("LocationRetentionService starting up. retentionDays={}", retentionDays);
        try {
            cleanupOnce();
        } catch (Exception e) {
            log.warn("Location retention initial cleanup failed: {}", e.getMessage(), e);
        }
    }

    // 每天 02:00 执行一次（server 时区）
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledCleanup() {
        log.info("Scheduled location retention cleanup triggered");
        try {
            cleanupOnce();
        } catch (Exception e) {
            log.warn("Scheduled location retention cleanup failed: {}", e.getMessage(), e);
        }
    }

    private void cleanupOnce() {
        if (retentionDays <= 0) {
            log.info("Retention disabled (retentionDays={}), skipping cleanup", retentionDays);
            return;
        }
        Instant cutoffInst = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        Date cutoff = Date.from(cutoffInst);
        long count = repo.countByRecvTimeBefore(cutoff);
        if (count <= 0) {
            log.info("No LocationRecord older than {} days (cutoff={}) found", retentionDays, cutoff);
            return;
        }
        log.info("Deleting {} LocationRecord entries older than {} (cutoff={})", count, retentionDays, cutoff);
        repo.deleteByRecvTimeBefore(cutoff);
        log.info("Deletion complete");
    }
}

