# 故障排查

按症状 → 诊断 → 解决的格式整理。遇到问题时先看症状匹配，依次尝试。

---

## 应用层

### 症状：浏览器打不开 `http://<公网IP>:8080`

**诊断**：

```bash
# 1. 在服务器本地能访问吗？
curl -fsS http://localhost:8080/actuator/health
# → {"status":"UP"}  说明应用在跑，问题在外网

# 2. 防火墙
sudo ufw status
# 期望：8080/tcp ALLOW IN

# 3. 容器是否 healthy？
cd ~/smart-watch && docker compose ps

# 4. 应用日志
docker compose logs --tail=50 app
```

**常见原因**：

| 原因 | 解决 |
|------|------|
| UFW 没放行 8080 | `sudo ufw allow 8080/tcp` |
| 云控制台安全组没放行 8080 | 在云控制台（阿里云/AWS/腾讯云）安全组加规则 |
| 容器在 restarting（健康检查不过） | 看应用日志找具体错误 |
| 端口被别的服务占了 | `sudo ss -tlnp | grep 8080` |

---

### 症状：`docker compose ps` 显示 app `Restarting` 状态

**诊断**：

```bash
# 看最近 100 行应用日志
cd ~/smart-watch
docker compose logs --tail=100 app
```

**常见错误**：

#### `IllegalStateException: address already in use`

9000 端口被占（通常是 `MpbandServer`）。

```bash
# 看谁占的
sudo ss -tlnp | grep 9000

# 选项 A：杀掉占用的进程
sudo kill <pid>

# 选项 B：改 .env 用别的端口
nano .env
# APP_MPBAND_PORT=19000
docker compose up -d app
```

#### `Communications link failure`

MySQL 还没就绪或密码错。

```bash
# 1. 看 MySQL 状态
docker compose ps mysql
# 期望：Up (healthy)

# 2. 看 MySQL 日志
docker compose logs --tail=50 mysql

# 3. 直接连测
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1;"
```

如果 MySQL 是 healthy 但 app 仍报这条：检查 `.env` 中 `DB_URL` 是否 `jdbc:mysql://mysql:3306/...`（`mysql` 是 compose 服务名，不能是 `localhost`）。

#### `Access denied for user 'smart_user'`

`.env` 中的 `MYSQL_PASSWORD` 与 init SQL 中的不一致。

```bash
# 看实际 init 的密码（首次启动的日志）
docker compose logs mysql | grep -i "password"

# 修正 .env，让 MYSQL_PASSWORD 与 init SQL 中的密码一致
# 默认 init SQL 用的是 'smart123'（参见 docker/mysql/init/01_full_database_schema.sql 第 543 行）
```

**根本修复**：要么改 `.env` 让 `MYSQL_PASSWORD=smart123`，要么改 init SQL 后 `docker compose down -v` 重建（**会丢数据**）。

#### `OutOfMemoryError` 或容器被 OOM Killer 杀

```bash
# 看 OOM 日志
dmesg | grep -i "oom\|killed process"
# 或
sudo journalctl -k | grep -i "oom"
```

**解决**：

- 选项 A：升服务器内存（推荐，最少 4 GB）。
- 选项 B：限制 JVM 堆。在 `.env` 加：
  ```
  JAVA_OPTS=-XX:MaxRAMPercentage=50
  ```
  然后 `docker compose up -d app`。

---

### 症状：登录返回 401 / "用户名或密码错误"

**诊断**：

```bash
# 看 users 表里有没有 admin
docker compose exec mysql \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch \
    -e "SELECT username, role FROM users;"
```

**常见原因**：

1. **恢复过自定义 dump，BCrypt 哈希变了** → 跑一次 `SecurityDataInitializer`（重启 app 即可）。注意：容器内 `ddl-auto=none`，SecurityDataInitializer 不会写库——它走 `users` 表，如果表存在但 admin 行不在，会尝试 `INSERT`。如果 `INSERT` 因为 schema 差异失败，则需要手动：
   ```bash
   docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch \
       -e "INSERT IGNORE INTO users (username, password, email, phone, real_name, role, status) VALUES
           ('admin', '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTBv38Eo1r2d7X17lFJU8o093YwM1Efe', 'admin@example.com', '13800138000', '系统管理员', 'admin', 'active');"
   ```
2. **应用重启后还没初始化完** → 等几秒再试。

---

## 数据库层

### 症状：MySQL 容器 `Restarting` 或 `Exit 1`

**诊断**：

```bash
docker compose logs --tail=100 mysql
```

**常见原因**：

#### `Permission denied` 在 `/var/lib/mysql`

数据卷权限错。

```bash
# 第一次启动时数据卷空，重新跑 init。
# 注意：会保留数据，但若 init 已部分写入可能冲突。
docker compose down -v   # ⚠ 会删数据，仅在能 restore 时用
docker compose up -d mysql
```

#### `Can't start server: Bind on TCP/IP port ... Address already in use`

3306 被占。

```bash
sudo ss -tlnp | grep 3306
# 杀掉或改 docker-compose.yml 把 mysql 端口映射删掉（推荐）
```

#### `Unknown collation: 'utf8mb4_0900_ai_ci'`

MySQL 8 默认 collation 与导入的 SQL 不一致。

```bash
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW VARIABLES LIKE 'collation%';"
```

如果导入了 MySQL 5.7 的 dump，把里面的 `utf8mb4_0900_ai_ci` 替换成 `utf8mb4_unicode_ci`。

---

### 症状：磁盘满导致 MySQL 启动失败

```bash
df -h /var/lib/docker
du -sh /var/lib/docker/volumes/* | sort -h | tail
```

清理：

