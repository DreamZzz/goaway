# ECS Deployment Guide

> 本文档参数与 2026-06-23 实际部署并经域名验证的状态一致（health UP / 游客鉴权 200）。

## Target Topology

- **Domain**: `goaway.868299.com`
- **ECS**: `root@101.37.209.236`（阿里云，Alibaba Cloud Linux 3，与 what-to-eat / social-app 共用同一台）
- **Reverse proxy**: Nginx on ports `80/443`（按 `server_name` 分流，与 eat 站点互不影响）
- **App**: Spring Boot on `127.0.0.1:8090`（`8080` 被 social-app、`8081` 被 what-to-eat 占用）
- **Database**: PostgreSQL `goaway_db`，用户 `goaway_user`（独立库，勿动 what-to-eat 的库）
- **Media**: Alibaba Cloud OSS
- **LLM**: DeepSeek（`openai-compatible`，model `deepseek-chat`）

> 同机部署铁律：goaway 一律使用**独立**的 DB / systemd 单元 / Nginx 站点 / 证书；
> 仅复用已存在的共享基建（Nginx 进程、PostgreSQL 实例、`deploy` 用户、`/var/www/certbot`），不修改 what-to-eat 任何资源。

## Pre-flight

1. 阿里云 DNS 为 `868299.com` 添加 A 记录：`goaway → 101.37.209.236`（验证：`dig +short goaway.868299.com`）
2. ECS 安全组已开放入方向 `22`、`80`、`443`
3. 不要对外开放 `5432` 和 `8090`（Spring Boot 绑定 `127.0.0.1`）

## First-time ECS Setup

```bash
# root 执行
id deploy 2>/dev/null || useradd -m -s /bin/bash deploy
mkdir -p /opt/goaway/backend/{current,shared}
chown -R deploy:deploy /opt/goaway
```

### PostgreSQL

```bash
# 角色 + 库（幂等；密码需与 .env 的 SPRING_DATASOURCE_PASSWORD 一致）
sudo -u postgres psql -d postgres <<'SQL'
DO $do$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='goaway_user') THEN
    CREATE ROLE goaway_user LOGIN PASSWORD 'your_strong_password';
  END IF;
END
$do$;
SQL
sudo -u postgres createdb -O goaway_user goaway_db 2>/dev/null || true
sudo -u postgres psql -d goaway_db <<'SQL'
GRANT ALL PRIVILEGES ON DATABASE goaway_db TO goaway_user;
GRANT ALL ON SCHEMA public TO goaway_user;
SQL

# 校验：用 goaway_user 经 TCP 密码登录
PGPASSWORD='your_strong_password' psql -h 127.0.0.1 -U goaway_user -d goaway_db -tAc "SELECT current_user;"
```

### Environment File

创建 `/opt/goaway/backend/shared/.env`。已确认必填项（本次部署实际生效）：

| 变量 | 值 / 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8090` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:5432/goaway_db` |
| `SPRING_DATASOURCE_USERNAME` | `goaway_user` |
| `SPRING_DATASOURCE_PASSWORD` | 与上面 PostgreSQL 角色一致的密码 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | 首启 `update` 建表，验证后切 `validate`（见下文 Smoke Test） |
| `APP_JWT_SECRET` | 32 字节以上随机串，`python3 -c "import secrets; print(secrets.token_hex(32))"` |
| `APP_LLM_PROVIDER` | `openai-compatible` |
| `APP_LLM_OPENAI_BASE_URL` | `https://api.deepseek.com/v1` |
| `APP_LLM_OPENAI_MODEL` | `deepseek-chat` |
| `APP_LLM_OPENAI_API_KEY` | DeepSeek API Key |
| `APP_MEDIA_STORAGE_PROVIDER` | `oss` |
| `ALIYUN_OSS_ACCESS_KEY_ID` / `ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS 凭据（Endpoint / Bucket 一并配置） |

> 其余能力（短信登录 `APP_AUTH_SMS_PROVIDER`、找回邮件 `APP_AUTH_PASSWORD_RESET_PROVIDER`、推送 `APP_PUSH_PROVIDER`、内容审核 `APP_MODERATION_PROVIDER`、埋点 `APP_ANALYTICS_PROVIDER`）
> 按 provider 模式**按需**开启：本地默认走 mock/log/passthrough，生产再切到 aliyun/apns/deepseek 等。详见根目录 `CLAUDE.md` 的 Provider 表。

```bash
chmod 600 /opt/goaway/backend/shared/.env
chown deploy:deploy /opt/goaway/backend/shared/.env
```

## Build and Deploy Jar（本地 → SCP）

```bash
# 本地打包（backend/ 目录）
mvn -DskipTests package

# 上传
scp target/goaway-backend-0.0.1-SNAPSHOT.jar \
    root@101.37.209.236:/opt/goaway/backend/current/app.jar

# ECS：修正 owner，重启服务
ssh root@101.37.209.236 "
  chown deploy:deploy /opt/goaway/backend/current/app.jar
  systemctl restart goaway-backend
  systemctl status goaway-backend --no-pager
