# demo2 登录/注册模块

本工程已集成基于 Spring Security 的表单登录与注册：

- /login：登录页面（Thymeleaf），表单提交到 /login，登录成功跳转 /home
- /register：注册页面，校验重复用户名/空密码/二次确认，注册成功跳转登录页
- /home：登录后访问的首页；/hello、/（重定向到 /home）
- 密码：BCrypt（强度 12），见 `com.example.demo.config.PasswordConfig`
- 用户存储：自建 `users` 表（字段与 `UserRepository` 一致，含 `password_hash`）
- 启动初始化：创建默认账户 admin/admin123、user/user123（仅当不存在时）
- CSRF：开启（默认），对 `/api/**` 放行，表单页面包含 CSRF 隐藏域

## 目录与关键文件
- 配置
  - `config/SecurityConfig.java`：Spring Security 过滤链、表单登录、放行路径、CSRF 例外
  - `config/PasswordConfig.java`：BCrypt 密码编码器 Bean
  - `config/SecurityDataInitializer.java`：默认用户初始化
  - `resources/schema.sql`：建表（IF NOT EXISTS，不覆盖数据）
  - `resources/application.properties`：数据源配置与 `spring.sql.init.mode=always`
- 业务
  - `service/AuthUserDetailsService.java`：从数据库加载用户给 Security 使用
  - `service/AuthService.java`：注册逻辑（写库并加密密码）
  - `controller/AuthController.java`：登录页、注册页、注册提交、首页
  - `templates/login.html`、`templates/register.html`、`templates/home.html`

## 数据库
- 确保已有 MySQL 数据库（例如 schema 名为 `1`，与 `application.properties` 的 url 保持一致）。
- 首次启动会自动执行 `schema.sql`，创建 `users` 表；若表已存在不会覆盖。

## 构建与运行（Windows）
- 使用 Maven（推荐）：在工程根目录执行 `mvn -DskipTests package`，产物在 `target/demo2.jar`。
- 使用 Gradle Wrapper：`gradlew.bat clean assemble -x test`
  - 若遇到 Gradle Worker Daemon 类找不到等本地编码/路径问题，可指定英文缓存目录：
    - 设置环境变量 `GRADLE_USER_HOME=C:\gradle-cache` 后重试
    - 或在命令后添加 `-g C:\gradle-cache`
- 运行：`java -jar target\demo2.jar` 或 `java -jar build\libs\demo2.jar`

## 快速验证
1) 启动应用后访问 `http://localhost:8080/login`
2) 使用默认账户登录：
   - admin / admin123
   - user / user123
3) 也可在 `/register` 注册新用户，然后回到登录页登录

## 常见问题
- 循环重定向到登录页：确认 MySQL 可连接、`users` 表存在、以及 `PasswordConfig` 的加密算法为 BCrypt（与初始化一致）。
- API 403：表单页面启用 CSRF，对 `/api/**` 已忽略；若有其它自定义路径上传/POST，按需加入忽略列表。

## 下行模块（服务器 -> 设备）

本工程新增了一个用于向已登录设备下发命令的下行模块，方便在设备通过 TCP 长连接（AP00 登录）连接到服务器后由平台向设备下发 BPxx 指令（例如授时 BP00、设置 SOS BP12 等）。下面说明如何使用与调试。

### 新增的主要类（路径）

- `com.example.demo.socket.downlink.DownlinkService`
  - 功能：按协议规则构造 BPxx 下行包（例如 BP00、BP12、BP14、BP15、BP40 等），并提供发送方法 `send(Socket, message)`。
  - 注意：BP40（快捷指令）会将 GB2312 编码后的字节转为十六进制；BP14 中的联系人名称使用 UTF-16BE（UNICODE）转十六进制下发。

- `com.example.demo.socket.downlink.DownlinkManager`
  - 功能：管理在线设备映射（IMEI -> Socket），提供按 IMEI 下发消息的 `sendToImei(imei, message)` 方法。
  - 使用场景：在 `MpbandServer` 解析到 AP00 登录包后，会调用 `register(imei, socket)` 将连接注册到该管理器；连接断开时调用 `unregisterBySocket(socket)` 清理映射。

- `com.example.demo.controller.DownlinkController`
  - 功能：提供若干运维/测试用的 HTTP 接口（未做鉴权，仅用于开发/受限环境）：
    - GET  `/api/downlink/online` — 返回当前在线 IMEI 列表
    - POST `/api/downlink/bp00?imei={imei}&timezone={tz}` — 向 imei 下发 BP00（授时）包
    - POST `/api/downlink/bp12` — 向 imei 下发 BP12（设置 SOS），请求 body JSON: { "imei": "...", "seq":"1", "sos":["138...","",""] }

- `com.example.demo.socket.MpbandServer`
  - 已集成对 AP00 登录的处理：在 `handleLogin` 中会注册下行连接并保存设备（数据库），并且默认会回复 BP00（授时响应），满足协议“设备每次连接都需发登录包，平台必须回复 BP00”要求。

### 如何端到端测试

前提：确保 Spring Boot 应用已启动且 `MpbandServer` 已在配置端口（默认 9000）监听，HTTP 服务（默认 8080）可用。

1) 模拟设备向服务器发送 AP00 登录包（使用 telnet 或 netcat）：
   - 使用 telnet��Windows）：
     - 在 cmd 中：
       telnet localhost 9000
     - 在 telnet 会话中输入并回车（示例）：
       IWAP00353456789012345#
     - 说明：IW(包头) AP00(协议) 353456789012345(15位 IMEI) #(结束符)，并且必须回车使服务器端 `readLine()` 返回。
   - 如果一切正常，服务器应回复类似：
     IWBP00,20251104123000,8#
     （BP00, UTC时间, 时区小时）

