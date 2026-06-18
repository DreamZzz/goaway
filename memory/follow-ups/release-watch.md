# Release Watch

更新时间：2026-06-14

## 当前发布基线

- App 名称：`今天吃点啥？`
- 当前线上 Version：`2.0.5`
- Bundle ID：`com.868299.eat`
- 设备范围：`iPhone-only`
- App Store 状态：**2.0.5 (29) 已于 2026-06-14 审核通过并上线**
- 历史通过版本：2.0.1 (4) 2026-05-07、2.0.2 (10) 2026-05-10、2.0.3 (27) 2026-05-18（自动发布）、2.0.4 (28) 2026-05-25、2.0.5 (29) 2026-06-14
- 下一版：待规划（候选方向：推送促活 Phase 2 · APNs 远程推送，见 active.md「下个版本」）
- Web 状态：根首页已切为 2.0 新版介绍页；`https://eat.868299.com/v2/` 继续作为兼容地址保留
- iOS 2.0 发布 checklist：见 [release-2.0-checklist.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/release-2.0-checklist.md)
- iOS 2.0.3 发布 checklist：见 [2-0-3-checklist-rosy-sutton.md](/Users/zhaoqiang/.claude/plans/2-0-3-checklist-rosy-sutton.md)

## 2.0.5 提审口径（已通过）

- ✅ **结果：2.0.5 (29) 已于 2026-06-14 审核通过并上线**
- 候选版本：`2.0.5 (29)`（工程已 bump，2026-06-13）
- 提审/通过：2026-06-14 审核通过上线（提审日期未单独记录）
- What's New（已定稿 2026-06-13）：
  - 优化菜谱朗读多音字识别，常见菜名播报更准确
  - 完善偏好页可选择是否使用冰箱库存参与推荐
  - 新增饭点提醒与体验会员到期提醒（系统通知，可在系统设置中关闭）
  - 优化多关键词推荐，避免遗漏指定食材
- 推广文本 Promotional Text（已定稿 2026-06-13，≤170 字符，改动免重新提审）：
  > 不知道今天吃什么？说一句、点一下，AI 立刻给你搭好今天的菜。做饭时还能听菜谱、边听边做，一键生成购物清单。再也不用纠结。
- 本版改动范围：
  - speech-rules 路径修复（commit `7872aab`）+ 第二批多音字 OTA 规则（P1-9，commit `5dde400`）：OTA 朗读规则首次真正在客户端生效（2.0.4 及更早因路径 bug 从未消费过 OTA 规则）
  - 库存参与推荐改为完善偏好页显式勾选（commit `0ba5305`）：未勾选时文字/语音自定义输入不使用库存
  - 本地通知 Phase 1（commit `afbae9f`）：饭点提醒（客户端本地调度）+ 体验会员到期提醒；A 链路 provisional 默认开启，B 链路点击落地灵感偏好页
  - 多关键词推荐丢食材修复链（P1-8，commit `a6fd092`）
- 提审前核对（已完成，2026-06-14 通过审核）：
  - 质量门全绿（2026-06-13）：后端 `mvn test` exit 0、前端 `npm run lint` 0 errors / 48 已知 warning、前端 `npm test` 26 suites / 153 tests 全过
  - 待办：archive 前注入生产 `runtime.generated.js`（`node scripts/write-runtime-config.js remote https://eat.868299.com ""`，参照 5/16 build 14 教训，避免 bundle 回落 127.0.0.1）
  - 待办：`xcodebuild` Debug + Release archive，确认 `CFBundleShortVersionString=2.0.5 / CFBundleVersion=29`
  - 待办：生产 API smoke（`./ops/scripts/smoke-api.sh https://eat.868299.com`）
  - 待办：真机验证本地通知 A 链路（删 App 重装重置权限：首启无弹窗、通知中心安静送达、primer 升级横幅）+ 菜谱朗读多音字 OTA 规则实际命中
  - 待办：App Store Connect What's New / 审核备注 / 截图

## 2.0.4 提审口径

- 候选版本：`2.0.4 (28)`
- 提审时间：2026-05-24
- What's New：
  - 推荐历史支持点击查看完整结果，含菜谱详情、步骤和购物清单
  - 购物清单新增"复制"按钮，可快速复制到备忘录或微信
  - 优化菜谱朗读多音字识别，提升常见场景播报准确度
