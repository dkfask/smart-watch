# Smart Watch Monitoring System — Docker 部署

本目录提供一键容器化部署：**两条命令**（`docker compose build` + `docker compose up -d`）即可启动完整的服务栈，包含后端、前端（已嵌入 jar）、MySQL 数据库。

---

## 架构概览

```
┌─────────────────────────┐
│  宿主机 / 局域网          │
│  8080 (HTTP + Vue SPA)   │◄── 浏览器访问 http://localhost:8080
│  9000 (Netty 设备)        │◄── 手表/手环 TCP 接入
└──────────┬──────────────┘
           │
   ┌───────▼─────────────────────────────────────┐
   │ docker compose (网络: smart_net)              │
   │                                              │
   │   ┌───────────────┐      ┌────────────────┐  │
   │   │  app 容器      │◄────►│ mysql 容器      │  │
   │   │ eclipse-temurin│     │ mysql:8.0       │  │
   │   │ -jre-alpine    │     │ + schema init   │  │
   │   │ demo2.jar      │     │ + event_scheduler│  │
   │   │ (含 Vue SPA)   │     │ + 时区 +08:00   │  │
   │   └───────┬───────┘      └────────┬───────┘  │
   │           │                       │           │
   │     mpband_data 卷           mysql_data 卷    │
   └──────────────────────────────────────────────┘
```

服务编排：`docker-compose.yml`（项目根）。
所有命令均假定 **项目根目录** 是当前工作目录。

---

## 快速开始

### 1. 准备环境变量

```bash
cp .env.docker .env
# 用编辑器改密码、端口、AMAP key 等（至少改 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD）
```

### 2. 构建镜像

```bash
docker compose build
```

首次构建约 5–10 分钟（三层缓存：Node 依赖 → Gradle wrapper → 完整镜像）。后续构建会命中缓存。

### 3. 启动服务

```bash
docker compose up -d
```

观察启动状态：

```bash
docker compose ps
# 期望：
# NAME                STATUS              PORTS
# smart-watch-mysql   Up (healthy)        0.0.0.0:3306->3306/tcp
# smart-watch-app     Up (healthy)        0.0.0.0:8080->8080, 9000->9000, 9090->9090/tcp
```

健康检查时序：
- MySQL：`start_period=60s` + 健康检查，约 30–60 秒进入 `healthy`。
- App：`start_period=120s`（Gradle 启动 + Netty 绑定），约 90–180 秒进入 `healthy`。

### 4. 访问系统

打开浏览器：`http://localhost:8080`

默认账号：`admin` / `admin123`
（首次登录后会提示修改密码；也可用 `user` / `user123` 普通账号。）

---

## 验证清单

构建并启动后，按顺序执行：

```bash
# (a) 应用健康端点
curl -fsS http://localhost:8080/actuator/health
# 期望：{"status":"UP"}

# (b) 登录拿 token
curl -fsS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 期望：返回 200 + JSON {token, ...}

# (c) MySQL 表结构
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch -e "SHOW TABLES;"
# 期望：users / devices / device_status / location_records / geo_fences /
#       fence_alerts / patients / patient_devices / device_config 等

# (d) 默认用户存在
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch \
  -e "SELECT username, role FROM users;"
# 期望：admin (admin) 与 user (user)，由 SecurityDataInitializer 创建

# (e) 设备端口可达
nc -zv localhost 9000
# 或 PowerShell：Test-NetConnection localhost -Port 9000
# 期望：succeeded

# (f) 应用日志（应看到 MpbandServer / Tomcat 启动完成）
docker compose logs app | grep -E "MpbandServer|Tomcat started|Started Demo2Application"
```

---

## 常用操作

### 查看日志

```bash
# 实时跟踪两个服务
docker compose logs -f

# 只看应用最近 200 行
docker compose logs --tail=200 app
```

### 进入容器调试

```bash
# 应用容器（已装 bash、curl、tini）
docker compose exec app sh

# MySQL 客户端
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch
```

### 修改代码后重建

```bash
# 仅后端 / 前端改了：重建 app 镜像
docker compose build app
docker compose up -d app

# 清掉所有缓存（首次构建卡住时有用）
docker compose build --no-cache app
```

### 重置（保留数据卷）

```bash
docker compose down
# 数据仍在 mysql_data / mpband_data 卷里；下次 up -d 恢复。
```

### 完全销毁（清数据）

```bash
docker compose down -v
# 同时删除 mysql_data 与 mpband_data，所有数据丢失。
```

---

## 端口说明

| 端口 | 用途 | 修改方式 |
|------|------|----------|
| 8080 | HTTP API + Vue 前端 | `.env` 中 `APP_HTTP_PORT` |
| 9000 | Netty 设备通信（mpband 协议） | `.env` 中 `APP_MPBAND_PORT` |
| 9090 | TCP 回显服务（可选） | `.env` 中 `APP_TCP_PORT` |
| 3306 | MySQL（暴露给宿主机便于调试） | `.env` 中 `MYSQL_HOST_PORT`，注释 compose 可彻底关闭 |

> **重要**：如果宿主机的 9000 已被占用（如本机已经跑了一个旧版实例），`app` 容器会因 `MpbandServer` 抛 `IllegalStateException: address already in use` 而不断重启。处理方法：在 `.env` 中把 `APP_MPBAND_PORT` 改成空闲端口（如 `19000`）。

