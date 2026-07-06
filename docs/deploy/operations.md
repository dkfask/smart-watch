# Day-2 运维手册

部署上线后，日常需要做的是：更新应用、备份恢复、监控、故障排查。本手册覆盖这些操作。

---

## 更新应用

每次有新提交推到 Git 后，在服务器上跑：

```bash
ssh deploy@<公网IP>
cd ~/smart-watch
bash scripts/deploy/update.sh --backup
```

`--backup` 标志会先跑一次 `backup.sh`，备份失败则中止更新。

更新流程：

1. `git pull --ff-only`（拒绝合并提交，强制本地解决冲突）。
2. `docker compose build app`（只重建 app，不动 MySQL）。
3. `docker compose up -d app`（约 30 秒停机窗口）。
4. 等待 `/actuator/health` 返回 200。
5. 打印 `Last commit: <sha>`。

可选加 `--prune` 清理无用镜像：

```bash
bash scripts/deploy/update.sh --backup --prune
```

> **零停机升级**目前未实现——停机窗口约 30 秒（Spring Boot 启动 + Netty 绑定 + Hikari 预热）。如需蓝绿部署 / 滚动升级，需要进一步引入 Nginx + 多副本 + 健康检查路由，不在本手册范围。

---

## 备份与恢复

### 备份

`backup.sh` 由 cron 自动跑（每天 03:00）。手动跑：

```bash
bash ~/smart-watch/scripts/deploy/backup.sh
```

输出示例：

```
[backup] OK: /home/deploy/backups/smart_watch_20260623-030000.sql.gz (4.2M); pruned 0 backup(s) older than 14 days.
```

特性：

- `--single-transaction`：InnoDB 表的一致性快照，不锁库。
- `--routines --triggers --events`：保留存储过程、触发器、事件。
- `--hex-blob`：二进制字段（如 IMEI 头像）不丢。
- `gunzip -t` 校验完整性。
- 自动删除 14 天前的旧备份。

### 远程备份（可选）

`backup.sh` 只写到本地 `/home/deploy/backups`。要异地容灾，加一行 cron 把它们 rsync 到对象存储 / 另一台机器：

```cron
# 备份后同步到 OSS（每天 03:30，需要先安装 ossutil / awscli / rclone）
30 3 * * * /usr/bin/rclone sync /home/deploy/backups/ remote:smart-watch-backups/ >> /home/deploy/logs/rclone.log 2>&1
```

### 恢复

```bash
# 1. 看下哪些备份文件存在
ls -la /home/deploy/backups/

# 2. 选一个恢复（交互确认）
bash ~/smart-watch/scripts/deploy/restore.sh /home/deploy/backups/smart_watch_20260623-030000.sql.gz

# 3. 脚本化恢复（跳过交互确认）
bash ~/smart-watch/scripts/deploy/restore.sh /home/deploy/backups/smart_watch_20260623-030000.sql.gz --yes
```

流程：

1. 校验 `.sql.gz` 完整性。
2. 交互确认（或 `--yes`）。
3. `docker compose stop app`（停止应用写入）。
4. `DROP DATABASE` + `CREATE DATABASE`（utf8mb4）。
5. `zcat | mysql` 导入。
6. `docker compose start app`。
7. 等健康。

如果导入失败，数据库处于不一致状态——此时**不要**启动 app，先检查 dump 文件是否完整、磁盘空间是否充足。

---

## 监控

### 健康检查

`healthcheck.sh` 由 cron 每 5 分钟跑一次。失败时追加一行到 `/home/deploy/logs/healthcheck.log`：

```
2026-06-23T14:35:00+08:00 FAIL http://localhost:8080/actuator/health
```

接入告警的几种方式：

1. **日志聚合**（推荐）：把 `/home/deploy/logs/healthcheck.log` 推到 Loki / ELK，配置告警规则。
2. **Prometheus Blackbox Exporter**：跑一个 exporter 探测 `/actuator/health`，由 Alertmanager 路由。
3. **简单 webhook**：写个 5 行的脚本扫日志，匹配 `FAIL` 时 POST 到企业微信 / 飞书 / Slack。

`healthcheck.sh` 故意**不**内置告警——告警渠道属于运维架构选择，不该硬编码到部署脚本里。

### Docker 状态

```bash
# 服务运行状态
docker compose ps

# 实时资源占用
docker stats

# 容器内进程
docker compose top

# 磁盘占用
docker system df
```