2) 在服务器上查看在线 IMEI：
   - curl 请求（Windows cmd）：
     curl -X GET "http://localhost:8080/api/downlink/online"
   - 返回示例： ["353456789012345"]

3) 通过 REST 下发下行包（示例）
   - 发送 BP00（授时）：
     curl -X POST "http://localhost:8080/api/downlink/bp00?imei=353456789012345&timezone=8"
   - 发送 BP12（设置 SOS）示例：
     curl -H "Content-Type: application/json" -d "{\"imei\":\"353456789012345\",\"seq\":\"1\",\"sos\":[\"13800000000\",\"\",\"\"]}" "http://localhost:8080/api/downlink/bp12"
   - 返回示例 JSON 包含 sent:true 与发送的 message 文本，服务器日志也会记录发送信息。

4) 验证设备是否收到：
   - 如果使用 telnet 模拟的“设备”会话仍在连接中，应能在终端看到服务器发送的下行文本（若 telnet 不显示可尝试 nc 或自定义 Java 客户端读取并打印服务端返回）。

### 编码与协议要点

- 下行包以 `IW` 开头、以 `#` 结尾；APxx（上行）对应 BPxx（下行），例如 AP00 -> BP00。
- BP40（快捷指令）：控制内容需先用 GB2312 编码后转为十六进制串下发，服务器端 `DownlinkService.buildBP40` 会自动处理。
- BP14（联系人名单）中姓名字段需以 UNICODE（UTF-16BE）字节转为十六进制串下发。
- `MpbandServer` 在解析 AP00 时，会把原始 payload 存入 `packet.params.payload`，并从中提取 IMEI（15位数字）用于注册与存库。

### 安全提示

- 目前 `DownlinkController` 为测试/运维接口，**未做任何鉴权**，请仅在开发或受限网络中启用；生产环境必须增加认证（如 JWT / API Key / 基于 RBAC 的访问控制）和传输加密（HTTPS）以及审计日志。
- 在大规模部署时，请对下行接口添加速率限制与操作审计，避免滥发指令。

### 常见问题与排查

- 设备没有收到下行命令：
  - 确认设备已完成 AP00 登录并在 `/api/downlink/online` 中可见；
  - 确认下行 REST 调用返回 sent=true，并且服务器日志显示成功写入 socket；
  - 确认设备连接是长连接，且设备端有读取返回数据的逻辑（readLine 或按协议读取到 `#`）。

- readLine 阻塞：
  - 服务器端 `MpbandServer` 使用 `BufferedReader.readLine()` 读取上行，请确保设备发送数据后带有换行（CR/LF）或使用能触发 `readLine()` 返回的分隔方式。

### 后续建议

- 为 `DownlinkController` 添加鉴权与审计；
- 为 `DownlinkManager` 支持同一 IMEI 多连接（如果需要）并提供优先级下发策略；
- 编写一个集成测试用的设备模拟器（JUnit + Socket）来做端到端自动化验证；
- 将在线设备数/下行成功率等指标导出为监控指标（Prometheus）以便运维观察。

(以上说明已追加到本仓库的 README，用于开发和运维参考)

## 上行模块（设备 -> 服务器）

本工程同时实现了设备上行（设备 -> 服务器）的解析与处理模块，用于接收、解析并持久化手环上报的数据（AP00、AP01、AP03 等）。下面对上行模块的实现与使用做详细说明，便于开发与调试。

### 覆盖的上行协议（常见）

- AP00（登录包）
  - 格式示例：IWAP00353456789012345# 或 IWAP00353456789012345,460|00|CMNET# 或 IWAP00357653050852602,89962030221137165263,416032113716526#
  - 说明：设备连接后必须发送登录包，平台需回复 BP00（授时）。MPbandServer 会从 payload 中提取 IMEI（15位）并在首次登录时保存到设备表（唯一）。

- AP01（定位数据包）
  - 包含 GPS（纬度经度）、速度、时间、方向角、LBS 基站数据与 Wi-Fi 列表等；示例较长，ProtocolParser 会把 GPS 主段与余下基站/Wi-Fi 存于不同 params 字段中以便后续处理。

- AP03（心跳/状态包）
  - 包含 GSM 信号、卫星数、计步等状态信息，服务器会把常见字段拆分并保存到 params 里；同时可简单记录到文件或数据库。

- 其它：AP04（低电报警）、AP10（报警与地址回复）、AP42（图片分包）、APJK（健康数据）、APTP（体温）等 — Parser 支持通用包解析并可扩展特定字段解析。

### 关键实现类（路径）

- `com.example.demo.socket.protocol.ProtocolParser`
  - 功能：把原始报文（字符串）解析为 `BraceletPacket` 或其子类（如 LoginPacket、LocationPacket、HeartbeatPacket）。
  - 行为：校验包头 `IW` 与结束符 `#`，提取协议号（位置 2-6），把 payload 放入 `params.payload`，并对 AP00/AP01/AP03 进行更细致解析。

- `com.example.demo.socket.protocol.BraceletPacket`（及子类）
  - 功能：承载解析后的包信息（protocol、raw、params、时间戳、针对协议的字段如 imei、lat/lng 等）。

- `com.example.demo.socket.MpbandServer`
  - 功能：基于 TCP ServerSocket 接收连接，逐行读取上行数据（使用 `BufferedReader.readLine()`），调用 `ProtocolParser.parse(message)` 取得 `BraceletPacket`，并在 `processPacket` 中根据协议类型处理数据。
  - AP00 特殊处理：在 `handleLogin` 中注册下行连接（调用 DownlinkManager.register(imei, socket)），并把设备信息保存到数据库（DeviceRepository）；同时 createResponse 会自动生成 BP00（授时）响应并发送回设备。
  - 数据保存：示例代码中对健康数据、位置数据做了文件追加写入（health_data.txt / location_data.txt），实际场景建议写入数据库或队列以便后续分析。

