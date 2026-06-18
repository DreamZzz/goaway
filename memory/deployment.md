# 部署知识

## 目标拓扑

- 对外域名默认占位：`api.what-to-eat.example.com`
- 反向代理：Nginx
- 应用：Spring Boot 监听 `127.0.0.1:8080`
- 数据库：PostgreSQL
- 可选依赖：OSS、SMTP、阿里云语音、OpenAI-compatible LLM

## 关键资产

- systemd: `ops/ecs/what-to-eat-backend.service`
- Nginx: `ops/ecs/nginx.api.what-to-eat.example.com.conf`
- TLS Nginx: `ops/ecs/nginx.api.what-to-eat.example.com.tls.conf`
- 部署脚本: `ops/ecs/deploy-backend.sh`
- 接口 smoke: `ops/scripts/smoke-api.sh`

## 上线前最重要的开关

- `APP_JWT_EXPIRATION_MS`
- `APP_GUEST_TOKEN_EXPIRATION_MS`
- `APP_GUEST_TRIAL_LIMIT`
- `APP_GUEST_INSTALL_ID_SALT`
- `APP_GUEST_IP_HASH_SALT`
- `APP_GUEST_AUTH_RATE_LIMIT_MAX_REQUESTS`
- `APP_GUEST_AUTH_RATE_LIMIT_WINDOW_SECONDS`
- `APP_GUEST_MEAL_RATE_LIMIT_MAX_REQUESTS`
- `APP_GUEST_MEAL_RATE_LIMIT_WINDOW_SECONDS`
- `APP_MEAL_FREE_DAILY_QUOTA`
- `APP_MEAL_PREMIUM_DAILY_QUOTA`
- `APP_MEAL_FREE_MAX_DISHES`
- `APP_MEAL_PREMIUM_MAX_DISHES`
- `APP_MEDIA_STORAGE_PROVIDER`
- `APP_AUTH_PASSWORD_RESET_PROVIDER`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_SMTP_AUTH`
- `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- `SPRING_MAIL_SMTP_SSL_ENABLE`
- `APP_MAIL_FROM_ADDRESS`
- `APP_MAIL_FROM_NAME`
- `APP_MAIL_REPLY_TO`
- `APP_AUTH_SMS_PROVIDER`
- `APP_SPEECH_PROVIDER`
- `APP_SPEECH_ALIYUN_*`
- `APP_LLM_PROVIDER`
- `APP_LLM_OPENAI_*`
- `APP_LLM_IMAGE_PROVIDER`
- `SPRING_RABBITMQ_HOST`
- `SPRING_RABBITMQ_PORT`
- `SPRING_RABBITMQ_USERNAME`
- `SPRING_RABBITMQ_PASSWORD`
- `SPRING_RABBITMQ_VHOST`
- `APP_MEAL_RECOMMENDATION_RECORD_MQ_EXCHANGE`
- `APP_MEAL_RECOMMENDATION_RECORD_MQ_COMPLETED_QUEUE`
- `APP_MEAL_RECOMMENDATION_RECORD_MQ_FEEDBACK_QUEUE`
- `APP_MEAL_RECOMMENDATION_RECORD_MQ_COMPLETED_ROUTING_KEY`
- `APP_MEAL_RECOMMENDATION_RECORD_MQ_FEEDBACK_ROUTING_KEY`
- `APP_MEAL_CATALOG_BOOTSTRAP_ENABLED`
- `APP_MEAL_CATALOG_DATASET_VERSION`
- `APP_MEAL_CATALOG_DATASET_TITLE`
- `APP_MEAL_CATALOG_SOURCE_FILE`
- `APP_INVENTORY_VISION_PROVIDER`
- `APP_INVENTORY_VISION_BASE_URL`
- `APP_INVENTORY_VISION_API_KEY`
- `APP_INVENTORY_VISION_MODEL`
- `APP_INVENTORY_VISION_MAX_ATTEMPTS`
- `APP_INVENTORY_VISION_RETRY_BACKOFF_MS`
- `APP_INVENTORY_IMAGE_RETENTION_DAYS`
- `APP_INVENTORY_SCAN_DAILY_QUOTA`

## 推荐策略