- 本版改动范围：
  - P1-7：推荐历史卡片可点击跳转结果页（`GET /api/meals/requests/{requestId}/recipes`）+ 购物清单复制按钮
  - P1-9：OTA 朗读规则框架（后端 `GET /api/meals/speech-rules` + 前端 `speechRulesService.js` + hook 集成）
  - P1-2/P2-4：actuator health check + 静态资源 404（基础设施，不体现 What's New）
  - P1-6：fat jar 78M→62M，移除 Aliyun SMS SDK 和 springdoc（基础设施，不体现 What's New）
- 提审前核对：
  - 质量门全绿：后端 192 通过 / 0 失败，前端 lint 0 errors，前端测试 129 通过
  - runtime.generated.js 已注入 `https://eat.868299.com`，archive 内 bundle 确认含生产地址
  - 生产 smoke 已通过（2026-05-23）：全部 13 个接口检查通过
  - Archive 版本确认：`CFBundleShortVersionString=2.0.4 / CFBundleVersion=28`

## 2.0.2 提审口径

- 候选版本：`2.0.2 (10)`
- What's New：
  - 支持 Apple ID 一键登录
  - 新用户专属福利：领取 7 天体验会员
  - 购物清单支持分享为图片
  - 优化食材类请求的菜谱推荐质量
- 提审前核心核对：
  - 生产 API smoke 已通过（2026-05-10）：使用新建账号 `smoketest20260510222700` 跑通 `./ops/scripts/smoke-api.sh https://eat.868299.com`；语音上传 smoke 未启用，仍按可选项处理
  - 首月 ¥8 StoreKit 沙盒购买弹窗、订阅状态回写和恢复购买已确认
  - 7 天体验会员内部 `review-reward/claim` 闭环已确认，不作为 App Store 订阅免费试用提交
  - 审核备注和前台文案不要写成“评价换会员”；权益应先发放，评分提示只能使用系统 API，且不要求用户提交评分或评价
  - 设置页真机验证已确认：邮箱更换、修改密码、账号注销
  - App Store What's New 已按本节四条确认
- 审核通过后待办：
  - 将生产环境 `/opt/what-to-eat/backend/shared/.env` 中 `APP_MEAL_FREE_DAILY_QUOTA` 从 `10` 调整为 `5`，重启后端后确认 app 与 admin console 的非会员每日推荐额度显示同步为 5 次/天
  - 调整前不要在 2.0.2 审核期变更生产额度，避免审核窗口内付费/试用口径漂移

## 2.0.3 发布计划

- 候选版本：`2.0.3`，Build 号待定；正式打包前必须高于当前工程 build `13`
- 版本定位：做菜执行体验 + Pro 食材库存能力增强，保持 iOS-first；本版包含 `inventory` 后端新上下文、新表、视觉识别 provider、库存辅助推荐理由、过期维护与做饭后库存扣减
- Feature list：
  - 菜谱详细做法支持语音播放：基于 iOS 本地 `AVSpeechSynthesizer` 朗读分步做法，不上传菜谱文本
  - 支持播放 / 暂停 / 继续 / 停止、上一/下一步、`0.8x / 1.0x / 1.2x` 倍速
  - 朗读时高亮当前步骤；步骤未生成时先补齐做法，完成后自动开始朗读
  - 游客试吃菜谱也可使用朗读能力；该能力作为基础免费功能，不触发 Paywall、不消耗会员额度
  - Pro 用户支持“我的食材”：拍照或从相册选择冰箱/厨房照片，服务端识别可见食材、分类和粗略余量
  - 识别结果进入复核页，用户可勾选、改名、改分类、改余量、补漏识别项，再保存为个人库存
  - 库存支持列表查看、手动补充和删除；同名食材按 `userId + normalizedName` upsert
  - 每个库存食材维护过期时间；临期/过期状态在库存页展示，过期食材可一键清理
  - Pro 用户可在完善偏好页显式勾选库存参与生成；未勾选时文字/语音自定义输入不使用库存，勾选后推荐理由和菜谱卡展示“您的冰箱里有 XX / 用到库存”
  - 购物清单按库存状态标注已有、少量、需购买、已过期；过期食材仍视为需购买
  - 菜谱卡支持“做过了”，用户确认后按粗略余量扣减或清理库存
  - 库存识别默认走 `APP_INVENTORY_VISION_PROVIDER=mock`，生产启用前切 `bailian-qwen-vl` 并配置百炼 Qwen-VL key