### 如何本地测试上行（设备 -> 服务器）

1) 启动应用（确保 MpbandServer 已启动并监听端口，默认 9000）：
   - `java -jar target\demo2.jar`（或使用 IDE 运行 Spring Boot 应用）

2) 使用 telnet 模拟设备连接并发送 AP00 登录：
   - Windows cmd：
     - `telnet localhost 9000`
     - 在 telnet 会话输入并回车：
       IWAP00353456789012345#
   - 说明：`MpbandServer` 使用 `readLine()` 读取一行数据，若你发送的行没有换行符，服务器将不会收到完整行；真实设备通常会发送 CRLF。
   - 预期结果：服务器在日志中会打印收到的原始数据，并回复像 `IWBP00,20150101125223,8#` 的授时包；同时数据库中若设备不存在将会保存一条 Device 记录。

3) 测试 AP01 / AP03 等：
   - 使用 telnet 发送 AP01 或 AP03 对应格式（以 `#` 结尾并回车），观察服务器日志中 Parser 解析出的字段（ProtocolParser 在 `params` 中放置 `lat`/`lng`/`rawGpsPart`/`rawExtraPart` 等键）。

### 日志与数据保存位置（示例）

- 服务器日志：使用 `slf4j` 打印，IDE 控制台或 jar 启动时的 stdout 可以看到解析信息与错误信息。
- 临时示例存储：
  - `health_data.txt`：append 模式保存心率/步数的简单文本记录（示例写在项目根目录下）
  - `location_data.txt`：append 模式保存位置信息
- 真实生产：建议将数据写入数据库（MySQL/Postgres）或投递到消息队列（Kafka/RabbitMQ）以便后续处理与分析。

### 常见问题与排查（上行）

- 解析返回 null（ProtocolParser.parse 返回 null）：
  - 检查上行包是否以 `IW` 开头并以 `#` 结尾；是否包含换行分隔（服务器的 readLine 需要换行）；
  - 检查协议号是否位于正确的位置（第 3~6 字符）且长度至少 6 字符。

- IMEI 解析失败或保存失败：
  - 确认 payload 第一段是否为 15 位数字 IMEI；
  - 检查数据库连接与 `DeviceRepository` 是否配置正确（Spring Data JPA 依赖与配置）。

- readLine 阻塞（看不到任何收到的数据）：
  - 设备发送数据但没有换行（CR/LF），readLine 会一直等待；在测试时务必在末尾加回车。

### 扩展与二次开发建议

- 将 `ProtocolParser` 的解析结果持久化结构化（例如：位置、心率、报警单独表）并加索引（IMEI、时间），便于查询与统计。
- 对图片（AP42）实现分包拼接逻辑并在最后一包收到后保存为文件/上传对象存储。
- 对 AP10（报警）实现地址解析并根据 `flag` 决定是否回复包含地址的 BP10 下行包；注意 BP10 下行可能需要把地址内容以 Unicode(hex) 下发（已有 DownlinkService 的工具可用）。
- 增加接收速率控制与异常连接（恶意/异常流量）防护，防止单个设备或攻击造成资源耗尽。

---

以上内容已经追加到本仓库的 README，用于说明上行解析/处理模块的设计、测试与排查方法。若需要，我可以：
- 将 README 中的示例命令改成可直接在 Windows cmd 下复制粘贴的脚本；
- 自动生成一个简单的 Java 设备模拟器放在 `tools/` 目录下用于集成测试；
- 或者把上行关键字段的持久化示例（JPA Entity + Repository）写入项目以便演示完整端到端落地。

## 前端（Vue）模块

为了把现有的 Thymeleaf 页面替换为现代单页应用（SPA），项目新增了一个基于 Vue 3 + Vite 的前端子工程 `frontend/`。该前端项目实现了登录、注册与首页（与后端现有接口兼容），并提供开发与构建说明。以下为重要说明与快速上手步骤（Windows cmd）：

- 开发（热重载）
  1) 进入前端目录：
     ```cmd
     cd frontend
     ```
  2) 安装依赖并启动开发服务器：
     ```cmd
     npm install
     npm run dev
     ```
  3) 默认 dev 服务器在 `http://localhost:5173`，页面将通过 Vite 提供。

- 构建（生产）并部署到后端静态目录（将构建产物放到后端 Spring Boot 的 `resources/static`，以便由同一后端 Jar 提供）
  1) 在 `frontend` 目录运行：
     ```cmd
     npm run build
     ```
  2) 构建产物位于 `frontend/dist`，将其内容复制到后端资源目录（示例）：
     ```cmd
     xcopy /E /I /Y frontend\dist ..\src\main\resources\static\
     ```
     说明：命令假定你在仓库根目录执行一次（若在 `frontend` 目录运行，请调整路径）。
  3) 重新打包后端 Jar（Gradle/Maven）并运行：
     ```cmd
     gradlew.bat clean assemble -x test
     java -jar build\libs\demo2.jar
     ```

- CSRF 与跨域注意事项
  - 后端默认启用 CSRF，且仅对 `/api/**` 放行。如果你在开发时使用 Vite dev-server（不同端口），浏览器会跨域请求后端登录/注册接口，可能触发 CSRF 验证。开发时有两种处理方式：
    1) 在后端临时放行开发机（例如允许 `http://localhost:5173` 的 CORS 并为测试目的放宽 CSRF 限制）；
    2) 在开发时通过 Vite 的代理将 API 请求代理到后端（`vite.config.js` 已包含代理示例）。
  - 推荐使用代理方案，或在构建后将静态文件部署到后端，这样可避免跨域与 CSRF 问题。