---

## 数据库是怎么初始化的？

容器首次启动时（`mysql_data` 卷为空），MySQL 官方镜像会按字典序执行 `/docker-entrypoint-initdb.d/` 下的所有 `*.sql`：

| 顺序 | 文件 | 来源 | 内容 |
|------|------|------|------|
| 00 | `00_bootstrap.sql` | 本目录手工 | `CREATE DATABASE IF NOT EXISTS smart_watch` |
| 01 | `01_full_database_schema.sql` | 项目根 `full_database_schema.sql` | 所有基础表、索引、存储过程、admin 种子 |
| 02 | `02_database_updates.sql` | 项目根 `database_updates.sql` | `patients`、`patient_devices`、相关 ALTER |
| 03 | `03_geo_fence_schema_update.sql` | 项目根 `geo_fence_schema_update.sql` | `fence_patients`、`fence_alerts` |

这些文件在 **镜像构建阶段** 通过 `Dockerfile` 拷进 `/docker-entrypoint-initdb.d/`，因此：

- **不是** bind mount（bind mount 会在 `docker compose restart` 时被反复触发，导致数据冲突）。
- 第二次启动时 MySQL **不再执行**它们（数据卷非空）。
- 想重新初始化 → `docker compose down -v` 然后 `docker compose up -d`。

`event_scheduler=ON` 与 `default-time-zone=+08:00` 通过 `docker/mysql/conf.d/my.cnf` 配置，避免在 init SQL 里写 `SET GLOBAL`（MySQL Docker 不保证一定允许）。

---

## 配置文件层级

```
src/main/resources/
├── application.properties           # 本地开发默认（不动）
└── application-docker.properties    # 容器专用（仅 SPRING_PROFILES_ACTIVE=docker 激活）

docker-compose.yml                   # 服务编排
.env.docker → .env                   # 运行时环境变量
Dockerfile                           # 三阶段构建
.dockerignore                        # 镜像构建上下文排除
scripts/docker-entrypoint.sh         # 容器入口（等待 MySQL、exec java）
docker/
├── mysql/conf.d/my.cnf              # MySQL 服务配置
├── mysql/init/*.sql                 # 首次启动的 schema 初始化
└── README.md                        # 本文件
```

**为什么用 Spring profile 而不是直接改 `application.properties`？**

`application.properties` 里的 `spring.jpa.hibernate.ddl-auto=update` 与手写 SQL schema 存在已知冲突（如 `Alarm` 实体表名 `alarms` vs SQL 表 `device_alert`）。在本地开发仍依赖 JPA 自动建表；容器内由 SQL 文件掌控 schema，因此 profile 中改为 `none`，避免 Hibernate 试图修改已建好的表。

---

## 故障排查

### 1. `app` 容器一直重启

```bash
docker compose logs app
```

| 关键日志 | 原因 | 处理 |
|---------|------|------|
| `IllegalStateException: address already in use` | 宿主机 9000 已被占用 | `.env` 改 `APP_MPBAND_PORT` |
| `Communications link failure` | MySQL 没就绪 | 等 `mysql` healthy；或 `docker compose restart app` |
| `Access denied for user 'smart_user'` | `.env` 密码与 init SQL 不一致 | 见下条 |
| `Unknown database 'smart_watch'` | MySQL 卷未初始化 | `docker compose down -v && docker compose up -d` |

### 2. 改了 `.env` 里的 `MYSQL_PASSWORD` 后登录失败

`/docker-entrypoint-initdb.d/` 里的 `01_full_database_schema.sql` 仍按 `smart123` 写入 `CREATE USER` 语句。解决方案：

- **方案 A**（推荐）：保持 `.env` 中 `MYSQL_PASSWORD=smart123`，或同步修改三份 init SQL 中的密码字符串。
- **方案 B**：`docker compose down -v` 全清后用新密码重新初始化。

### 3. 看不到 Vue 页面 / 资源 404

镜像里 frontend 阶段未成功跑 `npm run build`。重新构建并查看构建日志：

```bash
docker compose build --no-cache app 2>&1 | grep -E "vite|ERROR"
```

### 4. 设备连不上 9000

- 宿主机防火墙 / 安全组是否放行 `APP_MPBAND_PORT`。
- `docker compose ps` 中 `app` 是否 `healthy`（启动未完成时端口未真正暴露）。
- 用 `nc -zv localhost 9000` 在**宿主机**测可达性，再用 `docker compose exec app nc -zv localhost 9000` 测容器内部。

---

## 本地继续开发

容器化**不替代** `gradlew bootRun`。本地调试仍按原方式：

```bash
# 起本地 MySQL（或用 docker compose up mysql 单跑 mysql 服务）
docker compose up -d mysql

# 本地跑后端 + 前端热更新
gradlew.bat bootRun          # 后端（http://localhost:8080）
cd frontend && npm run dev   # 前端 dev server（http://localhost:5173，代理 /api → 8080）
```

`application-docker.properties` 在 `SPRING_PROFILES_ACTIVE=docker` 之外不会加载，本地开发行为与改动前完全一致。
