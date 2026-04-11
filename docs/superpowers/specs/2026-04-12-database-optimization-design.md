# Smart Watch 数据库优化设计文档

**日期**: 2026-04-12
**项目**: smart (智慧医疗定位追踪系统)
**方案**: 渐进式优化 (方案 A)
**目标用户规模**: 几千人

---

## 1. 现状分析

### 1.1 技术栈

- **后端**: Spring Boot + JPA/Hibernate
- **数据库**: MySQL 8.0 (远程: 8.156.83.206:3306/smart_watch)
- **连接池**: HikariCP
- **缓存**: Redis (本地)
- **DDL 策略**: `hibernate.ddl-auto=update` (自动更新表结构)
- **外键策略**: 全局禁用外键约束生成 (`hibernate.hbm2ddl.foreign_key_constraints=disable`)

### 1.2 当前表概览 (18 张)

| 表名 | 行数 | 数据 MB | 索引 MB | 分区 |
|------|------|---------|---------|------|
| heartbeat_records | 42 | 0.19 | 0.75 | month(recv_time) |
| health_records | 31 | 0.02 | 0.09 | 无 |
| fence_alerts | 31 | 0.19 | 1.31 | month(alert_time) |
| alarms | 25 | 0.02 | 0.00 | 无 |
| downlink_commands | 11 | 0.02 | 0.11 | 无 |
| location_records | 0 | 0.19 | 1.50 | month(recv_time) |
| system_log | 0 | 0.19 | 0.75 | month(created_at) |
| device_alert | 0 | 0.19 | 1.31 | month(alert_time) |
| 其他小表 | <5 | <0.1 | <0.1 | 无 |

### 1.3 关键问题汇总

#### CRITICAL

1. **分区设计致命缺陷**: `PARTITION BY RANGE (month(recv_time))` 只返回 1-12，所有年份的数据落入同一分区，分区形同虚设
2. **alarms 表零辅助索引**: 高频查询表无任何辅助索引，全表扫描

#### HIGH

3. **大量冗余索引**: 约 15 个索引与 UNIQUE KEY 或复合索引前缀重复，浪费写入开销和存储
4. **报警表架构重叠**: `alarms` + `device_alert` + `fence_alerts` 功能重叠，`FenceAlert`/`SimpleFenceAlert` 两个实体映射同一张表
5. **MySQL 配置不足**: `innodb_buffer_pool_size=128MB` (太小), `slow_query_log=OFF`

#### MEDIUM

6. **status/role 字段无约束**: 使用 varchar 而非 ENUM，无法防止非法值
7. **HikariCP 连接池配置欠优**: 远程连接下 idle-timeout/max-lifetime 过长
8. **外键策略不一致**: 部分表有外键 (patient_devices, geo_fences)，部分表禁用

---

## 2. 索引优化

### 2.1 删除冗余索引 (15 个)

```sql
-- ============================================================
-- Step 1: 删除与 UNIQUE KEY 重复的普通索引
-- ============================================================

-- users 表: UNIQUE KEY 已提供索引能力
ALTER TABLE users DROP INDEX idx_users_username;   -- 与 UNIQUE KEY username 重复
ALTER TABLE users DROP INDEX idx_users_email;      -- 与 UNIQUE KEY email 重复
ALTER TABLE users DROP INDEX idx_users_phone;      -- 与 UNIQUE KEY phone 重复

-- devices 表
ALTER TABLE devices DROP INDEX idx_devices_imei;   -- 与 UNIQUE KEY imei 重复

-- ============================================================
-- Step 2: 删除与其他索引重复/低效的索引
-- ============================================================

-- patient_devices 表
ALTER TABLE patient_devices DROP INDEX idx_pd_patient_id;                  -- 与 uk_patient_devices_patient_id 重复
ALTER TABLE patient_devices DROP INDEX idx_pd_device_id;                   -- 与 uk_patient_devices_device_id 重复
ALTER TABLE patient_devices DROP INDEX idx_pd_is_active;                   -- 与 idx_patient_devices_is_active 重复
ALTER TABLE patient_devices DROP INDEX idx_patient_devices_is_active;      -- 布尔列索引选择性极低，删除
ALTER TABLE patient_devices DROP INDEX idx_patient_devices_bind_time;      -- 极少按 bind_time 独立查询，删除

-- geo_fences 表
ALTER TABLE geo_fences DROP INDEX idx_geo_fences_patient_id;               -- 与 idx_gf_patient_id 完全重复

-- location_records 表
ALTER TABLE location_records DROP INDEX idx_device_recvtime;               -- 与 idx_lr_device_recv_time 功能相同

-- fence_patients 表
ALTER TABLE fence_patients DROP INDEX idx_fence_id;                        -- 被 uk_fence_patient (fence_id, patient_id) 前缀覆盖
```