- 快速验证（使用构建产物或代理）
  1) 使用 dev-server（并启用代理）访问 `http://localhost:5173`。
  2) 访问登录页，使用默认账户登录（admin / admin123 或 user / user123）。
  3) 如果将构建产物复制到后端静态目录，重启后端并访问 `http://localhost:8080/` 即可看到 SPA。

- 代码位置（新增）
  - `frontend/`：前端 Vue3 项目根
    - `frontend/src/main.js`：应用入口
    - `frontend/src/router/index.js`：路由配置（/login /register /home）
    - `frontend/src/views/Login.vue`、`Register.vue`、`Home.vue`：页面组件

### 新增前端页面快捷入口
本次前端迭代新增了以下页面（位于 `frontend/src/views`），已在路由中注册：
- `/dashboard`：系统仪表盘（设备统计、最近日志）
- `/devices`：设备列表（支持分页与跳转到详情）
- `/devices/:id`：设备详情页面（基础信息、最近位置、日志）
- `/realtime`：实时监控（示例页面，可接入 WebSocket 与地图）
- `/settings`：系统设置（示例，用于配置保存目录 / 轮询间隔）

在开发模式（Vite dev-server）下访问 `http://localhost:5173`，可以在地址栏直接打开以上路由进行调试；部署到后端静态目录后可通过 `http://localhost:8080` 访问（同域部署）。

### Gradle 集成前端构建（已实现）

本仓库已把 `frontend` 的构建集成到 Gradle 打包流程中：

- 在执行 `gradlew.bat clean assemble` 或 `gradlew.bat build` 时，默认会：
  1) 在仓库根的 `frontend/` 目录运行 `npm install && npm run build`（若目录不存在会跳过）；
  2) 将 `frontend/dist` 的内容复制到后端的 `src/main/resources/static/`，使得生成的 Jar 包中包含前端静态文件（可直接由 Spring Boot 提供）。

- 如果你的构建环境没有 Node.js / npm，或你在 CI 中已经预先构建好前端产物，可以通过 Gradle 属性跳过前端构建：

  在 Windows 上（cmd）运行：
  ```cmd
  gradlew.bat clean assemble -x test -PskipFrontend=true
  ```

  说明：`-PskipFrontend=true` 会跳过执行 `npm install && npm run build`，但仍会尝试复制 `frontend/dist`（若存在）。

- 若你希望在本地一键构建前端并打包后端（推荐开发者首次使用），执行：
  ```cmd
  gradlew.bat clean assemble -x test
  ```
  该命令会自动触发前端构建并将产物内嵌到 Jar 中。

## 渐进式切换到前端 SPA（保留旧模板）

如果你想在不破坏现有站点的前提下逐步迁移到新的 Vue SPA，可以采用“渐进式切换”策略：先保留并备份原有 Thymeleaf 模板，部署并验证前端静态文件，再通过配置开关把部分路由切换到 SPA。下面是推荐流程（Windows cmd 可直接复制）：

1) 备份原有模板（安全隐藏，但可随时回滚）
   ```cmd
   cd /d e:\smart\master\src\main\resources
   if not exist templates_backup mkdir templates_backup
   move templates\*.html templates_backup\
   ```
   - 这会把 `templates` 目录下的 `.html` 文件移动到 `templates_backup`，保留文件以便回滚。

2) 构建并部署前端静态文件到后端（把 `frontend/dist` 的内容复制到 `src/main/resources/static`）
   ```cmd
   cd /d e:\smart\master\frontend
   npm install
   npm run build
   cd /d e:\smart\master
   xcopy /E /I /Y frontend\dist\* src\main\resources\static\
   ```

3) 启用 SPA 回退控制器（切换路由到 SPA）
   - 在 `src/main/resources/application.properties` 中把 `app.spa.enabled` 设为 `true`，示例：
     ```ini
     app.spa.enabled=true
     ```
   - 重新启动后端应用，`/login`、`/register`、`/home` 等路由会被转发到 `index.html`（前端 SPA 入口），而无需删除服务器端模板文件（因为我们已把模板移动到备份目录）。

4) 验证与回滚
   - 启动应用后访问 http://localhost:8080/（或 /login）验证 SPA 是否正常。
   - 若需回滚，停止应用并把备份模板还原：
     ```cmd
     cd /d e:\smart\master\src\main\resources
     move templates_backup\*.html templates\
     ```

5) 进阶建议
   - 在切换过程中保留 API 与后端认证逻辑（我们已实现 `/api/login` 与 `/api/register`）。确保前端登录使用 `credentials: 'include'` 以携带 session cookie。

注意：关于 REST 登录端点的说明

- 为避免启动时出现重复路由映射（Ambiguous mapping），项目中现在存在两个登录接口，请根据调用场景选择：
  - `/api/login`：由 `com.example.demo.controller.ApiAuthController` 提供，现为默认的 API 登录端点（兼容前端 Vue 示例，返回 JSON 或在需要时发起重定向到 `/home`）。
  - `/api/auth/login`：由 `com.example.demo.controller.AuthApiController` 提供（该控制器原先也映射到 `/api/login`，后为避免与 `ApiAuthController` 冲突而改为 `/api/auth`）。

- 选择与迁移建议：
  - 若你的前端或脚本已调用 `/api/login`（仓库前端示例即如此），可以继续使用 `/api/login`（`ApiAuthController` 已提供相同功能）。
  - 若需要统一 API 命名或合并两处逻辑，可以把前端改为调用 `/api/auth/login`，或将两处登录实现合并到一个控制器（推荐在代码审查后统一处理）。
  - 无论使用哪个端点，登录时前端需使用 `credentials: 'include'` 以携带 session cookie（session-based 认证）。

