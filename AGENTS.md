# what-to-eat AGENTS

本文件是仓库内多 agent / 多入口协作的统一工作约束。目标不是重复代码细节，而是把**启动检查、事实来源、协作边界、发布与排障规则**固定下来，避免各处口径漂移。

## 1. Session Start Rules

每次进入仓库开始工作前，先读 follow-up 索引，再决定当前任务属于哪一类：

- `memory/follow-ups/README.md`
- 再按任务进入对应文件：
  - `memory/follow-ups/active.md`
  - `memory/follow-ups/release-watch.md`
  - `memory/follow-ups/tech-debt.md`

要求：

- 不要跳过这一步直接改代码。
- 发现已完成事项，要及时删除或迁移。
- 新识别出的延期事项，只能写入 **一个** authoritative follow-up 文件，避免多处分叉。

## 2. Repository Map

- `frontend/`: React Native iOS-first app。`src/app` 负责壳层；`src/features`（auth / meal / media / profile / subscription）负责业务能力出口；`src/shared` 负责共享 API、模型和通用工具。
- `backend/`: Spring Boot API。`contexts` 放业务域（account / meal / media / inventory / subscription）；`platform` 放安全、配置与 provider；`shared` 放通用响应模型。
- `ops/`: ECS / systemd / Nginx / smoke 脚本与发布材料。
- `contracts/`: contract sync 配置与生成元数据。
- `docs/`: 架构、provider、合同同步等长期说明。
- `memory/`: 稳定上下文、运行模式、术语、follow-ups；不放一次性调试日志。

## 3. Authoritative References

需要上下文时，优先读这些文件，而不是凭记忆推断：

- `memory/follow-ups/README.md` — follow-up 总索引
- `memory/api-contracts.md` — API 合同与对外语义
- `memory/deployment.md` — 生产部署与环境约束
- `memory/runtime-modes.md` — 前后端启动模式
- `memory/glossary.md` — 术语定义
- `backend/ARCHITECTURE.md` — 后端依赖方向与包边界
- `backend/DEPLOY_ECS.md` — ECS 部署步骤
- `docs/model-contracts.md` — DTO → JS / Swift 合同同步
- `docs/provider-matrix.md` — provider、env var 与切换矩阵
- `README.md` — 当前产品面、运行入口、能力概览

## 4. Collaboration Model

- 主 agent：负责架构、模块边界、API 合同、跨端集成、最终收口。
- `frontend_worker`：负责 RN 壳层、feature export、导航、运行时配置、前端测试。
- `backend_worker`：负责 provider、认证、持久层、模块 API、后端测试。
- `ops_worker`：负责 `ops/`、环境变量、部署、回滚说明、运行时配置。
- `test_guard`：负责验证矩阵、测试缺口、最小 smoke 与回归判断。

## 5. Core Working Principles

### 5.1 新能力优先按当前产品建模

- 新业务优先加模块，不要把通用能力改回旧样板私有实现。
- 已废弃的 `community / location / search` 模板业务不再保留；不要回滚到旧模板语义。
- `what-to-eat` 当前是 **iOS-first** 产品；Android 本阶段不做专门适配，但不能被破坏。

### 5.2 文档与代码必须同步

任何影响以下内容的改动，都要在同次提交里同步更新文档：

- API contract
- 环境变量 / provider 切换条件
- 生产部署方式
- schema baseline
- 产品面 / 运营面能力描述

最常见的同步目标：

- `memory/api-contracts.md`
- `memory/deployment.md`
- `README.md`
- `backend/sql/bootstrap.sql`

额外规则：

- `backend/sql/bootstrap.sql` 是 schema baseline。只要表结构、字段语义、关系或迁移策略变化，就必须同步更新该文件中的就地注释。
- 新增 `subscription`、`admin console`、`catalog image batch`、推荐记录、付费配额或 provider 能力时，必须同步更新 `README.md` 与 `memory/api-contracts.md`。

### 5.3 任务结束前检查文档是否过时

完成任意**非平凡改动**后，要主动检查：

- `CLAUDE.md`
- `AGENTS.md`
- `README.md`
- `docs/`
- `memory/`
- `memory/follow-ups/`

是否被这次改动打旧或打歪。  
如果有陈旧文档，结束前应提示用户：

`需要更新文档吗？`

### 5.4 默认入口不能漂移

- `frontend/start.sh` 和 `backend/start.sh` 是默认运行入口。
- 不要私造第二套常用启动命令而不落文档。
- `./ops/scripts/smoke-api.sh` 是默认 API smoke 入口，新增发布验收能力时优先扩它，而不是再造一套脚本。

### 5.5 登录与权限规则