### 2.2 添加缺失索引 (6 个)

```sql
-- ============================================================
-- Step 3: 为 alarms 表添加关键索引 (当前零辅助索引)
-- ============================================================

ALTER TABLE alarms ADD INDEX idx_alarms_device_triggered (device_id, triggered_time DESC);
ALTER TABLE alarms ADD INDEX idx_alarms_patient_triggered (patient_id, triggered_time DESC);
ALTER TABLE alarms ADD INDEX idx_alarms_status_triggered (status, triggered_time DESC);
ALTER TABLE alarms ADD INDEX idx_alarms_type_triggered (alarm_type, triggered_time DESC);
ALTER TABLE alarms ADD INDEX idx_alarms_is_read (is_read);

-- health_records 表: 添加 patient+time 复合索引
ALTER TABLE health_records ADD INDEX idx_hr_patient_recv_time (patient_id, recv_time DESC);
```

---

## 3. 分区策略修正

### 3.1 问题说明

当前 4 张表使用 `PARTITION BY RANGE (month(recv_time))`:

```sql
PARTITION BY RANGE (month(`recv_time`))
(PARTITION p1 VALUES LESS THAN (2), ... PARTITION p12 VALUES LESS THAN (13))
```

**致命缺陷**: `month()` 只返回 1-12，2025 年和 2026 年 1 月的数据都进入 `p1` 分区。无法通过 `DROP PARTITION` 清理旧数据，查询优化器也无法跨年裁剪分区。

### 3.2 新方案: RANGE COLUMNS 按年月分区

改为 `RANGE COLUMNS(recv_time)` 按日期分区，预建 2 年 + p_future：

