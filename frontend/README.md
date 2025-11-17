# frontend - Vue 3 前端子工程

这是一个最小的 Vite + Vue3 前端骨架，用于替换或并行部署现有的 Thymeleaf 页面。

主要特点：
- Vue 3 + Vite 开发体验（热重载）
- 路由：/login, /register, /home
- 已配置 Vite 代理：将 /api 请求代理到 `http://localhost:8080`（开发时避免跨域）

快速开始（Windows cmd）：
1) 进入目录并安装依赖：

```cmd
cd frontend
npm install
```

2) 启动开发服务器：

```cmd
npm run dev
```

构建：

```cmd
npm run build
```

构建完成后，产物在 `frontend/dist`，可将其内容复制到后端的 `src/main/resources/static/` 目录下，以便由 Spring Boot 同一 Jar 提供静态文件：

```cmd
xcopy /E /I /Y frontend\dist ..\src\main\resources\static\
```

注意事项：
- CSRF：仓库后端默认启用 CSRF，并对 `/api/**` 放行。前端示例中 `Register.vue` 使用 `/api/register`（JSON）接口进行注册以规避 CSRF，`Login.vue` 使用的是传统表单提交到 `/login`（在部署到后端静态目录并同域访问时能正常工作）。
- 如果后端注册接口不是 `/api/register`，请调整 `Register.vue` 或 `src/api/auth.js` 中的路径。
- 生产环境请使用 HTTPS 与后端鉴权机制，避免把未鉴权的管理接口暴露到公网。

如果你希望我：
- 将注册与登录都改为通过 `/api/**` 的 REST 接口调用（需要后端暴露相应 API）；
- 或者把 Vite 构建产物自动复制到后端静态目录并在 Gradle 构建任务中集成（Windows/Gradle 脚本）；
我可以继续为你实现。

## 新增页面（本次迭代）
前端新增并实现了以下页面组件（均位于 `frontend/src/views`）：

- `Dashboard.vue`：系统仪表盘，展示设备总数、在线设备数、今日报警和最近日志摘要（演示数据，建议接入后端统计接口）。
- `Devices.vue`：设备列表，支持分页/刷新与跳转到设备详情；使用 `src/api/device.js` 与后端 `GET /api/devices` 交互获取数据。
- `DeviceDetail.vue`：设备详情页面，展示设备基础信息、最近位置和日志摘要（演示 mock，可按需接入真实 API）。
- `Realtime.vue`：实时监控页面（演示），显示在线设备列表与地图占位；生产可接入 WebSocket/SSE 或第三方地图组件（Leaflet/Mapbox）。
- `Settings.vue`：系统设置页面（示例），用于配置数据保存目录与轮询间隔（需配合后端 API 实现持久化）。

公共组件：
- `components/NavBar.vue`：全局导航栏，包含仪表盘、设备、实时、设置等入口，所有页面已集成此组件（便于统一导航与 UX）。

## 路由（`frontend/src/router/index.js`）
当前路由（开发用）：
- `/` -> 重定向到 `/dashboard`
- `/login` -> 登录页（使用现有 `Login.vue`）
- `/register` -> 注册页（使用现有 `Register.vue`）
- `/dashboard` -> 仪表盘
- `/devices` -> 设备列表
- `/devices/:id` -> 设备详情
- `/realtime` -> 实时监控
- `/settings` -> 系统设置

## 前端与后端交互（简要）
- 设备相关接口（由后端 `DeviceController` 提供）：
  - `GET /api/devices?limit={limit}&offset={offset}` — 列表（`src/api/device.js` 的 `list`）
  - `GET /api/devices/{id}` — 获取设备详情（`get`）
  - `GET /api/devices/by-imei/{imei}` — 按 IMEI 查询（`getByImei`）
  - `POST /api/devices` — 创建设备（`create`）
  - `PUT /api/devices/{id}` — 更新（`update`）
  - `DELETE /api/devices/{id}` — 删除（`remove`）

- 身份与认证：本仓库后端使用 Spring Security；开发环境下：
  - `Login.vue` 当前使用传统表单提交到 `/login`（与后端表单登录配合）
  - `Register.vue` 使用 `/api/register` 的 REST 接口进行注册（示例）

- CORS / CSRF：开发时 Vite dev-server 已配置代理把 `/api` 转发到 `http://localhost:8080`，以避免跨域问题；生产部署时请使用 HTTPS 并根据后端 CSRF/认证策略调整。

## 本地开发 & 运行（Windows cmd）
1) 安装依赖并启动前端开发服务器（热重载）
```cmd
cd frontend
npm install
npm run dev
```
默认 dev-server 会打开或监听 `http://localhost:5173`，并通过 Vite 的代理把 `/api` 转发到后端 `http://localhost:8080`（请确保后端在 8080 端口运行，或修改 `vite.config.js` 的代理设置）。

2) 构建生产静态文件并部署到后端（将构建产物复制到后端静态目录）
```cmd
cd frontend
npm run build
xcopy /E /I /Y frontend\dist ..\src\main\resources\static\
```
然后重新构建后端 Jar 并运行（Gradle）
```cmd
cd ..\
gradlew.bat clean assemble -x test
java -jar build\libs\demo2.jar
```

3) 本地测试页面与后端交互（示例）
- 启动后端（8080）和前端 dev-server（5173），在浏览器打开 `http://localhost:5173/dashboard`。
- 访问设备页面 `/devices`，页面会调用 `GET /api/devices` 获取设备列表；确认后端 `DeviceController` 已启动并可访问。
- 在设备详情页面 `/devices/{id}`，组件会调用 `GET /api/devices/{id}` 获取设备信息（若后端不存在示例 id，可先使用 `/api/devices` 创建或在数据库中插入）。

## 开发建议与后续改进
- 鉴权：目前前端未实现完整的令牌或会话保持逻辑（依赖 Spring Security 的表单登录）；建议使用 REST login + JWT 实现单页应用的无缝鉴权与 API 调用控制。
- 实时数据：`Realtime.vue` 建议使用 WebSocket 或 SSE 来实现设备的实时位置与告警流，并将地图集成（Leaflet/Mapbox/Google Maps）。
- 日志与审计：设备详情页可提供“下载日志”按钮，调用后端按 IMEI 导出对应设备的日志文件（后端已按设备写日志到磁盘，可扩展导出 API）。
