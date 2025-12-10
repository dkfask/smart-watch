# 智能定位系统

智能定位系统是一个基于Spring Boot和Vue 3的全栈应用，用于实时监控、管理和追踪设备位置，支持电子围栏、历史轨迹回放和设备通信等功能。

## 技术栈

### 后端
- Java 17+
- Spring Boot 2.7+
- Spring Security 5
- Spring Data JPA
- MySQL
- Netty（设备通信）

### 前端
- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Leaflet（地图库）
- Element Plus（UI组件库）

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
- 设备低电报警
- 围栏越界报警
- 报警历史记录

## 项目结构

```
├── frontend/           # 前端项目目录
│   ├── src/            # 前端源代码
│   │   ├── api/        # API接口封装
│   │   ├── assets/     # 静态资源
│   │   ├── components/ # Vue组件
│   │   ├── router/     # 路由配置
│   │   ├── stores/     # 状态管理
│   │   ├── views/      # 页面组件
│   │   ├── App.vue     # 根组件
│   │   └── main.js     # 应用入口
│   ├── dist/           # 构建产物
│   └── package.json    # 前端依赖
├── src/                # 后端源代码
│   ├── main/           # 主源码
│   │   ├── java/       # Java代码
│   │   │   └── com/example/demo/
│   │   │       ├── config/     # 配置类
│   │   │       ├── controller/  # 控制器
│   │   │       ├── model/       # 数据模型
│   │   │       ├── repository/  # 数据访问层
│   │   │       ├── service/     # 业务逻辑层
│   │   │       ├── socket/      # 设备通信模块
│   │   │       └── tools/       # 工具类
│   │   └── resources/  # 资源文件
│   └── test/           # 测试代码
├── build.gradle        # Gradle构建配置
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

| 方法 | 路径 | 功能 | 请求格式 | 响应格式 |
|------|------|------|----------|----------|
| POST | /api/auth/login | 用户登录 | `{"username": "admin", "password": "123456"}` | `{"user": {"id": 1, "username": "admin"}, "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}` |
| POST | /api/auth/logout | 用户登出 | N/A | `{"logout": true}` |
| GET  | /api/auth/me | 获取当前用户信息 | N/A | `{"id": 1, "username": "admin", "name": "管理员"}` |

### 设备管理API

| 方法 | 路径 | 功能 | 请求参数 | 响应格式 |
|------|------|------|----------|----------|
| GET  | /api/devices | 获取设备列表 | `page`: 页码（默认0）<br>`size`: 每页数量（默认20）<br>`search`: 搜索关键词 | `{"list": [...], "total": 100, "page": 0, "size": 20}` |
| GET  | /api/devices/{id} | 获取设备详情 | N/A | 设备详情对象 |
| GET  | /api/devices/available | 获取可用设备列表 | N/A | 设备列表 |
| GET  | /api/devices/by-imei/{imei} | 根据IMEI获取设备 | N/A | 设备详情对象 |
| POST | /api/devices | 创建新设备 | 设备对象 | `{"id": 1}` |
| PUT  | /api/devices/{id} | 更新设备信息 | 设备对象 | `{"success": true}` |
| DELETE | /api/devices/{id} | 删除设备 | N/A | `{"success": true}` |

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

| 方法 | 路径 | 功能 | 请求参数 | 响应格式 |
|------|------|------|----------|----------|
| GET  | /api/locations/device/{deviceId} | 获取设备最近位置 | `limit`: 数量（默认50）<br>`offset`: 偏移量（默认0） | 位置列表 |
| GET  | /api/locations/device/{deviceId}/latest | 获取设备实时位置 | N/A | 位置对象 |
| GET  | /api/locations/device/{deviceId}/latest-with-address | 获取带地址的实时位置 | N/A | 带地址的位置对象 |
| GET  | /api/locations/device/{deviceId}/history | 获取设备历史位置 | `start`: 开始时间<br>`end`: 结束时间<br>`page`: 页码<br>`size`: 每页数量 | 位置列表 |
| GET  | /api/locations/device/{deviceId}/range | 获取设备历史轨迹（兼容旧接口） | `start`: 开始时间<br>`end`: 结束时间<br>`limit`: 数量<br>`offset`: 偏移量 | 位置列表 |

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

| 方法 | 路径 | 功能 | 请求参数 | 响应格式 |
|------|------|------|----------|----------|
| GET  | /api/patients | 获取病人列表 | `page`: 页码（默认0）<br>`size`: 每页数量（默认20）<br>`search`: 搜索关键词 | `{"list": [...], "total": 200, "page": 0, "size": 20}` |
| GET  | /api/patients/{id} | 获取病人详情 | N/A | 病人详情对象 |
| GET  | /api/patients/by-device/{deviceId} | 根据设备ID获取病人 | N/A | 病人详情对象 |
| POST | /api/patients | 创建新病人 | 病人对象 | 病人详情对象 |
| PUT  | /api/patients/{id} | 更新病人信息 | 病人对象 | 病人详情对象 |
| DELETE | /api/patients/{id} | 删除病人 | N/A | `{"success": true}` |

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

| 方法 | 路径 | 功能 | 请求格式 | 响应格式 |
|------|------|------|----------|----------|
| POST | /api/patient-devices | 关联设备和病人 | `{"patientId": 1, "deviceId": 1, "relationship": "wearing"}` | 关联对象 |
| DELETE | /api/patient-devices | 解除设备关联 | `patientId`: 病人ID<br>`deviceId`: 设备ID | `{"success": true}` |
| GET  | /api/patient-devices/by-patient/{patientId} | 获取病人关联设备 | N/A | 关联列表 |
| GET  | /api/patient-devices/by-device/{deviceId} | 获取设备关联病人 | N/A | 关联列表 |

### 实时数据推送API

使用WebSocket进行实时数据推送，连接地址：`ws://{server}/ws`

#### 推送数据类型

| 类型 | 说明 | 数据格式 |
|------|------|----------|
| device_status_update | 设备状态更新 | `{"deviceId": 1, "imei": "123456789012345", "isOnline": true, "batteryLevel": 85}` |
| location_update | 位置更新 | `{"deviceId": 1, "imei": "123456789012345", "latitude": 39.9042, "longitude": 116.4074, "time": "2023-01-01T12:00:00Z"}` |
| alarm | 告警信息 | `{"deviceId": 1, "imei": "123456789012345", "type": "fence", "message": "设备离开围栏", "level": "high"}` |

### 电子围栏API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET  | /api/fences | 获取围栏列表 |
| GET  | /api/fences/{id} | 获取围栏详情 |
| POST | /api/fences | 创建新围栏 |
| PUT  | /api/fences/{id} | 更新围栏信息 |
| DELETE | /api/fences/{id} | 删除围栏 |
| GET  | /api/fences/alerts | 获取围栏警报列表 |

### 下行命令API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET  | /api/downlink/online | 获取在线设备IMEI列表 |
| POST | /api/downlink/bp00 | 发送BP00授时命令 |
| POST | /api/downlink/bp12 | 发送BP12设置SOS命令 |
| POST | /api/downlink/custom | 发送自定义下行命令 |

## 快速开始

### 环境要求

- Java 17+
- Node.js 16+
- MySQL 8+

### 后端启动

1. 配置数据库连接
   - 修改 `src/main/resources/application.properties` 中的数据库配置

2. 启动后端服务
   ```bash
   # 使用Gradle
   gradlew.bat bootRun
   
   # 使用Maven
   mvn spring-boot:run
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

### 构建生产版本

1. 构建前端
   ```bash
   cd frontend
   npm run build
   ```

2. 构建后端
   ```bash
   # 使用Gradle
   gradlew.bat clean assemble -x test
   
   # 使用Maven
   mvn -DskipTests package
   ```

3. 构建产物
   - 前端：`frontend/dist`
   - 后端：`build/libs/demo2.jar` 或 `target/demo2.jar`

### 一键构建（推荐）

```bash
# 使用Gradle构建，包含前端构建
gradlew.bat clean assemble -x test
```

## 配置说明

### 后端配置

主要配置文件：`src/main/resources/application.properties`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| spring.datasource.url | 数据库连接URL | jdbc:mysql://localhost:3306/1 |
| spring.datasource.username | 数据库用户名 | root |
| spring.datasource.password | 数据库密码 | root |
| app.mpband.port | 设备通信端口 | 9000 |
| app.mpband.useDelimiterReader | 是否使用分隔符读取 | true |
| app.mpband.frameDelimiter | 帧结束符 | # |
| app.mpband.appendNewlineAfterResponse | 下行追加CRLF | true |
| app.mpband.soTimeoutMillis | 读超时时间 | 180000 |
| app.mpband.maxFrameLength | 单帧最大长度 | 4096 |
| app.mpband.saveDir | 日志保存目录 | mpband_data |

### 前端配置

主要配置文件：`frontend/.env`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| VITE_API_BASE_URL | 后端API基础URL | http://localhost:8080/api |

## 设备通信协议

### 上行协议（设备→服务器）

| 协议号 | 名称 | 功能描述 |
|--------|------|----------|
| AP00 | 登录包 | 设备登录到服务器，上报IMEI等基本信息 |
| AP01 | 定位数据包 | 上报设备当前位置信息，包含GPS、LBS和Wi-Fi数据 |
| AP02 | 健康包 | 上报设备健康数据，如心率、步数等 |
| AP03 | 心跳/状态包 | 定期上报设备状态，包含GSM信号、卫星数、计步等信息 |
| AP04 | 低电报警 | 当设备电量过低时上报报警信息 |
| AP10 | 报警与地址回复 | 上报各种报警信息，包含位置和状态数据 |
| AP42 | 图片分包 | 上报设备拍摄的图片数据，采用分包传输 |
| APJK | 健康数据 | 上报设备健康监测数据 |
| APTP | 体温数据 | 上报设备监测的体温数据 |

### 下行协议（服务器→设备）

| 协议号 | 名称 | 功能描述 |
|--------|------|----------|
| BP00 | 设置时区 | 向设备发送当前时间和时区信息 |
| BP12 | 设置SOS号码 | 配置最多3个SOS紧急联系人号码 |
| BP14 | 设置联系人白名单 | 配置最多10个联系人，名称使用UNICODE编码 |
| BP15 | 定位间隔设置 | 设置设备定位数据上报的时间间隔（秒） |
| BP16 | 立即定位指令 | 命令设备立即上报当前位置信息 |
| BP17 | 恢复出厂设置 | 将设备恢复到出厂默认设置 |
| BP18 | 重启设备 | 命令设备重启 |
| BP19 | 设置服务器信息 | 配置设备连接的服务器地址和端口 |
| BP20 | 设置设备语言与时区 | 配置设备语言和时区 |
| BP31 | 关机 | 命令设备关机 |
| BP32 | 拨打电话 | 命令设备拨打指定电话号码 |
| BP33 | 工作模式 | 设置设备的工作模式 |
| BP34 | 自定义定位模式 | 配置设备的自定义定位模式 |
| BP40 | 快捷指令下发 | 发送快捷指令，内容使用GB2312编码后转为十六进制 |
| BP46 | 立即拍照 | 命令设备立即拍摄照片 |
| BP50 | 心跳检测指令 | 下发心跳检测指令，检查设备连接状态 |
| BP51 | 下发电话本（单条） | 向设备添加单条电话本记录 |
| BP52 | 删除电话本（单条） | 从设备删除单条电话本记录 |
| BP84 | 白名单开关 | 开启或关闭设备的白名单功能 |
| BP86 | 健康监测间隔设置 | 设置设备健康数据上报的时间间隔 |
| BP88 | 寻找设备 | 命令设备发出声响，便于寻找设备 |

## 开发指南

### 后端开发

1. 新增API：在 `controller/api/` 目录下创建新的控制器类
2. 新增数据模型：在 `model/` 目录下创建新的实体类
3. 新增数据访问：在 `repository/` 目录下创建新的Repository接口
4. 新增业务逻辑：在 `service/` 目录下创建新的Service类

### 前端开发

1. 新增页面：在 `views/` 目录下创建新的Vue组件
2. 新增路由：在 `router/index.js` 中配置新的路由
3. 新增API封装：在 `api/` 目录下创建新的API文件
4. 新增状态管理：在 `stores/` 目录下创建新的Store
5. 新增组件：在 `components/` 目录下创建新的Vue组件

## 测试说明

### 设备模拟器

项目提供了Java版本的设备模拟器，用于测试设备通信：

```bash
# 编译模拟器
gradlew.bat classes

# 运行模拟器
java -cp build\classes\java\main com.example.demo.tools.BraceletClientSimulator
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
   gradlew.bat clean assemble -x test
   ```

2. 运行jar包
   ```bash
   java -jar build\libs\demo2.jar
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