## 修复：循环重定向 (ERR_TOO_MANY_REDIRECTS) / Servlet StackOverflowError 说明与验证

在本次迭代中，定位到的问题是：当启用 SPA 回退（`app.spa.enabled=true`）时，`SpaFallbackController` 会通过 `forward:/index.html` 把若干页面路由（例如 `/`, `/login`, `/register`, `/home`）转发到静态的 `index.html`；若 Spring Security 未把 `index.html` 及相关静态资源放行，则服务器在内部转发到 `/index.html` 时会被安全过滤器再次判定为未认证并重定向到 `/login`，从而造成 ` /login -> forward:/index.html -> redirect:/login -> ... ` 的循环，最终表现为浏览器的 `ERR_TOO_MANY_REDIRECTS` 或后端抛出 `java.lang.StackOverflowError`。

为了解决这个问题，已对后端安全配置文件 `src/main/java/com/example/demo/config/SecurityConfig.java` 做了以下变更（已加入仓库）：

- 把 `"/index.html"`、`"/favicon.ico"`、`"/assets/**"`、`"/static/**"`、`"/css/**"`、`"/js/**"` 等常见静态资源路径加入到 `permitAll()` 白名单中；
- 在 `SecurityConfig` 中增加了中文注释，说明为什么需要放行这些路径（避免 `forward` 被再次拦截导致的循环）。

为什么 README 里不再使用 `"/**/*.css"` / `"/**/*.js"` 示例：
- Spring 的 PathPatternParser 不允许在 `**` 通配符后紧接额外文本（例如 `/**/*.js`），这会在运行时触发 `PatternParseException: No more pattern data allowed after {*...} or ** pattern element`。因此在安全配置中请使用目录通配（例如 `/assets/**`、`/css/**`、`/js/**`）或显式列出所需路径，而不要使用 `**` 再接扩展名的写法。

修改位置（重要）：
- `src/main/java/com/example/demo/config/SecurityConfig.java`
- `README.md`（本节已更新以反映安全的写法）

本节给出在本地（Windows cmd）验证修复的步骤与示例输出。

注意：下面的命令假定服务运行端口为 `8080`，并且你在仓库根目录（`e:\smart\master`）。

1) 先重启/重新构建后端（如果你使用 Gradle）：

```cmd
cd /d e:\smart\master
gradlew.bat clean assemble -x test
java -jar build\libs\demo2.jar
```

（如果你使用 IDE 运行 Spring Boot，也请重启应用）

2) 使用 curl 验证根路径与 /login 是否正常返回 `index.html` 或登录页（不跟随重定向）：

```cmd
curl -v -I http://localhost:8080/
curl -v -I http://localhost:8080/login
```

3) 如果想查看完整页面内容（非仅响应头），执行：

```cmd
curl -v http://localhost:8080/  > response.html
notepad response.html
```

4) 期望的测试结果示例（成功时）：

- `curl -v -I http://localhost:8080/` 的关键输出包含：

```
HTTP/1.1 200 OK
Vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
...
```

- `curl -v http://localhost:8080/` 的返回体（`response.html`）应包含前端 `index.html` 的 `<title>` 等内容，例如：

```html
<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>demo2 - 端 (Vue)</title>
    ...
```

（你在本地测试时可能看到类似的响应头与 HTML 内容；这是表示 `index.html` 已被正确返回，循环中断）

5) 浏览器验证

- 建议先用无痕/隐私窗口访问 `http://localhost:8080/` 或 `http://localhost:8080/login`，以排除 Cookie 导致的会话问题。
- 若仍出现 `ERR_TOO_MANY_REDIRECTS`，请先清理 `localhost` 的 Cookie 并重试。

6) 如果仍旧失败，后续排查建议（按优先级）
- 检查 `src/main/resources/application.properties` 中 `app.spa.enabled` 的值：若为 `true`，确保 `frontend/dist/index.html` 已被复制到 `src/main/resources/static/index.html`（或构建产物已正确部署到 Jar）。
- 检查是否存在其它将 `/login` 或 `/index.html` 再次转发/重定向的 Controller 或 Filter（搜索 `forward:/`、`redirect:/`、`/login` 等）。
- 临时调试方法：在 `SecurityConfig` 中临时把所有请求设为 `permitAll()`，确认问题是否与 Security 引起；调试完成后记得恢复安全策略。

7) 回滚变更

- 如果你需要回退刚才对 `SecurityConfig` 的修改：
  - 如果使用 git：

```cmd
cd /d e:\smart\master
git checkout -- src/main/java/com/example/demo/config/SecurityConfig.java
```

  - 或从你本地的备份恢复原文件。

---

如果你已经执行了测试并把 `curl` 的输出粘贴上来（例如 `HTTP/1.1 200` 并且 HTML 内容显示了 `<title>`），那说明修复已生效；我可以再把这部分验证截图/输出摘录并直接追加到 README（便于其他协作者参考）。

## 登录成功后重定向到主菜单 (/menu) — 新增说明

为了在用户登录成功后直接进入主菜单，本次修改做了以下工作：

- 修改了 `src/main/java/com/example/demo/config/SecurityConfig.java`：把表单登录的默认成功重定向地址由 `/home` 改为 `/menu`（使用 `.defaultSuccessUrl("/menu", true)`，true 表示强制重定向到该地址）。
- 新增控制器：`src/main/java/com/example/demo/controller/MenuController.java`（含完整注释），负责处理 `/menu` GET 请求并把当前登录用户名传入视图。
- 新增模板：`src/main/resources/templates/menu.html`，使用 Thymeleaf 渲染主菜单页面并包含一个提交到 `/logout` 的退出表单。

如何构建并验证（Windows cmd）：

