# ECS Deployment Guide

## Target Topology

- **Domain**: `eat.868299.com`
- **ECS**: `root@101.37.209.236`（阿里云，Alibaba Cloud Linux 3）
- **Reverse proxy**: Nginx on ports `80/443`
- **App**: Spring Boot on `127.0.0.1:8081`（8080 已被 social-app 占用）
- **Database**: PostgreSQL `goaway_db`，用户 `goaway_user`
- **Media**: Alibaba Cloud OSS `test-ai-redbook`

## Pre-flight

1. 在阿里云 DNS 控制台为 `868299.com` 添加 A 记录：`eat → 101.37.209.236`
2. 确认 ECS 安全组已开放入方向 `22`、`80`、`443`
3. 不要对外开放 `5432` 和 `8081`（Spring Boot 绑定 `127.0.0.1`）

## First-time ECS Setup

```bash
# root 执行
id deploy 2>/dev/null || useradd -m -s /bin/bash deploy
mkdir -p /opt/goaway/backend/{current,shared}
chown -R deploy:deploy /opt/goaway
```

### PostgreSQL

```bash
sudo -u postgres psql << 'SQL'
CREATE DATABASE goaway_db;
CREATE USER goaway_user WITH PASSWORD 'your_strong_password';
GRANT ALL PRIVILEGES ON DATABASE goaway_db TO goaway_user;
\c goaway_db
GRANT ALL ON SCHEMA public TO goaway_user;
SQL
```

### Environment File

创建 `/opt/goaway/backend/shared/.env`，参考 `.env.prod.example`。
必填项：

| 变量 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8081` |
| `SERVER_ADDRESS` | `127.0.0.1` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:5432/goaway_db` |
| `SPRING_DATASOURCE_USERNAME` | `goaway_user` |
| `SPRING_DATASOURCE_PASSWORD` | 你设置的密码 |
| `APP_JWT_SECRET` | 32 字节以上随机串，`python3 -c "import secrets; print(secrets.token_hex(32))"` |
| `APP_LLM_PROVIDER` | `openai-compatible` |
| `APP_LLM_OPENAI_BASE_URL` | DeepSeek / OpenAI 兼容 base URL |
| `APP_LLM_OPENAI_API_KEY` | LLM API Key |
| `APP_LLM_OPENAI_TIMEOUT_MS` | `60000`（仅用于缺失 scene 配置时的默认值）|
| `APP_LLM_OPENAI_FALLBACK_TIMEOUT_MS` | `75000` |
| `APP_LLM_IMAGE_PROVIDER` | `web-search`（无需额外 API Key）|
| `APP_SPEECH_PROVIDER` | `aliyun` |
| `ALIYUN_OSS_*` | Endpoint / AK / SK / Bucket |
| `APP_AUTH_PASSWORD_RESET_PROVIDER` | `mail` |
| `SPRING_MAIL_HOST` | SMTP 服务地址，例如企业邮箱 / SES / Resend SMTP |
| `SPRING_MAIL_PORT` | 常见为 `587`（STARTTLS）或 `465`（SSL） |
| `SPRING_MAIL_USERNAME` | SMTP 登录账号 |
| `SPRING_MAIL_PASSWORD` | SMTP 登录密码或应用专用密码 |
| `SPRING_MAIL_SMTP_AUTH` | `true` |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | `true`（587） |
| `SPRING_MAIL_SMTP_SSL_ENABLE` | `false`（587）或 `true`（465） |
| `APP_MAIL_FROM_ADDRESS` | 发件地址，建议与 SMTP 账号同域 |
| `APP_MAIL_FROM_NAME` | `What To Eat` |
| `APP_MAIL_REPLY_TO` | 客服或支持邮箱 |
| `APP_CORS_ALLOWED_ORIGINS` | `https://eat.868299.com` |
| `APP_DOCS_ENABLED` | `false` |
| `APP_DEMO_TEST_LOGIN_ENABLED` | `false` |

> 注意：推荐与步骤模型的运行时事实源是数据库表 `llm_scene_config`。修改 `.env` 中的 `APP_LLM_OPENAI_*` 不会覆盖已经存在的 `RECOMMEND / STEPS` scene。生产切模型、切 fallback 或调整 timeout 时，必须同步更新数据库中的 scene 配置。

```bash
chmod 600 /opt/goaway/backend/shared/.env
chown deploy:deploy /opt/goaway/backend/shared/.env
```

### Password Reset Mail

生产环境要把找回密码从 demo/log 模式切到真实邮件，至少需要这些变量同时配置完成：

```env
APP_AUTH_PASSWORD_RESET_PROVIDER=mail
SPRING_MAIL_HOST=smtp.your-mail-provider.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=mailer@example.com
SPRING_MAIL_PASSWORD=replace_with_real_password
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
SPRING_MAIL_SMTP_SSL_ENABLE=false
APP_MAIL_FROM_ADDRESS=no-reply@example.com
APP_MAIL_FROM_NAME=What To Eat
APP_MAIL_REPLY_TO=support@868299.com
```

