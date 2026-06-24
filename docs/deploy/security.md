# 安全加固清单

部署上线后，按本清单逐项核对。每项给出"如何验证"。**所有项目应在部署 24 小时内完成**。

---

## SSH

### 1. 禁用密码登录（密钥专用）

**为什么**：SSH 密码可被暴力破解，密钥（2048+ bit）几乎不可能。

**怎么做**：

```bash
# 1. 上传 SSH 公钥（如未上传）
ssh-copy-id deploy@<公网IP>

# 2. 在服务器上验证密钥登录 OK
ssh deploy@<公网IP> "echo ok"

# 3. 关掉密码登录
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl reload ssh

# 4. 不要关闭当前会话 —— 另开终端验证密钥登录仍然可用
```

**验证**：

```bash
ssh -o PubkeyAuthentication=yes -o PasswordAuthentication=no deploy@<ip> "echo ok"
# 必须成功

ssh -o PubkeyAuthentication=no deploy@<ip>
# 必须失败（Permission denied (publickey)）
```

### 2. 禁用 root SSH 登录

`bootstrap.sh` 已经设置了 `PermitRootLogin no`。

**验证**：

```bash
ssh root@<ip>
# 必须失败：Permission denied (publickey)
```

### 3. fail2ban 已启用

**验证**：

```bash
sudo fail2ban-client status sshd
# 期望：Jail listed: sshd
```

被封的 IP：

```bash
sudo fail2ban-client status sshd | grep -E "Banned IP"
```

手动解封：

```bash
sudo fail2ban-client set sshd unbanip <ip>
```

---

## 防火墙

### 4. UFW 规则最小化

`bootstrap.sh` 默认开 22 / 8080 / 9000，**未**开 3306 / 9090。

**验证**：

```bash
sudo ufw status verbose
# 期望：22/tcp, 8080/tcp, 9000/tcp ALLOW IN; 9090/tcp 未列出
```

如果需要调试 TCP echo 服务（端口 9090）：

```bash
sudo ufw allow 9090/tcp comment 'Smart Watch TCP echo (debug only)'
```

不需要的话不要开。

### 5. 云控制台安全组

UFW 是服务器内的防火墙，**云控制台的安全组/网络 ACL 是服务器外的第一道防线**。两者都需要配置。

在阿里云 / AWS / 腾讯云控制台：

- **入站**：放行 22（限定你的 IP 或公司 IP 段）、8080（公网）、9000（公网）。
- **出站**：默认全放行（应用需要拉镜像、调用高德 API）。
- **3306**：**绝对不要**开（数据库仅容器内部访问）。
- **9090**：不开。

### 6. MySQL 不对外暴露

`docker-compose.yml` 中 `mysql` 服务的 `ports:` 段如果加了 `3306:3306`，**删掉**——容器内的 MySQL 通过 `mysql:3306` 服务名在 Docker 网络内访问即可。

**验证**：

```bash
# 从 deploy 用户的本地终端看 3306 是否监听 0.0.0.0
sudo ss -tlnp | grep 3306
# 期望：127.0.0.1:3306 或 docker bridge IP，不应有 0.0.0.0:3306
```

---

## 密钥管理

### 7. `.env` 权限

`deploy.sh` 已经设置 `chmod 600`。

**验证**：

```bash
ls -la ~/smart-watch/.env
# 期望：-rw------- 1 deploy deploy ... .env
```

### 8. 密钥轮换计划

每次怀疑泄露或每 6 个月轮换一次：

```bash
# 1. 备份当前 .env
cp ~/smart-watch/.env ~/smart-watch/.env.bak.$(date +%s)

# 2. 生成新密码
NEW_ROOT=$(openssl rand -hex 16)
NEW_USER=$(openssl rand -hex 16)

# 3. 替换 .env
sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${NEW_ROOT}|" ~/smart-watch/.env
sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=${NEW_USER}|" ~/smart-watch/.env
chmod 600 ~/smart-watch/.env

# 4. 在 MySQL 容器里也改密码（密码必须与 .env 一致）
docker compose -C ~/smart-watch exec mysql \
    mysql -uroot -p"${OLD_ROOT}" \
    -e "ALTER USER 'smart_user'@'%' IDENTIFIED BY '${NEW_USER}'; FLUSH PRIVILEGES;"

# 5. 重启 app 让它读到新密码
docker compose -C ~/smart-watch restart app
```