```sql
-- ============================================================
-- location_records 分区重建
-- ============================================================

ALTER TABLE location_records REMOVE PARTITIONING;

ALTER TABLE location_records PARTITION BY RANGE COLUMNS(recv_time) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p202607 VALUES LESS THAN ('2026-08-01'),
    PARTITION p202608 VALUES LESS THAN ('2026-09-01'),
    PARTITION p202609 VALUES LESS THAN ('2026-10-01'),
    PARTITION p202610 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202611 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202612 VALUES LESS THAN ('2027-01-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-02-01'),
    PARTITION p202702 VALUES LESS THAN ('2027-03-01'),
    PARTITION p202703 VALUES LESS THAN ('2027-04-01'),
    PARTITION p202704 VALUES LESS THAN ('2027-05-01'),
    PARTITION p202705 VALUES LESS THAN ('2027-06-01'),
    PARTITION p202706 VALUES LESS THAN ('2027-07-01'),
    PARTITION p202707 VALUES LESS THAN ('2027-08-01'),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================================
-- heartbeat_records 分区重建
-- ============================================================

ALTER TABLE heartbeat_records REMOVE PARTITIONING;

ALTER TABLE heartbeat_records PARTITION BY RANGE COLUMNS(recv_time) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p202607 VALUES LESS THAN ('2026-08-01'),
    PARTITION p202608 VALUES LESS THAN ('2026-09-01'),
    PARTITION p202609 VALUES LESS THAN ('2026-10-01'),
    PARTITION p202610 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202611 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202612 VALUES LESS THAN ('2027-01-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-02-01'),
    PARTITION p202702 VALUES LESS THAN ('2027-03-01'),
    PARTITION p202703 VALUES LESS THAN ('2027-04-01'),
    PARTITION p202704 VALUES LESS THAN ('2027-05-01'),
    PARTITION p202705 VALUES LESS THAN ('2027-06-01'),
    PARTITION p202706 VALUES LESS THAN ('2027-07-01'),
    PARTITION p202707 VALUES LESS THAN ('2027-08-01'),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================================
-- system_log 分区重建
-- ============================================================

ALTER TABLE system_log REMOVE PARTITIONING;

ALTER TABLE system_log PARTITION BY RANGE COLUMNS(created_at) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p202607 VALUES LESS THAN ('2026-08-01'),
    PARTITION p202608 VALUES LESS THAN ('2026-09-01'),
    PARTITION p202609 VALUES LESS THAN ('2026-10-01'),
    PARTITION p202610 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202611 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202612 VALUES LESS THAN ('2027-01-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-02-01'),
    PARTITION p202702 VALUES LESS THAN ('2027-03-01'),
    PARTITION p202703 VALUES LESS THAN ('2027-04-01'),
    PARTITION p202704 VALUES LESS THAN ('2027-05-01'),
    PARTITION p202705 VALUES LESS THAN ('2027-06-01'),
    PARTITION p202706 VALUES LESS THAN ('2027-07-01'),
    PARTITION p202707 VALUES LESS THAN ('2027-08-01'),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 3.3 数据保留策略

| 表 | 保留期限 | 清理方式 |
|----|---------|---------|
| location_records | 3 个月 | `ALTER TABLE location_records DROP PARTITION p202604` |
| heartbeat_records | 3 个月 | `ALTER TABLE heartbeat_records DROP PARTITION p202604` |
| alerts (合并后) | 6 个月 | `ALTER TABLE alerts DROP PARTITION p202604` |
| system_log | 12 个月 | `ALTER TABLE system_log DROP PARTITION p202504` |

### 3.4 分区维护计划

每月 1 号执行:
1. `ALTER TABLE xxx REORGANIZE PARTITION p_future INTO (PARTITION pYYYYMM VALUES LESS THAN ('YYYY-MM-01'), PARTITION p_future VALUES LESS THAN MAXVALUE)` — 为下月创建分区
2. `ALTER TABLE xxx DROP PARTITION p过期月份` — 删除过期数据

### 3.5 数据量估算 (中频上报 5-10 分钟, 3000 设备)

| 表 | 每设备每天 | 每月总量 | 每分区大小 |
|---|---|---|---|
| location_records | ~200 条 | ~1800 万 | ~2 GB |
| heartbeat_records | ~288 条 | ~2600 万 | ~3 GB |
| alerts | 事件驱动 | ~10 万 | ~15 MB |
| system_log | 事件驱动 | ~5 万 | ~5 MB |

---

## 4. 架构合并: 统一报警表

### 4.1 当前重叠分析

| 表 | 用途 | 字段差异 | JPA 实体 |
|----|------|---------|----------|
| alarms | 通用报警 | 无 fence_id, 有 alarm_level/alarm_data | Alarm.java |
| device_alert | 设备报警 | 无 fence_id, 有 alert_message | 无对应实体 |
| fence_alerts | 围栏报警 | 有 fence_id, 无 alarm_level | FenceAlert.java + SimpleFenceAlert.java |

### 4.2 新表 DDL: alerts

```sql
-- ============================================================
-- 创建统一报警表 alerts
-- ============================================================