### 系统资源

```bash
# 内存 / CPU / 负载
htop

# 磁盘占用（找 docker 卷）
du -sh /var/lib/docker/volumes/*

# Docker 日志总大小
du -sh /var/lib/docker/containers/*/
```

### 日志查看

便捷封装：

```bash
# 看应用最近 200 行并跟踪
bash ~/smart-watch/scripts/deploy/logs.sh app

# 看 MySQL 最近 200 行
bash ~/smart-watch/scripts/deploy/logs.sh mysql

# 两个服务都看
bash ~/smart-watch/scripts/deploy/logs.sh
```

直接用 `docker compose` 命令也行：

```bash
# 最近 10 分钟的 app 日志
docker compose logs --since=10m app

# 跟踪最新
docker compose logs -f app

# 最近 500 行 + 跟踪
docker compose logs --tail=500 -f app
```

---

## 常见问题

### 1. 端口 8080 / 9000 已被占用

症状：`docker compose up -d` 时 `app` 反复重启，日志含 `IllegalStateException: address already in use`。

原因：宿主机或别的服务占了端口。

处理：

```bash
# 在服务器上看谁在占
sudo ss -tlnp | grep -E ':(8080|9000)'

# 改 .env 中的端口映射
nano ~/smart-watch/.env
# 修改 APP_HTTP_PORT 或 APP_MPBAND_PORT，例如改成 8081 / 9001

# 重启
cd ~/smart-watch
docker compose up -d app
```

### 2. 容器 OOM（Out of Memory）

症状：`docker stats` 显示 app 内存接近上限；日志含 `OutOfMemoryError` 或被 OOM killer 杀掉。

处理：

```bash
# 选项 A：升服务器内存（推荐）
# 选项 B：限制 JVM 堆占比（.env 的 JAVA_OPTS）
nano ~/smart-watch/.env
# 添加：JAVA_OPTS=-XX:MaxRAMPercentage=50
docker compose up -d app
```

### 3. 磁盘满

症状：`docker compose up` 报 `no space left on device`；MySQL 启动失败。

处理：

```bash
# 看哪个目录占得多
du -sh /var/lib/docker/* | sort -h | tail

# 清理无用镜像（保留 smart-watch 相关）
docker image prune -a

# 清理停止的容器、悬空卷
docker system prune

# 谨慎：清理卷（会删数据！）
docker volume ls
docker volume rm <unused-volume-name>
```

### 4. MySQL 启动失败

```bash
docker compose logs mysql
```

常见原因：

- 端口 3306 被占（用 `sudo ss -tlnp | grep 3306` 查）。
- 数据卷损坏（`docker volume inspect smart-watch_mysql_data` 看路径，可能需要 `docker compose down -v` 重置——**会丢数据**）。
- `/etc/docker/daemon.json` 语法错（`sudo dockerd --validate` 或 `sudo systemctl status docker`）。

### 5. 备份失败

```bash
# 看日志
tail -20 /home/deploy/logs/backup.log
bash ~/smart-watch/scripts/deploy/backup.sh
```

常见原因：

- `.env` 中 `MYSQL_ROOT_PASSWORD` 与数据库实际不符（改过密码但没同步）。
- MySQL 容器没运行（`docker compose ps`）。
- 磁盘满。

### 6. 恢复后 admin 登录失败

`01_full_database_schema.sql` 第 521 行的 BCrypt 哈希 `$2a$10$N.zmdr9k7uOCQb376NoUnuTBv38Eo1r2d7X17lFJU8o093YwM1Efe` 对应明文 `admin123`。

如果你的恢复来源是其他 schema 文件（比如开发者手动改过），密码哈希可能不同。**`SecurityDataInitializer`（`CommandLineRunner`）每次启动都会幂等地创建 `admin/admin123` 和 `user/user123`**——所以即使你恢复了别人改过 schema 的数据库，只要 `ddl-auto=update` 或容器重启时 JPA 校验通过，admin 用户应该会自动恢复。

如果还是登不上：

```bash
# 临时把 ddl-auto 改回 update，重启一次
# 编辑 docker-compose.yml：去掉 SPRING_PROFILES_ACTIVE=docker 或者编辑 application-docker.properties
docker compose restart app
# 然后 SecurityDataInitializer 会兜底创建 admin
```
