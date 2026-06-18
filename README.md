# goaway

给职场打工人的 iOS 实用小工具集。围绕「上班前 / 上班中 / 下班后」三个场景，帮用户节约时间、
平衡工作与生活、提供情绪价值，并以**匿名全站排行榜**激励使用与传播。

基于 React Native iOS + Spring Boot + PostgreSQL 的端到端项目，云端部署在阿里云 ECS。
仓库由 `what-to-eat` 同构脚手架派生，沿用其已验证的全栈架构、鉴权体系与可切换 provider 工具链。

## 场景与工具

| 场景 | 方向 | 代表工具 |
|---|---|---|
| 上班前（打卡） | 节约时间 / 期待感 | 上班倒计时、距发薪/周末/假期天数、薪资实时进度 |
| 上班中（摸鱼） | 平衡 / 趣味 | 摸鱼计时、带薪如厕、喝水/久坐提醒、摸鱼时长榜 |
| 下班后（周报/情绪） | 提效 / 情绪价值 | AI 周报生成、吐槽树洞、每日毒鸡汤、骂老板模拟器 |

完整产品与实施计划见 `~/.claude/plans/ios-cozy-kitten.md`。

## 仓库结构

```text
goaway/
├── backend/                 # Spring Boot 3 API（Java 17）
│   ├── src/main/java/com/goaway/
│   │   ├── platform/        # config / security / llm / provider
│   │   ├── contexts/        # account / media / analytics（+ 规划：checkin/fishing/weekly/mood/leaderboard）
│   │   └── shared/          # 通用响应 DTO
│   ├── DEPLOY_ECS.md        # 阿里云 ECS 生产部署指南
│   └── start.sh             # 本地 / 生产启动入口（加载 .env.local）
├── frontend/                # React Native 0.84 App（iOS 为主）
│   ├── src/app/             # 导航 / Auth Context / runtime 配置
│   ├── src/features/        # home / auth / profile / media（+ 规划业务 feature）
│   └── src/shared/          # axios(JWT) / 契约模型 / analytics / 主题
├── contracts/               # model-sync.config.json（DTO→前端模型）
├── ops/                     # ECS / Nginx / systemd / smoke 脚本
├── scripts/                 # 契约同步 / bootstrap / env-check
├── docs/                    # 架构与 provider 文档
└── memory/                  # 持久化项目知识
```

## 快速开始

### 后端

```bash
cd backend
# 准备本地 .env.local（参考 .env.example），并确保本地 PostgreSQL 已建库 goaway_db
./start.sh local          # http://127.0.0.1:8080
mvn -o test               # 跑测试
```

### 前端

```bash
cd frontend
npm install
./start.sh local          # iOS 模拟器 + 本地后端
npm run lint
```

## 架构要点

- **DDD 分层**：每个 context 分 `api → application → domain/infrastructure`，`domain` 不依赖 `api`。
- **Provider 模式**：外部能力（OSS / 短信 / LLM / 推送 / 内容审核 / 埋点）皆为可切换 provider，
  由 `app.*.provider` 环境变量激活，本地默认 mock/log/local，生产切到阿里云/APNs/deepseek。
- **契约同步**：后端 DTO 是 API 契约唯一事实源，自动生成前端 JS / iOS Swift 模型。
- **账号体系**：游客 token + 手机号/Apple 登录 + JWT；工具游客即用，上榜/云同步/AI 周报需登录。

## 当前进度

- ✅ **Phase 0**：脚手架派生、根包 `com.goaway`、裁剪业务上下文、account/媒体/安全链路跑通、前端裁剪与导航重建。
- ⏳ **Phase 1**：打卡 + 摸鱼 + 匿名排行榜（首个完整闭环）。
- ⏳ **Phase 2**：AI 周报（LLM + SSE）。
- ⏳ **Phase 3**：情绪价值（树洞 + 内容审核）+ 推送提醒（喝水/久坐/下班）。
