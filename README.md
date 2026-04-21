# 智能定位系统

智能定位系统是一个基于Spring Boot、Vue 3和Android的全栈应用，用于实时监控、管理和追踪设备位置，支持电子围栏、历史轨迹回放和设备通信等功能。系统包含Web管理后台和Android移动端APP。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5
- Spring Security 6
- Spring Data JPA
- MySQL 8
- Netty（设备通信）
- H2（测试用内存数据库）

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Leaflet + leaflet-draw（地图库）
- Element Plus（UI组件库）
- Inter + PingFang SC（字体）

### Android移动端

- Java 17
- Android SDK 36（minSDK 26）
- Material Design 3
- Retrofit 2.9 + OkHttp 4.12（网络请求）
- Gson（JSON解析）
- Navigation Component（页面导航）
- Room（本地数据库缓存）
- MPAndroidChart（健康数据图表）
- 高德地图SDK（地图与定位）
- ViewBinding（视图绑定）
- MVVM架构

## 核心功能

### 设备管理

- 设备注册与管理
- 设备状态监控
- 设备在线状态跟踪

### 定位功能

- 实时定位：显示所有在线设备的最新位置
- 历史轨迹：按设备和时间范围查看历史位置轨迹
- 轨迹回放：在地图上绘制并回放设备移动轨迹

### 电子围栏

- 创建/编辑/删除围栏（圆形、多边形）
- 围栏警报：设备进入/离开围栏时触发警报
- 围栏列表与管理

### 设备通信

- 支持设备上行数据解析（APxx协议）
- 支持服务器下行命令发送（BPxx协议）
- 设备心跳监控
- 设备通信日志记录

### 用户管理

- 用户注册与登录
- 权限控制
- 角色管理

### 报警管理

- 多种报警类型：围栏越界、低电量、SOS报警、跌倒报警、心率异常等
- 报警级别：紧急、警告、信息
- 报警处理流程：支持未处理、已处理、误报状态管理
- 报警历史记录和查询
- 报警统计功能：按类型、状态、时间范围统计报警数据

## 项目结构

```
├── frontend/           # 前端项目目录
│   ├── src/            # 前端源代码
│   │   ├── api/        # API接口封装
│   │   ├── assets/     # 静态资源（含全局样式 styles.css）
│   │   ├── components/ # Vue组件（CommonTable, CommonForm）
│   │   ├── router/     # 路由配置
│   │   ├── stores/     # 状态管理
│   │   ├── utils/      # 工具函数（handleApiResponse等）
│   │   ├── views/      # 页面组件
│   │   ├── App.vue     # 根组件（侧边栏布局）
│   │   └── main.js     # 应用入口
│   ├── dist/           # 构建产物
│   └── package.json    # 前端依赖
├── android/            # Android移动端项目目录
│   ├── app/            # 应用模块
│   │   └── src/main/
│   │       ├── java/com/smartwatch/monitor/
│   │       │   ├── api/        # API接口定义（Retrofit）
│   │       │   ├── data/       # Room数据库实体与DAO
│   │       │   ├── model/      # 数据模型
│   │       │   ├── repository/ # 数据仓库层
│   │       │   ├── ui/         # UI层（Fragment + Adapter）
│   │       │   │   ├── alarm/  # 报警页面
│   │       │   │   ├── fence/  # 围栏页面
│   │       │   │   ├── health/ # 健康数据页面
│   │       │   │   ├── home/   # 首页/仪表盘
│   │       │   │   ├── login/  # 登录页面
│   │       │   │   ├── patient/# 病人列表
│   │       │   │   └── realtime/# 实时定位
│   │       │   ├── utils/      # 工具类
│   │       │   └── MainActivity.java
│   │       ├── res/            # 资源文件（布局、图标、导航图）
│   │       └── AndroidManifest.xml
│   ├── build.gradle   # 项目级构建配置
│   └── settings.gradle # 仓库与模块配置
├── src/                # 后端源代码
│   ├── main/           # 主源码
│   │   ├── java/       # Java代码
│   │   │   └── com/example/demo/
│   │   │       ├── config/     # 配置类（WebConfig、GlobalResponseAdvice等）
│   │   │       ├── controller/ # 控制器
│   │   │       │   ├── api/    # REST API控制器
│   │   │       │   └──         # 页面控制器（SpaFallback等）
│   │   │       ├── model/      # 数据模型
│   │   │       │   └── dto/    # 数据传输对象（PageResponse、DeviceInfoDto等）
│   │   │       ├── repository/ # 数据访问层（JPA + JdbcTemplate）
│   │   │       ├── service/    # 业务逻辑层
│   │   │       ├── socket/     # 设备通信模块
│   │   │       ├── util/       # 工具类（GeoUtils）
│   │   │       └── tools/      # 辅助工具
│   │   └── resources/  # 资源文件
│   └── test/           # 测试代码
│       ├── java/       # 测试Java代码
│       └── resources/  # 测试配置（application-test.properties）
├── pom.xml             # Maven构建配置
└── README.md           # 项目说明文档
```

## API文档

### 统一响应格式