- `meal`、偏好收藏、推荐历史、推荐反馈等接口默认要求登录；`voice` 仅允许游客在审核试吃链路中做转写，不开放账号型能力。
- 当前首版游客审核路径：
  - `POST /api/auth/guest`
  - `POST /api/meals/guest-inspirations`
  - `POST /api/voice/transcriptions`（仅转写，后续仍走游客 1 道菜试吃）
- 新匿名接口必须是明确设计结果，而不是“顺手放开”。
- guest token 只能访问受限游客链路：游客“来点灵感”固定 3 道基础菜单，游客文字/语音入口固定 1 道菜；文字/语音强 catalog 命中直接返回基础菜单，弱匹配或无匹配时仅允许按用户输入意图生成 1 道 LLM 试吃菜谱，不能绕过到完整多菜推荐、收藏、订阅、反馈、历史或购物清单等账号型接口。

## 6. Bug Investigation Protocol

发现线上或高优先级问题时，严格按这个顺序来：

1. **先看日志**  
   例如：
   - `journalctl -u what-to-eat-backend`
   - Nginx access / error logs
   - ECS 运行日志
2. **从用户症状追到日志证据，再确认根因**
3. **先向用户汇报发现和修复方案**
4. **得到确认后再改代码**

不要在根因未确认前盲改主链路。

## 7. Architecture Guardrails

### 7.1 Backend

`com.quickstart.template` 下当前三类顶级命名空间：

- `contexts/` — 业务域，分层为 `api -> application -> domain/infrastructure`
- `platform/` — provider、安全、配置等跨域技术能力
- `shared/` — 跨 context 的共享 DTO 与工具

依赖方向：

- `api` → `application`
- `application` → `domain/infrastructure`
- `domain` 不反向依赖 `api`
- 业务规则不写到 `platform`

### 7.2 Frontend

- `src/app/`：导航、provider、runtime config、应用壳层
- `src/features/`：业务能力出口
- `src/shared/`：共享 API client、生成模型、通用工具

不要把 feature 逻辑重新散回 `src/app` 或 ad-hoc 全局目录。

### 7.3 Provider Pattern

外部能力默认都按 provider 模式切换，通过 `app.*.provider` / `APP_*` env var 控制。  
新增 provider 时，至少要同步：

- provider 接口与实现
- 条件装配
- provider matrix 文档
- 部署文档中的 env 示例

## 8. Product-Specific Rules

### 8.1 Meal 主链路

- 基础菜单、推荐、图片、步骤、收藏、详情页是当前主产品面。
- 推荐结果、步骤流、推荐反馈、推荐记录等增强能力必须是**弱依赖**：不能阻塞菜谱主结果返回。
- 像 RabbitMQ、埋点、推荐记录保存这类非核心链路，允许降级，但不能拖慢主路径。

### 8.2 Subscription / Quota

- 当前公共版本按实际产品策略配置，不要让代码默认值停留在历史试验状态。
- 改动免费额度、套餐、配额判断时，要同步：
  - 后端默认配置
  - 前端展示口径
  - 运营台与文档说明

### 8.3 Admin Console

- 后台默认是内部运营台，不是对外产品面。
- 保持 `X-Admin-Secret` 保护模型。
- 运营台优先支持“查询、诊断、补齐、重试”，不要轻易开放危险写操作。

## 9. Commands

### Backend

从 `backend/` 运行：

```bash
./start.sh local
mvn -DskipTests package
mvn test
mvn test -Dtest="MealServiceTest,OpenAiCompatibleMealGenerationProviderTest"
```

### Frontend

从 `frontend/` 运行：

```bash
./start.sh local
./start.sh device remote "设备名"
npx react-native run-ios
npm run lint
npm test
npm run sync-models
```

### Contract Sync

```bash
./scripts/sync-models.sh
```

任何 DTO 改动之后，都要确认合同同步产物是否更新。

## 10. Validation Commands

提交前默认至少检查：

- `cd backend && mvn package -Dmaven.test.skip=true`
- `cd frontend && npm run lint`
- `cd frontend && npm test -- --runInBand`
- `./ops/scripts/smoke-api.sh`

如果有充分理由没跑，要在交付时明确说明原因和缺口。

## 11. Follow-up Management

遗留事项统一归口到：

- `memory/follow-ups/active.md`
- `memory/follow-ups/release-watch.md`
- `memory/follow-ups/tech-debt.md`

规则：

- 同一事项只放一个 authoritative 文件
- 完成后立即删或迁移
- 不要重新在仓库根目录散落新的 `TASKS` / `TECH_DEBT` 文件

---

如果 `CLAUDE.md` 与本文件有冲突，以 **`CLAUDE.md` + 仓库内最新代码事实** 为准；本文件应随之更新，而不是长期背离。
