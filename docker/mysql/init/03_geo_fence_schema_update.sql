-- MySQL Docker 的 init SQL 每个文件独立连接；显式 USE 以保证后续语句
-- 落到正确的 schema。
USE smart_watch;

-- 1. 创建围栏-病人中间表，支持一个围栏关联多个病人
CREATE TABLE IF NOT EXISTS `fence_patients` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fence_id` bigint(20) NOT NULL COMMENT '围栏ID',
  `patient_id` bigint(20) NOT NULL COMMENT '病人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fence_patient` (`fence_id`,`patient_id`),
  KEY `idx_fence_id` (`fence_id`),
  KEY `idx_patient_id` (`patient_id`),
  CONSTRAINT `fk_fence_patient_fence` FOREIGN KEY (`fence_id`) REFERENCES `geo_fences` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fence_patient_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='围栏病人关联表';

-- 2. 修改geo_fences表，添加支持多病人关联的字段（保留patient_id字段以兼容旧数据）
ALTER TABLE `geo_fences` 
  ADD COLUMN `is_multi_patient` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否支持多病人关联',
  MODIFY COLUMN `patient_id` bigint(20) NULL COMMENT '关联的病人ID（兼容旧数据）';

-- 3. 创建围栏报警记录表
CREATE TABLE IF NOT EXISTS `fence_alerts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `patient_id` bigint(20) NOT NULL COMMENT '病人ID',
  `fence_id` bigint(20) NOT NULL COMMENT '围栏ID',
  `alert_type` varchar(20) NOT NULL COMMENT '报警类型：fence_breach',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '报警状态：pending, handled, false_alarm',
  `latitude` double NOT NULL COMMENT '报警时纬度',
  `longitude` double NOT NULL COMMENT '报警时经度',
  `address` varchar(255) DEFAULT NULL COMMENT '报警时地址',
  `alert_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报警时间',
  `handled_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handled_by` bigint(20) DEFAULT NULL COMMENT '处理人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_fence_id` (`fence_id`),
  KEY `idx_status` (`status`),
  KEY `idx_alert_time` (`alert_time`),
  CONSTRAINT `fk_fence_alert_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fence_alert_patient` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fence_alert_fence` FOREIGN KEY (`fence_id`) REFERENCES `geo_fences` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='围栏报警记录表';

-- 4. 迁移旧数据：将geo_fences表中的patient_id数据迁移到fence_patients表
INSERT IGNORE INTO `fence_patients` (`fence_id`, `patient_id`) 
SELECT `id`, `patient_id` FROM `geo_fences` WHERE `patient_id` IS NOT NULL;

-- 5. 更新geo_fences表，标记支持多病人关联
UPDATE `geo_fences` SET `is_multi_patient` = 1 WHERE `id` IN (
  SELECT `fence_id` FROM `fence_patients` GROUP BY `fence_id` HAVING COUNT(*) > 1
);