1) 在仓库根目录构建项目（示例使用 Maven）：

```cmd
cd /d e:\smart\master
mvn -DskipTests package
```

或者使用 Gradle Wrapper：

```cmd
cd /d e:\smart\master
gradlew.bat clean assemble -x test
```

2) 运行生成的 Jar（示例）：

```cmd
java -jar target\demo2.jar
```

（如果使用 Gradle 构建，Jar 可能在 `build\libs\demo2.jar`，请根据实际路径运行）

3) 在浏览器中打开登录页并验证重定向：

- 打开： http://localhost:8080/login
- 使用默认账户登录（项目启动初始化会创建这些账户，见 README 顶部）：
  - admin / admin123
  - user / user123
- 登录成功后应被重定向到： http://localhost:8080/menu

4) 手动测试（使用 curl，在不跟随重定向的情况下查看返回头）：

```cmd
curl -v -I http://localhost:8080/login
curl -v -L --max-redirs 0 -c cookies.txt -d "username=admin&password=admin123" -X POST http://localhost:8080/login
```

说明：
- 页面重定向由 `SecurityConfig` 的 `.defaultSuccessUrl("/menu", true)` 配置控制；如果你的前端为 SPA 并有回退转发（forward:/index.html），请确保 `index.html` 与静态资源路径已在安全配置中放行，避免循环重定向。
- `menu.html` 提供了一个 POST 到 `/logout` 的表单，退出会按 `SecurityConfig` 中配置的 `/logout` 路径处理并跳转到 `/login?logout=true`。

新增文件一览（便于代码审阅）

- src/main/java/com/example/demo/controller/MenuController.java
  - 作用：提供 `/menu` 页面并把当前用户名传给视图（含详尽注释）。
- src/main/resources/templates/menu.html
  - 作用：主菜单静态模板（Thymeleaf），显示用户名并提供若干示例菜单项和退出表单。

需求覆盖状态：

- 登录成功后重定向到主菜单：Done（已修改 `SecurityConfig` 并新增 `/menu` 页面）
- 新增控制器与视图（含注释）：Done
- 在 README 中补充运行/测试说明：Done

## 连接稳定性：设备反复重连的原因与解决

在实际联调中，若出现“设备发送 AP00 后服务器已应答 BP00，但设备仍立即断开并反复重连”的现象，常见原因有：
- 服务器端按行读取（readLine）而设备仅使用 `#` 结尾，不发送 `\r\n`，导致服务器未及时读取到一整帧、设备端因等不到 ACK 超时主动断开重连；
- 服务器已发送应答，但设备固件期待帧末尾带换行（CRLF），若没有换行会认为帧未结束而主动断开。

本项目已内置两项可配置的改动用于解决：
- 基于分隔符（默认 `#`）的逐字节读取模式，避免对换行的依赖：`app.mpband.useDelimiterReader=true`
- 在服务器下行应答后追加 CRLF：`app.mpband.appendNewlineAfterResponse=true`

相关配置集中在 `src/main/resources/application.properties`：

- app.mpband.useDelimiterReader=true  开启分隔符读取，避免 readLine 阻塞
- app.mpband.frameDelimiter=#        帧结束符，默认 `#`
- app.mpband.appendNewlineAfterResponse=true   下行追加 CRLF
- app.mpband.soTimeoutMillis=180000   读超时，防僵尸连接
- app.mpband.maxFrameLength=4096      单帧最大长度，防御性限制

建议逐项启用并观察日志：
- 若启用后仍断开，请确认设备是否在自身超时时间内收到了 `IWBP00,...,#\r\n` 并且未主动关闭；
- 如果设备明确要求“仅 `#` 结尾且不允许 CRLF”，可把 `app.mpband.appendNewlineAfterResponse=false` 再测试。

## 新增工具：BraceletClientSimulator（手环客户端模拟器）

为方便本地/联机复现与排查“反复重连”等问题，项目新增了一个纯 Java 的设备端模拟器：
- 代码位置：`src/main/java/com/example/demo/tools/BraceletClientSimulator.java`
- 功能：连接服务器 -> 发送 AP00 登录 -> 读取 BP00 -> 连续发送若干 AP03 心跳并读取应答；
- 可通过参数控制是否在上行末尾追加 CRLF，以模拟不同固件的行为。

运行步骤（Windows cmd）：

1) 启动后端（确保 MpbandServer 监听的端口与 application.properties 一致，默认 9000）
```cmd
cd /d e:\smart\master
gradlew.bat clean assemble -x test
java -jar build\libs\demo2.jar
```

2) 新开一个窗口，编译并运行模拟器（默认连接 127.0.0.1:9000，IMEI=353456789012345，不追加 CRLF，发送 2 次心跳，间隔 3 秒）
```cmd
cd /d e:\smart\master
gradlew.bat classes
java -cp build\classes\java\main com.example.demo.tools.BraceletClientSimulator
```

3) 自定义参数运行（host port imei appendCRLF heartbeats intervalMillis）：
```cmd
java -cp build\classes\java\main com.example.demo.tools.BraceletClientSimulator 127.0.0.1 9000 353456789012345 true 3 2000
```
- 若把第 4 个参数 appendCRLF 设为 `true`，模拟器每帧末尾会额外发送 `\r\n`，便于验证设备端对换行的需求；
- 观察后端日志应出现：收到 AP00 -> 处理登录 -> 发送 BP00 -> 接收 AP03 -> 回复 -> 连接保持不再反复断开。

故障排查建议：
- 确认服务器日志中的“📤 发送回复”之后未立即出现“Unregistered device ... for socket ...”；
- 如仍断开，优先尝试设置 `app.mpband.appendNewlineAfterResponse=true`；
- 使用模拟器切换 `appendCRLF` 参数对照测试，迅速定位是否为换行相关；
- 网络因素（NAT 保活/运营商侧超时）也可能导致断开，可适当缩短设备心跳间隔并保持服务器读超时合理（如 180s）。

