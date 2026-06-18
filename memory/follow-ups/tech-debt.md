# Tech Debt

更新时间：2026-06-13

## P1 · 近期值得跟进

### [P1-9] 朗读规则 OTA 下发 + 多音字消歧（✅ 基础框架已完成，2026-05-22）

- **已完成**
  - 后端：`GET /api/meals/speech-rules` 返回 `{version, rules: [{id, pattern, replacement, enabled, flags}]}`，初始 13 条规则（10 条单位归一化 + 长消歧 + `没过→淹过`），静态 hardcode，无需 DB
  - 前端：`speechRulesService.js`（拉取 + AsyncStorage 1 小时 TTL 缓存 + `applyOtaRules`）；`useRecipeStepSpeech.js` 挂载时异步加载规则，`start()` 调用前对 step.content 预处理后再传给 Swift bridge
  - Swift hardcode 规则保持不变作为兜底
  - `generate-contract-models.mjs` 已更新支持 `public record` 语法（新增 record 组件解析分支），`SpeechRuleDTO` / `SpeechRulesResponseDTO` 已同步到 `generatedContracts.js` + `APIContractModels.swift`
  - 编译：`mvn test` 192 通过 / 0 失败；`npm run lint` 0 errors；`npm test` 111 通过

- **第二批多音字规则已补（2026-06-13，版本 `2026-06-13`，含 SpeechRulesTest 守护）**
  - 着（zháo 义短语）：`点着→点燃`、`烫着→烫伤`、`烧着了→烧起来了`；助词 zhe 不动
  - 重：`重+数字 → 重量+数字`（"约重750克"）；`重复/重新` 默认读音正确不加规则
  - 调：`调大火→转大火`、`调小火→转小火`；`调味/调匀` 默认读音正确不加规则
  - 干 / 还：菜谱语境下默认读音即正确（干煸/晾干、还要/还有），评估后不加规则
  - 注意：2.0.4 及更早客户端因路径 bug 从未消费过 OTA 规则（speechRulesService 已修，随 2.0.5 生效）
- **未来扩展（OTA 落地后随时热更新，无需 iOS 发版）**
  - 没：`没过` 已修；`水没过羊排` 等其他形式可继续补
  - Admin 后台维护规则（当前 hardcode 在 MealController，未来可改为读 DB + admin UI）
  - IPA 注音方案（iOS 16+）：OTA JSON 可承载 IPA 字段，效果比字词替换更精准；需先测真机接受度

- **相关文件**
  - [MealController.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/api/MealController.java) `buildSpeechRules()` — 规则数据在此维护
  - [speechRulesService.js](/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/src/features/meal/speechRulesService.js) — 缓存与应用层
  - [useRecipeStepSpeech.js](/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/src/features/meal/hooks/useRecipeStepSpeech.js) — `otaRulesRef` + `loadSpeechRules` hook
  - [MealSpeechSynthesizer.swift](/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/ios/frontend/MealSpeechSynthesizer.swift) `speechFriendlyText` — Swift 兜底规则

### [P1-8] 多关键词推荐"丢食材"badcase（✅ 修复链已落地，2026-06-13）

- **已完成（与原建议的三点差异及理由）**
  - catalog 数据：解析器新增伞型"蘑菇"标签（具体菇种 香菇/平菇/金针菇/杏鲍菇/茶树菇/口蘑/菌菇 自动附加"蘑菇" INGREDIENT tag，另补"口蘑"关键词）；用**启动期幂等回填** `MealCatalogService.backfillIngredientTags()` 给已入库 dataset 补差量标签，替代原建议的数据迁移——无需手动 SQL、无需 dataset 版本提升
  - 代码层：`resolveIntentProfile` 改为 **catalog ∪ fallback 并集**（catalog 优先、fallback 补缺），根治"收录词吞掉未收录词"；原建议的"QualityGate 补强"经分析不需要——gate 的 requiredCoverage 本身已足够严（dishCount+1），badcase 断点在 profile 丢词而非 gate 规则。当年担心的并集负向风险（蘑菇 catalog 评分=0 导致 title/配料不一致）已被伞型标签消除
  - 缓存层：ingredient-intent 多菜请求此前已整体跳过缓存（2026-04-24）；本次对**剩余缓存命中路径**（单菜/非 ingredient-intent 的多食材 profile）加 QualityGate 复验，老语义缓存覆盖不全则放弃命中重新生成——替代原建议的"缓存键纳入 hash"，无需新增列/DDL，且只拒真正坏的缓存
  - 测试：MealCatalogMarkdownParserTest 伞型标签 ×2、MealIntentServiceTest 并集 ×2、MealServiceTest 缓存复验 ×1，全量后端 203 通过