- What's New 草案：
  - 新增菜谱做法语音播放，做饭时可边听边操作
  - 支持分步朗读、步骤高亮、倍速播放和上一/下一步控制
  - Pro 新增“我的食材”，可拍照识别冰箱食材并整理库存
  - 推荐菜时优先提示家里已有食材，并支持做饭后维护库存
- 本版明确不纳入：
  - 云端 TTS / 音色选择 / 音频缓存或分享
  - 后台播放、锁屏控制、厨房模式大改版
  - 强约束“只按库存做菜”、精确克数库存账本、AI 识别包装生产日期、游客库存或游客购买后绑定库存
  - Android 专项适配、菜谱视频前台入口、订阅/额度规则调整
- 发布前核心核对：
  - Xcode 工程版本改为 `MARKETING_VERSION = 2.0.3`，`CURRENT_PROJECT_VERSION` 使用新的递增 build
  - 后端 schema：生产执行 `inventory_scans / inventory_scan_items / inventory_items / inventory_recipe_consumptions` 相关 DDL，确认唯一索引、过期图片清理任务、库存过期字段与消费幂等字段可用；若用 `postgres` 手工建表，必须将表和序列 owner/权限同步给 `what_to_eat_user`
  - 生产 env：补齐 `APP_INVENTORY_VISION_PROVIDER=bailian-qwen-vl`、`APP_INVENTORY_VISION_API_KEY`、`APP_INVENTORY_VISION_MODEL=qwen-vl-plus`、图片保留周期与 Pro 每日扫描额度
  - 真实 provider 验证：冰箱全景、蔬菜抽屉、包装食材、剩菜容器、模糊/遮挡照片各跑一次，确认空识别/低置信度/失败状态都可恢复
  - 权限验证：guest 调库存接口返回 `GUEST_RESTRICTED`；非会员点入口弹 `inventory_scan` Paywall；7 天体验会员和正式 Pro 可扫描
  - 真机验证：推荐结果页、菜谱详情页、游客试吃结果页均可朗读；离开页面后停止朗读
  - 真机验证：语音输入后再进入菜谱朗读，确认录音与播放音频会话不互相污染
  - 真机验证：pending steps 自动补齐后开始朗读；断网/步骤失败时不崩溃并保留重试入口
  - 真机验证：我的食材拍照、相册选择、复核保存、手动补充、删除、关闭重进库存页
  - 真机验证：构造未过期、临期、过期库存后，推荐理由、菜谱库存命中标签、购物清单标注和“做过了”扣减弹窗符合预期
  - 回归 guest 路径：未登录可进入首页文字/语音入口并试吃 1 道菜，收藏/购物清单/历史仍按既有登录规则
  - 跑 `cd backend && mvn test`、`cd frontend && npm run lint`、`cd frontend && npm test -- --runInBand`、iOS Debug/Archive 构建
  - 后端生产 API smoke 保留为标准检查；库存识别建议新增单独的 Pro 测试账号手工 smoke，避免自动脚本误触真实图片识别成本
- 2026-05-12 ECS 联调记录：
  - 已部署后端 release `20260512220139-inventory-fix` 到生产 ECS，当前服务 active
  - 生产 schema 已补齐 `inventory_*` 表、索引、owner 与 `what_to_eat_user` 权限
  - 完整 API smoke 通过：登录、意图、同步/流式推荐、详情、补图、步骤、收藏、订阅状态
  - 库存专项 smoke 通过：guest 拦截、非 Pro 拦截、7 天体验会员授权、图片上传识别、复核保存、库存列表、删除清理
  - 真机 remote Release 包已安装并启动到 `赵强的iPhone`，运行时 API 指向 `https://eat.868299.com`
  - 2026-05-12 23:03 已将生产库存识别切为 `APP_INVENTORY_VISION_PROVIDER=bailian-qwen-vl`、模型 `qwen-vl-plus`；API key 已写入 ECS `.env`，不入库不入文档
  - 百炼 Qwen-VL 最小 smoke 已通过：库存扫描返回 `COMPLETED`，provider=`bailian-qwen-vl`，model=`qwen-vl-plus`
  - 真实冰箱照片质量验证仍需在真机上人工点验
  - 物理拍照/相册选择/复核页手势流需要在手机上人工点验；命令行已覆盖到构建、安装、启动和线上 API 端到端
