# Active Follow-ups

更新时间：2026-06-16

## 待验证

### 本地通知 Phase 1 真机收尾验证（2026-06-12）
- **上线**：随 `2.0.5 (29)` 于 2026-06-14 审核通过上线
- **内容**：推送促活 Phase 1（饭点提醒 + 体验会员到期提醒）已实现并装机，B 链路（点击落地灵感偏好页）已验证
- **待验证**：A 链路（provisional 默认开启）需删 App 重装重置权限状态后确认：首启无弹窗、通知中心安静送达、primer 升级横幅
- **度量**：上线 2 周后（**2026-06-28 起**）跑 D1 周 cohort 对比基线（~33%）；Umeng 看 `notif_perm_result` 授权率与 `notif_tap` 点击率

### 邮箱验证码生产链路验证（2026-05-01 更新）
- **内容**：`POST /api/auth/email/change/send-code` 与 `confirm` 已随后端多次发布到 ECS，但仍需用真实账号跑一次邮箱更换完整闭环
- **验证目标**：邮件可达、验证码校验成功、邮箱变更后登录/个人页展示正常

## 当前阻塞 / 外部依赖

### 短信验证码登录仍是日志模式
- **现状**
  - `APP_AUTH_SMS_PROVIDER=log`
- **说明**
  - 不纳入 iOS 2.0 发布阻断门；如果后续要正式开放短信登录，需要真实 provider 与生产短信模板。

## 下个版本（推送促活 Phase 2 · APNs 远程推送）

### device token + PushSender provider + 流失召回
- **内容**：本地通知已覆盖饭点提醒；Phase 2 做服务端推送——`user_devices` 表（手动迁移 SQL）、`POST /api/devices` 注册接口（契约同步）、`platform/provider/push/` 下 PushSender 接口 + LogPushSender 默认 + ApnsPushSender（Pushy 库 + .p8 token auth）、`NotificationDispatchJob` 定时任务（流失召回 N=2/4/7 天、体验会员到期接管、额度重置提醒）、`push_send_log` 频控表（每用户每日 ≤1 条，10:30–20:00 窗口）
- **场景所有权**：饭点提醒永久归客户端本地；体验会员提醒在 token 上报成功后由服务端接管；流失召回归服务端
- **Ops**：Apple Developer 创建 APNs Auth Key（.p8），env 加 `APP_PUSH_PROVIDER` 等；详细方案见 2026-06-12 迭代计划

## 下个优化冲刺（S4 · TagEnrichmentService — 暂缓）

### 推荐路径 → 分类树 enrichment pipeline
- **内容**：读取 `recommendation_path_log` 中 matchGap > 0 或 match 缺失的请求，由管理员审核后补齐/新增叶节点
- **依赖条件**：需要先积累 1-2 周生产数据，观察 F_catalogIngredient 命中率和 matchGap 分布
- **相关文件**：`PathLogService`、`HierarchicalIngredientResolver`、`recommendation_path_log` 表

## 最近已完成（不再作为 active）

- **阿里云智能语音商用版（2026-06-16 完成）**：「一句话识别」(aliyun-short / `APP_SPEECH_PROVIDER=aliyun`) 从新用户免费试用升级为商用，env var 不变自动转商用计费，赶在 6-30 试用到期前完成。
- **2.0.2 发审前核心验证（2026-05-10 完成）**：生产 API smoke、首月 ¥8 StoreKit 沙盒购买弹窗/订阅回写/恢复购买、设置页真机流程、App Store What's New 均已确认。
- **购物清单分享图片化（2026-05-10 完成）**：购物清单分享从 `.txt` 改为品牌 PNG 分享卡，带 App 标识、分类清单和下载引导，真机功能验证通过。
- **7 天体验会员（2026-05-10 完成）**：内部体验会员奖励闭环已进入 2.0.2 发布范围；用户领取后由 `POST /api/subscription/review-reward/claim` 发放 7 天会员权益，不走 Apple 订阅免费试用流程，权益不以提交评分或评价为条件。
- **Apple ID 登录（2026-05-09 真机验证通过）**：`POST /api/auth/apple`（RS256 JWKS 验证）、`User.appleUserId`、`UserDTO.appleUserId`、前端 `AppleButton` + `appleLogin()`；生产 DDL 已执行，entitlements 已配置，App ID 在 Developer Portal 已开启 Sign in with Apple。
- **食材意图多菜 Hybrid Catalog 优化（S1-S3，2026-05-08 部署）**：PathLogService 异步写入 recommendation_path_log 表、TaxonomySeeder LLM 初始化分类树（97 个食材标签→三层 TAXONOMY 树）、HierarchicalIngredientResolver 特性开关（当前默认 `legacy`）；F_catalogIngredient/F_hybridIngredient 路径已激活，标题选择从 10-12s 降至 <200ms（catalog 命中时）。DB 迁移（`meal_catalog_tags` 三列新增、`recommendation_path_log` 表、check constraint 扩展）已执行完毕。
- V2 静态页发布并提升到根首页：新增 `backend/src/main/resources/static/v2/`，公开 `https://eat.868299.com/v2/`；2026-05-02 已将同版页面提升到 `https://eat.868299.com/`，继续保留 `/v2/` 兼容地址。
- iOS 新版图标与首页 mood 图标接入（2026-05-01）：AppIcon 全尺寸替换，首页 6 个 mood 图从 catalog 抽样直达结果页，commit `536112a` / `aee3800`
- 食材清单微信分享修复（2026-05-01）：iOS 原生桥接将采购清单作为 `.txt` 分享，commit `57f99c4`
- dishCount=5 被共享葱姜蒜过滤为 4 道的问题修复（2026-05-01）：扩充通用调味/底料去重排除表，非流式标题选择加候选 buffer，commit `26321dd`
- 非订阅用户被错误判定为订阅用户（2026-04-27）：QuotaService/SubscriptionService 三处补 expiresAt 过期检查，commit `183e5bb`
- 显式菜名被食材多样性约束过滤（2026-04-27）：MealService.constrainedStreamConsumer 改为运行时读 isExplicitSelection()，commit `3f92acc`
- 付费墙与结果页额度展示数据统一（2026-04-27）：改为统一读 SubscriptionContext，commit `851e72e`
- 设置页 + 邮箱/密码管理 + 付费墙 UI 升级（2026-04-27）：commit `a7ea64f`
- App Store 提审包已上传并提交审核
- iOS 工程已切为 iPhone-only
- 找回密码已切正式 SMTP
- 真机 `remote` 构建、安装、启动链路已跑通
- 运营后台已补到可查询推荐记录、用户与订阅、内容运营
- RabbitMQ 降级噪音已收口：新增 `MqCircuitBreaker`（3次失败→断路5分钟），Spring AMQP 内部连接日志压至 ERROR 级别
- 异步推荐记录链路已收稳（2026-04-24）：LazyInitializationException 清零，feedbackStatus 真机验证写入正常
- 免费额度口径已对齐（2026-04-24，2026-05-11 文档复核）：免费用户每天 5 次、一次最多 3 道菜；Pro 用户每天 15 次、一次最多 5 道菜。付费墙代码保留，下一版本正式开放购买入口