如果线上 `.env` 里只有 `APP_AUTH_PASSWORD_RESET_PROVIDER=log`，或缺少任何 `SPRING_MAIL_* / APP_MAIL_*` 核心项，`POST /api/auth/password/forgot` 就仍然会保持 demo 行为，不会真的发信。

> 如果使用腾讯企业邮 SMTP，`APP_MAIL_FROM_ADDRESS` / `APP_MAIL_FROM` 需要与 `SPRING_MAIL_USERNAME` 保持一致；否则会被服务端以 `mail from address must be same as authorization user` 拒绝。

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

```bash
scp ops/ecs/goaway-backend.service \
    root@101.37.209.236:/etc/systemd/system/goaway-backend.service

ssh root@101.37.209.236 "
  systemctl daemon-reload
  systemctl enable goaway-backend
  systemctl start goaway-backend
"
```

服务日志：

```bash
journalctl -u goaway-backend -f
```

## Install Nginx (HTTP first)

```bash
scp ops/ecs/nginx.eat.868299.com.conf \
    root@101.37.209.236:/etc/nginx/conf.d/eat.868299.com.conf

ssh root@101.37.209.236 "nginx -t && systemctl reload nginx"
```

验证：

```bash
curl -i http://eat.868299.com/api/meals/catalog   # 期望 401（服务正常，需要登录）
```

## HTTPS via Let's Encrypt

确认 DNS A 记录已生效（`dig +short eat.868299.com` 返回 `101.37.209.236`）：

```bash
ssh root@101.37.209.236 "
  mkdir -p /var/www/certbot
  docker run --rm \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/lib/letsencrypt:/var/lib/letsencrypt \
    -v /var/www/certbot:/var/www/certbot \
    certbot/certbot certonly \
    --webroot -w /var/www/certbot \
    -d eat.868299.com \
    --email you@868299.com \
    --agree-tos --no-eff-email
"
```

切换到 TLS 配置：

```bash
scp ops/ecs/nginx.eat.868299.com.tls.conf \
    root@101.37.209.236:/etc/nginx/conf.d/eat.868299.com.conf

ssh root@101.37.209.236 "nginx -t && systemctl reload nginx"
```

证书自动续期（ECS root cron）：

```
0 3 * * * docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/lib/letsencrypt:/var/lib/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot renew --webroot -w /var/www/certbot && systemctl reload nginx
```

## Smoke Test

```bash
# 直连后端（ECS 上）
curl -i http://127.0.0.1:8081/api/meals/catalog          # 401 = 正常

# 通过 nginx HTTP
curl -i http://eat.868299.com/api/meals/catalog

# 通过 nginx HTTPS（证书签发后）
curl -i https://eat.868299.com/api/meals/catalog
```

## Frontend Remote Mode

HTTPS 就绪后，`frontend/.env.local`（已创建）：

```env
APP_REMOTE_API_BASE_URL=https://eat.868299.com
APP_REMOTE_PROXY_TARGET=https://eat.868299.com
```

构建并部署到真机：

```bash
cd frontend
./start.sh device remote "赵强的iPhone"
```

## Rollout Notes

- `prod` profile 绑定 `127.0.0.1:8081`，外网不可直接访问
- Swagger 在 prod 已禁用（`APP_DOCS_ENABLED=false`）
- Hibernate DDL：`prod` 下为 `validate`，首次部署若表不存在需将 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 临时加入 `.env`，或手动执行 `sql/bootstrap.sql`
- LLM timeout 60s：≤5 道菜走单次 LLM 调用，无需并发拆分
- 菜谱图片异步加载：主请求立即返回 `imageStatus=PENDING`，前端按菜逐个 POST `/api/meals/recipes/{id}/image` 触发图片抓取
- 回滚顺序：LLM → `mock`；图片 → `disabled`；语音 → `mock`；OSS → `local`；搜索 → `database`

## Provider 生产推荐

| Provider | 变量 | 推荐值 |
|---|---|---|
| 媒体存储 | `APP_MEDIA_STORAGE_PROVIDER` | `oss` |
| 语音识别 | `APP_SPEECH_PROVIDER` | `aliyun` |
| 大模型 | `APP_LLM_PROVIDER` | `openai-compatible` |
| 菜谱图片 | `APP_LLM_IMAGE_PROVIDER` | `web-search` |
| 短信登录 | `APP_AUTH_SMS_PROVIDER` | `aliyun`（凭据就绪后）|
| 密码找回 | `APP_AUTH_PASSWORD_RESET_PROVIDER` | `mail`（SMTP 就绪后）|
