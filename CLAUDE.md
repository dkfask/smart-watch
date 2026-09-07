# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Smart Watch Monitoring System** is a full-stack healthcare application for real-time device tracking, patient monitoring, and alert management. It combines a Spring Boot backend with device communication protocols (Netty), a Vue 3 frontend, and integrates mapping (Leaflet), databases (MySQL), and geospatial features.

**Tech Stack:**
- **Backend**: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Netty (device communication), MySQL 8, Redis
- **Frontend**: Vue 3, Vite, Pinia (state), Axios, Leaflet (maps), Element Plus (UI)
- **Build**: Gradle (backend), npm (frontend), integrated via Gradle `npmBuild` task
- **Testing**: JUnit 5, Mockito, H2 (test DB), JaCoCo coverage (target: 80%+)

---

## Build & Development Commands

### Backend Commands

```bash
# Build full JAR (includes frontend)
gradlew.bat build -DskipTests

# Build backend only (skip frontend build)
gradlew.bat build -DskipTests -PskipFrontend=true

# Run tests with coverage report
gradlew.bat test jacocoTestReport -PskipFrontend=true
# Report: build/reports/jacoco/test/html/index.html

# Run single test class
gradlew.bat test --tests "com.example.demo.service.FenceServiceTest" -PskipFrontend=true

# Start backend dev server (auto-reload)
gradlew.bat bootRun

# Clean build artifacts
gradlew.bat clean
```

### Frontend Commands

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start dev server (with hot reload on port 5173)
npm run dev

# Build production bundle (outputs to frontend/dist)
npm run build

# Preview production build locally
npm run preview
```

### Full Stack (One Command)

```bash
# From root: build both backend and frontend, outputs JAR with embedded frontend
gradlew.bat clean build -DskipTests

# Run the JAR
java -jar build/libs/demo2.jar
# Access at http://localhost:8080
```

---

## Architecture & Code Organization

### Backend Layered Architecture

```
src/main/java/com/example/demo/
├── config/              # Spring configuration, security, WebSocket, Redis, validation, rate limiting
├── controller/api/      # REST API endpoints (Device, Location, Alarm, GeoFence, Patient, Health, etc.)
├── service/             # Business logic layer (DeviceService, FenceService, AlarmService, TrackingService, etc.)
├── repository/          # Data access (JPA + JdbcTemplate for advanced queries)
├── model/               # Entity classes (Device, Location, Patient, Alarm, Fence, HealthRecord, etc.)
│   └── dto/             # Data Transfer Objects (PageResponse, DeviceInfoDto, FenceDto, AlarmQueryCondition, etc.)
├── socket/              # Netty-based device communication (protocol parsing, frame handling)
├── util/                # Utilities (GeoUtils for distance/point-in-polygon calculations)
└── tools/               # Helpers (device simulator, protocol utilities)
```

**Key Design Patterns:**
- **Repository Pattern**: Abstract data access behind consistent interfaces (findAll, findById, create, update, delete)
- **DTO Pattern**: API contracts use DTOs (PageResponse, FenceDto, etc.) — Controllers never return raw entities
- **Service Layer**: Complex logic extracted from Controllers into Services (DeviceService, FenceService, AlarmService, TrackingService)
- **Unified Response**: All APIs return `GlobalResponseAdvice` envelope with code, message, data, timestamp

### Frontend Component Structure

```
frontend/src/
├── api/                 # Axios API clients (deviceApi, locationApi, alarmApi, fenceApi, etc.)
├── assets/              # Static files (styles.css with CSS variables, images, fonts)
├── components/          # Reusable Vue components (CommonTable, CommonForm, etc.)
├── router/              # Vue Router config (routes, lazy loading)
├── stores/              # Pinia state management (userStore, deviceStore, etc.)
├── utils/               # Helpers (handleApiResponse for pagination, error handling)
├── services/            # Business logic (data transformation, WebSocket handlers)
├── views/               # Page components (Dashboard, RealTimeTracking, Alarms, etc.)
├── App.vue              # Root layout with sidebar navigation (responsive: desktop/tablet/mobile)
└── main.js              # App entry point, plugin setup
```

**Design Direction**: Light professional theme (clean white, medical trust blue #2563EB, coral accents #FF6B6B). See DESIGN.md for full visual language, including color tokens, typography (Inter + PingFang SC), spacing, and component patterns.

---

## Key Architectural Decisions

### 1. Device Communication (Socket Layer)

The `socket/` package handles Netty-based TCP communication with GPS watches via proprietary protocols:
- **Uplink (AP protocol)**: Devices send AP00 (login), AP01 (location), AP10 (alarms), etc.
- **Downlink (BP protocol)**: Server sends BP00 (time sync), BP16 (immediate location), BP15 (location interval), etc.
- Frame format: `IW{protocol},{imei},{seq},...#{CRLF}`