- 本地默认全部走 `local/log/mock/disabled`
- 语音与大模型本地默认允许 `mock`，生产只有在凭据和外部服务就绪后才切到真实 provider
- `APP_MEAL_CATALOG_DATASET_VERSION` 只作为 bootstrap / fallback 基线；运行时中文主菜单读取当前 `active=true` 的 `cn-home-menu-vN` 数据集
- 基础菜单建议以“随包资源 + 初始数据集版本号”或 admin console 扩容版本的方式发布，不建议在服务器上手工改库
- 若短信或邮件未准备好，先保留 `log`
- 生产环境若要启用正式邮件找回密码，必须同时配置 `APP_AUTH_PASSWORD_RESET_PROVIDER=mail` 与完整的 `SPRING_MAIL_* / APP_MAIL_*` 变量；只切 provider 不补 SMTP 凭据，接口仍无法真实发信
- 若图像生成不稳定，先保留 `APP_LLM_IMAGE_PROVIDER=disabled`
- 食材库存识别本地默认 `APP_INVENTORY_VISION_PROVIDER=mock`；生产启用拍照识别前需切 `bailian-qwen-vl` 并配置 `APP_INVENTORY_VISION_API_KEY`，默认模型为 `qwen-vl-plus`，视觉 provider 默认最多尝试 3 次、重试间隔 300ms，图片保留默认 30 天，Pro 每日扫描额度默认 10 次
- LLM 场景配置以数据库表 `llm_scene_config` 为运行时事实源；修改 `.env` 里的 `APP_LLM_OPENAI_*` 只会影响“缺失场景的默认值”，不会覆盖已存在的 `RECOMMEND / STEPS` 配置。生产切模型、切 fallback 或改 timeout 时，必须同步更新数据库中的对应 scene。
- 推荐链路当前建议：
  - `RECOMMEND`：`deepseek-v4-flash`（原 `deepseek-chat`，已于 2026-04-24 切换）
  - `fallback_model`：留空或仅在明确验证后开启
  - `STEPS`：单独配置，不要误把它当推荐快模型
- 若线上出现“明明改了 `.env` 里的模型，但推荐行为没变”，先查 `llm_scene_config`，不要误判为部署未生效。
- 推荐记录与满意度反馈优先走 RabbitMQ 异步链路；若 MQ 不可用，服务端必须自动降级为本地日志，不阻塞推荐主链路，也不回退为同步写库
- 游客模式必须可在生产环境独立运行：`POST /api/auth/guest`、`POST /api/meals/guest-inspirations` 与 `POST /api/voice/transcriptions` 需要对外可用；guest 只能访问“来点灵感”随机推荐或文字/语音 1 道菜试吃。文字/语音强命中 catalog 时直接返回基础菜单，弱命中或无匹配时仅允许调用 LLM 生成 1 道试吃菜谱，不允许落到登录用户完整多菜推荐主链路
- 生产环境必须为游客模式配置独立 salt 与限流阈值，避免沿用默认 JWT secret 造成上下文可预测
- ingredient-intent 多菜请求（如 `鸡排 / 2道菜`）当前会主动跳过旧缓存，并优先约束成“主菜 + 搭配菜”，避免两道同核心食材菜被直接复用出来
- 多食材 + 偏好请求（如 `鸡蛋，番茄，土豆，想吃热乎的，别太复杂`）会先做内部 intent profile 解析，再走 catalog 多关键词菜单匹配与质量门禁；预加载质量不达标时不向前端发送 recipe，由正式 LLM/stream 继续生成

## what-to-eat 首版发布注意事项

- 前端仅以 iOS 为发布目标；Android 不在本次发布范围。
- 真机 `remote` 联调或 Release 安装依赖 Apple Developer Program 团队下的 `Apple Development` 证书；只有 `Apple Distribution` 证书时，命令行 `run-ios` 无法直接装机。
- 命令行真机安装还依赖 Xcode CLI 可读的账号凭据和本地 provisioning profile。若 `./frontend/start.sh device remote` 报 `No Accounts`、`missing Xcode-Username` 或 `No profiles for ... were found`，正式修复方式是：
  1. 在 Xcode -> Settings -> Accounts 中重新登录当前 Developer 账号
  2. 打开 `ios/frontend.xcworkspace`
  3. `Signing & Capabilities` 里启用 `Automatically manage signing`
  4. 选择正式的 Developer Team，并使用唯一的开发包名
  5. 对真机手动点一次 `Run`，让 Xcode 自动创建并下载开发 profile
  6. 成功后再回到命令行执行 `./frontend/start.sh device remote "你的iPhone名称"`