所有API返回统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1672531200
}
```

错误响应格式：

```json
{
  "code": 401,
  "message": "认证失败",
  "data": null,
  "timestamp": 1672531200,
  "error": {
    "type": "UnauthorizedException",
    "detail": "用户名或密码错误"
  }
}
```

### 认证相关API

| 方法   | 路径               | 功能       | 请求格式                                          | 响应格式                                                                                           |
| ---- | ---------------- | -------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| POST | /api/auth/login  | 用户登录     | `{"username": "admin", "password": "123456"}` | `{"user": {"id": 1, "username": "admin"}, "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}` |
| POST | /api/auth/logout | 用户登出     | N/A                                           | `{"logout": true}`                                                                             |
| GET  | /api/auth/me     | 获取当前用户信息 | N/A                                           | `{"id": 1, "username": "admin", "name": "管理员"}`                                                |

### 设备管理API

| 方法     | 路径                          | 功能         | 请求参数                                                                                 | 响应格式                                                                                 |
| ------ | --------------------------- | ---------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| GET    | /api/devices                | 获取设备列表     | `page`: 页码（默认0）`size`: 每页数量（默认20）`limit`: 数量（兼容参数）`offset`: 偏移量（兼容参数）`search`: 搜索关键词 | `{"content": [...], "totalElements": 100, "number": 0, "size": 20, "totalPages": 5}` |
| GET    | /api/devices/{id}           | 获取设备详情     | N/A                                                                                  | 设备详情对象                                                                               |
| GET    | /api/devices/available      | 获取可用设备列表   | N/A                                                                                  | 设备列表                                                                                 |
| GET    | /api/devices/by-imei/{imei} | 根据IMEI获取设备 | N/A                                                                                  | 设备详情对象                                                                               |
| POST   | /api/devices                | 创建新设备      | 设备对象                                                                                 | `{"id": 1}`                                                                          |
| PUT    | /api/devices/{id}           | 更新设备信息     | 设备对象                                                                                 | `{"success": true}`                                                                  |
| DELETE | /api/devices/{id}           | 删除设备       | N/A                                                                                  | `{"success": true}`                                                                  |

#### 设备信息格式

```json
{
  "id": 1,
  "imei": "123456789012345",
  "mcc": "460",
  "mnc": "01",
  "iccid": "8986011911111111111",
  "isOnline": true,
  "batteryLevel": 85,
  "lastLocationTime": "2023-01-01T12:00:00Z",
  "patient": {
    "id": 1,
    "name": "张三",
    "bedNumber": "302-1"
  }
}
```

### 位置管理API

| 方法  | 路径                                                   | 功能              | 请求参数                                             | 响应格式     |
| --- | ---------------------------------------------------- | --------------- | ------------------------------------------------ | -------- |
| GET | /api/locations/device/{deviceId}                     | 获取设备最近位置        | `limit`: 数量（默认50）`offset`: 偏移量（默认0）              | 位置列表     |
| GET | /api/locations/device/{deviceId}/latest              | 获取设备实时位置        | N/A                                              | 位置对象     |
| GET | /api/locations/device/{deviceId}/latest-with-address | 获取带地址的实时位置      | N/A                                              | 带地址的位置对象 |
| GET | /api/locations/device/{deviceId}/history             | 获取设备历史位置        | `start`: 开始时间`end`: 结束时间`page`: 页码`size`: 每页数量   | 位置列表     |
| GET | /api/locations/device/{deviceId}/range               | 获取设备历史轨迹（兼容旧接口） | `start`: 开始时间`end`: 结束时间`limit`: 数量`offset`: 偏移量 | 位置列表     |

#### 位置信息格式

```json
{
  "id": 1,
  "deviceId": 1,
  "imei": "123456789012345",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "accuracy": 10,
  "altitude": 50,
  "batteryLevel": 85,
  "source": "gps",
  "time": "2023-01-01T12:00:00Z",
  "address": "北京市东城区东华门街道天安门广场" // 仅在带地址的接口中返回
}
```

### 病人管理API

| 方法     | 路径                                 | 功能         | 请求参数                                                                                 | 响应格式                                                                                  |
| ------ | ---------------------------------- | ---------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------- |
| GET    | /api/patients                      | 获取病人列表     | `page`: 页码（默认0）`size`: 每页数量（默认20）`limit`: 数量（兼容参数）`offset`: 偏移量（兼容参数）`search`: 搜索关键词 | `{"content": [...], "totalElements": 200, "number": 0, "size": 20, "totalPages": 10}` |
| GET    | /api/patients/{id}                 | 获取病人详情     | N/A                                                                                  | 病人详情对象                                                                                |
| GET    | /api/patients/by-device/{deviceId} | 根据设备ID获取病人 | N/A                                                                                  | 病人详情对象                                                                                |
| POST   | /api/patients                      | 创建新病人      | 病人对象                                                                                 | 病人详情对象                                                                                |
| PUT    | /api/patients/{id}                 | 更新病人信息     | 病人对象                                                                                 | 病人详情对象                                                                                |
| DELETE | /api/patients/{id}                 | 删除病人       | N/A                                                                                  | `{"success": true}`                                                                   |

#### 病人信息格式

```json
{
  "id": 1,
  "name": "张三",
  "age": 65,
  "gender": "male",
  "idCard": "110101195801011234",
  "bedNumber": "302-1",
  "department": "神经内科",
  "diagnosis": "高血压",
  "devices": [
    {
      "id": 1,
      "imei": "123456789012345",
      "isOnline": true,
      "lastLocation": {
        "latitude": 39.9042,
        "longitude": 116.4074,
        "time": "2023-01-01T12:00:00Z"
      }
    }
  ]
}
```

### 病人设备关联API

| 方法     | 路径                                          | 功能       | 请求格式                                                         | 响应格式                |
| ------ | ------------------------------------------- | -------- | ------------------------------------------------------------ | ------------------- |
| POST   | /api/patient-devices                        | 关联设备和病人  | `{"patientId": 1, "deviceId": 1, "relationship": "wearing"}` | 关联对象                |
| DELETE | /api/patient-devices                        | 解除设备关联   | `patientId`: 病人ID`deviceId`: 设备ID                            | `{"success": true}` |
| GET    | /api/patient-devices/by-patient/{patientId} | 获取病人关联设备 | N/A                                                          | 关联列表                |
| GET    | /api/patient-devices/by-device/{deviceId}   | 获取设备关联病人 | N/A                                                          | 关联列表                |

### 报警管理API

| 方法  | 路径                              | 功能       | 请求参数                                                                                                                    | 响应格式                |
| --- | ------------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------- | ------------------- |
| GET | /api/alarms                     | 获取报警列表   | `page`: 页码（默认0）`size`: 每页数量（默认20）`deviceId`: 设备ID（可选）`patientId`: 病人ID（可选）`status`: 状态（可选，pending/handled/false\_alarm） | 分页报警列表              |
| GET | /api/alarms/{id}                | 获取报警详情   | N/A                                                                                                                     | 报警详情对象              |
| GET | /api/alarms/device/{deviceId}   | 按设备获取报警  | `page`: 页码（默认0）`size`: 每页数量（默认20）`status`: 状态（可选）                                                                       | 分页报警列表              |
| GET | /api/alarms/patient/{patientId} | 按病人获取报警  | `page`: 页码（默认0）`size`: 每页数量（默认20）`status`: 状态（可选）                                                                       | 分页报警列表              |
| PUT | /api/alarms/{id}/read           | 标记报警为已读  | `read`: 是否已读（默认true）                                                                                                    | `{"success": true}` |
| PUT | /api/alarms/{id}/handle         | 处理报警     | JSON Body: `{"status": "handled", "result": "处理结果", "remark": "处理备注"}`                                                  | `{"success": true}` |
| GET | /api/alarms/stats               | 获取报警统计   | N/A                                                                                                                     | 报警统计数据              |
| GET | /api/alarms/unread-count        | 获取未读报警数量 | `deviceId`: 设备ID（可选）`patientId`: 病人ID（可选）                                                                               | `{"count": 5}`      |
| GET | /api/alarms/recent              | 获取最近报警   | `limit`: 数量（默认10）                                                                                                       | 最近报警列表              |

#### 报警信息格式

```json
{
  "id": 1,
  "deviceId": 1,
  "patientId": 1,
  "alarmType": "fence_breach",
  "alarmLevel": "critical",
  "alarmData": "{\"fenceId\": 1, \"action\": \"exit\"}",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "address": "北京市东城区东华门街道天安门广场",
  "triggeredTime": "2023-01-01T12:00:00Z",
  "status": "pending",
  "isRead": false,
  "createdAt": "2023-01-01T12:00:00Z"
}
```

### 实时数据推送API

使用WebSocket进行实时数据推送，连接地址：`ws://{server}/ws`

#### 推送数据类型

| 类型                     | 说明     | 数据格式                                                                                                                                                                                                                                                  |
| ---------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| device\_status\_update | 设备状态更新 | `{"deviceId": 1, "imei": "123456789012345", "isOnline": true, "batteryLevel": 85}`                                                                                                                                                                    |
| location\_update       | 位置更新   | `{"deviceId": 1, "imei": "123456789012345", "latitude": 39.9042, "longitude": 116.4074, "time": "2023-01-01T12:00:00Z"}`                                                                                                                              |
| alarm                  | 告警信息   | `{"id": 1, "deviceId": 1, "imei": "123456789012345", "alarmType": "fence_breach", "alarmLevel": "critical", "latitude": 39.9042, "longitude": 116.4074, "address": "北京市东城区东华门街道天安门广场", "triggeredTime": "2023-01-01T12:00:00Z", "status": "pending"}` |

### 电子围栏API

| 方法     | 路径                            | 功能         | 请求参数                                                                     | 响应格式                |
| ------ | ----------------------------- | ---------- | ------------------------------------------------------------------------ | ------------------- |
| GET    | /api/fences                   | 获取围栏列表     | N/A                                                                      | 围栏列表（含patientIds）   |
| GET    | /api/fences/active            | 获取活跃围栏     | N/A                                                                      | 活跃围栏列表              |
| GET    | /api/fences/by-user/{userId}  | 按用户获取围栏    | N/A                                                                      | 围栏列表                |
| GET    | /api/fences/{id}              | 获取围栏详情     | N/A                                                                      | 围栏详情对象（含patientIds） |
| POST   | /api/fences                   | 创建新围栏      | JSON Body（FenceCreateRequest）                                            | `{"id": 1}`         |
| PUT    | /api/fences/{id}              | 更新围栏信息     | JSON Body（FenceUpdateRequest）                                            | `{"success": true}` |
| DELETE | /api/fences/{id}              | 删除围栏       | N/A                                                                      | 204 No Content      |
| GET    | /api/fences/alerts            | 获取围栏警报列表   | `page`: 页码（默认0）`size`: 每页数量（默认20）`fenceId`: 围栏ID（可选）`deviceId`: 设备ID（可选） | 分页警报列表              |
| GET    | /api/fences/device/{deviceId} | 获取设备关联的围栏  | N/A                                                                      | 围栏列表                |
| POST   | /api/fences/{fenceId}/devices | 关联设备到围栏    | `{"deviceIds": [1, 2, 3]}`                                               | `{"success": true}` |
| DELETE | /api/fences/{fenceId}/devices | 解除设备与围栏的关联 | `deviceIds`: 设备ID列表                                                      | `{"success": true}` |

#### 围栏信息格式

```json
{
  "id": 1,
  "name": "医院围栏",
  "type": "circle", // circle 或 polygon
  "radius": 100, // 圆形围栏半径（米）
  "center": {
    "latitude": 39.9042,
    "longitude": 116.4074
  },
  "points": [ // 多边形围栏顶点
    {"latitude": 39.9042, "longitude": 116.4074},
    {"latitude": 39.9052, "longitude": 116.4084},
    {"latitude": 39.9062, "longitude": 116.4074},
    {"latitude": 39.9052, "longitude": 116.4064}
  ],
  "status": "active", // active 或 inactive
  "createdAt": "2023-01-01T12:00:00Z",
  "updatedAt": "2023-01-01T12:00:00Z",
  "devices": [ // 关联的设备
    {
      "id": 1,
      "imei": "123456789012345",
      "name": "老人手表 1"
    }
  ]
}
```

### 下行命令API

| 方法   | 路径                   | 功能              | 请求参数                                                                                        | 响应格式                                                                                                 |
| ---- | -------------------- | --------------- | ------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| GET  | /api/downlink/online | 获取在线设备IMEI列表    | N/A                                                                                         | `{"imeis": ["123456789012345", "987654321098765"]}`                                                  |
| POST | /api/downlink/bp00   | 发送BP00授时命令      | `imei`: 设备IMEI`seq`: 序列号（默认1）                                                               | `{"success": true, "command": "IWBP00,123456789012345,012345,20230101120000#"}`                      |
| POST | /api/downlink/bp12   | 发送BP12设置SOS命令   | `imei`: 设备IMEI`seq`: 序列号（默认1）`sos1`: SOS号码1`sos2`: SOS号码2（可选）`sos3`: SOS号码3（可选）             | `{"success": true, "command": "IWBP12,123456789012345,012345,13800138000,13900139000,13700137000#"}` |
| POST | /api/downlink/bp15   | 发送BP15定位间隔设置命令  | `imei`: 设备IMEI`seq`: 序列号（默认1）`interval`: 定位间隔（秒）                                            | `{"success": true, "command": "IWBP15,123456789012345,012345,60#"}`                                  |
| POST | /api/downlink/bp16   | 发送BP16立即定位命令    | `imei`: 设备IMEI`seq`: 序列号（默认1）                                                               | `{"success": true, "command": "IWBP16,123456789012345,012345#"}`                                     |
| POST | /api/downlink/bp19   | 发送BP19设置服务器信息命令 | `imei`: 设备IMEI`seq`: 序列号（默认1）`domainFlag`: 域名标志（0=IP，1=域名）`hostOrIp`: 服务器IP或域名`port`: 服务器端口 | `{"success": true, "command": "IWBP19,123456789012345,012345,0,192.168.1.1,9000#"}`                  |
| POST | /api/downlink/custom | 发送自定义下行命令       | `imei`: 设备IMEI`command`: 自定义命令内容（不含IW前缀和#后缀）                                                | `{"success": true, "command": "IWBP00,123456789012345,012345,20230101120000#"}`                      |

### 健康监测API

| 方法  | 路径                                     | 功能         | 请求参数                                                                                              | 响应格式     |
| --- | -------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------- | -------- |
| GET | /api/health/device/{deviceId}          | 获取设备健康数据   | `start`: 开始时间`end`: 结束时间`type`: 数据类型（heartRate/step/temperature）`page`: 页码（默认0）`size`: 每页数量（默认20） | 分页健康数据列表 |
| GET | /api/health/device/{deviceId}/latest   | 获取设备最新健康数据 | `type`: 数据类型（heartRate/step/temperature）                                                          | 最新健康数据对象 |
| GET | /api/health/patient/{patientId}        | 获取病人健康数据   | `start`: 开始时间`end`: 结束时间`type`: 数据类型（heartRate/step/temperature）`page`: 页码（默认0）`size`: 每页数量（默认20） | 分页健康数据列表 |
| GET | /api/health/patient/{patientId}/latest | 获取病人最新健康数据 | `type`: 数据类型（heartRate/step/temperature）                                                          | 最新健康数据对象 |
| GET | /api/health/stats                      | 获取健康数据统计   | `deviceId`: 设备ID（可选）`patientId`: 病人ID（可选）`start`: 开始时间`end`: 结束时间                                 | 健康数据统计对象 |

#### 健康数据格式

```json
{
  "id": 1,
  "deviceId": 1,
  "patientId": 1,
  "type": "heartRate", // heartRate, step, temperature
  "value": 75, // 心率（次/分钟）、步数、体温（摄氏度）
  "unit": "bpm", // bpm（心率）、步（步数）、℃（体温）
  "time": "2023-01-01T12:00:00Z",
  "createdAt": "2023-01-01T12:00:00Z"
}
```

## 快速开始

### 环境要求

- Java 21（后端）/ Java 17（Android）
- Node.js 16+
- MySQL 8+
- Maven 3.8+
- Android SDK（compileSdk 36，minSdk 26）
- Gradle 8.9（Android项目自带wrapper）

### 后端启动

1. 配置数据库连接
   - 修改 `src/main/resources/application.properties` 中的数据库配置
2. 启动后端服务
   ```bash
   # 使用Maven（推荐）
   mvn spring-boot:run

   # 使用Gradle
   gradlew.bat bootRun
   ```
3. 后端服务默认运行在 `http://localhost:8080`

### 前端启动

1. 进入前端目录
   ```bash
   cd frontend
   ```
2. 安装依赖
   ```bash
   npm install
   ```
3. 启动开发服务器
   ```bash
   npm run dev
   ```
4. 前端服务默认运行在 `http://localhost:5173`

### Android端构建

1. 进入Android项目目录
   ```bash
   cd android
   ```
2. 配置后端API地址
   - 修改 `app/src/main/java/com/smartwatch/monitor/api/ApiClient.java` 中的 `BASE_URL`
3. 构建Debug APK
   ```bash
   gradlew.bat assembleDebug
   ```
4. 构建产物
   - APK：`android/app/build/outputs/apk/debug/app-debug.apk`
5. 安装到设备
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

#### Android端功能

- 登录/登出（Session Cookie认证）
- 首页仪表盘（设备在线统计、未处理报警、最近报警）
- 实时定位（高德地图显示设备位置）
- 病人列表与搜索
- 围栏管理（地图绘制圆形/多边形围栏）
- 健康数据（心率/步数/体温趋势图表）
- 报警列表与处理
- WebSocket实时推送
- Room本地数据缓存

### 构建生产版本

1. 构建前端
   ```bash
   cd frontend
   npm run build
   ```
2. 构建后端
   ```bash
   # 使用Maven（推荐）
   mvn -DskipTests package

   # 跳过前端构建（仅后端）
   mvn -DskipTests -DskipFrontend=true package
   ```
3. 构建产物
   - 前端：`frontend/dist`
   - 后端：`target/demo2.jar`

### 一键构建（推荐）

```bash
# 使用Maven构建，包含前端构建
mvn clean package -DskipTests
```

## 配置说明

### 后端配置

主要配置文件：`src/main/resources/application.properties`

| 配置项                                   | 说明        | 默认值                                            |
| ------------------------------------- | --------- | ---------------------------------------------- |
| spring.datasource.url                 | 数据库连接URL  | jdbc:mysql://localhost:3306/1                  |
| spring.datasource.username            | 数据库用户名    | root                                           |
| spring.datasource.password            | 数据库密码     | root                                           |
| server.compression.enabled            | Gzip压缩开关  | true                                           |
| server.compression.mime-types         | 压缩的MIME类型 | application/json,text/html,text/xml,text/plain |
| server.compression.min-response-size  | 最小压缩响应大小  | 1024                                           |
| app.mpband.port                       | 设备通信端口    | 9000                                           |
| app.mpband.useDelimiterReader         | 是否使用分隔符读取 | true                                           |
| app.mpband.frameDelimiter             | 帧结束符      | #                                              |
| app.mpband.appendNewlineAfterResponse | 下行追加CRLF  | true                                           |
| app.mpband.soTimeoutMillis            | 读超时时间     | 180000                                         |
| app.mpband.maxFrameLength             | 单帧最大长度    | 4096                                           |
| app.mpband.saveDir                    | 日志保存目录    | mpband\_data                                   |

#### 静态资源缓存

`WebConfig` 配置了前端静态资源的缓存策略：

- `/assets/**`：缓存365天（含hash指纹，长期缓存）
- 其他静态资源：缓存1小时

#### 测试配置

`src/test/resources/application-test.properties` 配置了测试环境：

- H2内存数据库（MySQL兼容模式）
- 排除Redis自动配置
- 禁用外部API调用

### 前端配置

主要配置文件：`frontend/.env`

| 配置项                  | 说明         | 默认值                         |
| -------------------- | ---------- | --------------------------- |
| VITE\_API\_BASE\_URL | 后端API基础URL | <http://localhost:8080/api> |

## 设备通信协议

### 上行协议（设备→服务器）

| 协议号  | 名称      | 功能描述                         |
| ---- | ------- | ---------------------------- |
| AP00 | 登录包     | 设备登录到服务器，上报IMEI等基本信息         |
| AP01 | 定位数据包   | 上报设备当前位置信息，包含GPS、LBS和Wi-Fi数据 |
| AP02 | 健康包     | 上报设备健康数据，如心率、步数等             |
| AP03 | 心跳/状态包  | 定期上报设备状态，包含GSM信号、卫星数、计步等信息   |
| AP04 | 低电报警    | 当设备电量过低时上报报警信息               |
| AP10 | 报警与地址回复 | 上报各种报警信息，包含位置和状态数据           |
| AP42 | 图片分包    | 上报设备拍摄的图片数据，采用分包传输           |
| APJK | 健康数据    | 上报设备健康监测数据                   |
| APTP | 体温数据    | 上报设备监测的体温数据                  |

### 下行协议（服务器→设备）

| 协议号  | 名称        | 功能描述                       |
| ---- | --------- | -------------------------- |
| BP00 | 设置时区      | 向设备发送当前时间和时区信息             |
| BP12 | 设置SOS号码   | 配置最多3个SOS紧急联系人号码           |
| BP14 | 设置联系人白名单  | 配置最多10个联系人，名称使用UNICODE编码   |
| BP15 | 定位间隔设置    | 设置设备定位数据上报的时间间隔（秒）         |
| BP16 | 立即定位指令    | 命令设备立即上报当前位置信息             |
| BP17 | 恢复出厂设置    | 将设备恢复到出厂默认设置               |
| BP18 | 重启设备      | 命令设备重启                     |
| BP19 | 设置服务器信息   | 配置设备连接的服务器地址和端口            |
| BP20 | 设置设备语言与时区 | 配置设备语言和时区                  |
| BP31 | 关机        | 命令设备关机                     |
| BP32 | 拨打电话      | 命令设备拨打指定电话号码               |
| BP33 | 工作模式      | 设置设备的工作模式                  |
| BP34 | 自定义定位模式   | 配置设备的自定义定位模式               |
| BP40 | 快捷指令下发    | 发送快捷指令，内容使用GB2312编码后转为十六进制 |
| BP46 | 立即拍照      | 命令设备立即拍摄照片                 |
| BP50 | 心跳检测指令    | 下发心跳检测指令，检查设备连接状态          |
| BP51 | 下发电话本（单条） | 向设备添加单条电话本记录               |
| BP52 | 删除电话本（单条） | 从设备删除单条电话本记录               |
| BP84 | 白名单开关     | 开启或关闭设备的白名单功能              |
| BP86 | 健康监测间隔设置  | 设置设备健康数据上报的时间间隔            |
| BP88 | 寻找设备      | 命令设备发出声响，便于寻找设备            |

## 开发指南

### 后端开发

1. 新增API：在 `controller/api/` 目录下创建新的控制器类
2. 新增数据模型：在 `model/` 目录下创建新的实体类
3. 新增DTO：在 `model/dto/` 目录下创建数据传输对象
4. 新增数据访问：在 `repository/` 目录下创建新的Repository接口
5. 新增业务逻辑：在 `service/` 目录下创建新的Service类
6. 新增工具类：在 `util/` 目录下创建工具类

#### 开发规范

- **构造器注入**：使用构造器注入代替 `@Autowired` 字段注入
- **DTO模式**：Controller层使用DTO（数据传输对象）代替直接返回实体类，避免暴露内部字段
- **分页统一**：使用 `PageResponse.from(Page)` 统一分页响应格式
- **缓存规范**：使用 `CacheService.getOrSet()` 实现缓存穿透保护
- **Service提取**：复杂业务逻辑从Controller提取到Service层，保持Controller简洁
- **GeoUtils工具**：地理计算使用 `GeoUtils` 工具类，不要在业务代码中直接计算

### 前端开发

1. 新增页面：在 `views/` 目录下创建新的Vue组件
2. 新增路由：在 `router/index.js` 中配置新的路由
3. 新增API封装：在 `api/` 目录下创建新的API文件
4. 新增状态管理：在 `stores/` 目录下创建新的Store
5. 新增组件：在 `components/` 目录下创建新的Vue组件

## 测试说明

### 测试框架

- **JUnit 5**：测试框架
- **Mockito**：Mock框架，用于Service层和Repository层单元测试
- **@WebMvcTest**：Controller层集成测试（MockMvc）
- **@DataJpaTest**：JPA Repository层集成测试（H2内存数据库）
- **H2**：内存数据库，用于测试环境（MySQL兼容模式）
- **JaCoCo**：测试覆盖率报告

### 测试覆盖率

| 维度 | 覆盖率 |
|------|--------|
| 项目整体 LINE | 42.3% |
| Service层 LINE | ~87% |
| Controller/API层 LINE | ~80% |

### 测试覆盖

项目共有 **317个测试用例**，0个失败，覆盖以下层次：

| 层次              | 测试类                                      | 测试数 | LINE覆盖率 |
| --------------- | ---------------------------------------- | --- | --------- |
| **工具类**         | GeoUtilsTest                             | 11  | —         |
| **DTO**         | DtoTest                                  | 14  | —         |
| **协议解析**        | ProtocolParserTest, LocationProtocolTest | 15  | —         |
| **Service层**    | FenceServiceTest                         | 18  | 85.7%     |
| <br />          | AlarmServiceTest                         | 17  | 95.5%     |
| <br />          | HealthMonitorServiceTest                 | 17  | 86.7%     |
| <br />          | RedisCacheServiceTest                    | 20  | 68.8%     |
| <br />          | AmapLocationServiceTest                  | 17  | 95.5%     |
| <br />          | DeviceServiceTest                        | 13  | 83.5%     |
| <br />          | DeviceStatusServiceTest                  | 12  | 98.3%     |
| <br />          | DefaultCacheServiceTest                  | 8   | 100%      |
| <br />          | MySqlServiceTest                         | 7   | 100%      |
| <br />          | AuthServiceTest                          | 5   | 100%      |
| <br />          | AuthUserDetailsServiceTest               | 5   | 76.3%     |
| <br />          | TrackingServiceTest                      | 4   | 95.8%     |
| <br />          | CacheServiceTest                         | 2   | 100%      |
| **Controller层** | AlarmControllerTest                      | 11  | ~80%      |
| <br />          | DeviceControllerTest                     | 10  | (api包整体)  |
| <br />          | GeoFenceControllerTest                   | 10  | (api包整体)  |
| <br />          | PatientControllerTest                    | 6   | (api包整体)  |
| <br />          | LocationControllerTest                   | 7   | (api包整体)  |
| <br />          | UserControllerTest                       | 5   | (api包整体)  |
| <br />          | PatientDeviceControllerTest              | 4   | (api包整体)  |
| <br />          | UserDeviceControllerTest                 | 4   | (api包整体)  |
| <br />          | AuthApiControllerTest                    | 3   | (api包整体)  |
| <br />          | DeviceStatusControllerTest               | 3   | (api包整体)  |
| <br />          | FenceAlertControllerTest                 | 3   | (api包整体)  |
| **Repository层** | JpaRepositoryTest                        | 6   | —         |
| <br />          | JdbcRepositoryTest                       | 6   | —         |

### 运行测试

```bash
# 运行全部测试（跳过前端构建）
gradlew.bat test -PskipFrontend=true

# 运行单个测试类
gradlew.bat test --tests "com.example.demo.service.FenceServiceTest" -PskipFrontend=true

# 生成测试报告+覆盖率
gradlew.bat test jacocoTestReport -PskipFrontend=true
# 报告位置：build/reports/jacoco/test/html/index.html
```

### 测试配置

测试环境使用 `src/test/resources/application-test.properties` 配置：

- H2内存数据库（MySQL兼容模式）
- 排除Redis自动配置
- 禁用外部API调用

### 设备模拟器

项目提供了Java版本的设备模拟器，用于测试设备通信：

```bash
# 运行模拟器
java -cp target/classes com.example.demo.tools.BraceletClientSimulator
```

### API测试

使用curl或Postman测试API：

```bash
# 测试设备列表API
curl http://localhost:8080/api/devices

# 测试登录API
curl -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' http://localhost:8080/api/auth/login
```

## 部署说明

### 单机部署

1. 构建完整的jar包
   ```bash
   mvn clean package -DskipTests
   ```
2. 运行jar包
   ```bash
   java -jar target\demo2.jar
   ```
3. 访问应用
   ```
   http://localhost:8080
   ```

### Docker部署

（待实现）

## 常见问题

### 设备反复重连

- 检查设备通信端口是否正确
- 检查 `app.mpband.useDelimiterReader` 和 `app.mpband.appendNewlineAfterResponse` 配置
- 检查设备固件是否期待帧末尾带换行（CRLF）

### 前端无法访问后端API

- 检查后端服务是否正常运行
- 检查前端 `.env` 配置中的 `VITE_API_BASE_URL` 是否正确
- 检查后端CORS配置是否允许前端域名访问

### 地图无法显示

- 检查Leaflet地图库是否正确加载
- 检查地图API密钥是否配置正确
- 检查网络连接是否正常

## 许可证

MIT License

## 联系方式

（待补充）

## 更新日志

### v3.0.0 - 2026-04-21 Android移动端APP开发

新增Android原生移动端APP，实现与Web后台功能对齐的移动端体验。

#### Android端架构

- **MVVM架构**：Repository层封装数据源（API + Room本地缓存），ViewModel管理UI状态
- **网络层**：Retrofit 2.9 + OkHttp 4.12，ApiResponse自动解包拦截器，CookieJar Session管理
- **地图**：高德地图SDK集成，支持围栏绘制与设备定位
- **图表**：MPAndroidChart展示心率/步数/体温趋势
- **导航**：Navigation Component管理Fragment路由
- **离线缓存**：Room数据库缓存报警、健康数据等

#### Android端功能

- 登录/登出（Session Cookie认证，401自动跳转登录页）
- 首页仪表盘（在线/离线/报警设备统计，未处理报警红色横幅）
- 实时定位（高德地图标记设备位置，点击查看详情）
- 病人列表（搜索、分页加载）
- 围栏管理（地图绘制圆形/多边形围栏，关联设备）
- 健康数据（心率/步数/体温折线图，时间范围筛选）
- 报警列表（按状态筛选，快捷处理）

#### 后端修复

- **PatientDeviceController**：unbind方法添加@Transactional事务注解
- **PatientDevice模型**：添加@JsonIgnore解决循环引用序列化问题
- **SecurityDataInitializer**：修复admin用户默认角色为"admin"而非"user"
- **FenceService**：创建围栏时设置默认状态为active

#### 前端修复

- **Fence.vue**：集成leaflet-draw实现地图绘制围栏，自动填充坐标；调整布局为列表33%+地图66%
- **auth.js**：移除DEV模式mock登录，始终调用API获取Session
- **axios.js**：添加withCredentials: true确保Cookie传递

#### 构建配置

- Android项目：AGP 8.7.3 + Gradle 8.9 + compileSdk 36
- 使用腾讯镜像加速Gradle和Maven依赖下载
- ProGuard规则：保留Model类和AMap SDK

### v2.2.0 - 2026-04-19 测试覆盖率大幅提升

从189个测试用例扩展到317个，Service层覆盖率从~20%提升至~87%，项目整体LINE覆盖率提升至42.3%。

#### 阶段一：Service层测试补全

- **新增 HealthMonitorServiceTest(17)**：覆盖血压/心率/血氧/体温异常检测、报警创建、边界值、无效输入等场景，覆盖率86.7%
- **新增 DefaultCacheServiceTest(8)**：覆盖no-op缓存实现全部方法，覆盖率100%
- **新增 RedisCacheServiceTest(20)**：覆盖Redis缓存set/get/delete/deleteByPattern/exists/expire/getExpire/increment/setIfAbsent/clear/getKey/getHitRate/preheat，覆盖率68.8%
- **新增 MySqlServiceTest(7)**：覆盖SQL查询testConnection/query（含参数/无参数/异常），覆盖率100%
- **扩展 DeviceStatusServiceTest(4→12)**：新增缓存未命中DB查询、无效设备ID、预热缓存等测试，覆盖率45.8%→98.3%
- **扩展 AmapLocationServiceTest(3→17)**：新增mock RestTemplate测试逆地理编码/地址转坐标/POI搜索（含成功/失败/异常/null场景），覆盖率10.7%→95.5%

#### 阶段二：Controller层测试

- **扩展 GeoFenceControllerTest(9→10)**：新增DELETE不存在围栏返回404测试

#### 构建变更

- build.gradle新增 `spring-boot-starter-data-jpa`、`spring-boot-starter-websocket`、`io.micrometer:micrometer-core`、`com.h2database:h2`、`jacoco`插件
- Demo2ApplicationTests添加 `@Disabled`（需MySQL/Redis基础设施）
- 测试命令从Maven迁移到Gradle：`gradlew.bat test jacocoTestReport -PskipFrontend=true`

### v2.1.0 - 2026-04-13 浅色专业主题重构

从暗色赛博朋克主题全面迁移到浅色专业医疗主题，提升系统专业感和可读性。

#### 设计系统

- **新增 DESIGN.md**：定义完整的视觉语言，包含色板、排版、组件、布局、阴影、响应式等 9 个维度
- **设计灵感**：Airbnb（友好圆润）+ Sentry（监控仪表盘）+ Revolut（精准数据）

#### 主题变更

- **色彩**：霓虹青 `#00d4ff` → 医疗信任蓝 `#2563EB`；紫色渐变 CTA → 珊瑚色 `#FF6B6B`；深黑背景 `#0f1419` → 暖白 `#F8FAFC`
- **排版**：Arial → Inter + PingFang SC + Microsoft YaHei 字体栈；JetBrains Mono 用于设备 ID 和时间戳；中文最低 14px
- **圆角**：4-12px → 8-24px（更圆润友好的视觉风格）
- **阴影**：大面积黑色阴影+neon glow → 微妙 elevation 1-5 层级
- **健康指标专属色**：心率红、血压紫、血氧蓝、体温琥珀、步数绿

#### 布局重构

- **导航栏**：顶部水平导航 → 左侧 240px 可折叠侧边栏（平板自动折叠至 64px 图标模式，手机隐藏）
- **内容区域**：独立顶部栏显示页面标题和用户信息
- **响应式**：Desktop ≥1280px / Tablet 768-1279px / Mobile <768px 三档断点

#### 组件优化

- **卡片**：白底 16px 圆角，微妙 elevation 阴影，hover 时仅提升阴影层级（不再使用 translateY 位移）
- **表格**：新增 zebra striping，清洁 `#F1F5F9` 表头，hover 高亮
- **标签/徽章**：pill 形状（border-radius 20px），按严重性着色
- **输入框**：12px 圆角，蓝色 focus ring，明确的 error/error 状态
- **搜索框**：pill 形状（border-radius 24px）
- **状态指示器**：在线/离线/报警使用动画圆点（脉冲呼吸效果），而非纯文字标签
- **报警列表项**：左侧 4px 严重性色条（红=critical，琥珀=warning，蓝=info）

#### Element Plus 主题覆盖

- 完整覆盖 Element Plus CSS 变量以匹配设计系统
- 主色 `#2563EB`，危险色 `#EF4444`，成功色 `#10B981`，警告色 `#F59E0B`
- 圆角 12px，字体 Inter + PingFang SC

### v2.0.0 - 2026-04-11 医院用户视角优化

从医院护士/医生用户角度进行全方位优化，让系统更贴合临床使用场景。

#### 关键体验修复

- **Dashboard仪表盘改造**：热力图和轨迹日历的模拟数据替换为真实API数据；新增未处理报警红色闪烁横幅（可点击跳转）；新增病区概览卡片（按病区统计在线/离线/报警数）；设备列表显示病人姓名+病房+床位；地图标记显示病人姓名
- **报警页面优化**：报警列表默认显示病人姓名+病房（而非IMEI号）；新增声音提醒（Web Audio API）；新增浏览器推送通知（Notification API）；新增快捷处理选项（已通知医生/病人已返回/误报/需进一步处理）；超过15分钟未处理的报警自动高亮为红色超时状态；每30秒自动刷新
- **新增病人详情页**：整合基本信息+最新位置地图+最近报警+关联围栏；路由 `/patients/:id`
- **病人列表优化**：新增病房筛选下拉框；新增监控状态列；新增详情按钮跳转

#### 功能补全

- **新增健康数据API**：`HealthRecordController` 提供 `/api/health-records`、`/api/health-records/latest`、`/api/health-records/stats` 端点；`HealthRecordRepository` 新增按病人、按类型、按时间范围查询方法
- **新增围栏管理页面**：`Fence.vue` 支持围栏列表查看、创建、编辑、删除；围栏关联病人支持搜索选择
- **新增位置API**：`locationApi.getLatestLocation()` 获取设备最新位置

#### 体验优化

- **实时定位页面**：设备列表显示病人姓名（而非IMEI）；搜索支持病人姓名+IMEI双维度；地图标记显示病人姓名首字
- **历史轨迹页面**：轨迹点优先显示地址（而非经纬度坐标）；新增快捷时间选择按钮（今天/昨天/最近7天）
- **设备列表页面**：隐藏MCC、MNC、APN、ICCID、IMSI等技术参数；新增电量列

### v1.5.0 - 2026-04-11 完全测试

从75个测试用例扩展到189个，实现全层覆盖。

### v1.4.0 - 2026-04-11 前后端适配与加载速度优化

- **前后端API适配**：修复分页格式不统一（统一为PageResponse）；修复handleApiResponse丢失分页元数据；设备/病人Controller支持limit/offset兼容参数；AlarmController.handle改为@RequestBody；LocationController使用真实count查询
- **加载速度优化**：启用Gzip压缩；新增WebConfig静态资源缓存（assets 365天，其他1小时）；增强SpaFallbackController路由覆盖；Dashboard替换mock数据为真实API调用；Vite配置emptyOutDir清理旧构建产物

### v1.3.0 - 2026-04-11 代码重构与解耦

- **DTO模式**：新增 PageResponse、DeviceInfoDto、FenceDto、FenceCreateRequest、FenceUpdateRequest、AlarmStatsDto、AlarmQueryCondition
- **Service层提取**：新增 DeviceService 从 DeviceController 提取业务逻辑；FenceService 扩展 createFence/updateFence/getFenceDetail；AlarmService 扩展 populateRelations/getAlarmStats
- **工具类提取**：GeoUtils 从 FenceService 提取地理计算方法
- **缓存规范**：CacheService 新增 getOrSet() 默认方法
- **依赖注入**：AuthService 改为构造器注入
- **代码简化**：DeviceController 从301行减至89行；GeoFenceController 从236行减至103行；TrackingService 合并重复方法
