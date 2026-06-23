# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 产品定位

**goaway** —— 面向职场打工人的 iOS 实用小工具集。围绕「上班前 / 上班中 / 下班后」三个场景，
帮用户节约时间、平衡工作与生活、提供情绪价值，并以**匿名全站排行榜**激励使用与传播。

场景与工具方向：
- **上班前（打卡）**：上班倒计时、距发薪/周末/假期天数、薪资实时进度（今天已赚多少）。
- **上班中（摸鱼）**：摸鱼计时、带薪如厕、喝水/久坐提醒、今日摸鱼时长上报。
- **下班后（周报/情绪）**：AI 周报生成、吐槽树洞、每日毒鸡汤、骂老板模拟器、今日宜忌。

完整产品与实施计划见 `~/.claude/plans/ios-cozy-kitten.md`。

> 仓库由 `quick-start-end2end-template` / `what-to-eat` 同构脚手架派生而来，沿用其全栈架构与工具链。

## Working Principles

- **API 向后兼容**：App 一旦上线，DTO 字段只增不删改、枚举只增、接口路径不变、Schema 增量迁移。
- **Bug 调查**：先看日志与证据，定位根因并与用户确认后再改代码。
- **文档同步**：非平凡改动后检查 CLAUDE.md / docs / memory 是否过期。

## Commands

### Backend（在 `backend/` 下）

```bash
./start.sh local          # 加载 .env.local，启动 Spring Boot（mvn spring-boot:run）
mvn -o test               # 跑全部测试
mvn -o test -Dtest="FooTest,BarTest"
mvn -DskipTests package   # 打包 goaway-backend-0.0.1-SNAPSHOT.jar
```

本地数据库 `goaway_db`（用户 `app_user`），端口 8080。游客 token：
`POST /api/auth/guest`，需带请求头 `X-Guest-Installation-Id`。

### Frontend（在 `frontend/` 下）

```bash
./start.sh local          # iOS 模拟器 + 本地后端
npm run lint              # ESLint
npm test                  # Jest
npm run sync-models       # 由后端 DTO 重新生成前端契约模型
```

### 契约同步

后端 DTO 是 API 契约的唯一事实源。`scripts/generate-contract-models.mjs` 读取
`contracts/model-sync.config.json`，生成 `frontend/src/shared/models/generatedContracts.js`
与 iOS Swift 模型。启动前后端时会自动运行；改 DTO 后手动跑 `./scripts/sync-models.sh`。

## Architecture

### 仓库布局

```
backend/    Spring Boot 3（Java 17，本地 8080）
frontend/   React Native 0.84（iOS 为主）
scripts/    跨端工具（契约同步、bootstrap）
contracts/  model-sync.config.json + 生成的 model-registry.json
docs/       架构与 provider 文档
memory/     持久上下文笔记
```

### 后端包结构（根包 `com.goaway`）

- **`contexts/`** —— 业务域，每域分 `api → application → domain/infrastructure`。
  当前域：`account`、`media`、`analytics`、`workprofile`（打工人/最讨厌的人画像）、`checkin`、`fishing`、
  `weekly`、`mood`、`leaderboard`、`activity`、`admin`、`roleplay`（AI 对线）、
  `push`（设备 token + 推送偏好 + 活跃水位）、`taunt`（主动毒舌推送：定时/场景/召回触发，千人千面 LLM 生成）。
- **`platform/`** —— 横切技术关注点：`security`、`config`、`llm`（LlmSceneConfig 场景→模型路由）、provider 抽象。
- **`shared/`** —— 与域无关的响应 DTO。

依赖方向：`api → application → domain/infrastructure`；`domain` 不得依赖 `api`；`platform` 可被任意域引用。

### Provider 模式

每个外部能力都是可切换 provider，由 `app.*.provider` 环境变量激活，本地默认走安全的 mock/log/local 实现，
通过 `@ConditionalOnProperty` 在 `platform/provider/` 注册。业务代码只依赖接口。

| 能力 | Env 变量 | 本地默认 | 生产 |
|---|---|---|---|
| 媒体存储 | `APP_MEDIA_STORAGE_PROVIDER` | `local` | `oss`（阿里云） |
| 短信登录 | `APP_AUTH_SMS_PROVIDER` | `log` | `aliyun` |
| 密码找回邮件 | `APP_AUTH_PASSWORD_RESET_PROVIDER` | `log` | `mail` |
| LLM（周报/毒鸡汤/骂老板） | `APP_LLM_PROVIDER` | `mock` | `openai-compatible`（deepseek） |
| 远程推送（毒舌主动推送等） | `APP_PUSH_PROVIDER` | `log` | `apns`（token 鉴权 .p8，`app.push.apns.*`） |
| 内容审核（树洞 UGC） | `APP_MODERATION_PROVIDER` | `passthrough` | 阿里云内容安全 |
| 行为埋点 | `APP_ANALYTICS_PROVIDER` | `log` | `umeng` |

> `APP_PUSH_PROVIDER` 已落地：`PushProvider` 接口 + `LogPushProvider`（本地）/`ApnsPushProvider`（JDK HttpClient HTTP/2 直连 APNs）。
> `app.moderation.*` 仍仅有配置占位，待 Phase 3。LLM 配置键为通用的 `app.llm.openai.*`（毒舌推送复用 `GENERAL` 场景）。

### 前端结构

- **`src/app/`** —— 导航（`navigation/AppNavigator.js`，首页/我的两 Tab + 鉴权与账号管理 Stack）、
  Auth Context、生成的 runtime 配置。
- **`src/features/`** —— 功能模块：`home`（场景占位首页）、`auth`、`profile`、`media`。
  **规划新增**：`checkin`、`fishing`、`weekly`、`mood`、`leaderboard`。
- **`src/shared/`** —— axios 客户端（自动注入 JWT）、生成的契约模型、analytics、主题。

本地优先：工具配置存 AsyncStorage，游客即用；登录后再同步到后端、参与排行榜、生成 AI 周报。

## 生产部署（阿里云 ECS）

详见 `backend/DEPLOY_ECS.md`。ECS + Nginx（TLS + 对 SSE 周报路由关闭 `proxy_buffering`）+ PostgreSQL +
systemd（`goaway-backend`）。生产 env 集中存放于 `/opt/goaway/backend/shared/.env`，provider 切到 aliyun/apns/deepseek。

## 当前进度

- **Phase 0（已完成）**：骨架就绪——重命名根包、裁剪 meal/inventory/subscription、配置 goaway_db、
  account/媒体/安全链路跑通（编译/启动/建表/health UP/游客鉴权/40 测试全绿），前端裁剪并重建导航与占位首页。
- **Phase 1（待开始）**：打卡 + 摸鱼 + 匿名排行榜（首个完整业务闭环）。
- **Phase 2**：AI 周报（LLM + SSE）。**Phase 3**：情绪价值（树洞/审核）+ 推送提醒。