- `frontend/.env.local` 建议显式配置 `IOS_DEVELOPMENT_TEAM` 和 `IOS_APP_BUNDLE_ID`，避免脚本误用工程默认值。
- `APP_JWT_EXPIRATION_MS` 需要按毫秒配置，生产可使用 30 天级别的 long 值，例如 `2592000000`；不要再按 Java `int` 上限假设处理。
- 所有账号型 meal/favorites 接口需要登录，Nginx 与 smoke 要覆盖鉴权场景；`POST /api/voice/transcriptions` 允许 guest token 只做语音转写。
- 游客审核路径额外允许 `POST /api/auth/guest`、`POST /api/meals/guest-inspirations` 与 `POST /api/voice/transcriptions`；前两条接口要纳入 smoke，确保免注册即可完成一次“来点灵感”体验，文字/语音试吃保持 1 道菜：强 catalog 命中走基础菜单，弱命中或无匹配走 1 道 LLM 试吃。
- 语音文件上传体积和超时时间需要与 Nginx/body-size 配置一致。
- 菜谱图片采用异步补图；主请求先返回 `imageStatus=PENDING`，客户端再逐道触发补图。
- 即梦图片生成存在并发上限；生产前端应保持首次补图串行，服务端会对 `429` 做重试并在必要时降级到 `web-search`，但不能把补图链路设计成主结果依赖。
- 基础菜单库表会随着 `bootstrap.sql` 一起创建；初始菜单数据由应用启动后按 `APP_MEAL_CATALOG_DATASET_VERSION` 幂等导入，之后扩容版本通过复制当前 active 数据集并追加候选菜生成。
- 线上若要通过资源文件修订初始基础菜单，应发布新的资源文件并提升 `APP_MEAL_CATALOG_DATASET_VERSION`，不要直接覆盖旧版本数据；通过 admin console 批量收录候选时会生成 `cn-home-menu-vN` 新版本并自动切为唯一 active。
- 回滚基础菜单使用 admin console 的基础库版本管理，或调用 `POST /admin/console/catalog/datasets/{version}/activate`；回滚只切 active 指针，不删除历史版本，也不改历史 `meal_recipes`。
- 若启动日志出现“版本已存在但资源内容发生变化”，说明发版包与当前版本号不匹配，应先修正版本号再上线。
- 游客模式的 `X-Guest-Installation-Id` 由前端生成并保存在 iOS Keychain；服务端只记录 hash，不记录原始安装标识或原始 IP。
- 库存联动推荐上线前必须同步 `bootstrap.sql` 新增的 `inventory_items.expires_at`、`inventory_recipe_consumptions` 及索引；生产当前 `ddl-auto=validate` 时，需先执行 schema 变更再重启后端。自定义文字/语音推荐默认不使用库存，只有 Pro 用户在完善偏好页显式勾选库存参与时才会带 `useInventory: true`。
- 生产手工执行新增 schema 时，优先使用应用数据库用户执行；若以 `postgres` 执行 DDL，必须同步 `ALTER OWNER` 给 `what_to_eat_user` 并补齐表/序列权限，否则 Hibernate 启动校验或 `ddl-auto=update` 元数据处理可能因非 owner 失败。

## GitHub Actions（第一阶段）

### 当前已规划的 workflow

- `CI`
  - 触发：`pull_request`、`push` 到 `main`
  - 运行：
    - `cd backend && mvn package -Dmaven.test.skip=true`
    - `cd frontend && npm ci`
    - `cd frontend && npm run lint`
    - `cd frontend && npm test -- --runInBand`
- `Deploy Backend`
  - 触发：`workflow_dispatch`
  - 作用：手动发布 ECS 后端
- `Rollback Backend`
  - 触发：`workflow_dispatch`
  - 作用：回滚到 `previous` 或指定的 `release_id`
- `iOS CI Placeholder`
  - 当前只做结构预留，不执行真实 iOS 构建与签名

### 版本基线

GitHub Actions 里的运行时版本，当前按“ECS 已有版本优先、仓库必需版本补齐”的原则固定：

- Java：`17`（对齐 ECS 当前 `17.0.18`）
- Maven：`3.9.9`（对齐 ECS 当前版本）
- Node：`22.11.x`（对齐仓库要求）

说明：

- ECS 当前已安装：
  - Java `17.0.18`
  - Maven `3.9.9`
- ECS 当前未安装 Node / npm
- 但仓库构建链需要 Node：
  - `frontend/package.json` 要求 `>= 22.11.0`
  - `backend/pom.xml` 在构建阶段会执行 `scripts/generate-contract-models.mjs`

因此，GitHub Actions 中：

