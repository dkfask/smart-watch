# 云服务器部署指南

本指南说明如何把 Smart Watch Monitoring System 部署到一台全新的 Ubuntu 云服务器。所有命令**均在服务器上执行**——你只需要把本仓库克隆到服务器，然后用 `deploy` 用户运行两个脚本即可上线。

---

## 总览

部署目标：在一台空白的 Ubuntu 22.04 / 24.04 LTS 服务器上运行 `bootstrap.sh`（root，一次）+ `deploy.sh`（deploy，一次），得到：

| 资源 | 端口 | 用途 |
|------|------|------|
| HTTP API + Vue SPA | **8080** | 浏览器访问 `http://<公网IP>:8080`（默认账号 `admin / admin123`） |
| Netty 设备通信 | **9000** | 手表 / 手环 TCP 接入 |
| TCP 回显（默认关闭） | 9090 | 仅开发调试用，生产 UFW 默认不开 |
| MySQL（不对外暴露） | 3306 | 容器内部网络访问 |

---

## 架构

```
                        Internet
                           │
                  ┌────────┴────────┐
                  │  云安全组 (allow  │   ← 在云控制台配：22/8080/9000
                  │   22/8080/9000)   │
                  └────────┬────────┘
                           │
                  ┌────────┴────────┐
                  │  UFW on host     │   ← bootstrap.sh 配：22/8080/9000
                  └────────┬────────┘
                           │
              ┌────────────┴────────────┐
              │  Docker (bridge net)    │
              │                         │
              │   ┌──────────┐ ┌─────────────┐
              │   │   app    │ │   mysql     │
              │   │ :8080    │ │ :3306       │
              │   │ :9000    │ │ (内部)      │
              │   │ :9090    │ │             │
              │   └────┬─────┘ └──────┬──────┘
              │        │              │
              │   mpband_data      mysql_data
              │   (命名卷)         (命名卷)
              └─────────────────────────┘
```

---

## 前置条件

1. **云服务器账号**：阿里云 / AWS / 腾讯云 / DigitalOcean 等可获取 root SSH 的厂商。
2. **服务器规格**：
   - **最低**：2 vCPU + 4 GB 内存 + 40 GB SSD
   - **推荐**：2 vCPU + 8 GB 内存（设备连接数多、location 表分区大时更舒适）
   - **原因**：BCrypt cost=12 + Hikari 池 + JVM metaspace 约需 600 MB 堆，余量给 OS 与 MySQL。
3. **OS**：Ubuntu 22.04 LTS（jammy）或 Ubuntu 24.04 LTS（noble）。其它发行版**未测试**。
4. **Root SSH 访问**：首次用密码登录即可（`bootstrap.sh` 完成后会禁用 root 登录）。
5. **代码仓库**：把本仓库推到 GitHub / Gitee（部署时通过 `git clone` 拉取）。

---

## Step 1 — 选购并配置云服务器

选购时注意：

- **公网 IP**：必须分配（弹性公网 IP / EIP）。
- **安全组**：在云控制台放行 `22/tcp`、`8080/tcp`、`9000/tcp`（入站）。`3306` **不要**开。
- **磁盘**：系统盘 40 GB+，数据盘建议独立挂载到 `/var/lib/docker`（可选，让 Docker 数据与系统分离）。

首次用 root SSH 登录：

```bash
ssh root@<公网IP>
```

---

## Step 2 — 把仓库克隆到服务器（手动一次）

为了运行 `bootstrap.sh`，先把本仓库传到服务器（任选其一）：

### 方式 A：Git（推荐）

```bash
ssh root@<公网IP> "apt-get update && apt-get install -y git"
ssh root@<公网IP> "git clone https://github.com/<你的用户名>/smart-watch.git /opt/smart-watch"
```

如果 Git 仓库是私有的，需要先在服务器上配 SSH key 或 PAT。

### 方式 B：scp 压缩包

```bash
# 在本地
git archive --format=tar.gz -o /tmp/smart-watch.tar.gz HEAD
scp /tmp/smart-watch.tar.gz root@<公网IP>:/tmp/
ssh root@<公网IP> "mkdir -p /opt/smart-watch && tar -xzf /tmp/smart-watch.tar.gz -C /opt/smart-watch --strip-components=1"
```

---

## Step 3 — 运行 `bootstrap.sh`（root 跑一次）

```bash
ssh root@<公网IP>
cd /opt/smart-watch
bash scripts/deploy/bootstrap.sh
```