## 新增：上行报文保存与设备日志（按 IMEI）

为了便于离线分析与审计，服务器现在会把每条接收到的上行报文以文件形式保存，并且按设备 IMEI 追加设备专属的通信日志，默认行为由 `MpbandServer` 实现。

- 默认保存位置（相对于应用运行目录，即 System.getProperty("user.dir")）：
  - 原始报文文件：`{saveDir}/raw/yyyyMMdd_HHmmss_SSS_<uuid>.txt`
  - 设备通信日志：`{saveDir}/devices/{imei}.log`（若无法提取 IMEI，会保存为 `devices/unknown.log` 或者以客户端地址作为文件名）
  - 默认 `saveDir` 为 `mpband_data`，因此默认路径示例：`mpband_data/raw/` 与 `mpband_data/devices/`

- 可配置项（添加到 `application.properties` 或相应配置源）：
  - `app.mpband.saveDir`（可选，默认 `mpband_data`）
  - 举例：在 `src/main/resources/application.properties` 中加入：
    ```ini
    # 将上行报文与设备日志保存到自定义目录
    app.mpband.saveDir=mpband_data
    ```

- 实现说明（概要，便于维护）
  - 在 `com.example.demo.socket.MpbandServer` 中新增保存逻辑：启动时创建目录 `raw` 与 `devices`；每次收到上行报文时：
    1) 把上行原始字符串写入 `raw` 目录下的独立文件（时间戳+UUID），用于完整保留原始数据用于离线分析；
    2) 尝试从报文中提取 15 位 IMEI（若存在），并把该条报文以时间戳前缀追加到 `devices/{imei}.log` 文件中，用于按设备审计与快速查看；
    3) 并发写入时使用每设备的锁（内存 map）以保证同一设备日志的写入顺序与原子性。
  - 该实现不会阻塞主循环（写文件在收到报文的处理线程中同步写入，但写入开销通常很小）；在高吞吐场景建议改成异步入队 + 单线程写磁盘或使用日志库按文件轮询/分片（Logback/Log4j RollingFileAppender 或专门的审计服务）。

- 本地快速测试（Windows cmd）
  1) 启动后端应用（确保 `MpbandServer` 配置端口默认 9000）：
     ```cmd
     cd /d e:\smart\master
     gradlew.bat clean assemble -x test
     java -jar build\libs\demo2.jar
     ```
  2) 使用 telnet 模拟设备发送 AP00 登录包（注意要回车）
     ```cmd
     rem 打开 telnet 会话（Windows 可能需要先启用 Telnet Client）
     telnet localhost 9000
     rem 在 telnet 内输入并回车：
     IWAP00353456789012345#
     ```
     - 成功后服务器会回复类似 `IWBP00,yyyyMMddHHmmss,<tz>,<ZoneId>#`，并在应用运行目录下生成：
       - `mpband_data/raw/` 下的原始文件（包含 IWAP00...#）
       - `mpband_data/devices/353456789012345.log` 中追加一条带时间戳的日志行
       - 成功后服务器会回复类似 `IWBP00,yyyyMMddHHmmss,<tz>,<ZoneId>#`，并在应用运行目录下生成：
         - `mpband_data/raw/raw_YYYYMMDD.log`（每天一个归档文件，文件名示例 `raw_20251117.log`），其中每条上行报文按行追加并包含时间与来源信息；
         - `mpband_data/devices/353456789012345.log` 中追加一条带时间戳的日志行

  3) 如果没有 telnet，可以用 PowerShell 的 TcpClient 或编写一个简单的 Java/Python 脚本对端口写入字符串并关闭连接（同样需保证数据以 `#` 结尾并回车/换行以便某些读取模式触发）。

- 日志格式示例（设备日志行）
  2025-11-17 10:02:03.123 [127.0.0.1:58321] IWAP00353456789012345#

- 原始归档示例（mpband_data/raw/raw_20251117.log）内容示例（每行一条上行报文，行末有换行）：
  2025-11-17 10:02:03.123 [127.0.0.1:58321] IWAP00353456789012345#
  2025-11-17 10:02:07.456 [127.0.0.1:58321] IWAP01080524A2232.9806N11404.9355E000.1...#

- 注意事项与后续改进建议
  - 当前实现把原始上行报文按照日期归档为每天一个文件（`raw_yyyyMMdd.log`），文件按追加方式写入，每条报文占一行并以换行符结尾；长期存储仍会增长，建议按天或按月打包归档、压缩或转移到对象存储以便长期保留。
  - 当前设备锁为内存 map（短期可用），在多实例部署时应使用集中式锁或由单实例承担磁盘写入职责以免并发冲突。
  - 如果业务要求极高吞吐（每秒数千条），建议将写磁盘改为异步队列+单线程落盘，或使用高性能日志采集（Fluentd/Logstash/Vector）接收上行原始流。

---

以上内容已经追加到本仓库的 README，用于说明上行报文保存与设备日志的设计与测试方法。若需要，我可以：
- 提供示例脚本用于清理旧的日志文件（按日期删除）；
- 或者实现一个简单的日志查询工具（基于 Spring Boot 的小应用）用于按 IMEI/时间范围查询上行日志。

## 实时定位、围栏与轨迹绘制（前端：Leaflet + 后端 API）

为增强设备定位与围栏管理体验，前端整合了 Leaflet 地图与 Leaflet Draw 插件，提供实时位置展示、历史轨迹绘制与创建/编辑围栏的功能。下面用中文说明该模块的实现位置、运行方式与典型操作流程。

