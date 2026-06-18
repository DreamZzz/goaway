# iOS 2.0 发布验收清单

本文件是发布验收门的长期入口；本次 2.0 的完整执行清单见 [release-2.0-checklist.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/release-2.0-checklist.md)。

## 发布范围

2.0 纳入：HomeV2、新版 mood 场景、食材采购清单、推荐历史、What To Eat Pro、Apple IAP 订阅/次数包、新版图标与主链路稳定性修复。

2.0 不纳入：短信验证码正式登录、菜谱视频前台入口、iPad、Android、用户侧菜谱搜索。

## Gate 1：静态质量门

在准备发布前，先确保这三条基础检查全绿：

```bash
cd backend && mvn package -Dmaven.test.skip=true
cd frontend && npm run lint
cd frontend && npm test -- --runInBand
```

通过标准：

- 后端可正常打包
- 前端 lint 无错误
- 前端 Jest 全绿，不允许出现 IAP 原生环境误报或未包 `act(...)` 的异步告警

若失败，优先看：

- `frontend/jest.setup.js`
- `frontend/src/app/providers/SubscriptionContext.js`
- `frontend/src/app/navigation/AppNavigator.js`
- `frontend/src/features/subscription/useSubscriptionViewModel.js`

## Gate 2：本地或远端 API smoke

统一使用：

```bash
./ops/scripts/smoke-api.sh
```

远端环境示例：

```bash
./ops/scripts/smoke-api.sh https://eat.868299.com
```

当前 smoke 覆盖：

- `auth/login`
- `meals/intent`
- `meals/recommendations`
- `meals/recommendations/stream`
- `meals/recipes/{id}`
- `meals/recipes/{id}/image`
- `meals/recipes/{id}/steps/stream`
- `meals/favorites`
- `subscription/status`

2.0 发布前额外人工确认：

- `GET /api/meals/history`
- `GET /api/meals/requests/{requestId}/shopping-list`
- `POST /api/subscription/apple/verify`
- `POST /api/subscription/apple/restore`

语音 smoke 默认跳过；如需启用：

```bash
SMOKE_VOICE_FILE=/absolute/path/to/sample.wav ./ops/scripts/smoke-api.sh
```

通过标准：

- recommendations stream 至少收到 `summary`、`recipe`、`done`
- steps stream 至少收到 `token` 或 `step`，并收到 `done`
- 收藏后能在 favorites 读回
- subscription status 可正常返回
- 购物清单、推荐历史、IAP receipt 校验在真机或 TestFlight 专项中通过

## Gate 3：App 主链路验收

以 iOS `remote` 模式为准：

```bash
cd frontend && ./start.sh device remote "你的 iPhone 设备名"
```

固定验收路径：

1. 冷启动未登录进入首页
2. 游客点击“来点灵感”完成一次推荐
3. 登录成功
4. 新首页 mood 场景卡进入结果页
5. 首页输入低相关词，出现澄清
6. 确认后进入偏好页
7. 结果页显示推荐理由与流式菜谱
8. 图片和做法可补齐
9. 点击收藏后，“收藏”页出现菜谱
10. 点击收藏卡片进入详情页，看到完整做法
11. 购物清单可生成、展示、分享
12. 推荐历史页可打开并分页加载
13. 非会员访问 Pro 功能时展示付费墙
14. 设置页邮箱更换、修改密码、账号注销路径可用

订阅专项：

1. 免费配额耗尽
2. 唤起付费墙
3. 首月 `¥8` 价格展示符合 StoreKit 资格
4. 沙盒购买月订阅成功
5. 后端 receipt 校验成功，Pro 状态生效
6. 恢复购买成功
7. 次数包购买后到账并可抵扣
8. 隐私政策 / EULA 链接可打开

## Gate 4：App Store Connect 提审确认

- 版本号：`2.0.0`
- Build：`5`，若 App Store Connect 已占用则继续递增
- 截图覆盖：新首页、偏好页、结果页、购物清单、推荐历史、Pro 权益
- What’s New 已更新
- 审核备注包含：测试账号、guest 路径、Pro 入口、IAP 测试方式、账号注销路径
- IAP 商品随版本一并提交审核
- 支持 URL、隐私政策 URL、营销 URL 均可访问
- 建议选择 `Manual Release`，审核通过后人工发布

## 默认约束

- 当前发布门以 iOS-first 为准，Android 不纳入阻断门
- 短信、视频、iPad、用户侧搜索不纳入 2.0 阻断门
- 不新增第四套启动命令；统一复用 `backend/start.sh`、`frontend/start.sh`、`ops/scripts/smoke-api.sh`
