package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分区维护定时任务组件：每月1号凌晨2点自动执行分区管理。
 * - 为下个月创建新分区（REORGANIZE PARTITION p_future）
 * - 删除过期分区（DROP PARTITION）
 * - 不同表有不同的数据保留期限
 */
@Component
public class PartitionMaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenanceService.class);
    private static final DateTimeFormatter PARTITION_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-01");

    private final JdbcTemplate jdbcTemplate;

    /**
     * 分区表配置：表名、分区列、保留月数
     */
    private static final List<PartitionTableConfig> TABLE_CONFIGS = List.of(
            new PartitionTableConfig("location_records", "recv_time", 3),
            new PartitionTableConfig("heartbeat_records", "recv_time", 3),
            new PartitionTableConfig("fence_alerts", "alert_time", 6),
            new PartitionTableConfig("system_log", "created_at", 12)
    );

    public PartitionMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 每月1号凌晨2点执行分区维护任务
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void monthlyPartitionMaintenance() {
        log.info("===== 开始执行分区维护任务 =====");
        for (PartitionTableConfig config : TABLE_CONFIGS) {
            try {
                createNextMonthPartition(config);
                dropExpiredPartitions(config);
            } catch (Exception e) {
                log.error("分区维护失败，表: {}, 错误: {}", config.tableName, e.getMessage(), e);
            }
        }
        log.info("===== 分区维护任务执行完毕 =====");
    }

    /**
     * 为指定表创建下个月的分区
     * 使用 REORGANIZE PARTITION 将 p_future 拆分为新月份分区 + 新的 p_future
     * @param config 分区表配置
     */
    private void createNextMonthPartition(PartitionTableConfig config) {
        LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        String partitionName = "p" + nextMonth.format(PARTITION_FMT);
        String lessThanValue = nextMonth.plusMonths(1).format(DATE_FMT);

        String sql = String.format(
                "ALTER TABLE %s REORGANIZE PARTITION p_future INTO " +
                        "(PARTITION %s VALUES LESS THAN ('%s'), PARTITION p_future VALUES LESS THAN MAXVALUE)",
                config.tableName, partitionName, lessThanValue
        );

        try {
            log.info("为表 {} 创建下月分区: {}, VALUES LESS THAN ('{}')", config.tableName, partitionName, lessThanValue);
            jdbcTemplate.execute(sql);
            log.info("表 {} 分区 {} 创建成功", config.tableName, partitionName);
        } catch (Exception e) {
            log.warn("为表 {} 创建分区 {} 失败（可能已存在）: {}", config.tableName, partitionName, e.getMessage());
        }
    }

    /**
     * 删除指定表的过期分区
     * 根据保留月数计算过期分区的截止月份，删除所有早于该月份的分区
     * @param config 分区表配置
     */
    private void dropExpiredPartitions(PartitionTableConfig config) {
        LocalDate expiryMonth = LocalDate.now().minusMonths(config.retentionMonths).withDayOfMonth(1);
        List<String> partitionsToDrop = getExpiredPartitionNames(config.tableName, expiryMonth);

        if (partitionsToDrop.isEmpty()) {
            log.info("表 {} 无过期分区需要删除", config.tableName);
            return;
        }

        for (String partitionName : partitionsToDrop) {
            try {
                log.info("删除表 {} 的过期分区: {}", config.tableName, partitionName);
                jdbcTemplate.execute("ALTER TABLE " + config.tableName + " DROP PARTITION " + partitionName);
                log.info("表 {} 分区 {} 删除成功", config.tableName, partitionName);
            } catch (Exception e) {
                log.warn("删除表 {} 分区 {} 失败: {}", config.tableName, partitionName, e.getMessage());
            }
        }
    }

    /**
     * 查询指定表中所有早于 expiryMonth 的分区名称
     * 通过查询 information_schema.PARTITIONS 获取分区列表，筛选出格式为 pYYYYMM 且月份早于 expiryMonth 的分区
     * @param tableName 表名
     * @param expiryMonth 过期截止月份，早于此月份的分区视为过期
     * @return 需要删除的过期分区名称列表
     */
    private List<String> getExpiredPartitionNames(String tableName, LocalDate expiryMonth) {
        List<String> expired = new ArrayList<>();
        try {
            String sql = "SELECT PARTITION_NAME FROM information_schema.PARTITIONS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND PARTITION_NAME IS NOT NULL " +
                    "AND PARTITION_NAME != 'p_future'";
            jdbcTemplate.query(sql, rs -> {
                String partitionName = rs.getString("PARTITION_NAME");
                if (partitionName != null && isPartitionExpired(partitionName, expiryMonth)) {
                    expired.add(partitionName);
                }
            }, tableName);
        } catch (Exception e) {
            log.warn("查询表 {} 的分区信息失败: {}", tableName, e.getMessage());
        }
        return expired;
    }

    /**
     * 判断分区名称对应的月份是否早于过期截止月份
     * 分区名格式为 pYYYYMM（如 p202604），解析后与 expiryMonth 比较
     * @param partitionName 分区名称
     * @param expiryMonth 过期截止月份
     * @return true 如果该分区已过期
     */
    private boolean isPartitionExpired(String partitionName, LocalDate expiryMonth) {
        if (!partitionName.startsWith("p") || partitionName.length() != 7) {
            return false;
        }
        try {
            String datePart = partitionName.substring(1);
            LocalDate partitionDate = LocalDate.parse(datePart + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
            return partitionDate.isBefore(expiryMonth);
        } catch (Exception e) {
            log.warn("无法解析分区名: {}", partitionName);
            return false;
        }
    }

    /**
     * 分区表配置内部类
     */
    private static class PartitionTableConfig {
        final String tableName;
        final String partitionColumn;
        final int retentionMonths;

        PartitionTableConfig(String tableName, String partitionColumn, int retentionMonths) {
            this.tableName = tableName;
            this.partitionColumn = partitionColumn;
            this.retentionMonths = retentionMonths;
        }
    }
}
