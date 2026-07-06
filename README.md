# 智能手环监护系统

面向护理人员和值班人员的智能手环监护平台，包含 Web 管理端、Spring Boot 后端、设备 TCP 通信服务、Android 客户端和测试手环模拟器。

系统支持设备接入、患者绑定、实时定位、历史轨迹、电子围栏、告警处置、健康数据、设备下行控制和原始协议日志查询。

## 主要功能

- **监护总览**：在线设备、待处理告警、患者分布和异常状态汇总。
- **患者与设备**：患者档案、设备状态、一对一绑定、患者详情和健康信息卡片。
- **实时定位**：GPS、Wi-Fi、LBS 定位，地图标记、聚集点区分和地址逆地理解析。
- **历史轨迹**：按设备和时间范围查询轨迹、定位起终点和查看轨迹点。
- **告警管理**：告警分级、筛选、单条处置、批量处置和声音提醒。
- **电子围栏**：圆形/多边形围栏、患者关联和越界告警。
- **健康监测**：体温、心率、血压、血氧等最新数据和历史记录。
- **设备控制**：立即定位、实时追踪、重启、关机、健康测量和自定义下行指令。
- **设备日志**：原始协议日志、状态历史、电池报告和日志导出。
- **测试工具**：批量创建测试设备/患者，模拟定位、心跳、告警和健康数据。

## 技术架构

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5.7、Spring Security、JPA/JdbcTemplate |
| 数据库 | MySQL 8 |
| Web 前端 | Vue 3、Vite 4、Element Plus、Axios、Leaflet |
| 设备通信 | TCP/Netty、IW AP/BP 协议 |
| Android | Java 17、Android SDK 36、XML、Retrofit、Room、Navigation |
| 部署 | Docker、Docker Compose、宿主机 MySQL |

### 运行端口

| 端口 | 用途 |
| --- | --- |
| `8080` | Web 页面和 REST API |
| `9000` | 智能手环 TCP 接入 |
| `9090` | TCP 调试服务，生产环境通常不对外开放 |
| `3306` | MySQL，仅建议本机访问 |

## 项目结构

```text
smart-watch/
├─ frontend/                 Vue 3 管理端
├─ android/                  Android 客户端
├─ scripts/
│  └─ generate_test_bracelets.py
├─ src/main/java/            Spring Boot 后端与设备协议服务
├─ src/main/resources/
│  ├─ static/                Web 生产构建产物
│  └─ application*.properties
├─ docs/deploy/              部署、运维、安全和排障文档
├─ docker-compose.yml
├─ Dockerfile
├─ pom.xml
└─ build.gradle
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- Node.js 18+
- MySQL 8

### 1. 准备数据库

```sql
CREATE DATABASE smart_watch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

本地启动前设置数据库环境变量：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/smart_watch?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8'
$env:DB_USERNAME='smart_user'
$env:DB_PASSWORD='<数据库密码>'
```

### 2. 启动后端

```powershell
mvn spring-boot:run
```

后端会监听：

- Web/API：`http://localhost:8080`
- 设备 TCP：`localhost:9000`

### 3. 启动前端开发服务器

```powershell
cd frontend
npm install
npm run dev
```

开发页面地址为 `http://localhost:5173`，Vite 会把 `/api` 代理到后端。

## 构建

### 完整构建

Maven 会自动安装前端依赖、构建 Vue 页面并打包 Spring Boot：

```powershell
mvn clean package -DskipTests
```

Jar 输出位置：

```text
target/demo2-<构建时间>.jar
```

运行最新 Jar：

```powershell
$jar = Get-ChildItem target\demo2-*.jar |
  Where-Object { $_.Name -notlike '*.original' } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

java -jar $jar.FullName
```

### 仅构建前端

```powershell
cd frontend
npm run build
```

生产资源会生成到：

```text
src/main/resources/static/
```

### Android

```powershell
cd android
.\gradlew.bat assembleDebug
```

Debug APK：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Docker 部署

当前 Compose 使用 `network_mode: host`，应用连接宿主机的 MySQL。

### 1. 准备环境变量

```bash
cp .env.docker .env
chmod 600 .env
```

至少修改：

