# App Store 发布清单

## 🔴 阻塞项

- [ ] **[人工]** 确认 Apple Developer 账号会员资格有效（$99/年）
- [ ] **[人工]** 在 App Store Connect 创建 App 条目（Bundle ID、App 名称、分类）
- [x] **[人工]** 完成 Xcode Organizer / Distribute App 上传到 App Store Connect
- [ ] **[人工]** 在 App Store Connect 里确认本次上传的 build 已完成处理，可用于版本提交

## 🟡 强烈建议

- [x] **[AI]** 修改 Bundle ID：`com.quickstart.template.frontend` → `com.868299.eat`
- [x] **[AI]** `package.json` version 对齐：`0.0.1` → `1.0`
- [x] **[AI]** 准备隐私政策页面并部署上线（https://eat.868299.com/privacy）
- [x] **[AI]** 准备 App Store 截图（6.9" + 6.5"）
- [ ] **[人工]** 最终确认截图中的品牌、图标、状态栏和线上包一致
- [x] **[AI]** 准备支持页面（https://eat.868299.com/support）
- [ ] **[人工]** 在 App Store Connect 填写 App 描述、关键词、宣传语
- [x] **[AI]** 接入 Sentry 崩溃监控（前端代码部分）
- [ ] **[人工]** 注册 Sentry 账号，获取 DSN，填入环境变量

## 🟢 发布流程

- [x] **[人工]** Xcode Archive → Distribute App → 上传到 App Store Connect
- [ ] **[人工]** App Store Connect 填写年龄分级、出口合规（加密声明）、广告标识符声明，并确认 `App Uses Non-Exempt Encryption = No`
- [ ] **[人工]** 提交审核，等待苹果审核结果（通常 1–3 个工作日）

## ✅ 当前仓库事实

- 正式 Team：`K6FK4L8DC5`
- 正式 Bundle ID：`com.868299.eat`
- 当前 iOS 工程版本：`2.0.2 (10)`
- 当前设备族：`iPhone-only`
- 隐私政策：`https://eat.868299.com/privacy`
- 支持页面：`https://eat.868299.com/support`
- 营销首页：`https://eat.868299.com`

## ⚠️ 当前已知非阻塞告警

- 上传阶段仍会看到 `Upload Symbols Failed`
  - 原因：`hermesvm.framework` 的 dSYM 未被一并上传
  - 影响：Hermes 相关 native 崩溃的符号化不完整
  - 结论：**不阻塞 App Store Connect 接收 build，也不阻塞提审**