CREATE TABLE alerts (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    device_id       BIGINT NOT NULL,
    patient_id      BIGINT,
    imei            VARCHAR(15),
    fence_id        BIGINT,                                    -- 仅围栏报警有值
    alert_type      ENUM('fence_breach','low_battery','sos','fall','heart_rate','device_offline','other') NOT NULL,
    alert_level     ENUM('critical','warning','info') NOT NULL DEFAULT 'warning',
    alert_data      TEXT,                                      -- JSON 格式详细数据
    latitude        DOUBLE,
    longitude       DOUBLE,
    address         VARCHAR(255),
    alert_time      DATETIME NOT NULL,
    status          ENUM('pending','handled','false_alarm') NOT NULL DEFAULT 'pending',
    is_read         TINYINT NOT NULL DEFAULT 0,
    handle_result   VARCHAR(50),
    handle_remark   TEXT,
    handled_by      BIGINT,
    handled_time    DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id, alert_time),
    INDEX idx_alerts_device_time (device_id, alert_time DESC),
    INDEX idx_alerts_patient_time (patient_id, alert_time DESC),
    INDEX idx_alerts_fence_time (fence_id, alert_time DESC),
    INDEX idx_alerts_type_time (alert_type, alert_time DESC),
    INDEX idx_alerts_level_time (alert_level, alert_time DESC),
    INDEX idx_alerts_status_time (status, alert_time DESC),
    INDEX idx_alerts_is_read (is_read),
    INDEX idx_alerts_imei_time (imei, alert_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE COLUMNS(alert_time) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p202607 VALUES LESS THAN ('2026-08-01'),
    PARTITION p202608 VALUES LESS THAN ('2026-09-01'),
    PARTITION p202609 VALUES LESS THAN ('2026-10-01'),
    PARTITION p202610 VALUES LESS THAN ('2026-11-01'),
    PARTITION p202611 VALUES LESS THAN ('2026-12-01'),
    PARTITION p202612 VALUES LESS THAN ('2027-01-01'),
    PARTITION p202701 VALUES LESS THAN ('2027-02-01'),
    PARTITION p202702 VALUES LESS THAN ('2027-03-01'),
    PARTITION p202703 VALUES LESS THAN ('2027-04-01'),
    PARTITION p202704 VALUES LESS THAN ('2027-05-01'),
    PARTITION p202705 VALUES LESS THAN ('2027-06-01'),
    PARTITION p202706 VALUES LESS THAN ('2027-07-01'),
    PARTITION p202707 VALUES LESS THAN ('2027-08-01'),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 4.3 数据迁移脚本

```sql
-- ============================================================
-- 从 alarms 迁移数据到 alerts
-- ============================================================

INSERT INTO alerts (device_id, patient_id, imei, fence_id, alert_type, alert_level,
    alert_data, latitude, longitude, address, alert_time, status, is_read,
    handle_result, handle_remark, handled_by, handled_time, created_at, updated_at)
SELECT
    a.device_id,
    a.patient_id,
    NULL AS imei,
    NULL AS fence_id,
    a.alarm_type,
    a.alarm_level,
    a.alarm_data,
    a.latitude,
    a.longitude,
    a.address,
    a.triggered_time AS alert_time,
    a.status,
    IF(a.is_read, 1, 0) AS is_read,
    a.handle_result,
    a.handle_remark,
    NULL AS handled_by,
    a.handled_time,
    a.created_at,
    a.updated_at
FROM alarms a;

-- ============================================================
-- 从 fence_alerts 迁移数据到 alerts (仅迁移 alerts 中不存在的)
-- ============================================================

INSERT INTO alerts (device_id, patient_id, imei, fence_id, alert_type, alert_level,
    alert_data, latitude, longitude, address, alert_time, status, is_read,
    handle_result, handle_remark, handled_by, handled_time, created_at, updated_at)
SELECT
    fa.device_id,
    fa.patient_id,
    fa.imei,
    fa.fence_id,
    fa.alert_type,
    'warning' AS alert_level,
    NULL AS alert_data,
    fa.latitude,
    fa.longitude,
    NULL AS address,
    fa.alert_time,
    fa.status,
    fa.is_read,
    NULL AS handle_result,
    NULL AS handle_remark,
    fa.handled_by,
    fa.handled_time,
    fa.created_at,
    fa.updated_at
FROM fence_alerts fa;

-- ============================================================
-- 从 device_alert 迁移数据到 alerts (仅迁移 alerts 中不存在的)
-- ============================================================

INSERT INTO alerts (device_id, patient_id, imei, fence_id, alert_type, alert_level,
    alert_data, latitude, longitude, address, alert_time, status, is_read,
    handle_result, handle_remark, handled_by, handled_time, created_at, updated_at)
SELECT
    da.device_id,
    da.patient_id,
    da.imei,
    NULL AS fence_id,
    da.alert_type,
    da.alert_level,
    da.alert_message AS alert_data,
    da.latitude,
    da.longitude,
    NULL AS address,
    da.alert_time,
    da.status,
    0 AS is_read,
    NULL AS handle_result,
    NULL AS handle_remark,
    NULL AS handled_by,
    da.handled_time,
    da.created_at,
    da.updated_at
FROM device_alert da;

-- ============================================================
-- 验证数据完整性后，删除旧表
-- ============================================================

-- 验证: SELECT COUNT(*) FROM alerts; 与三张旧表总和一致后再执行:
-- DROP TABLE alarms;
-- DROP TABLE fence_alerts;
-- DROP TABLE device_alert;
```

### 4.4 JPA 实体变更

**删除**:
- `model/Alarm.java`
- `model/FenceAlert.java`
- `model/SimpleFenceAlert.java`

**新建**: `model/Alert.java`

```java
@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(length = 15)
    private String imei;

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "alert_level", nullable = false, length = 20)
    private String alertLevel;

    @Column(name = "alert_data", columnDefinition = "text")
    private String alertData;

    private Double latitude;
    private Double longitude;

    @Column(length = 255)
    private String address;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "alert_time", nullable = false)
    private Date alertTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "is_read", nullable = false, columnDefinition = "tinyint default 0")
    private Integer isRead;

    @Column(name = "handle_result", length = 50)
    private String handleResult;

    @Column(name = "handle_remark", columnDefinition = "text")
    private String handleRemark;

    @Column(name = "handled_by")
    private Long handledBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "handled_time")
    private Date handledTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "datetime default current_timestamp")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "datetime default current_timestamp on update current_timestamp")
    private Date updatedAt;

    // Constructors, getters, setters...
}
```

**Repository 变更**:
- `AlarmRepository` + `FenceAlertRepository` + `SimpleFenceAlertRepository` → 合并为 `AlertRepository`
- 保留所有查询方法，统一使用 `Alert` 实体

**DDL 管理策略变更**:

**Service 变更**:
- `AlarmService` → 适配 `Alert` 实体
- `FenceService` → 报警逻辑适配 `Alert` 实体

---

## 5. 字段类型优化

将 status/role/type 等 varchar 字段改为 ENUM，限定合法值并减少存储空间:

```sql
-- ============================================================
-- 字段类型优化: varchar -> ENUM
-- ============================================================

-- users 表
ALTER TABLE users MODIFY COLUMN role ENUM('admin','user','guardian') NOT NULL DEFAULT 'user';
ALTER TABLE users MODIFY COLUMN status ENUM('active','disabled','locked') NOT NULL DEFAULT 'active';

-- devices 表
ALTER TABLE devices MODIFY COLUMN status ENUM('active','inactive','maintenance') NOT NULL DEFAULT 'inactive';

-- patients 表
ALTER TABLE patients MODIFY COLUMN gender ENUM('male','female','other');
ALTER TABLE patients MODIFY COLUMN status ENUM('admitted','discharged') NOT NULL DEFAULT 'admitted';

-- patient_devices 表
ALTER TABLE patient_devices MODIFY COLUMN relationship ENUM('wearing','monitoring') DEFAULT 'wearing';

-- alerts 表 (新表已使用 ENUM 理念，实际建表时可直接用 ENUM)
-- 注意: JPA 与 MySQL ENUM 配合需要在实体中使用 @Enumerated(EnumType.STRING)
-- 或在 columnDefinition 中指定 ENUM 类型

-- geo_fences 表
ALTER TABLE geo_fences MODIFY COLUMN type ENUM('circle','polygon','rectangle') NOT NULL DEFAULT 'circle';
ALTER TABLE geo_fences MODIFY COLUMN status ENUM('active','inactive') NOT NULL DEFAULT 'active';

-- downlink_commands 表
ALTER TABLE downlink_commands MODIFY COLUMN status ENUM('pending','sent','delivered','failed','timeout') DEFAULT 'pending';
ALTER TABLE downlink_commands MODIFY COLUMN priority ENUM('urgent','normal','low') NOT NULL DEFAULT 'normal';

-- user_devices 表
ALTER TABLE user_devices MODIFY COLUMN relationship ENUM('owner','viewer','guardian') DEFAULT 'owner';
```

### JPA 适配说明

对于使用 ENUM 的字段，JPA 实体中应使用 Java enum + `@Enumerated(EnumType.STRING)`:

```java
public enum AlertStatus { PENDING, HANDLED, FALSE_ALARM }

@Column(nullable = false, length = 20)
@Enumerated(EnumType.STRING)
private AlertStatus status;
```

---

## 6. MySQL 服务器配置优化

### 6.1 推荐配置 (假设服务器 4GB 内存)

```ini
[mysqld]
# 内存
innodb_buffer_pool_size = 1G                    # 从 128MB 提升
innodb_buffer_pool_instances = 1                # 1GB 池用 1 实例

# 日志
slow_query_log = ON                             # 开启慢查询日志
long_query_time = 2                             # 2 秒阈值
slow_query_log_file = /var/log/mysql/slow.log
log_queries_not_using_indexes = ON              # 记录未使用索引的查询

# 并发
max_connections = 300                           # 从 151 提升

# 持久化 (云服务器适当放宽，提升写入吞吐)
innodb_flush_log_at_trx_commit = 2              # 从 1 改为 2
sync_binlog = 100                               # 从 1 改为 100

# InnoDB 优化
innodb_io_capacity = 200                        # 云盘 IO 能力
innodb_io_capacity_max = 400
innodb_write_io_threads = 4
innodb_read_io_threads = 4

# 字符集 (已正确)
character_set_server = utf8mb4
collation_server = utf8mb4_unicode_ci
```

### 6.2 安全说明

- `innodb_flush_log_at_trx_commit = 2` 意味着最多丢失 1 秒事务数据，对定位追踪系统可接受
- `sync_binlog = 100` 同理，建议搭配定期备份策略

---

## 7. HikariCP 连接池优化

### 7.1 当前配置问题

| 参数 | 当前值 | 问题 |
|------|-------|------|
| maximum-pool-size | 10 | 远程连接下可能不足 |
| idle-timeout | 600000 (10分钟) | 远程连接空闲太久可能断开 |
| max-lifetime | 1800000 (30分钟) | 远程连接不宜太长 |
| keepalive-time | 未设置 | 缺少心跳保活 |

### 7.2 优化后配置

```properties
# HikariCP 优化配置
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=900000
spring.datasource.hikari.keepalive-time=120000
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.pool-name=SmartWatchHikariPool
```

---

## 8. 实施计划

### Phase 1: 索引清理与补充 (低风险)

1. 执行索引删除脚本 (2.1)
2. 执行索引添加脚本 (2.2)
3. 验证查询性能

### Phase 2: 分区重建 (中风险)

1. 在低峰期逐表重建分区
2. 验证分区裁剪: `EXPLAIN PARTITIONS SELECT ...`
3. 更新 LocationRetentionService 使用 DROP PARTITION 替代 DELETE

### Phase 3: 报警表合并 (高风险)

1. 创建 alerts 表
2. 数据迁移
3. 验证数据完整性
4. 修改 JPA 实体和 Repository
5. 修改 Service 层
6. 修改 Controller 层
7. 验证功能
8. 删除旧表

### Phase 4: 字段优化 & 配置调优 (低风险)

1. 执行 ENUM 字段修改
2. 更新 JPA 实体使用 Java enum
3. 应用 MySQL 配置
4. 应用 HikariCP 配置

### Phase 5: 验证与监控

1. 开启慢查询日志监控
2. 性能基准测试
3. 建立分区维护 cron job

---

## 9. 风险与回滚

| 风险 | 缓解措施 |
|------|---------|
| 分区重建期间表锁定 | 低峰期执行，当前数据量小，执行迅速 |
| 数据迁移丢失 | 迁移前全量备份，迁移后验证行数 |
| ENUM 值不兼容 | 先分析现有数据中的实际值，确保 ENUM 覆盖 |
| JPA 实体变更引入 bug | 充分的单元测试和集成测试 |

回滚策略:
- 每个 Phase 独立，可单独回滚
- 数据库备份在 Phase 3 前完成
- 旧表在 Phase 3 验证通过后再删除