- Java / Maven 直接跟 ECS 对齐
- Node 跟 repo 构建要求对齐

### ECS 发布方式

GitHub Actions 中的后端发布不复用 [deploy-backend.sh](/Users/zhaoqiang/Documents/Project/what-to-eat/ops/ecs/deploy-backend.sh) 的“远端编译”路径，而采用：

1. Actions runner 本地构建 jar
2. 通过 SSH / SCP 上传到 ECS 临时路径
3. 调用 [remote-release.sh](/Users/zhaoqiang/Documents/Project/what-to-eat/ops/ecs/remote-release.sh) 在远端完成原子发布
4. `systemctl restart what-to-eat-backend`
5. 通过 [verify-backend.sh](/Users/zhaoqiang/Documents/Project/what-to-eat/ops/ecs/verify-backend.sh) 做发布后 smoke：
   - `http://127.0.0.1:8081/api/auth/captcha` => `200`
   - `https://eat.868299.com/api/auth/captcha` => `200`
   - `https://eat.868299.com/api/meals/catalog` => `401`

这样可以避免因为 ECS 没有 Node 而让远端构建卡住，也更容易把失败定位到：

- Java/Maven 环境
- Node 环境
- Maven 构建
- SSH 上传
- systemd 重启
- smoke 校验

### 发布保护与串行化

- `Deploy Backend` 与 `Rollback Backend` 使用同一个 workflow `concurrency` 组：
  - `deploy-backend-prod`
- 这保证同一时间只能有一个生产发布动作在跑，避免 jar 覆盖错乱
- `prod` environment 保护规则仍需在 GitHub 网页中配置：
  - required reviewers
  - deployment branches 限制为 `main`

### 版本信息与产物保留

每次发布都会保留：

- GitHub Actions artifact：`backend-jar-<release_id>`
- release 元信息：
  - `release_id`
  - `commit_sha`
  - `jar_sha256`
  - `deployed_at`

ECS 上的 release 目录默认位于：

- `/opt/what-to-eat/backend/releases/<release_id>`

当前激活 release 记录在：

- `/opt/what-to-eat/backend/shared/releases/current-release.txt`

### GitHub Secrets

发布 workflow 至少需要：

- `ECS_HOST`
- `ECS_USER`
- `ECS_SSH_KEY`
- 可选：`ECS_PORT`

当前不放入 GitHub Actions 的凭据：

- Apple 证书 / provisioning profile
- App Store Connect API key
- 生产 `.env`

ECS 仍继续使用远端已有：

- `/opt/what-to-eat/backend/shared/.env`

### 回滚方式

- 直接回滚到上一版：
  - 运行 `Rollback Backend`
  - `target_release=previous`
- 回滚到指定版本：
  - 运行 `Rollback Backend`
  - `target_release=<release_id>`

如果需要先查看 ECS 上可用的 release，可在服务器执行：

```bash
find /opt/what-to-eat/backend/releases -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -r
```

## Xcode Cloud（iOS Archive 健康状态）

当前仓库已补齐 Xcode Cloud 自定义脚本，用于保证云端 iOS 构建在 archive 前具备和本地一致的关键前置条件：

- `/Users/zhaoqiang/Documents/Project/what-to-eat/ci_scripts/ci_post_clone.sh`
- `/Users/zhaoqiang/Documents/Project/what-to-eat/ci_scripts/ci_pre_xcodebuild.sh`

脚本会完成：

- 安装 `Node >= 22.11`
- `cd frontend && npm ci`
- `cd frontend/ios && pod install --deployment`
- 写入 `frontend/ios/.xcode.env.local`，固定 `NODE_BINARY`
- 在 xcodebuild 前校验：
  - `frontend.xcworkspace`
  - `Pods-frontend.release.xcconfig`
  - `Pods-frontend-frameworks-Release-output-files.xcfilelist`

这样当 Xcode Cloud 再次报错时，失败更可能是：

- iOS 工程真实编译问题
- Podfile / Podfile.lock 不一致
- workspace/scheme 配置问题

而不是“云端没执行 `pod install`”这种环境伪失败。

为兼容 Xcode Cloud 针对不同工程根目录查找脚本的行为，仓库同时提供：

- `/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/ci_scripts/ci_post_clone.sh`
- `/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/ci_scripts/ci_pre_xcodebuild.sh`
- `/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/ios/ci_scripts/ci_post_clone.sh`
- `/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/ios/ci_scripts/ci_pre_xcodebuild.sh`

这两个包装脚本会转调根目录的标准实现。