Protocol parsing is in `socket/` classes; business logic (alarm creation, device status update) is triggered by Services.

### 2. Geospatial Queries

All geospatial operations (distance calculation, point-in-polygon fence detection) are centralized in `GeoUtils`. Fence alerts are triggered in `FenceService` when device location changes, not in the socket layer.

### 3. Caching Strategy

- `CacheService` interface with two implementations:
  - `DefaultCacheService`: No-op (for local dev)
  - `RedisCacheService`: Redis-backed (for production)
- Use `CacheService.getOrSet()` to prevent cache-aside pattern bugs.
- Example: `deviceStatusCache` is preheated on app startup.

### 4. Pagination Standardization

`PageResponse` wrapper standardizes pagination across all list endpoints:
- Incoming: `page`, `size` (or legacy `limit`, `offset`)
- Outgoing: `{"content": [...], "totalElements": 100, "number": 0, "size": 20, "totalPages": 5}`

Controllers call `PageResponse.from(Page<Entity>)` to avoid format drift.

### 5. Frontend State Management

- **Server state** (devices, locations, alarms): Fetched on-demand, cached in stores, invalidated on mutations
- **Client state** (UI toggles, filters): Pinia stores
- **URL state** (pagination, search): Query parameters (preserved on refresh)
- **Form state**: Vue reactive data (no external library, keep it simple)

### 6. Response Envelope

All API responses use `GlobalResponseAdvice`:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1234567890,
  "error": null  // populated on error
}
```

Frontend `handleApiResponse()` utility extracts data and handles pagination metadata.

---

## Database Schema Highlights

**Core Entities:**
- `Device`: GPS watch metadata (IMEI, battery, online status, location)
- `Location`: Time-series GPS positions (indexed by deviceId, time)
- `Patient`: Hospital patient info (name, bed, department, diagnosis)
- `PatientDevice`: M:M relation between patients and devices
- `Alarm`: Events triggered by device (fence breach, low battery, SOS, fall detection, heart rate anomaly, etc.)
- `GeoFence`: Circular or polygon boundaries (indexed by patientId for fast lookup)
- `HealthRecord`: Time-series health data (heart rate, steps, temperature)

**Indexes for Performance:**
- `(deviceId, time DESC)` on Location (historical queries)
- `(patientId)` on Patient, Alarm, GeoFence (filtering)
- `(status, createdAt DESC)` on Alarm (unread alarm queries)

**Testing Database:**
- Local dev: MySQL (configured in `application.properties`)
- Tests: H2 in-memory (MySQL-compatible mode, see `application-test.properties`)

---

## API Endpoints (Summary)

| Module | Key Endpoints |
|--------|--|
| **Auth** | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| **Devices** | `GET /api/devices`, `GET /api/devices/{id}`, `POST /api/devices`, `PUT /api/devices/{id}`, `DELETE /api/devices/{id}` |
| **Locations** | `GET /api/locations/device/{deviceId}/latest`, `GET /api/locations/device/{deviceId}/history` |
| **Patients** | `GET /api/patients`, `GET /api/patients/{id}`, `POST /api/patients`, `PUT /api/patients/{id}` |
| **Alarms** | `GET /api/alarms`, `GET /api/alarms/{id}`, `PUT /api/alarms/{id}/handle`, `GET /api/alarms/stats` |
| **GeoFences** | `GET /api/fences`, `POST /api/fences`, `PUT /api/fences/{id}`, `DELETE /api/fences/{id}` |
| **Health Records** | `GET /api/health/device/{deviceId}`, `GET /api/health/patient/{patientId}`, `GET /api/health/stats` |
| **Downlink Commands** | `POST /api/downlink/bp00` (time sync), `POST /api/downlink/bp15` (location interval), `POST /api/downlink/bp16` (immediate location) |

See README.md for full endpoint documentation.

---

## Testing & Coverage

**Minimum Coverage Target**: 80% LINE coverage

**Test Structure (AAA Pattern):**
```java
test("description of behavior", () => {
  // Arrange: set up test data
  // Act: call the method
  // Assert: verify the result
})
```

**Test Organization:**
- **Unit tests** for utilities, DTOs, protocol parsing (no DB, no HTTP)
- **Service layer tests** with Mockito mocks (repositories mocked, CacheService mocked)
- **Controller layer tests** with `@WebMvcTest` + MockMvc (integration with MockMvc)
- **Repository tests** with `@DataJpaTest` + H2 (real SQL, in-memory DB)
- **E2E tests** with Playwright for critical user flows (frontend + backend integration)

**Key Test Classes:**
- `com.example.demo.service.*Test`: ~18 test classes, ~87% LINE coverage
- `com.example.demo.controller.api.*Test`: ~11 test classes, ~80% average coverage
- `com.example.demo.util.GeoUtilsTest`: Boundary conditions, edge cases
- `com.example.demo.config.*Test`: Protocol parsing, DTO validation

**Run Tests:**
```bash
# Full test suite with coverage
gradlew.bat test jacocoTestReport -PskipFrontend=true