### 功能概览
- 实时定位（Realtime 页面）：通过轮询或可扩展为 WebSocket/SSE 获取在线设备的最新位置并在地图上显示为标记（Marker）。
- 轨迹回放（Device Detail 页面）：按设备加载历史位置（支持时间范围）并在地图上绘制轨迹（Polyline）。
- 围栏绘制与持久化：支持在地图上绘制圆形/多边形围栏，当前实现会把圆形围栏持久化到后端 `geo_fences` 表（center + radius）；也可显示后端返回的 GeoJSON 围栏。
- 围栏编辑/删除：通过 Leaflet Draw 编辑并触发对应事件；可在前端捕获并同步到后端。
- 与下发命令结合：在设备详情页同时保留“下发命令”面板，便于在查看轨迹/围栏时直接向设备下发 BPxx 指令。

### 关键前端文件（位置）
- `frontend/src/components/LeafletMap.vue`：Leaflet 封装组件，支持 markers、tracks（polyline）、fences（GeoJSON）以及 Leaflet Draw 的绘制与编辑事件（emit: `fence-created`, `fence-edited`, `fence-deleted`）。
- `frontend/src/views/Realtime.vue`：实时监控页面，轮询 `/api/downlink/online` 获取在线设备 IMEI，查询对应设备的最近位置并在地图上显示（每10秒轮询，示例实现）。
- `frontend/src/views/DeviceDetail.vue`：设备详情页面，加载设备最近位置与最近 24 小时轨迹（通过 `/api/locations/device/{id}` 与 `/api/locations/device/{id}/range`），并允许绘制围栏；绘制圆形时会调用围栏 API 将其保存到服务器。
- `frontend/src/api/location.js`：前端到后端的定位查询封装（`recentByDevice`、`rangeByDevice`）。
- `frontend/src/api/fence.js`：前端到后端的围栏创建封装（`createFence`）。
- `frontend/src/api/downlink.js`：下发命令 API 封装（`getOnline()`、`sendBp00()`、`sendBp12()`、`sendCustom()`），用于 DeviceDetail 的下发按钮。

### 后端相关接口（已存在）
- `GET /api/downlink/online`：返回当前在线设备 IMEI 列表（用于实时页面与 DeviceDetail 判断在线状态）。
- `GET /api/locations/device/{deviceId}`：返回设备最近位置（recent）。
- `GET /api/locations/device/{deviceId}/range?start=...&end=...`：按时间范围获取设备轨迹（用于轨迹回放）。
- `POST /api/fences`：创建围栏（示例接受 `GeoFence` 实体的 JSON，持久化到数据库）。

### 使用/调试步骤（快速）
1. 启动后端（Spring Boot）：

```cmd
cd /d e:\smart\master
mvn -DskipFrontend=true -DskipTests spring-boot:run
```

2. 启动前端开发服务器（热重载）：

```cmd
cd /d e:\smart\master\frontend
npm install
npm run dev
```

3. 访问前端（开发模式）：
   - 打开浏览器访问 `http://localhost:5173`。
   - 菜单 -> 实时监控（Realtime）查看在线设备并在地图上显示位置。
   - 设备管理 -> 点击某设备进入详情页，可查看轨迹并绘制围栏。

4. 绘制围栏并持久化（在 Device Detail 页面）：
   - 使用 Leaflet Draw 的圆形工具绘制圆形围栏；绘制完成后前端会构建 `GeoFence` 对象并调用 `POST /api/fences` 将其保存。
   - 成功后围栏会在地图上显示，并可以到后端 `geo_fences` 表中查看记录（包含 center + radius）。

5. 下发命令（在 Device Detail 页面右侧面板）：
   - 点击“发送 BP00”将调用 `POST /api/downlink/bp00?imei=...`（授时）；
   - 填入 SOS 号码并点击“发送 BP12”会调用 `POST /api/downlink/bp12`；
   - 也可以直接输入自定义原始包并发送到 `POST /api/downlink/custom`。

### 示范 curl 调用（README 中已有下行示例，此处补充围栏与轨迹相关）
- 查询设备最近位置（示例 deviceId=123）：

```cmd
curl -v "http://localhost:8080/api/locations/device/123?limit=1"
```

- 查询设备时间段轨迹（ISO8601 时间）：

```cmd
curl -v "http://localhost:8080/api/locations/device/123/range?start=2025-11-17T00:00:00&end=2025-11-18T00:00:00"
```

- 创建围栏（示例）

```cmd
curl -v -X POST "http://localhost:8080/api/fences" -H "Content-Type: application/json" -d "{
  \"userId\":1,
  \"name\":\"map-fence-demo\",
  \"centerLatitude\":22.3830,
  \"centerLongitude\":114.0823,
  \"radius\":100.0,
  \"triggerType\":\"both\",
  \"isActive\":true
}"
```

### 注意事项与扩展建议
- 围栏数据模型：当前数据库模型以中心点 + 半径表示圆形围栏（适合轻量场景）。若需要保存任意多边形，建议扩展 `geo_fences` 表以存储 GeoJSON 字符串（或另建 geo 表），前端也应支持将画出的多边形 GeoJSON 直接 POST 到后端。
- 实时数据源：当前实现使用轮询和数据库读取。为降低延迟和提高实时性，建议后端提供 WebSocket 或 SSE 推送实时位置，前端订阅后实时更新地图标注。
- 性能：轨迹点较多时，前端可做线段简化（Douglas-Peucker）或服务端降采样以减少客户端渲染压力。
- 鉴权：DeviceDetail 的下发命令与围栏创建属于敏感操作，建议在后端加 RBAC（仅管理员或具备相应权限的用户可执行），并在前端隐藏/禁用非授权用户的操作按钮。

---