"
```

## Install systemd Service

`/etc/systemd/system/goaway-backend.service`（独立单元，指向 `/opt/goaway`）：

```ini
[Unit]
Description=Goaway Backend
After=network.target postgresql.service

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/goaway/backend/current
EnvironmentFile=/opt/goaway/backend/shared/.env
ExecStart=/usr/bin/java -jar /opt/goaway/backend/current/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
ssh root@101.37.209.236 "
  systemctl daemon-reload
  systemctl enable goaway-backend
  systemctl start goaway-backend
"

# 服务日志
journalctl -u goaway-backend -f
```

## Install Nginx (HTTP first)

```bash
scp ops/ecs/nginx.goaway.868299.com.conf \
    root@101.37.209.236:/etc/nginx/conf.d/goaway.868299.com.conf

ssh root@101.37.209.236 "mkdir -p /var/www/certbot && nginx -t && systemctl reload nginx"
```

HTTP 站点已把 `/` 反代到 `127.0.0.1:8090`，并对 SSE（AI 周报）关闭 `proxy_buffering`。验证：

```bash
curl -i http://goaway.868299.com/actuator/health   # 期望 200 {"status":"UP"}
```

## HTTPS via Let's Encrypt

DNS 生效后，用系统 certbot（webroot）只为 goaway 签证书，不影响 eat 证书：

```bash
ssh root@101.37.209.236 "
  certbot certonly --webroot -w /var/www/certbot \
    -d goaway.868299.com \
    --non-interactive --agree-tos --keep-until-expiring \
    --email you@868299.com
"
# 证书落在 /etc/letsencrypt/live/goaway.868299.com/{fullchain,privkey}.pem
# certbot 会自动登记后台续期任务（systemd timer）
```

切换到 TLS 配置（HTTP 自动 301→HTTPS）：

```bash
scp ops/ecs/nginx.goaway.868299.com.tls.conf \
    root@101.37.209.236:/etc/nginx/conf.d/goaway.868299.com.conf

ssh root@101.37.209.236 "nginx -t && systemctl reload nginx"
```

> 注意：本机 nginx 版本使用旧式 `listen 443 ssl http2;` 语法，**不支持** `http2 on;` 独立指令（仓库 conf 已对齐为旧语法）。

## Smoke Test

```bash
# 直连后端（ECS 上）
ssh root@101.37.209.236 'curl -s http://127.0.0.1:8090/actuator/health'   # {"status":"UP"}

# 经域名 HTTPS：health
curl -s https://goaway.868299.com/actuator/health                          # {"status":"UP"}

# HTTP→HTTPS 跳转
curl -s -o /dev/null -w '%{http_code}\n' http://goaway.868299.com/actuator/health   # 301

# 游客鉴权：X-Guest-Installation-Id 必须是合法 UUID，否则 400「缺少合法的游客设备标识」
UUID=$(python3 -c 'import uuid; print(uuid.uuid4())')
curl -s -X POST https://goaway.868299.com/api/auth/guest \
  -H "X-Guest-Installation-Id: $UUID" -H "Content-Type: application/json"
# 期望 200：返回 token(JWT) / type:Bearer / isGuest:true / guestTrialRemaining:3
```

### 首启建表流程（DDL）

`prod` 下 `SPRING_JPA_HIBERNATE_DDL_AUTO` 应为 `validate`。首次部署表不存在时：

1. 临时设 `.env` 为 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`，`systemctl restart goaway-backend`，等 health UP（建表）。
2. 改回 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`，再 `restart` 复验仍 UP（确认无 schema 漂移）。

## Frontend Remote Mode

HTTPS 就绪后，`frontend/.env.local`：

```env
APP_REMOTE_API_BASE_URL=https://goaway.868299.com
APP_REMOTE_PROXY_TARGET=https://goaway.868299.com
```

构建并部署到真机：

```bash
cd frontend
./start.sh device remote "赵强的iPhone"
```

## Rollout Notes

- `prod` profile 绑定 `127.0.0.1:8090`，外网仅经 Nginx HTTPS 访问。
- 游客即用：`POST /api/auth/guest` 需带请求头 `X-Guest-Installation-Id`（合法 UUID）。
- DB 凭据与 JWT 密钥只存在于 `/opt/goaway/backend/shared/.env`（`600` / `deploy:deploy`），不要写进命令行或仓库。
- 回滚顺序（出问题降级）：LLM → `mock`；OSS → `local`；其余 provider → 各自的 log/mock/passthrough。

## Provider 生产推荐

| 能力 | 变量 | 推荐值 |
|---|---|---|
| 媒体存储 | `APP_MEDIA_STORAGE_PROVIDER` | `oss` |
| 大模型（周报/毒鸡汤/骂老板） | `APP_LLM_PROVIDER` | `openai-compatible`（DeepSeek） |
| 短信登录 | `APP_AUTH_SMS_PROVIDER` | `aliyun`（凭据就绪后） |
| 密码找回 | `APP_AUTH_PASSWORD_RESET_PROVIDER` | `mail`（SMTP 就绪后） |
| 推送（喝水/久坐/下班提醒） | `APP_PUSH_PROVIDER` | `apns` / 阿里云 EMAS（Phase 3 落地） |
| 内容审核（树洞 UGC） | `APP_MODERATION_PROVIDER` | 阿里云内容安全（Phase 3 落地） |
| 行为埋点 | `APP_ANALYTICS_PROVIDER` | `umeng` |