# View coverage report
# build/reports/jacoco/test/html/index.html
```

---

## Configuration

### Backend (src/main/resources/application.properties)

| Setting | Default | Notes |
|---------|---------|-------|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/1` | MySQL 8 connection |
| `spring.datasource.username` | `root` | Database user |
| `spring.datasource.password` | `root` | Database password |
| `app.mpband.port` | `9000` | Device communication port (Netty) |
| `app.mpband.maxFrameLength` | `4096` | Max device message size |
| `app.ai.api-key` | (set via env `AI_API_KEY`) | LLM key for the AI assistant; empty disables it (OpenAI-compatible, default DeepSeek) |
| `app.ai.base-url` / `app.ai.model` | `https://api.deepseek.com` / `deepseek-chat` | AI provider endpoint and model (env `AI_BASE_URL` / `AI_MODEL`) |
| `tianditu.key` | (set via env `TIANDITU_KEY`) | Tianditu token for Leaflet vector map and geocoding |
| `server.compression.enabled` | `true` | Gzip compression |
| `app.jwt.secret` | (set via env) | JWT signing key (never hardcode) |

**Environment Variables** (override properties):
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://prod-db:3306/smart
export SPRING_DATASOURCE_PASSWORD=secure_pwd
export APP_JWT_SECRET=your_jwt_secret
export TIANDITU_KEY=your_tianditu_key
```

### Frontend (frontend/.env)

| Variable | Default |
|----------|---------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` |
| `VITE_MAP_ACCESS_TOKEN` | (set for Leaflet tiles) |

---

## Code Conventions & Patterns

### Backend Java

1. **Constructor Injection** (not `@Autowired`):
   ```java
   public class DeviceService {
     private final DeviceRepository repo;
     public DeviceService(DeviceRepository repo) { this.repo = repo; }
   }
   ```

2. **DTO Pattern** (never expose raw entities):
   ```java
   public PageResponse<DeviceInfoDto> getDevices(...) {
     Page<Device> page = repo.findAll(pageable);
     return PageResponse.from(page, d -> new DeviceInfoDto(d.getId(), d.getImei(), ...));
   }
   ```

3. **Unified Exception Handling** (via `GlobalExceptionHandler`):
   ```java
   throw new NotFoundException("Device not found");  // auto-caught, returns 404 with envelope
   ```

4. **Immutable DTOs**: Mark with records or use `@Value` annotation (Lombok).

5. **File Organization**: Max 800 lines per file, high cohesion. Extract utilities to `util/` or `service/`.

### Frontend Vue 3

1. **Composition API** with `<script setup>`:
   ```vue
   <script setup>
   import { ref, computed } from 'vue'
   const count = ref(0)
   </script>
   ```

2. **Store Access** (Pinia):
   ```javascript
   import { useDeviceStore } from '@/stores/deviceStore'
   const devices = computed(() => useDeviceStore().devices)
   ```

3. **API Calls** (with error handling):
   ```javascript
   import { deviceApi } from '@/api/deviceApi'
   const devices = await deviceApi.getDevices().catch(err => handleApiError(err))
   ```

4. **Naming**: Components PascalCase (`DeviceList.vue`), composables `use*` (`useDeviceState.js`).

5. **No Magic Numbers**: Use CSS variables (e.g., `var(--color-primary)`) and design tokens from DESIGN.md.

---

## Common Development Tasks

### Add a New API Endpoint

1. **Create Entity** (if needed) in `model/`
2. **Create DTO** in `model/dto/`
3. **Create Repository** in `repository/` (extend JpaRepository or JdbcRepository)
4. **Create Service** in `service/` (extract business logic)
5. **Create Controller** in `controller/api/`
6. **Write Tests**: Service test (Mockito), Controller test (@WebMvcTest), Repository test (@DataJpaTest)
7. **Add Frontend API Client** in `frontend/src/api/`
8. **Add Frontend View/Component** in `frontend/src/views/` or `components/`
9. **Update Routes** in `frontend/src/router/index.js`

