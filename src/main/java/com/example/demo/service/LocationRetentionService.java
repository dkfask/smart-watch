package com.example.demo.service;

import com.example.demo.repository.LocationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 定期清理 LocationRecord 的组件：保留最近 N 天的数据，删除更早的记录。
 * - 优先使用 DROP PARTITION 方式清理过期数据（高效，直接删除整个分区）
 * - 如果分区删除失败，回退到原有的 DELETE 逻辑作为 fallback
 * - 在应用启动时立即执行一次清理
 * - 此外每天在凌晨 02:00 执行一次
 */
@Component
public class LocationRetentionService {
    private static final Logger log = LoggerFactory.getLogger(LocationRetentionService.class);

    private final LocationRecordRepository repo;
    private final JdbcTemplate jdbcTemplate;
    private final int retentionDays;

    public LocationRetentionService(LocationRecordRepository repo,
                                    JdbcTemplate jdbcTemplate,
                                    @Value("${app.location.retentionDays:30}") int retentionDays) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
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

    /**
     * 执行一次数据清理：优先使用 DROP PARTITION，失败则回退到 DELETE
     */
    private void cleanupOnce() {
        if (retentionDays <= 0) {
            log.info("Retention disabled (retentionDays={}), skipping cleanup", retentionDays);
            return;
        }
        Instant cutoffInst = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        Date cutoff = Date.from(cutoffInst);

        boolean partitionDropped = tryDropPartitionsBefore(cutoff);
        if (!partitionDropped) {
            fallbackDelete(cutoff);
        }
    }

    /**
     * 尝试使用 DROP PARTITION 删除 cutoff 之前的所有月份分区
     * @param cutoff 保留截止时间，早于此时间的数据需要清理
     * @return true 如果至少成功删除了一个分区，false 如果分区删除失败或无分区可删
     */
    private boolean tryDropPartitionsBefore(Date cutoff) {
        LocalDate cutoffDate = cutoff.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate partitionMonth = cutoffDate.withDayOfMonth(1);

        boolean anyDropped = false;

        while (partitionMonth.isBefore(currentMonth)) {
            String partitionName = "p" + partitionMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            try {
                log.info("Attempting to DROP PARTITION {} on location_records", partitionName);
                jdbcTemplate.execute("ALTER TABLE location_records DROP PARTITION " + partitionName);
                log.info("Successfully dropped partition {} on location_records", partitionName);
                anyDropped = true;
            } catch (Exception e) {
                log.warn("Failed to drop partition {} on location_records: {}", partitionName, e.getMessage());
            }
            partitionMonth = partitionMonth.plusMonths(1);
        }

        return anyDropped;
    }

    /**
     * 回退到原有的 DELETE 逻辑清理过期数据
     * @param cutoff 保留截止时间，早于此时间的记录将被删除
     */
    private void fallbackDelete(Date cutoff) {
        long count = repo.countByRecvTimeBefore(cutoff);
        if (count <= 0) {
            log.info("No LocationRecord older than cutoff={} found (fallback)", cutoff);
            return;
        }
        log.info("Fallback DELETE: deleting {} LocationRecord entries older than {}", count, cutoff);
        repo.deleteByRecvTimeBefore(cutoff);
        log.info("Fallback deletion complete");
    }
}