- 2026-05-13 库存识别线上排障：
  - 失败 scan `id=5` 的上传图片、OSS 访问、百炼 API key 与 `qwen-vl-plus` 模型均验证可用；同图直连百炼成功，生产 API 重放也可成功，根因判断为百炼/网络瞬时失败叠加后端错误信息过于笼统
  - 已为 `bailian-qwen-vl` provider 增加 429 / 5xx / 网络异常重试、provider 错误摘要脱敏与 scan 失败日志，默认 `APP_INVENTORY_VISION_MAX_ATTEMPTS=3`、`APP_INVENTORY_VISION_RETRY_BACKOFF_MS=300`
  - 已部署后端 release `20260513215411-inventory-vision-retry-fix`；发布 smoke 通过，复用失败图片的线上库存扫描返回 `COMPLETED`，识别出 `四季豆`、`腊肉`
- 2026-05-16 build 14 TestFlight 不可用事件（仅内测人员受影响，0 真实用户）：
  - 症状：TestFlight 装上 `2.0.3 (14)` 后，从 onboarding 第 7 页进入登录页登录账户 `zhao` 失败
  - 根因：build 14 通过命令行 `xcodebuild archive` 打包，未先跑 `./start.sh remote` 注入 `frontend/src/app/config/runtime.generated.js`，RN bundle 回落到 `runtime.js` 默认值 `http://127.0.0.1:18080`，请求全部打到 iPhone 自身的 127.0.0.1，连接被拒
  - 证据：jsbundle 里 grep 出 `http://127.0.0.1:18080`；生产 nginx access.log 整个文件 + 历史归档 `0 行 frontend/14`；同时段 frontend/13（线上 2.0.2）`/api/auth/login` 与 `/api/subscription/*` 调用正常
  - 影响范围：仅 build 14 这一个 TestFlight 包，仅你这台 iPhone 装到（10:53 上传 → 13:42 报告，约 3 小时）；真实线上 App Store 2.0.2 用户全程未受影响，后端服务正常
  - 修复：升 `CURRENT_PROJECT_VERSION` 到 15、先跑 `node scripts/write-runtime-config.js remote https://eat.868299.com ""`、重新 `xcodebuild archive` 打 `2.0.3 (15)`、Organizer 上传 TestFlight；同时把 `ensure_runtime_config` 步骤加入 `ci_scripts/ci_pre_xcodebuild.sh`，覆盖命令行手工 archive 与 Xcode Cloud 两条路径，避免再次裸跑
- 2026-05-16 步骤双调用 race 修复（badcase："擂辣椒皮蛋"推荐结果页与收藏详情页步骤不一致）：
  - 症状：同一道菜在推荐结果页显示 4 步做法，收藏后进详情页显示 6 步且文字完全不同
  - 根因：推荐生成时 [MealService.java:1111](backend/src/main/java/com/quickstart/template/contexts/meal/application/MealService.java) `CompletableFuture.runAsync(preGenerateStepsAsync)` 与前端 `RecipeCard.useEffect` 触发的 `/steps/stream` 并发跑两次 LLM；LLM `temperature > 0` 两次结果不同。预生成赢了 `stepsStatus=PENDING` 写入检查、把 stream A1 落库；前端 stream 输出 stream B 给用户看，但写库被跳过。收藏后再进详情读 DB 看到 A1 → 与之前看到的 B 不一致
  - 修复：删掉推荐链路里的 `preGenerateStepsAsync` 触发（保留收藏链路的兜底，覆盖"没进结果页直接收藏"的小众场景）；前端 `/steps/stream` 成为唯一的 LLM 调用路径并写库
  - 部署：后端 release `20260516154937-fix-steps-race`，jar sha256 `af743b50...542b`，verify-backend.sh 通过
  - 用户视角：每条推荐少跑一次 LLM（节约成本），步骤稳定一致
  - 自己反省：第一次问到这个问题时我看代码 cache-first 分支就断言"绝大多数情况下不会调 LLM，视觉错觉"，没画时序图分析 race。用户拿截图打脸后第二次复查才发现 race。该教训已写入 `feedback_evidence_over_inference` memory