### Add a Device Protocol Handler

1. Add protocol parsing to `socket/` package
2. Trigger service method from protocol handler (e.g., `alarmService.createAlarm()`)
3. Write unit test for protocol parsing
4. Write integration test for end-to-end flow

### Run Production Build Locally

```bash
# Build full JAR
gradlew.bat clean build -DskipTests

# Run JAR
java -jar build/libs/demo2.jar

# Access http://localhost:8080
```

---

## Debugging & Troubleshooting

### Backend Won't Start

- **Check MySQL**: `mysql -u root -p` → verify database exists and is accessible
- **Port 8080 in use**: Change `server.port` in `application.properties`
- **Port 9000 (device comm) in use**: Change `app.mpband.port`
- **Logs**: Check `target/logs/` or console output for stack trace

### Frontend Can't Connect to Backend

- **Check CORS**: `WebMvcConfig` should whitelist `http://localhost:5173`
- **Check API base URL**: `frontend/.env` should have `VITE_API_BASE_URL=http://localhost:8080/api`
- **Proxy mismatch**: Vite dev server ≠ backend server; ensure both are running

### Tests Fail

- **H2 database issue**: Clear `build/` and rebuild
- **Mock not configured**: Check `@BeforeEach` setup in test class
- **Port conflicts**: Tests use random ports; check for lingering processes

### Device Won't Connect

- **Wrong port**: Check `app.mpband.port` matches device firmware
- **Frame delimiter mismatch**: Check `app.mpband.frameDelimiter` (usually `#`)
- **Timeout**: Increase `app.mpband.soTimeoutMillis` if connection drops

---

## Skill Routing

When the user's request matches an available skill, **ALWAYS invoke it first** using the Skill tool.

Key routing rules:
- **Product ideas, brainstorming** → `/office-hours`
- **Bugs, errors, 500 errors** → `/investigate`
- **Ship, deploy, create PR** → `/ship`
- **QA, test the site** → `/qa`
- **Code review, check my diff** → `/review`
- **Update docs after shipping** → `/document-release`
- **Design system, brand** → `/design-consultation`
- **Visual audit, polish** → `/design-review`
- **Architecture review** → `/plan-eng-review`
- **Code quality, health check** → `/health`

---

## Quick Reference

| Task | Command |
|------|---------|
| **Start backend** | `gradlew.bat bootRun` |
| **Start frontend** | `cd frontend && npm run dev` |
| **Run tests** | `gradlew.bat test jacocoTestReport -PskipFrontend=true` |
| **Build JAR** | `gradlew.bat build -DskipTests` |
| **View test report** | `build/reports/jacoco/test/html/index.html` |
| **Device simulator** | `java -cp target/classes com.example.demo.tools.BraceletClientSimulator` |
| **API docs** | See README.md (endpoint reference) |
| **Design system** | See DESIGN.md (colors, typography, components) |

---

## Docker Deployment

完整容器化部署说明：[docker/README.md](docker/README.md)。

```bash
cp .env.docker .env       # 编辑密码/端口/天地图 Token
docker compose build      # 首次构建 ~5-10 分钟
docker compose up -d      # 后台启动两个服务
docker compose ps         # 等 mysql 与 app 都进入 healthy
```

| 资源 | 端口 | 说明 |
|------|------|------|
| HTTP API + Vue SPA | 8080 | 浏览器访问 `http://localhost:8080`（默认 `admin`/`admin123`） |
| Netty 设备通信（mpband） | 9000 | 手表/手环 TCP 接入 |
| TCP 回显服务 | 9090 | 可选 |
| MySQL | 3306 | 暴露给宿主机便于调试 |

关键文件：
- `Dockerfile` — 三阶段构建（Node 20 前端 → JDK 21 后端 → JRE 21 运行时）。
- `docker-compose.yml` — `app` + `mysql` 服务编排、健康检查、命名卷。
- `src/main/resources/application-docker.properties` — 仅在 `SPRING_PROFILES_ACTIVE=docker` 时激活（`ddl-auto=none`、Actuator health 暴露、`app.mpband.saveDir` 指向挂载卷）。
- `docker/mysql/init/*.sql` — MySQL 首次启动时执行的 schema 初始化（来自仓库根的 SQL 文件）。
- `docker/mysql/conf.d/my.cnf` — `event_scheduler=ON`、时区 `+08:00`、utf8mb4。
- `scripts/docker-entrypoint.sh` — 等待 MySQL 可达后启动 jar。

容器化**不替代**本地开发：`gradlew.bat bootRun` + `npm run dev` 仍按原方式工作，`application-docker.properties` 不会影响默认 profile。
