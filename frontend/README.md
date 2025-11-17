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

