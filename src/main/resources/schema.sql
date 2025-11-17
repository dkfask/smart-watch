-- 应用业务所需 users 表（与 UserRepository 字段一致）
CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100),
    password_hash VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(30),
    avatar_url    VARCHAR(255),
    created_at    TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 设备表: devices（若 Device 实体存在，但数据库中未创建时使用）
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    imei VARCHAR(15) NOT NULL UNIQUE,
    mcc VARCHAR(8),
    mnc VARCHAR(8),
    apn VARCHAR(100),
    iccid VARCHAR(64),
    imsi VARCHAR(64),
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 定位记录表（上行 AP01）
CREATE TABLE IF NOT EXISTS location_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    gps_raw VARCHAR(1024),
    extra_raw TEXT,
    latitude DOUBLE,
    longitude DOUBLE,
    speed VARCHAR(64),
    direction VARCHAR(64),
    address VARCHAR(255),
    source VARCHAR(50),
    INDEX idx_device_recv (device_id, recv_time),
    CONSTRAINT fk_loc_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 心跳/状态记录表（上行 AP03 / APJK 一部分）
CREATE TABLE IF NOT EXISTS heartbeat_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    status_block VARCHAR(255),
    counter VARCHAR(64),
    roll_count VARCHAR(64),
    work_mode VARCHAR(64),
    interval_seconds INT,
    raw_payload TEXT,
    INDEX idx_hb_device_time (device_id, recv_time),
    CONSTRAINT fk_hb_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 健康数据表（上行 APJK/APTP 等）
CREATE TABLE IF NOT EXISTS health_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    recv_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    data_type VARCHAR(32),
    value TEXT,
    extra TEXT,
    INDEX idx_health_device_time (device_id, recv_time),
    CONSTRAINT fk_health_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 下行命令记录表（记录服务端下发的 BPxx 命令与状态）
CREATE TABLE IF NOT EXISTS downlink_commands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT,
    imei VARCHAR(15),
    protocol VARCHAR(16),
    seq VARCHAR(32),
    payload TEXT,
    status VARCHAR(32),
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    response TEXT,
    retry_count INT DEFAULT 0,
    INDEX idx_down_device_status (device_id, status),
    CONSTRAINT fk_down_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 结束
