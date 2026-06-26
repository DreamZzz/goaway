# goaway iOS 上架 App Store Checklist

> 对照 what-to-eat 2.0 发布日志（`~/Documents/Project/what-to-eat/memory/release-2.0-checklist.md`）改写，
> 把其两轮真实拒审教训前置规避。状态：✅ 就绪 / ⚠️ 待办 / 🚧 阻断（提交前必须解决）。
> 更新时间：2026-06-26（首次评估）。

## what-to-eat 踩过的坑（必须前置规避）

1. **Guideline 5.1.1(v) 强制登录被拒**：首页「文字/语音」入口要求先注册/登录被拒，被迫开放游客试用。
   → goaway 对应风险：**AI 对线（roleplay/chat）完全锁登录**。见 🚧 B1。
2. **Guideline 3.1.2(c) 缺 EULA**：App 描述与元数据需提供 Terms of Use (EULA) 链接。→ 见 ⚠️ M3。
3. **Guideline 2.1(b) IAP 未随版本提交**：goaway **无 IAP/订阅**（Phase 0 已裁剪），此坑不适用 ✅。
4. **价格/本地化文案 bug**（沙盒美元拼成 `¥$…元`）：无 IAP，不适用 ✅。
5. **元数据三个 URL 必须可达**（privacy/support/marketing 均 200）→ goaway 当前 401，见 🚧 B2。

## 🚧 阻断项（提交前必须解决）

### B1. AI 对线强制登录（Guideline 5.1.1(v) 同款风险）
- 现状：`/api/roleplay/chat/stream` 走 `currentUserService.requireRealUserId()`，游客完全不能用 AI 对线。
- 风险：与 what-to-eat 被拒原因一致——非账号必需的核心功能不应强制登录。
- 建议：放开**游客试用 AI 对线**（仿 what-to-eat 的「游客 1 道试吃」：游客可对线 N 条/天，超出再引导登录）；
  账号型能力（毒舌推送、排行榜上报、周报存档）继续要求登录。
- 影响文件：`RoleplayController`、`GuestSessionService`（已有游客额度 `consumeInspirationTrial` 范式可仿）、前端对线入口。

### B2. 隐私政策 / 支持 / 营销 URL 不可达（当前 401）
- 现状：`/v2/` = 200；但 `/`、`/privacy`、`/support` = **401**。根因：`/` forward 到 `/web/index.html` 而 `/web/**` 未放行；
  且**没有 /privacy、/support 页面**（静态目录只有 web/ admin/ v2/）。
- 必须：① 新增 goaway 品牌的隐私政策页 + 支持页；② `SecurityConfig` 放行 `/privacy`、`/support`、`/web/**`（GET）；
  ③ 确认 `https://goaway.868299.com/{,/privacy,/support}` 均 200。
- 影响文件：`SecurityConfig.java`、`src/main/resources/static/`（新增页面）、`GoawayBackendApplication`（视图转发）。

## ⚠️ 待办（重要，多数我可做）

### M1. Info.plist 权限说明是 what-to-eat 残留且 goaway 未用
- `NSCameraUsageDescription` / `NSMicrophoneUsageDescription` / `NSPhotoLibraryUsageDescription` 文案全是
  「冰箱/食材/用餐」——goaway 前端**无相机/麦克风/相册调用**（grep 无 ImagePicker/launchCamera/recorder）。
- 但工程里残留 5 个 dead 原生文件（`MealVoiceRecorder/MealSpeechSynthesizer*`）会引用 mic/audio API，
  Apple 二进制扫描可能据此要求 purpose string，或质疑「打工人工具为何要相机」。
- 建议：**移除 5 个 dead Meal* 原生文件 + 对应 pbxproj 条目 + 删 3 条无用权限说明**（最干净）；
  或保留则把权限文案改诚实。需确认 `media/api.js`（存在但似未接图片选择器）是否真要相册。

### M2. 版本号沿用 what-to-eat 克隆值
- 现状：`MARKETING_VERSION=2.0.5`、`CURRENT_PROJECT_VERSION=29`。goaway 是**全新 App 记录**。
- 建议：首发重置为 `MARKETING_VERSION=1.0.0` / `CURRENT_PROJECT_VERSION=1`（ASC build 号按需递增、不可复用）。

### M3. App Store Connect 元数据（人工，提交时）
- App 名称「狗啊喂」、副标题、关键词、描述、分类、年龄分级、截图（6.9" 1290×2796 + 6.5" 1242×2688）。
- **描述末尾必须附 EULA 链接**（标准款 `https://www.apple.com/legal/internet-services/itunes/dev/stdeula/`）+ 隐私政策链接。
- **App Review Notes**：提供测试账号、游客可用路径说明（打卡/摸鱼/毒鸡汤本地可用；AI 对线游客试用边界）、账号注销路径。
- 发布方式建议 `Manual Release`。

### M4. 毒舌内容年龄分级 / 客观内容
- 「骂老板 / 毒舌推送」语气具攻击性。年龄分级问卷大概率 12+/17+（Mature/Suggestive Themes 或 Profanity & Crude Humor）。
- `COMMON_CONSTRAINTS` 已禁辱骂/人身攻击/歧视，符合 1.1；但 AI 生成内容建议提供「举报不当回复」入口以稳妥过审（1.2 AI 内容）。

### M5. 推送环境 sandbox→production（提交/Archive 时切，勿提前）
- `frontend.entitlements` `aps-environment` development→**production** + ECS `.env` `APP_PUSH_APNS_PRODUCTION` false→**true**。
- 注意：现在切会让开发签名真机联调收不到推送，**仅在做 Archive/分发包时切**。

### M6. app.json displayName 仍为 "What To Eat"（✅ 本次已改为「狗啊喂」）
- Info.plist `CFBundleDisplayName` 已是「狗啊喂」（桌面名正确）；app.json 仅构建期信息，已对齐。

## ✅ 已就绪

- 账号注销：`SettingsScreen` → `/api/.../deleteAccount`（PII 匿名化）——满足 Apple 强制要求。
- Apple 登录：`/api/auth/apple-login` 已有（若用第三方登录，Apple 要求的 Sign in with Apple 已具备）。
- 导出合规：`ITSAppUsesNonExemptEncryption=false`（免每次构建追问）。
- AppIcon：含 1024×1024 及全尺寸（提交前确认 1024 **无 alpha 通道**）。
- PrivacyInfo.xcprivacy 存在（提交前核对申报的数据类型与实际一致）。
- 部署目标 iOS 15.1；自动签名；Team `K6FK4L8DC5`。
- 无 UGC 公开 feed（mood 仅每日毒鸡汤 `/api/soup`，树洞未实现）→ 无举报/拉黑硬要求。
- APNs 通道已打通（V2/V3 已上线，真机验证过）。

## 发布前质量门（沿用 what-to-eat gates）

- Gate 1 静态：`mvn -o package -DskipTests`、`npm run lint`、`npm test`、DTO 变更跑 `sync-models.sh`。
- Gate 2 生产：ECS 后端最新包；`https://goaway.868299.com/{,/privacy,/support}` 均 200；游客与登录主链路 smoke。
- Gate 4 真机主链路：冷启动游客可用工具（打卡/摸鱼/毒鸡汤）；AI 对线游客试用 + 登录全量；毒舌推送收发 + 深链；
  IM 历史本地保留；账号注销；登录/注册/Apple 登录。
- Gate 5 ASC 元数据 + 截图 + EULA/隐私/支持 URL + Review Notes + Manual Release。
- Gate 6 发布后观察：ECS 日志、推送投递、登录失败率、崩溃/反馈。
