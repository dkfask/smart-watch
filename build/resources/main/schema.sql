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