- **生产验证（2026-06-13，release `20260613094314-p18-p19-meal-quality`）**
  - 启动回填：606 个新 INGREDIENT 关联 / 491 道菜（含伞型"蘑菇"77 道、"菌菇"52 道），item_tags 12436→13042 与日志一致
  - 原 badcase 复验：`青椒，猪肉、蘑菇。` dishCount=2 → 「青椒肉丝 + 蘑菇炒肉片」三食材全覆盖（deepseek-v4-flash 鲜配路径，多菜 ingredient-intent 按设计跳缓存）
  - speech-rules 端点返回版本 `2026-06-13`、19 条规则
  - 回归：verify-backend 3 项 + smoke-api 13 阶段 + guest 路径（token/灵感 3 道）全过，部署后 ERROR 计数 0
- **遗留观察项**：持续观察 recommendation_path_log 的 matchGap 分布是否改善

#### 原始分析存档（badcase 背景）
- **基线案例**
  - requestId `f0a0c59b-c2e4-4f4f-b5ed-d98b30b13f1c`
  - 用户语音输入：`青椒，猪肉、蘑菇。`，dishCount=2，totalCalories=900，staple=RICE
  - 实际结果：`青椒肉丝` + `白菜猪肉炖粉条`，**蘑菇完全缺席**；timing 日志里 `ingredientKeyword=青椒,猪肉`，第二道菜既未用青椒也未用蘑菇却 `matchGap=0`
- **三层根因**
  - 数据层：`meal_catalog_tags` 的 INGREDIENT 类型里只有具体菇类品种（香菇/平菇/金针菇/杏鲍菇），没有泛指的"蘑菇"或"口蘑"
  - 代码层 short-circuit：`MealIntentService.resolveIntentProfile` 是 catalog vs fallback 二选一，只要 `findIngredientKeywordsInText` 返回非空就**完全不走** `fallbackIngredientsInText`（含"蘑菇/香菇"等 keyword），catalog 已收录的食材组合会把未收录词静默吞掉
  - QualityGate 层：`MealRecommendationQualityGate` 当前只检查"被解析出的 ingredient 是否被选中菜覆盖"，不检查"用户原文里还有哪些 ingredient 没被任何菜用到"，所以"白菜猪肉炖粉条"对 `matchGap=0` 是预期内的（但对用户视角错了）
  - 缓存层附加风险：`findLatestReusableRequestId(sourceText, dishCount, totalCalories, staple, locale)` 缓存键**不含 IntentProfile**，老 badcase 缓存已落库，未来同输入直接命中老结果
- **为什么 2.0.3 不修**
  - 单点修复（把 catalog ∪ fallback 直接合并）有负向跳变风险：catalog 评分对"蘑菇"仍 = 0，title selection 行为不变；但 phase 2 LLM prompt 多看到"蘑菇"会把它塞进 recipe 配料表，造成 title 是"白菜猪肉炖粉条"但配料里有蘑菇的不一致 UX
  - 真正修复需要数据迁移 + 代码 + cache key 一起改，影响面大，不适合在 2.0.3 发版前做
  - 当前结果虽然丢食材，但仍是合法菜谱，不崩溃，不影响其他能力
- **真正的修复链（建议下个版本）**
  - catalog 数据：把 `蘑菇/口蘑` 作为 INGREDIENT tag 加进 `meal_catalog_tags`，关联到现有的香菇/平菇等菜（或新增 1-2 道含菇 catalog 菜），让 `profileItemScore` 能给"蘑菇"评分
  - QualityGate 补强：用 sourceText 里所有 ingredient 字面 vs 选中菜的 title/tag 做全覆盖检查，未覆盖的 ingredient 应触发重选或拒绝缓存
  - 缓存键纳入 IntentProfile 的 ingredients/preferences hash，fix 上线后老 badcase 自然失效，无需手动 evict
