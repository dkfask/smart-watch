-- 完整智能手环定位系统数据库架构
-- 版本：1.0
-- 创建日期：2025-12-02
-- 用途：初始化完整的数据库结构，包括表、索引、分区、存储过程和定时事件

-- MySQL Docker 的 init SQL 每个文件独立连接，USE 不会跨文件保留。
-- 由 00_bootstrap.sql 已经 CREATE DATABASE，这里切到该数据库。
USE smart_watch;

-- ------------------------------
-- 1. 基础表结构
-- ------------------------------

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'user', -- admin, user
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- active, disabled
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_phone (phone),
    INDEX idx_users_role (role),
    INDEX idx_users_status (status),
    INDEX idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 设备表
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    imei VARCHAR(15) NOT NULL UNIQUE,
    device_model VARCHAR(50),
    mcc VARCHAR(8),
    mnc VARCHAR(8),
    apn VARCHAR(100),
    iccid VARCHAR(64),
    imsi VARCHAR(64),
    firmware_version VARCHAR(20),
    hardware_version VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'inactive', -- inactive, active, offline, online
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_devices_imei (imei),
    INDEX idx_devices_status (status),
    INDEX idx_devices_created_at (created_at),
    INDEX idx_devices_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户设备关联表
CREATE TABLE IF NOT EXISTS user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_device (user_id, device_id),
    INDEX idx_ud_user_id (user_id),
    INDEX idx_ud_device_id (device_id),
    INDEX idx_ud_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------
-- 2. 核心业务表结构
-- ------------------------------

