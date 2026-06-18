# Android 开发计划

整理时间：2026-05-03（方向更新：国内应用商店 + 微信/支付宝支付）
目标：在现有 React Native 工程基础上完成 Android 版本，功能对齐 iOS 2.0.1，优先上架华为、小米、腾讯等国内应用商店，订阅付费接入微信支付和支付宝（手动续费先行，后续补免密代扣）。

---

## 现状扫描

### 已有基础
- `frontend/android/` 目录存在，是标准 RN 0.84.1 脚手架（`MainActivity.kt` + `MainApplication.kt`）。
- RN 层绝大多数业务代码（导航、API、UI 组件）跨平台共用，无需重写。
- `runtime.js` 已有 Android 模拟器 URL（`http://10.0.2.2:8080`）。
- UMeng adapter 已写好 fallback，Android 无原生模块时自动降级。

### iOS 已上线能力，Android 对应状态
| 项目 | iOS 2.0.1 | Android 现状 |
|---|---|---|
| HomeScreenV2 | ✅ | 纯 JS，未在真机全量验证 |
| 推荐历史页 | ✅ | 纯 JS，未验证 |
| 购物清单 + 分享 | ✅ iOS 原生分享 | `shoppingListShare.js` 已有 `Share.share()` fallback |
| 付费墙 UI | ✅ | JS 共用，但 ViewModel 含 iOS-only API，会 crash |
| 订阅/IAP | ✅ Apple IAP | 未接支付，Phase B 接微信/支付宝 |
| 语音输入 | ✅ | ✅ Phase 2-A 已完成（2026-04-26） |
| 友盟统计 | ✅ | ✅ Phase 2-B 已完成（2026-04-25） |
| 版本号 | 2.0.1 | versionName 仍 1.0.3，需更新 |

---

## 支付方案说明

- **个人资质无法直接申请微信支付/支付宝 APP 支付商户号**，需先注册个体工商户。
- **个体工商户注册**：全程网办，免费，多数城市 1-3 个工作日；注册完成后即可申请微信支付商户号和支付宝企业账号。
- **订阅模型**：第一版手动续费（购买 = N 个月权益），后续视用户量补微信/支付宝免密代扣。
- Apple IAP 仍是 iOS 唯一支付通道，不变。

---

## 阶段划分

### Phase A：UI 验证 + 防崩溃适配（约 1-2 天，无外部依赖）✅ 进行中

**目标**：Android 上跑通 2.0.1 全部非付费主链路，无 crash。

#### A-1 版本号与渠道配置
- `android/app/build.gradle`：`versionName "2.0.1"`，`versionCode 2`
- `productFlavors` 改为多渠道：`huawei`、`xiaomi`、`tencent`、`general`（去掉 `play`）
- 每个 flavor 对应不同 `CHANNEL`，友盟 `umengChannel` 读 `BuildConfig.CHANNEL`

#### A-2 订阅 ViewModel iOS-only API 守卫
- `useSubscriptionViewModel.js`：`getReceiptIOS`、`checkIntroEligibilityIOS`、`restorePurchases` 均用 `Platform.OS === 'ios'` 保护
- Android 上付费墙购买按钮暂显示「即将开放」，不 crash
- 待 Phase B 完成后替换为微信/支付宝入口

#### A-3 Android 真机主链路验证
| 验证项 | 结果 |
|---|---|
| 冷启动 → guest 首页（HomeScreenV2） | ✅ |
| 游客灵感推荐 + 第N次被拦截 | ✅ |
| 登录 / 注册 / 找回密码 | ✅ |
| 文字输入 → 偏好页 → 流式结果页 | ✅（修复 SSE keepalive + sync fallback prompt 锚定后验证） |
| 语音输入 | ⚠️ 模拟器无麦克风，返回空结果，需真机验证 |
| 图片/做法补齐 | ✅（做法自动异步加载，无需点按） |
| 收藏读回 | ✅ |
| 购物清单生成 + 系统分享 | ⏭️ 订阅权益，Phase B 后验证 |
| 推荐历史页分页 | ⏭️ 订阅权益，Phase B 后验证 |
| 打开付费墙不 crash | ✅（显示「Android版购买功能即将开放」占位） |
| 设置页：邮箱更换/修改密码/注销 | ✅ |
| 硬件返回键行为 | ✅ |

