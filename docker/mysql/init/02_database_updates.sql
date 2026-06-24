-- 数据库架构完善计划实施脚本
-- 版本：1.0
-- 创建日期：2025-12-03
-- 用途：实现病人管理相关的数据库表结构

-- MySQL Docker 的 init SQL 每个文件独立连接；显式 USE 以保证后续语句
-- 落到正确的 schema。
USE smart_watch;

-- ------------------------------
-- 1. 创建新表
-- ------------------------------

-- 创建病人表
CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    age INT,
    id_card VARCHAR(18),
    ward VARCHAR(20),
    bed VARCHAR(10),
    phone VARCHAR(20),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    diagnosis TEXT,
    admission_date DATETIME,
    discharge_date DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'admitted', -- admitted, discharged
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patients_name (name),
    INDEX idx_patients_id_card (id_card),
    INDEX idx_patients_ward (ward),
    INDEX idx_patients_status (status),
    INDEX idx_patients_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建病人-设备关联表
CREATE TABLE IF NOT EXISTS patient_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    relationship VARCHAR(20) DEFAULT 'wearing', -- wearing, monitoring
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE KEY uk_patient_device (patient_id, device_id),
    INDEX idx_pd_patient_id (patient_id),
    INDEX idx_pd_device_id (device_id),
    INDEX idx_pd_is_active (is_active),
    INDEX idx_pd_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------
-- 2. 修改现有表
-- ------------------------------

-- 修改健康记录表，添加patient_id字段
ALTER TABLE health_records 
    ADD COLUMN patient_id BIGINT AFTER device_id,
    ADD INDEX idx_hr_patient_id (patient_id),
    ADD CONSTRAINT fk_hr_patient_id FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL;

-- 修改定位记录表，添加patient_id字段
ALTER TABLE location_records 
    ADD COLUMN patient_id BIGINT AFTER device_id,
    ADD INDEX idx_lr_patient_id (patient_id, recv_time DESC);

-- 修改围栏告警表，添加patient_id字段
ALTER TABLE fence_alerts 
    ADD COLUMN patient_id BIGINT AFTER device_id,
    ADD INDEX idx_fa_patient_id (patient_id, alert_time DESC);

-- 修改设备告警表，添加patient_id字段
ALTER TABLE device_alert 
    ADD COLUMN patient_id BIGINT AFTER device_id,
    ADD INDEX idx_da_patient_id (patient_id, alert_time DESC);

-- 修改地理围栏表，添加patient_id字段
ALTER TABLE geo_fences 
    ADD COLUMN patient_id BIGINT AFTER created_by,
    ADD INDEX idx_gf_patient_id (patient_id);

-- ------------------------------
-- 3. 更新存储过程
-- ------------------------------

-- 更新清理过期数据存储过程
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
    
    -- 删除已经出院30天且没有关联设备的病人记录（可选，根据业务需求调整）
    DELETE FROM patients 
    WHERE status = 'discharged' 
    AND discharge_date < DATE_SUB(NOW(), INTERVAL 30 DAY)
    AND id NOT IN (SELECT DISTINCT patient_id FROM patient_devices WHERE is_active = TRUE);
    
    -- 删除无效的病人-设备关联（设备或病人不存在）
    DELETE FROM patient_devices 
    WHERE NOT EXISTS (SELECT 1 FROM patients p WHERE p.id = patient_devices.patient_id)
    OR NOT EXISTS (SELECT 1 FROM devices d WHERE d.id = patient_devices.device_id);
    
    -- 优化表结构，减少碎片
    OPTIMIZE TABLE location_records, heartbeat_records, health_records, fence_alerts, device_alert, system_log, patients, patient_devices;
END //
DELIMITER ;

-- 更新生成定位统计汇总存储过程
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_location_summary(IN summary_date DATE)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_device_id BIGINT;
    DECLARE v_imei VARCHAR(15);
    DECLARE v_patient_id BIGINT;
    
    -- 游标声明，获取所有活跃设备及其关联的病人ID
    DECLARE device_cursor CURSOR FOR 
        SELECT d.id, d.imei, pd.patient_id 
        FROM devices d
        LEFT JOIN patient_devices pd ON d.id = pd.device_id AND pd.is_active = TRUE
        WHERE d.status = 'active';
    
    -- 游标异常处理
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 打开游标
    OPEN device_cursor;
    
    read_loop: LOOP
        -- 获取设备信息
        FETCH device_cursor INTO v_device_id, v_imei, v_patient_id;
        
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
        
        -- 更新定位记录表中的patient_id
        UPDATE location_records 
        SET patient_id = v_patient_id 
        WHERE device_id = v_device_id AND DATE(recv_time) = summary_date;
        
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
-- 4. 初始化数据
-- ------------------------------

-- 插入示例病人数据
INSERT IGNORE INTO patients (name, gender, age, id_card, ward, bed, phone, emergency_contact, emergency_phone, diagnosis, status)
VALUES 
('张三', 'male', 65, '110101196001011234', '3号楼', '301', '13800138001', '李四', '13800138002', '高血压', 'admitted'),
('李四', 'female', 58, '110101196702022345', '2号楼', '205', '13800138003', '王五', '13800138004', '糖尿病', 'admitted'),
('王五', 'male', 72, '110101195303033456', '1号楼', '102', '13800138005', '赵六', '13800138006', '冠心病', 'admitted');

-- 显示执行结果
SELECT '数据库架构更新完成' AS result;