可选地，把 SSH 公钥路径作为参数传入，省去首次传密钥的步骤：

```bash
# 在本地
scp ~/.ssh/id_ed25519.pub root@<公网IP>:/tmp/deploy_key.pub

# 在服务器上
bash scripts/deploy/bootstrap.sh /tmp/deploy_key.pub
```

`bootstrap.sh` 会：

1. 安装 Docker Engine + Compose 插件（Docker 官方源，非 Ubuntu 自带旧版）。
2. 设置时区为 `Asia/Shanghai`。
3. 配置 Docker daemon（log 轮转 10 MB × 3，`live-restore=true`）。
4. 配 UFW：默认拒绝入站，仅开 22 / 8080 / 9000。
5. 配 fail2ban（SSH 5 次失败封 1 小时）。
6. 启用 unattended-upgrades（自动安全补丁）。
7. 创建 `deploy` 用户，加入 `docker` 组，授予有限的密码免 sudo。
8. 改 `/etc/ssh/sshd_config`：`PermitRootLogin no`、`AllowUsers deploy`。
9. **保留密码登录**作为兜底，并打印强警告。

执行完毕后，按提示从**本地新开一个终端**用 `deploy` 用户登录：

```bash
ssh deploy@<公网IP>
```

确认可用后，**立即关掉密码登录**：

```bash
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl reload ssh
# 验证：另开终端用 deploy + 密钥登录成功；用密码登录应被拒
```

---

## Step 4 — 运行 `deploy.sh`（deploy 跑一次）

环境变量可以覆盖默认值。最常用的：

```bash
# 在服务器上（deploy 用户）
cd /opt/smart-watch
export SMART_WATCH_REPO_URL="https://github.com/<你的用户名>/smart-watch.git"
export SMART_WATCH_BRANCH="main"
export SMART_WATCH_HOME="/home/deploy/smart-watch"
bash scripts/deploy/deploy.sh
```

`deploy.sh` 会：

1. 校验 Docker / git / openssl / curl 是否齐全。
2. 克隆（或拉取）代码到 `~/smart-watch`。
3. 首次运行时：
   - 复制 `.env.docker` 为 `.env`，权限 600。
   - 用 `openssl rand -hex 16` 生成两个强随机数据库密码写入 `.env`。
   - 提示你回车继续或输入 `override` 编辑 `.env`。
4. `docker compose build`（首次 5–10 分钟）。
5. `docker compose up -d`。
6. 每 5 秒探测一次 `/actuator/health`，4 分钟内必须健康。
7. 打印访问 URL、默认账号、日志/更新命令。

部署完成后：

```bash
# 在 deploy 用户下，验证
docker compose -C ~/smart-watch ps
curl -fsS http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## Step 5 — 在浏览器验证

打开 `http://<公网IP>:8080`，用 `admin / admin123` 登录。

**立刻修改默认密码**（个人中心 → 修改密码），否则任何能访问 8080 的人都是管理员。

---

## 文件布局（部署完成后）

| 路径 | 内容 |
|------|------|
| `/home/deploy/smart-watch/` | 代码（git 仓库） |
| `/home/deploy/smart-watch/.env` | 运行时密钥（chmod 600，仅 deploy 可读） |
| `/home/deploy/backups/` | 数据库备份（`smart_watch_<时间戳>.sql.gz`） |
| `/home/deploy/logs/` | 健康检查 / 备份日志 |
| `/var/lib/docker/volumes/smart-watch_mysql_data/` | MySQL 数据卷 |
| `/var/lib/docker/volumes/smart-watch_mpband_data/` | 设备原始帧 + 每日日志 |

---

## 下一步

1. **改默认密码**：`admin / admin123` 改成自己的强密码。
2. **配置定时任务**：`crontab -e`，把 `config/crontab.example` 复制进去（每日 03:00 备份，每 5 分钟健康检查）。
3. **接入设备**：手表 / 手环配置指向 `<公网IP>:9000`。
4. **配置告警**（可选）：把 `healthcheck.log` 接入到 Slack / 企业微信 / Prometheus Alertmanager。

更多 Day-2 运维（更新、备份恢复、监控、故障排查）见：

- [operations.md](./operations.md) — 更新、备份、监控、常见问题
- [security.md](./security.md) — 加固清单
- [troubleshooting.md](./troubleshooting.md) — 故障排查表