-- 设备状态表
CREATE TABLE IF NOT EXISTS device_status (
    device_id BIGINT PRIMARY KEY,
    last_location_time DATETIME,
    last_latitude DOUBLE,
    last_longitude DOUBLE,
    battery_level INT,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    gsm_signal INT,
    satellite_count INT,
    arm_status VARCHAR(2),
    work_mode VARCHAR(2),
    imei VARCHAR(15),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    INDEX idx_ds_imei (imei),
    INDEX idx_ds_is_online (is_online),
    INDEX idx_ds_last_location_time (last_location_time),
    INDEX idx_ds_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 定位记录表（按月份分区）
CREATE TABLE IF NOT EXISTS location_records (
    id BIGINT AUTO_INCREMENT,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time DATETIME NOT NULL,
    gps_raw VARCHAR(1024),
    extra_raw TEXT,
    latitude DOUBLE,
    longitude DOUBLE,
    speed DECIMAL(5,2),
    direction DECIMAL(6,2),
    address VARCHAR(255),
    source VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 分区表主键必须包含分区列，使用复合主键
    PRIMARY KEY (id, recv_time),
    -- 分区表不支持外键约束，通过应用程序保证数据一致性
    INDEX idx_lr_imei_recv_time (imei, recv_time DESC),
    INDEX idx_lr_device_recv_time (device_id, recv_time DESC),
    INDEX idx_lr_recv_time (recv_time),
    INDEX idx_lr_lat_lng (latitude, longitude),
    INDEX idx_lr_source (source),
    INDEX idx_lr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (MONTH(recv_time)) (
    PARTITION p1 VALUES LESS THAN (2),
    PARTITION p2 VALUES LESS THAN (3),
    PARTITION p3 VALUES LESS THAN (4),
    PARTITION p4 VALUES LESS THAN (5),
    PARTITION p5 VALUES LESS THAN (6),
    PARTITION p6 VALUES LESS THAN (7),
    PARTITION p7 VALUES LESS THAN (8),
    PARTITION p8 VALUES LESS THAN (9),
    PARTITION p9 VALUES LESS THAN (10),
    PARTITION p10 VALUES LESS THAN (11),
    PARTITION p11 VALUES LESS THAN (12),
    PARTITION p12 VALUES LESS THAN (13)
);

-- 心跳记录表（按月份分区）
CREATE TABLE IF NOT EXISTS heartbeat_records (
    id BIGINT AUTO_INCREMENT,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time DATETIME NOT NULL,
    status_block VARCHAR(255),
    counter VARCHAR(64),
    roll_count VARCHAR(64),
    work_mode VARCHAR(64),
    interval_seconds SMALLINT,
    raw_payload TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 分区表主键必须包含分区列，使用复合主键
    PRIMARY KEY (id, recv_time),
    -- 分区表不支持外键约束，通过应用程序保证数据一致性
    INDEX idx_hb_imei_recv_time (imei, recv_time DESC),
    INDEX idx_hb_device_recv_time (device_id, recv_time DESC),
    INDEX idx_hb_recv_time (recv_time),
    INDEX idx_hb_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (MONTH(recv_time)) (
    PARTITION p1 VALUES LESS THAN (2),
    PARTITION p2 VALUES LESS THAN (3),
    PARTITION p3 VALUES LESS THAN (4),
    PARTITION p4 VALUES LESS THAN (5),
    PARTITION p5 VALUES LESS THAN (6),
    PARTITION p6 VALUES LESS THAN (7),
    PARTITION p7 VALUES LESS THAN (8),
    PARTITION p8 VALUES LESS THAN (9),
    PARTITION p9 VALUES LESS THAN (10),
    PARTITION p10 VALUES LESS THAN (11),
    PARTITION p11 VALUES LESS THAN (12),
    PARTITION p12 VALUES LESS THAN (13)
);

-- 健康记录表
CREATE TABLE IF NOT EXISTS health_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time DATETIME NOT NULL,
    data_type VARCHAR(50) NOT NULL, -- blood_pressure, heart_rate, temperature, spo2, etc.
    value VARCHAR(255) NOT NULL,
    raw_data TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL,
    INDEX idx_hr_imei_recv_time (imei, recv_time DESC),
    INDEX idx_hr_device_recv_time (device_id, recv_time DESC),
    INDEX idx_hr_data_type (data_type),
    INDEX idx_hr_recv_time (recv_time),
    INDEX idx_hr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------
-- 2. 扩展业务表结构
-- ------------------------------

-- 地理围栏表
CREATE TABLE IF NOT EXISTS geo_fences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- circle, polygon, rectangle
    center_lat DOUBLE,
    center_lng DOUBLE,
    radius INT, -- 单位：米（仅用于圆形围栏）
    coordinates TEXT NOT NULL, -- 围栏坐标，JSON格式
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- active, inactive
    description TEXT,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_gf_name (name),
    INDEX idx_gf_type (type),
    INDEX idx_gf_status (status),
    INDEX idx_gf_created_by (created_by),
    INDEX idx_gf_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 围栏告警表（按月份分区）
CREATE TABLE IF NOT EXISTS fence_alerts (
    id BIGINT AUTO_INCREMENT,
    device_id BIGINT,
    imei VARCHAR(15),
    fence_id BIGINT,
    alert_type VARCHAR(20) NOT NULL, -- enter, exit
    latitude DOUBLE,
    longitude DOUBLE,
    alert_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'unhandled', -- unhandled, handled
    handled_time DATETIME,
    handled_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 分区表主键必须包含分区列，使用复合主键
    PRIMARY KEY (id, alert_time),
    -- 分区表不支持外键约束，通过应用程序保证数据一致性
    INDEX idx_fa_device_imei (device_id, imei),
    INDEX idx_fa_fence_id (fence_id),
    INDEX idx_fa_alert_type (alert_type),
    INDEX idx_fa_status (status),
    INDEX idx_fa_alert_time (alert_time),
    INDEX idx_fa_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (MONTH(alert_time)) (
    PARTITION p1 VALUES LESS THAN (2),
    PARTITION p2 VALUES LESS THAN (3),
    PARTITION p3 VALUES LESS THAN (4),
    PARTITION p4 VALUES LESS THAN (5),
    PARTITION p5 VALUES LESS THAN (6),
    PARTITION p6 VALUES LESS THAN (7),
    PARTITION p7 VALUES LESS THAN (8),
    PARTITION p8 VALUES LESS THAN (9),
    PARTITION p9 VALUES LESS THAN (10),
    PARTITION p10 VALUES LESS THAN (11),
    PARTITION p11 VALUES LESS THAN (12),
    PARTITION p12 VALUES LESS THAN (13)
);

-- 下行命令表
CREATE TABLE IF NOT EXISTS downlink_commands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    command_type VARCHAR(50) NOT NULL,
    command_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, sent, delivered, failed
    priority VARCHAR(20) NOT NULL DEFAULT 'normal', -- high, normal, low
    sent_time DATETIME,
    delivered_time DATETIME,
    failed_reason TEXT,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_dc_device_id (device_id),
    INDEX idx_dc_imei (imei),
    INDEX idx_dc_command_type (command_type),
    INDEX idx_dc_status (status),
    INDEX idx_dc_priority (priority),
    INDEX idx_dc_created_by (created_by),
    INDEX idx_dc_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------
-- 3. 新增表结构
-- ------------------------------

-- 设备配置表
CREATE TABLE IF NOT EXISTS device_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE KEY uk_device_config (device_id, config_key),
    INDEX idx_dc_device (device_id),
    INDEX idx_dc_key (config_key),
    INDEX idx_dc_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 设备告警表（按月份分区）
CREATE TABLE IF NOT EXISTS device_alert (
    id BIGINT AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    imei VARCHAR(15) NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- low_battery, offline, fall, sos, etc.
    alert_message VARCHAR(255) NOT NULL,
    alert_level VARCHAR(20) NOT NULL DEFAULT 'info', -- critical, high, medium, low, info
    latitude DOUBLE,
    longitude DOUBLE,
    alert_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'unhandled', -- unhandled, handled, ignored
    handled_time DATETIME,
    handled_by VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 分区表主键必须包含分区列，使用复合主键
    PRIMARY KEY (id, alert_time),
    -- 分区表不支持外键约束，通过应用程序保证数据一致性
    INDEX idx_da_device_imei (device_id, imei),
    INDEX idx_da_alert_type (alert_type),
    INDEX idx_da_alert_level (alert_level),
    INDEX idx_da_status (status),
    INDEX idx_da_alert_time (alert_time),
    INDEX idx_da_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (MONTH(alert_time)) (
    PARTITION p1 VALUES LESS THAN (2),
    PARTITION p2 VALUES LESS THAN (3),
    PARTITION p3 VALUES LESS THAN (4),
    PARTITION p4 VALUES LESS THAN (5),
    PARTITION p5 VALUES LESS THAN (6),
    PARTITION p6 VALUES LESS THAN (7),
    PARTITION p7 VALUES LESS THAN (8),
    PARTITION p8 VALUES LESS THAN (9),
    PARTITION p9 VALUES LESS THAN (10),
    PARTITION p10 VALUES LESS THAN (11),
    PARTITION p11 VALUES LESS THAN (12),
    PARTITION p12 VALUES LESS THAN (13)
);

-- 定位统计汇总表
CREATE TABLE IF NOT EXISTS location_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    imei VARCHAR(15) NOT NULL,
    summary_date DATE NOT NULL,
    total_records INT NOT NULL DEFAULT 0,
    avg_latency DECIMAL(5,2),
    max_speed DECIMAL(5,2),
    min_speed DECIMAL(5,2),
    avg_speed DECIMAL(5,2),
    distance DECIMAL(10,2), -- 总里程，单位：公里
    online_hours DECIMAL(5,2), -- 在线时长，单位：小时
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ls_device_date (device_id, summary_date),
    INDEX idx_ls_imei_date (imei, summary_date),
    INDEX idx_ls_date (summary_date),
    INDEX idx_ls_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统操作日志表（按月份分区）
CREATE TABLE IF NOT EXISTS system_log (
    id BIGINT AUTO_INCREMENT,
    operate_user VARCHAR(50) NOT NULL,
    operate_type VARCHAR(50) NOT NULL, -- login, logout, create, update, delete, etc.
    operate_content TEXT NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 分区表主键必须包含分区列，使用复合主键
    PRIMARY KEY (id, created_at),
    INDEX idx_sl_operate_user (operate_user),
    INDEX idx_sl_operate_type (operate_type),
    INDEX idx_sl_ip_address (ip_address),
    INDEX idx_sl_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (MONTH(created_at)) (
    PARTITION p1 VALUES LESS THAN (2),
    PARTITION p2 VALUES LESS THAN (3),
    PARTITION p3 VALUES LESS THAN (4),
    PARTITION p4 VALUES LESS THAN (5),
    PARTITION p5 VALUES LESS THAN (6),
    PARTITION p6 VALUES LESS THAN (7),
    PARTITION p7 VALUES LESS THAN (8),
    PARTITION p8 VALUES LESS THAN (9),
    PARTITION p9 VALUES LESS THAN (10),
    PARTITION p10 VALUES LESS THAN (11),
    PARTITION p11 VALUES LESS THAN (12),
    PARTITION p12 VALUES LESS THAN (13)
);

-- ------------------------------
-- 4. 存储过程和函数
-- ------------------------------

-- 存储过程：清理过期数据
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS clean_expired_data()
BEGIN
    -- 删除30天前的定位记录（热数据保留30天）
    DELETE FROM location_records WHERE recv_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    -- 删除90天前的心跳记录（温数据保留90天）
    DELETE FROM heartbeat_records WHERE recv_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
    
    -- 删除1年前的健康记录（冷数据保留1年）
    DELETE FROM health_records WHERE recv_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
    
    -- 删除1年前的围栏告警记录
    DELETE FROM fence_alerts WHERE alert_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
    
    -- 删除1年前的设备告警记录
    DELETE FROM device_alert WHERE alert_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
    
    -- 删除30天前的系统日志
    DELETE FROM system_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    -- 优化表结构，减少碎片
    OPTIMIZE TABLE location_records, heartbeat_records, health_records, fence_alerts, device_alert, system_log;
END //
DELIMITER ;

-- 存储过程：生成设备定位统计汇总
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_location_summary(IN summary_date DATE)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_device_id BIGINT;
    DECLARE v_imei VARCHAR(15);
    
    -- 游标声明
    DECLARE device_cursor CURSOR FOR 
        SELECT id, imei FROM devices WHERE status = 'active';
    
    -- 游标异常处理
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 打开游标
    OPEN device_cursor;
    
    read_loop: LOOP
        -- 获取设备信息
        FETCH device_cursor INTO v_device_id, v_imei;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 计算当日定位记录总数
        SELECT COUNT(*) INTO @total_records 
        FROM location_records 
        WHERE device_id = v_device_id AND DATE(recv_time) = summary_date;
        
        -- 计算平均延迟（假设延迟是从设备发送到服务器接收的时间差，这里简化处理）
        SELECT AVG(TIMESTAMPDIFF(SECOND, recv_time, created_at)) INTO @avg_latency 
        FROM location_records 
        WHERE device_id = v_device_id AND DATE(recv_time) = summary_date;
        
        -- 计算最大、最小、平均速度
        SELECT MAX(speed), MIN(speed), AVG(speed) 
        INTO @max_speed, @min_speed, @avg_speed 
        FROM location_records 
        WHERE device_id = v_device_id AND DATE(recv_time) = summary_date AND speed IS NOT NULL;
        
        -- 计算总里程（简化处理，实际需要根据坐标计算）
        SET @distance = 0.0;
        
        -- 计算在线时长（简化处理，实际需要根据心跳记录计算）
        SET @online_hours = 0.0;
        
        -- 插入或更新统计数据
        INSERT INTO location_summary 
        (device_id, imei, summary_date, total_records, avg_latency, max_speed, min_speed, avg_speed, distance, online_hours)
        VALUES 
        (v_device_id, v_imei, summary_date, @total_records, @avg_latency, @max_speed, @min_speed, @avg_speed, @distance, @online_hours)
        ON DUPLICATE KEY UPDATE
            total_records = @total_records,
            avg_latency = @avg_latency,
            max_speed = @max_speed,
            min_speed = @min_speed,
            avg_speed = @avg_speed,
            distance = @distance,
            online_hours = @online_hours,
            updated_at = NOW();
    
    END LOOP;
    
    -- 关闭游标
    CLOSE device_cursor;
END //
DELIMITER ;

-- ------------------------------
-- 5. 定时事件
-- ------------------------------

-- 事件：每日凌晨清理过期数据
CREATE EVENT IF NOT EXISTS daily_cleanup
ON SCHEDULE EVERY 1 DAY STARTS '2025-01-01 00:00:00'
ON COMPLETION PRESERVE
DO CALL clean_expired_data();

-- 事件：每日凌晨2点生成前一天的定位统计汇总
CREATE EVENT IF NOT EXISTS daily_generate_location_summary
ON SCHEDULE EVERY 1 DAY STARTS '2025-01-01 02:00:00'
ON COMPLETION PRESERVE
DO CALL generate_location_summary(DATE_SUB(CURDATE(), INTERVAL 1 DAY));

-- ------------------------------
-- 6. 初始数据
-- ------------------------------

-- 插入默认管理员用户（密码：admin123，加密方式：BCrypt）
INSERT IGNORE INTO users (username, password, email, phone, real_name, role, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTBv38Eo1r2d7X17lFJU8o093YwM1Efe', 'admin@example.com', '13800138000', '系统管理员', 'admin', 'active');

-- 插入默认配置
INSERT IGNORE INTO device_config (device_id, config_key, config_value, description)
VALUES (0, 'location_retention_days', '30', '定位记录保留天数');

INSERT IGNORE INTO device_config (device_id, config_key, config_value, description)
VALUES (0, 'heartbeat_retention_days', '90', '心跳记录保留天数');

INSERT IGNORE INTO device_config (device_id, config_key, config_value, description)
VALUES (0, 'health_retention_days', '365', '健康记录保留天数');

-- ------------------------------
-- 7. 权限配置
-- ------------------------------

-- 临时降低密码策略要求（仅用于初始化）
SET GLOBAL validate_password.policy = LOW;
SET GLOBAL validate_password.length = 6;

-- 创建数据库用户并授权
CREATE USER IF NOT EXISTS 'smart_user'@'localhost' IDENTIFIED BY 'smart123';
CREATE USER IF NOT EXISTS 'smart_user'@'%' IDENTIFIED BY 'smart123';

GRANT ALL PRIVILEGES ON smart_watch.* TO 'smart_user'@'localhost';
GRANT ALL PRIVILEGES ON smart_watch.* TO 'smart_user'@'%';

FLUSH PRIVILEGES;

-- 恢复密码策略（可选，根据实际需求调整）
-- SET GLOBAL validate_password.policy = MEDIUM;
-- SET GLOBAL validate_password.length = 8;

-- ------------------------------
-- 数据库架构创建完成
-- ------------------------------

SELECT '数据库架构创建完成' AS result;
SELECT '用户：smart_user，密码：smart_password' AS database_user;
SELECT '默认管理员：admin，密码：admin123' AS admin_user;
