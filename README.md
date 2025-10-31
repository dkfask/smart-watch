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