- 2026-05-16 2.0.3 候选构建与生产后端配套部署：
  - iOS 工程版本升至 `MARKETING_VERSION=2.0.3 / CURRENT_PROJECT_VERSION=14`，commit `d99ac4b`；`2.0.3 (14)` 已通过 Xcode Organizer 上传 App Store Connect TestFlight 并显示"完成"
  - 本地静态质量门全绿：`mvn package` SUCCESS、`mvn test` 192 通过 / 0 失败 / 4 skipped（live tests）、`npm run lint` 0 errors / 46 已知 warning、`npm test` 23 suites / 110 tests 全过、`xcodebuild Debug iphonesimulator` BUILD SUCCEEDED、`xcodebuild Release archive` ARCHIVE SUCCEEDED
  - 部署后端 release `20260516114443-2-0-3-inventory-fixes`（commit `d99ac4b`，jar sha256 `5cba0d59...4947`）经 `remote-release.sh deploy` 原子替换，systemctl active；`verify-backend.sh` 通过（local captcha 200 / public captcha 200 / public protected 401）
  - 修复 5/13 release 缺失的 `InventoryItemDTO.expiresAt / expiryStatus` 字段回填与 `POST /api/inventory/items/expired/clear` 端点（5/13 jar 早于 378ddb4 引入这两项，导致 smoke 中 expiresAt 字段被静默丢弃、expired/clear 返回 500）
  - 新建 smoke 账号 `smoke20260516110519` 跑通：注册 / 登录 / `smoke-api.sh`（auth → intent → recommend sync/stream → recipe detail/image/steps → favorites → subscription）/ 7 天体验会员领取 / 百炼 Qwen-VL 扫描 `provider=bailian-qwen-vl, model=qwen-vl-plus, COMPLETED` / 库存 upsert + list（含 `expiresAt` + `expiryStatus`）/ `expired/clear` 正确删除过期项 / 单条 DELETE
  - 库存权限边界已验证：非 Pro 调 inventory 接口返回 403 `PRO_REQUIRED`；guest token 调 inventory 接口被 `GuestAccessControlFilter` 在 controller 前拦截，返回 401 `INVALID_GUEST_CONTEXT`（与 plan 中 `GUEST_RESTRICTED` 等价拦截）
  - 静态页 4/4 200：`/`、`/v2/`、`/support`、`/privacy`
  - 真机 Gate 3 / 4 / 5 仍需在 `赵强的iPhone` 上人工 double-check（拍照识别完整链路、菜谱朗读、主链路回归），命令行只覆盖到 API 与权限边界

## 历史提审记录

| 版本 | 结果 | 原因 |
|------|------|------|
| 1.0.0 Build 1 | 失败 | 提审时误选了旧版本包（不含注销账号功能），Apple 要求必须提供账号注销能力 |
| 1.0.1 | 驳回 | Apple 认定非账号型能力被强制登录拦住，要求未注册用户可直接体验核心功能 |
| 1.0.2 | — | 中间版本，跳过或未单独记录 |
| 1.0.3 | 通过 | 含 guest 模式修复，2026-04-26 通过审核 |
| 2.0.1 (2) | 驳回 | App Store 元数据缺少 Terms of Use (EULA)；Apple 认为 promo monthly membership 对应 IAP 未随版本完整提交审核 |
| 2.0.1 (3) | 驳回 | Guideline 5.1.1(v)：Apple 要求未注册用户可访问首页”文字”和”语音”入口 |
| 2.0.1 (4) | **通过** | 2026-05-07 审核通过 |
| 2.0.2 (10) | **通过** | 2026-05-10 审核通过（Apple ID 登录 / 7 天体验会员 / 购物清单分享图 / 食材请求质量优化） |
| 2.0.3 (27) | **通过** | 2026-05-18 审核通过，等待 Manual Release。本版滚动了 13 个修复 build：菜谱朗读 + Pro 我的食材（拍照识别）+ 用库存推荐入口 + 收藏取消 + 收藏分类筛选 + 多关键词推荐质量优化 + Apple introOffer eligibility 后端权威化 + 步骤持久化 race 修复 + 空库存兜底 + LLM prompt 净化与真实命中文案 |
| 2.0.4 (28) | **通过** | 2026-05-25 审核通过（推荐历史可点查完整结果 / 购物清单复制按钮 / OTA 朗读规则框架） |
| 2.0.5 (29) | **通过** | 2026-06-14 审核通过（菜谱朗读多音字优化 / 库存参与推荐改显式勾选 / 饭点 + 体验会员到期本地通知 / 多关键词推荐丢食材修复） |

## 发布期重点关注

### 1. 线上稳定性优先
- 当前允许继续修：
  - 生产稳定性
  - ECS 配置
  - 邮件/登录/推荐成功率
  - 日志与告警
- 下一次正式发版前仍应避免无计划改动：
  - 付费规则与前台入口
  - 主要页面文案和交互
  - 已通过版本的核心 guest 体验路径