### 9. AMAP / MPBAND 密钥

`AMAP_WEB_KEY` 和 `MPBAND_RESPONSE_KEY` 写在 `.env` 中。如果不小心提交了：

1. 立即在高德 / 设备控制台撤销该密钥。
2. 申请新密钥。
3. 改 `.env`。
4. `docker compose up -d app`。

---

## 应用层

### 10. 修改默认 admin 密码

部署后立即改 `admin / admin123`。Web UI 登录后 → 个人中心 → 修改密码。

### 11. JWT 密钥

`application.properties` 中 `app.jwt.secret` 当前是占位值。生产环境必须改成强随机：

```bash
# 生成 64 字节 base64 密钥
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# 写入 .env
echo "APP_JWT_SECRET=${JWT_SECRET}" >> ~/smart-watch/.env

# 同时改 application.properties 或在 application-docker.properties 里覆盖
# 让 Spring 读取 ${APP_JWT_SECRET}

docker compose -C ~/smart-watch up -d app
```

> **如果当前代码不读 `APP_JWT_SECRET` 环境变量**（需要确认），那就在 `application-docker.properties` 中加一行 `app.jwt.secret=${APP_JWT_SECRET:...}` 并同步改动。

### 12. CORS

当前应用是**前后端同源**（SPA 嵌入 jar），所以 CORS 不是问题。如果将来拆开前端单独部署，必须：

- 在 `SecurityConfig.java` 加 `CorsConfigurationSource` Bean。
- 仅允许 `https://你的域名.com`（不要 `*`）。
- 启用 `allowCredentials(true)`。

### 13. 限流

`build.gradle` 包含 `spring-boot-starter-actuator` 但**不包含** `spring-boot-starter-data-redis` 的限流 starter。当前应用**没有**接口限流。

如果担心被刷接口，在 Nginx 反向代理层加限流（不在本计划范围）。

---

## 自动更新

### 14. unattended-upgrades 已启用

**验证**：

```bash
sudo tail /var/log/unattended-upgrades/unattended-upgrades.log
# 期望：有最近运行记录（每天一次）
```

### 15. 每月手动升级

`unattended-upgrades` 只装安全补丁。大版本升级（Ubuntu 22.04 → 24.04、Docker 27 → 28）需要手动：

```bash
# 系统包（每季度）
sudo apt update && sudo apt upgrade -y

# Docker（按 Docker 官方公告）
sudo apt update && sudo apt upgrade -y docker-ce docker-ce-cli containerd.io

# MySQL（每 6-12 月；建议先在测试环境验证）
# 改 docker-compose.yml 中的 image tag: mysql:8.0 → mysql:8.4
cd ~/smart-watch
docker compose pull mysql
# 在测试环境验证后再上生产
```

---

## 监控与告警

### 16. 健康检查日志被监控

`/home/deploy/logs/healthcheck.log` 应被某种监控系统读取（Slack / 企业微信 / Prometheus / Loki）。

详见 [operations.md](./operations.md#监控)。

### 17. 审计日志

```bash
# SSH 登录历史
last -F

# sudo 使用历史
sudo journalctl -u sudo

# Docker 事件
docker events --since='24h' --until='0m'
```

---

## 加固清单（自检表）

部署 24 小时内对照：

- [ ] SSH 密钥登录可用，密码登录已禁用
- [ ] root SSH 登录已禁用
- [ ] fail2ban 启用并能查 sshd jail
- [ ] UFW 仅 22 / 8080 / 9000 开
- [ ] 云控制台安全组最小化（不开 3306）
- [ ] MySQL 端口在宿主机 127.0.0.1 而非 0.0.0.0
- [ ] `.env` 权限 600
- [ ] AMAP / MPBAND / JWT 密钥已轮换
- [ ] admin 默认密码已修改
- [ ] 健康检查日志接入告警（Slack / 邮件）
- [ ] 自动备份 cron 已部署
- [ ] unattended-upgrades 启用

每季度：

- [ ] `apt upgrade` 跑过
- [ ] 备份可恢复（演练一次 restore.sh）
- [ ] `.env` 密钥考虑轮换
- [ ] 审计日志有人看过