---

### Phase B：微信支付 + 支付宝接入（约 5-7 天，依赖个体工商户注册完成）

**前提**（外部依赖，用户并行准备）：
- 注册个体工商户（网办，免费，1-3 工作日）
- 申请微信支付商户号（需营业执照，审核 1-3 工作日）
- 升级支付宝企业账号（即时）+ 开通 APP 支付

#### B-1 后端：支付订单与回调接口
```
POST /api/payment/order/create      → 创建微信/支付宝订单，返回 payParams
POST /api/payment/wechat/notify     → 微信异步回调，验签 + 发放权益
POST /api/payment/alipay/notify     → 支付宝异步回调，验签 + 发放权益
GET  /api/payment/order/{orderId}   → 前端主动查询订单状态
```
复用现有 `SubscriptionService` 写权益逻辑，与 Apple 路径并列，不破坏现有接口。

#### B-2 前端：Android 支付 Native Module
- `WechatPayModule.kt`：封装微信支付 Android SDK，唤起微信收银台
- `AlipayModule.kt`：封装支付宝 Android SDK，唤起支付宝收银台
- `PaywallModal.js` Android 分支：替换 IAP 按钮，显示「微信支付 / 支付宝」入口
- 支付流程：创建订单 → 收到 payParams → 唤起支付 → 轮询订单状态 → 刷新 Pro 状态

#### B-3 联调验证
- 微信/支付宝沙盒购买月会员和 10 次包
- 后端 notify 回调签名验证
- 订单超时/支付失败场景

---

### Phase C：多渠道打包 + 合规 + 应用商店提交（约 2-3 天）

#### C-1 隐私合规弹窗（国内应用商店强制要求）
- App 首次启动显示隐私协议弹窗，用户点击「同意」后再初始化 UMeng 和支付 SDK
- 友盟 `preInit` 已在 `attachBaseContext`，符合要求；`init` 需移到同意后

#### C-2 多渠道 Release 包构建
- 每个 flavor 输出独立 APK / AAB
- 友盟渠道自动注入，各商店来源可区分

#### C-3 各应用商店提交
| 商店 | 优先级 | 说明 |
|---|---|---|
| 华为应用市场 | P0 | 市场份额最大，优先上 |
| 小米应用商店 | P0 | 审核相对快 |
| 腾讯应用宝 | P1 | 需开发者认证 |
| OPPO/vivo | P2 | 后续追加 |

---

## 关键风险

| 风险 | 应对 |
|---|---|
| 个体工商户注册周期 | Phase A 期间并行办理，不阻塞开发 |
| 微信支付审核周期（1-3工作日） | Phase B 开发期间同步申请 |
| 支付宝 APP 支付沙盒配置 | 提前在开放平台创建沙盒应用，不等正式账号 |
| 国内应用商店首次上架审核 | 预留 3-7 天缓冲，早提交 |
| 软著（部分商店要求） | 个体工商户注册后可申请，约 1-3 个月；先试上架，被拒再补 |

---

## 里程碑

| 里程碑 | 状态 |
|---|---|
| Phase 1（工程基础）+ Phase 2-A（语音）+ Phase 2-B（友盟） | ✅ 完成 |
| Phase A：2.0.1 UI 对齐 + 防崩溃 + 真机验证 | ⏳ 进行中 |
| 个体工商户注册 + 商户号申请 | ⏳ 用户并行办理 |
| Phase B：微信支付 + 支付宝接入 | ⏳ 待商户号到手 |
| Phase C：多渠道打包 + 各商店提交 | ⏳ Phase B 完成后 |
