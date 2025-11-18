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

## 地图组件（LeafletMap）

此项目包含一个可复用的地图库组件 `LeafletMap`，文件位置：`frontend/src/components/LeafletMap.vue`。
该组件基于 Leaflet 与 leaflet-draw，提供：
- 展示点（markers）、轨迹（tracks）、围栏（fences）
- 可选的画围栏/编辑/删除（editable 模式）
- 通过事件把围栏的创建/编辑/删除回传给父组件

下面是快速使用说明（中文）：

1) 安装依赖（在 `frontend` 目录下执行，Windows cmd）：

```cmd
cd frontend
npm install leaflet leaflet-draw
```

> 注意：项目中已在 `LeafletMap.vue` 内部使用了 Leaflet 的图片资源（marker 图标等）。需要确保 Vite 或打包器能够处理图片资源（默认 Vite 可以）。同时在全局样式或入口处引入 Leaflet CSS：
>
> 在 `frontend/src/main.js`（或 `main.ts`）中加入：
>
> ```js
> import 'leaflet/dist/leaflet.css'
> import 'leaflet-draw/dist/leaflet.draw.css'
> ```

2) 组件 props（输入）说明：
- center: Array - [lat, lon]，默认 [22.383, 114.0823]
- zoom: Number - 地图缩放级别，默认 13
- markers: Array - 标记点数组，元素格式为 { id, lat, lon, label }，label 会显示为弹窗
- tracks: Array - 轨迹，可为：
  - [[lat, lon], ...] 的坐标数组，或
  - [{ latitude, longitude } | { lat, lon }] 的对象数组
- fences: Array - 围栏数组，支持 GeoJSON Feature、Geometry（Polygon）或包含 geometry 的对象
- editable: Boolean - 是否启用绘制/编辑工具（leaflet-draw），默认 false
- height: String - 地图容器高度（例如 '400px'），默认 '400px'

3) 组件事件（输出）说明：
- `fence-created` — 当用户在可编辑模式下新建围栏时触发，payload 举例：
  - 圆：{ type: 'circle', center: [lat, lon], radius: r（米）, geojson }
  - 多边形/矩形：{ type: 'geojson', geojson }
- `fence-edited` — 编辑后触发，payload 同上（包含 geojson）
- `fence-deleted` — 删除后触发，payload 同上（包含 geojson）
- `marker-click` — 点击地图标记时触发，payload 为该标记对象（{id, lat, lon, label}）
- `map-ready` — 地图初始化完成后触发，payload 为 Leaflet 的 map 实例

4) 父组件可调用的方法：
- 组件暴露 `centerOn(lat, lon, zoom)` 方法（通过 `ref` 访问），用于让地图移动到某个坐标并可选择设置缩放级别，例如：

示例（Vue 3 `<script setup>` 风格）：

```vue
<script setup>
import { ref } from 'vue'
import LeafletMap from './components/LeafletMap.vue'

const markers = ref([
  { id: 'dev1', lat: 22.383, lon: 114.0823, label: '设备 1' }
])

const fences = ref([])
const mapRef = ref(null)

function onMarkerClick(m) {
  console.log('marker clicked', m)
}

function onFenceCreated(payload) {
  console.log('fence created', payload)
  // 可以把 payload.geojson 发送到后端保存
}

function zoomToDevice() {
  if (mapRef.value && mapRef.value.centerOn) {
    mapRef.value.centerOn(22.383, 114.0823, 15)
  }
}
</script>

<template>
  <button @click="zoomToDevice">聚焦设备</button>
  <LeafletMap
    ref="mapRef"
    :markers="markers"
    :fences="fences"
    :editable="true"
    height="500px"
    @marker-click="onMarkerClick"
    @fence-created="onFenceCreated"
  />
</template>
```

5) 关于 fences 的格式与保存建议：
- 组件在绘制/编辑后会回传 GeoJSON（通过 `layer.toGeoJSON()`）。建议后端以 GeoJSON 格式保存围栏，便于后续加载并直接传回组件的 `fences` prop。
- 对于圆形围栏，组件会额外返回 center 与 radius（米），因为 GeoJSON 标准并未直接表达圆的半径信息。

6) 常见问题与调试：
- 地图标记图标不见了：通常是因为打包器没有正确处理 leaflet 的图片资源。`LeafletMap.vue` 已包含把图片合并到 Icon 的代码（通过 import），在 Vite 下默认可工作；若出现问题，请检查构建输出和资源路径。
- 瓦片加载慢或被屏蔽：组件默认使用 OpenStreetMap 公共瓦片服务，生产环境请替换为有 SLA 的瓦片服务或 CDN。
- 样式冲突：确保全局引入 Leaflet CSS（见上文），否则控件样式和画布可能错位。

7) 扩展建议（可选）：
- 如果需要更多地图功能（热力图、聚合、地图叠加），可以在 `LeafletMap.vue` 的基础上扩展或把组件拆分成更细粒度的子组件。
- 若项目对 IE 或较旧浏览器有兼容性要求，请根据 Leaflet 与 leaflet-draw 的兼容性说明处理 polyfill。

---

以上为 `LeafletMap` 组件的快速上手与使用说明，已覆盖安装、props、事件、示例与常见注意事项。如需我将 README 中的示例代码改为 Options API 风格或把 CSS 引入改为按需引入（仅在该组件内），我可以继续修改文档或代码。