```bash
# 清理无用镜像
docker image prune -a

# 清理悬空卷（不删 smart-watch_mysql_data）
docker volume prune

# 清理旧备份（14 天前的已自动删，30 天前的也清）
find /home/deploy/backups -name '*.sql.gz' -mtime +30 -delete
```

---

## 网络层

### 症状：手表 / 手环连不上 `公网IP:9000`

**诊断**：

```bash
# 1. 应用是否在监听 9000？
docker exec smart-watch-app ss -tlnp | grep 9000
# 期望：0.0.0.0:9000  LISTEN

# 2. UFW 是否开 9000？
sudo ufw status | grep 9000

# 3. 云控制台安全组是否开 9000？（在控制台看）

# 4. 从服务器本地测端口可达
nc -zv localhost 9000

# 5. 从设备所在网络测公网 IP 端口
nc -zv <公网IP> 9000
```

**常见原因**：

| 失败点 | 现象 | 解决 |
|--------|------|------|
| 应用没监听 9000 | `ss` 无输出 | 看 `docker compose logs app | grep -i mpband` |
| UFW 拦截 | 服务器本地 nc 通，外网不通 | `sudo ufw allow 9000/tcp` |
| 云安全组拦截 | 服务器本地 + 外网 nc 都不通 | 云控制台加规则 |
| 设备在企业内网 | 设备能上网但 9000 被企业网关拦 | 让设备切到 4G / 不同网络测 |

---

### 症状：可以访问 8080，但接口超时

```bash
# 看应用是否在处理请求
docker compose logs --tail=100 app | grep -i timeout
```

如果大量 `ConnectionPool` 报错，可能是 Hikari 池耗尽：

```bash
docker compose exec mysql \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW PROCESSLIST;"
# 看连接数
```

调大 `application-docker.properties` 的 `spring.datasource.hikari.maximum-pool-size` 然后重启。

---

## 备份与恢复

### 症状：`backup.sh` exit non-zero

```bash
# 跑一遍带详细输出
bash -x ~/smart-watch/scripts/deploy/backup.sh
```

**常见原因**：

- `.env` 不存在或权限错。
- MySQL 容器没跑。
- 备份目录磁盘满。
- `docker compose` 命令路径错（脚本依赖在 `~/smart-watch` 下运行）。

---

### 症状：`restore.sh` 后 admin 登不上

详见上面的 "登录返回 401"。常见：恢复了自定义 dump，BCrypt 哈希不是 `admin123`。

**应急方案**：手动重置 admin 密码：

```bash
# 临时改 ddl-auto 让 SecurityDataInitializer 能跑（或用 SQL 强制覆盖）
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch <<'SQL'
DELETE FROM users WHERE username='admin';
INSERT INTO users (username, password, email, phone, real_name, role, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTBv38Eo1r2d7X17lFJU8o093YwM1Efe',
        'admin@example.com', '13800138000', '系统管理员', 'admin', 'active');
SQL
```

然后用 `admin / admin123` 登录，立即改密码。

---

## 容器化层

### 症状：`docker compose build` 失败

```bash
cd ~/smart-watch
docker compose build --no-cache app 2>&1 | tail -50
```

**常见原因**：

- **网络问题**：Docker Hub / Maven Central 拉不到镜像 → `docker pull eclipse-temurin:21-jdk` 手动试。
- **磁盘满**：`df -h /var/lib/docker`。
- **内存不够构建**：Gradle 在 frontend 阶段需要 ~1 GB，backend 阶段需要 ~1.5 GB。低于 2 GB 内存的服务器会失败。

---

### 症状：更新后应用起不来

```bash
# 看完整启动日志
docker compose logs --tail=200 app

# 看是不是数据库迁移问题
docker compose exec mysql \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_watch \
    -e "SHOW TABLES;"
```

如果 schema 不匹配（新增字段在 `device_alert` 而新代码用 `alarms`）：

- 选项 A：回退代码：`git checkout HEAD~1 && bash scripts/deploy/update.sh`
- 选项 B：手动同步 schema（不推荐）。

---

## 系统层

### 症状：服务器 SSH 连不上

1. **先验证服务器本身**：在云控制台的"连接"功能 / VNC 登录上去。
2. **看 `/var/log/auth.log`**：通过控制台的 VNC 看 `sudo tail -50 /var/log/auth.log`。
3. **常见原因**：
   - 误操作改了 `/etc/ssh/sshd_config`。
   - fail2ban 把当前 IP 封了。
   - 云控制台安全组被改。

**应急**：在云控制台重启服务器 → VNC 进入 → 修 sshd_config 或解封 IP。

---

## 收集诊断信息

提交 issue / 求助前，收集以下信息：

```bash
cd ~/smart-watch

echo "=== OS ==="
cat /etc/os-release | grep -E "PRETTY_NAME|VERSION_ID"

echo "=== Resources ==="
free -h
df -h /var/lib/docker

echo "=== Docker ==="
docker --version
docker compose version
docker compose ps

echo "=== UFW ==="
sudo ufw status

echo "=== App logs (last 100 lines) ==="
docker compose logs --tail=100 app

echo "=== MySQL logs (last 50 lines) ==="
docker compose logs --tail=50 mysql

echo "=== Health ==="
curl -fsS http://localhost:8080/actuator/health || echo "UNREACHABLE"

echo "=== Recent backups ==="
ls -lat /home/deploy/backups/ | head -10

echo "=== Health check log (last 20 lines) ==="
tail -20 /home/deploy/logs/healthcheck.log 2>/dev/null || echo "no healthcheck.log"

echo "=== Last update log ==="
git log --oneline -5
```

把以上输出保存到文件发给支持人员。