### 2. guest 路径按 2.0.1 拒审要求扩展
- guest 模式已上线，核心路径：
  - 冷启动直接进入首页
  - 未登录可直接体验“来点灵感”
  - 未登录可进入“文字”和“语音”入口，并在偏好页固定试吃 1 道菜
  - guest token 只允许访问 `POST /api/auth/guest`、`POST /api/meals/guest-inspirations` 与 `POST /api/voice/transcriptions`
  - 游客文字/语音只生成 1 道菜：强 catalog 命中直接返回，弱命中或无匹配调用 LLM 试吃；收藏、推荐反馈、购物清单、推荐历史、订阅与购买能力仍要求登录

### 3. Xcode Cloud 构建链路已修复并验证
- Ruby/Bundler 版本不兼容：已修复（ensure_ruby + ensure_bundler）
- hermes-engine checksum drift：已修复（--no-repo-update 代替 --deployment）
- 友盟 CDN（oss-cn-shanghai）国际不可达：已修复（vendored 进 frontend/ios/vendor/）
- **已验证（2026-04-24）**：Archive 绿过，构建链路稳定

### 4. Hermes dSYM warning 记录在案
- **现状**
  - Upload 阶段存在 `hermesvm.framework` dSYM 缺失 warning
- **结论**
  - 不影响 build 进入 App Store Connect
  - 不影响 TestFlight / 审核
  - 影响的是 Hermes native crash 的符号化质量
- **建议**
  - 作为后续单独改进项处理，不为此单独重打包

### 5. 线上静态页必须保持可访问
- 当前应保持：
  - [https://eat.868299.com/](https://eat.868299.com/)（2.0 新版介绍页）
  - [https://eat.868299.com/v2/](https://eat.868299.com/v2/)（兼容地址，同版 2.0 页面）
  - [https://eat.868299.com/support](https://eat.868299.com/support)
  - [https://eat.868299.com/privacy](https://eat.868299.com/privacy)
- 任一返回非 `200` 都会影响审核/对外体验

### 6. 真机 remote 基线保持不动
- 当前已验证：
  - `./start.sh device remote "赵强的iPhone"` 可用
  - 正式 Team / 正式 Bundle 当前已稳定
- 不要再主动清理或重建证书链，只在真实阻塞时处理

### 7. 命令行 archive 必须落到 Organizer 标准目录（多次踩坑）
- **症状**：命令行 `xcodebuild ... archive` 跑完 `ARCHIVE SUCCEEDED`，但 Xcode Organizer 里看不到包，重启 Xcode 也没有。
- **根因**：Organizer 只索引 `~/Library/Developer/Xcode/Archives/<YYYY-MM-DD>/` 下的 archive。用 `-archivePath /tmp/xxx.xcarchive` 或任意自定义路径打的包不在这个目录，所以 Organizer 永远扫不到。
- **正确做法（命令行 archive 二选一）**：
  - 直接把 `-archivePath` 指到标准目录：
    ```bash
    xcodebuild -workspace ios/frontend.xcworkspace -scheme frontend -configuration Release \
      -destination 'generic/platform=iOS' -allowProvisioningUpdates \
      -archivePath "$HOME/Library/Developer/Xcode/Archives/$(date +%F)/frontend-<版本>.xcarchive" archive
    ```
  - 或打到临时目录后 `cp -R` 到 `~/Library/Developer/Xcode/Archives/$(date +%F)/`，再重开 Organizer 窗口（Window → Organizer）即可显示，通常无需重启 Xcode。
- **校验**：archive 的 `Info.plist` 需含 `Name` / `SchemeName` / `CreationDate` / `ArchiveVersion` / `ApplicationProperties.ApplicationPath`，命令行 archive 默认会写齐；缺失才会导致 Organizer 列表空白。

## iOS 2.0 发布前必须完成

- StoreKit 沙盒 → TestFlight → 生产购买链路完整验证
- App Store Connect 元数据更新：截图、What's New、审核备注、EULA、IAP 一并提交
- 设置页真机验证：邮箱更换、修改密码、账号注销
- 生产 smoke 与线上静态页 200 检查
- 根首页切 V2 后，继续确认审核期 Web、支持页、隐私页均保持 200

## 2.0 明确不纳入

- 短信验证码正式登录
- 菜谱视频前台入口
- iPad 支持
- Android 发布
- 用户侧菜谱搜索

## 后续非阻断项

- Hermes dSYM 完整上传链路