- **相关文件**
  - [MealIntentService.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/application/MealIntentService.java) 第 310-320 行（resolveIntentProfile）、88-92 行（FALLBACK_INGREDIENT_KEYWORDS）
  - [MealCatalogService.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/application/MealCatalogService.java) 第 436-463 行（findIngredientKeywordsInText）、466-514 行（resolveMenuTitlesForProfile）
  - [MealRecommendationQualityGate.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/application/MealRecommendationQualityGate.java)
  - [MealRecipeRepository.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/infrastructure/persistence/MealRecipeRepository.java) `findLatestReusableRequestId` 查询

### [P1-7] 购物清单 & 推荐历史体验打磨（✅ 主要优化已完成，2026-05-22）

- **已完成**
  - 推荐历史：`HistoryScreen` 卡片改为可点击（`TouchableOpacity`），点击后调用 `GET /api/meals/requests/{requestId}/recipes` 拉取菜谱，通过 `navigation.navigate('HomeTab', { screen: 'MealResults', params: { preloadedRecipes, preloadedRequestId } })` 跳转到结果页；加载中显示 spinner，加载失败弹 `Alert`
  - 购物清单：`ShoppingListModal` summaryPanel 新增"复制"按钮（`copy-outline` icon），调用 `Share.share({ message: buildShareText(...) })` 打开 iOS share sheet，用户可一键拷贝到剪贴板；"分享"按钮（图片分享）保持不变
  - 后端：`GET /api/meals/requests/{requestId}/recipes` 新增端点，验证 userId 所有权后返回菜谱列表

- **剩余待优化（低优先级）**
  - 购物清单：加载中 skeleton、更细的食材归类词库与单位解析
  - 推荐历史：空态插图优化、支持「重新生成」入口
  - 两者均缺少非订阅用户的友好引导（当前直接弹付费墙）

- **相关文件**
  - [HistoryScreen.js](/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/src/features/meal/screens/HistoryScreen.js)
  - [ShoppingListModal.js](/Users/zhaoqiang/Documents/Project/what-to-eat/frontend/src/features/meal/components/ShoppingListModal.js)
  - [MealController.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/api/MealController.java) `getRecipesByRequestId`
  - [MealService.java](/Users/zhaoqiang/Documents/Project/what-to-eat/backend/src/main/java/com/quickstart/template/contexts/meal/application/MealService.java) `getRecipesByRequestId`

### [P1-1] 引入 Flyway 数据库迁移
- **现状**
  - 仍以 `bootstrap.sql` + 手工迁移为主
  - 生产 schema 变更缺少版本化管理
- **为什么值得做**
  - 当前表结构已经包含 catalog、subscription、recommendation records 等多条线，手工迁移越来越容易漂移
- **建议**
  - 引入 `flyway-core`
  - 将当前基线沉淀为 `V1__init_schema.sql`
  - 后续 schema 变化都走增量迁移

### [P1-2] 补 Health Check / Actuator（✅ 已完成，2026-05-22）
- `spring-boot-starter-actuator` 已加入 pom.xml
- `GET /actuator/health` 公开无需 auth（SecurityConfig 已加 permitAll）
- `show-details=never`，公网安全；RabbitMQ 排除在 roll-up 外（circuit breaker 降级，不把 MQ 状态算进整体 UP/DOWN）
- DB health indicator 默认启用，连接正常则返回 `{"status":"UP"}`
- 部署脚本可 `curl /actuator/health` 代替人工 smoke 第一步

### [P1-3] 部署过程改为原子替换（✅ 已完成）
- `ops/scripts/remote-release.sh deploy` 已实现：临时上传 → 停服务 → `mv` 原子替换 → 启服务
- 每次发布均走此脚本，不再直接覆盖运行中 jar

### [P1-4] `smoke-api.sh` 去 demo 假设化（✅ 已完成，2026-05-22）
- 去掉 `demo_admin` 硬编码默认值，要求显式注入 `SMOKE_AUTH_TOKEN` 或 `SMOKE_AUTH_USERNAME + SMOKE_AUTH_PASSWORD`，缺失时脚本直接报错退出
- 新增 `skip_phase` 函数：主动跳过（exit 0）vs 断言失败（exit 1）语义清晰
- 补推荐反馈断言：从 sync 推荐响应提取 `requestId`，断言 `POST .../feedback` 返回 `feedbackStatus`
- 新增 smoke 阶段：`speech-rules`（OTA 规则）、`history`（历史列表）、`history-detail`（按 requestId 取菜谱）
- 用法：`SMOKE_AUTH_USERNAME=xxx SMOKE_AUTH_PASSWORD=yyy ./ops/scripts/smoke-api.sh https://eat.868299.com`