```dotenv
MYSQL_DATABASE=smart_watch
MYSQL_USER=smart_user
MYSQL_PASSWORD=<数据库密码>
AMAP_WEB_KEY=<高德 WebService Key>
MPBAND_RESPONSE_KEY=<设备网络定位 SDK Key>
```

真实 `.env` 已被 Git 忽略，不应提交到仓库。

### 2. 构建并启动

```bash
docker compose up -d --build
docker compose ps
docker logs -f smart-watch-app
```

停止服务：

```bash
docker compose down
```

设备原始日志保存在 Docker 卷 `mpband_data` 中。

更完整的部署与运维说明：

- [部署指南](docs/deploy/README.md)
- [日常运维](docs/deploy/operations.md)
- [安全配置](docs/deploy/security.md)
- [故障排查](docs/deploy/troubleshooting.md)

## 地图与网络定位配置

### 地址逆地理解析

`AMAP_WEB_KEY` 用于把经纬度转换为具体地址。若未配置或 Key 权限不足，页面会退回显示经纬度。

### 手环网络定位

对于 GPS 无效、由 Wi-Fi/LBS 定位的设备：

1. 服务端通过 `MPBAND_RESPONSE_KEY` 在 `IWBP00` 登录响应中下发 SDK Key。
2. 支持网络定位的设备需首次下发一次：

   ```text
   >*networkloc@1*<
   ```

3. 设备成功启用后，`IWAP01` 报文末尾会包含：

   ```text
   [纬度@经度]
   ```

系统会把该坐标以 `source=wifi` 入库，并通过高德接口解析地址。

## 测试手环模拟器

脚本不依赖第三方 Python 包：

```powershell
python scripts\generate_test_bracelets.py gui
```

命令行模式：

```powershell
# 检查协议帧，不连接服务器
python scripts\generate_test_bracelets.py dry-run

# 创建并模拟测试设备
python scripts\generate_test_bracelets.py create-and-simulate `
  --host 127.0.0.1 `
  --username admin `
  --count 5

# 清理测试数据
python scripts\generate_test_bracelets.py cleanup --yes
```

密码可通过 `--password`、环境变量 `SMART_PASSWORD` 或交互输入提供。

## 常用 API

所有业务接口以 `/api` 开头，登录使用 Session Cookie。

| 资源 | 路径 |
| --- | --- |
| 登录 | `POST /api/auth/login` |
| 当前用户 | `GET /api/auth/me` |
| 设备 | `/api/devices` |
| 患者 | `/api/patients` |
| 患者设备关联 | `/api/patient-devices` |
| 实时/历史位置 | `/api/locations` |
| 告警 | `/api/alarms` |
| 围栏 | `/api/fences` |
| 健康数据 | `/api/health-records` |
| 下行指令与日志 | `/api/downlink` |

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1783062976,
  "error": null
}
```

## 测试

```powershell
# Maven 测试
mvn test

# Gradle 测试（跳过前端构建）
.\gradlew.bat test -PskipFrontend=true

# 指定测试类
.\gradlew.bat test `
  --tests "com.example.demo.controller.api.LocationControllerTest" `
  -PskipFrontend=true
```

提交前推荐执行：

```powershell
mvn clean package -DskipTests
```

## 常见问题

### 设备在线但没有位置

1. 在设备日志中确认收到 `IWAP01`。
2. GPS 无效时确认报文末尾是否有 `[纬度@经度]`。
3. 检查容器中的 `MPBAND_RESPONSE_KEY` 是否非空。
4. 首次接入设备需要下发网络定位开关。
5. 检查 `/api/locations/device/{id}/latest-with-amap` 返回值。

### 页面只显示经纬度

检查 `AMAP_WEB_KEY` 是否进入运行进程，以及高德 Key 是否具有逆地理编码权限。

### 设备日志不更新

检查：

```bash
docker exec smart-watch-app \
  ls -lah /app/mpband_data/devices
```

### 登录循环或接口返回 401

确认浏览器允许 Session Cookie，并检查：

```text
GET /api/auth/me
```

## 安全要求

- 不要把 `.env`、数据库密码、地图 Key 或服务器私钥提交到仓库。
- 部署后立即修改默认管理员密码。
- 生产环境仅开放必要端口：`22`、`8080`、`9000`。
- MySQL `3306` 不应直接暴露到公网。

## License

[MIT](LICENSE)