### [P1-5] LLM 厂商 PK 继续用正式资源复测
- **现状**
  - 已完成一轮 live PK，报告见 [llm-pk-2026-04-18.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/llm-pk-2026-04-18.md)
  - 当前结论是：
    - `deepseek-chat`（现已升级为 `deepseek-v4-flash`）最稳，适合作为默认主链路
    - `qwen3.6-flash` 值得继续观察
    - `qwen3.6-plus` 稳定性不足
    - `deepseek-reasoner` 时延过高
- **为什么值得继续做**
  - 当前 Qwen 使用的是免费试用 Key，时延/稳定性可能受额度与限流影响
  - 厂商选型已经开始影响主链路性能与成本
- **建议**
  - 用正式付费 Qwen Key 再复测一次 `qwen3.6-flash`
  - 增加更复杂业务输入，验证结论是否稳定

### [P1-6] 收缩后端 fat jar 体积
- **现状**
  - 当前后端 fat jar 约 `73MB`
  - 业务 class 只有约 `1.2MB`，大部分体积来自运行时依赖
- **为什么值得做**
  - 当前 ECS 热部署已经受到大文件传输稳定性影响
  - 包体积偏大也会拖慢发布、回滚和故障恢复
- **当前已识别的大头**
  - `hibernate-core` / JPA 基础设施：合理，但属于主要体积来源
  - `springdoc-openapi-starter-webmvc-ui` + `swagger-ui`：生产环境可评估收起
  - `dysmsapi20170525` 及其引入的 `bcprov-jdk18on`：如果短信不是主链路，值得重点评估
  - `spring-boot-starter-amqp`：如仍长期降级为 log-only，可评估改成更明确的可选能力
  - `aliyun-sdk-oss` 及其传递依赖：取决于媒体存储是否必须
- **建议**
  - 先把 Swagger UI 调整为非生产依赖或仅开发环境启用
  - 评估短信 SDK 是否仍需放在主后端运行时
  - 将 MQ 能力做成更明确的可选模块或 profile
  - 如 OSS 仅少量环境需要，继续做 provider 层模块化收缩

## P2 · 架构与数据模型

### [P2-1] Recipe JSON 列改为 PostgreSQL `jsonb`
- **现状**
  - `ingredients_json` / `steps_json` / `seasonings_json` 仍主要按文本 JSON 使用
- **为什么值得做**
  - 后续做搜索、统计、运营筛查时，`jsonb + GIN` 更适合

### [P2-2] Prompt 外部化
- **现状**
  - Prompt 逻辑仍主要在 Java 代码里维护
- **为什么值得做**
  - 调优频繁时，代码部署成本高，Diff 也不够聚焦

### [P2-3] 契约同步与文档语义继续收口
- **现状**
  - 契约体系已经能生成前端模型，但 `contracts/model-sync.config.json` 的职责与实际 DTO 生成面不够直观
- **为什么值得做**
  - 现在 DTO、前端生成模型、Swift 模型都在用，契约链路需要更容易理解和维护

### [P2-4] 静态资源缺失不要进入全局 500（✅ 已完成，2026-05-22）
- `GlobalExceptionHandler` 新增 `NoResourceFoundException` 处理器，直接返回 404 空体，不再走 catch-all ERROR 日志

## P3 · 长期改善

### [P3-1] Refresh Token 机制
- **现状**
  - 仍以单 JWT 会话为主
- **为什么值得做**
  - 审核通过后用户量一上来，登录态体验和可撤销性都会更重要

### [P3-2] 基础可观测性
- **现状**
  - 缺少标准 metrics 和 dashboard
- **为什么值得做**
  - 目前线上排障仍 heavily 依赖 SSH + 日志
- **建议**
  - Prometheus / Grafana 或同等方案
  - 先覆盖 HTTP、LLM、图片、MQ、DB pool

### [P3-3] 后端集成测试（Testcontainers）
- **现状**
  - 单元测试较多，但 auth → meal → favorites → recommendation record 的端到端集成验证仍薄
- **为什么值得做**
  - 当前功能已经跨 account / meal / subscription / admin，多上下文交叉更适合集成测试守住

### [P3-4] 前端发布级回归进一步自动化
- **现状**
  - Jest 覆盖已不算少，但对“审核级主链路”仍主要靠人工 remote 验证
- **为什么值得做**
  - 真机远端回归代价高，适合继续补强发布前的关键路径自动验证
