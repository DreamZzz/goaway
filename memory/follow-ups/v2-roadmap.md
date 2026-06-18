# V2.0 迭代计划

整理时间：2026-04-26（1.0.3 审核通过，第一批已接入）
前提：1.0.3 通过 App Store 审核后开始迭代。✅ 已通过（2026-04-26）

---

## 第一批：已实现、直接接入即可发布 ✅ 2026-04-26 完成

### 1. 购物清单入口 ✅（已上线，体验待打磨）
- `MealResultsScreen` 新增「一键生成食材采购清单」按钮，点击 fetch 后弹出 `ShoppingListModal`，支持系统分享。
- 逻辑在 `useMealResultsViewModel`：`openShoppingList` / `closeShoppingList` / `shoppingListLoading`。
- **已知问题已修**：SSE 缓存命中路径下 requestId 不一致导致 404，已修复并部署（2026-04-26）。
- **待优化**：整体体验仍较粗糙，列入 tech-debt [P1-7]。

### 2. 推荐历史页 ✅（已上线，体验待打磨）
- `HistoryScreen` 注册进 `RootStack`（路由名 `History`），`ProfileScreen` 新增入口行。
- **待优化**：整体体验仍较粗糙，列入 tech-debt [P1-7]。

### 3. 付费订阅入口 ✅
- `ProfileScreen` 配额卡内新增「开通会员，解锁更多次数」按钮，触发 `PaywallModal`；订阅后隐藏按钮，用户名旁显示会员徽章。
- **待验证**：StoreKit 沙盒 → TestFlight → 生产完整购买链路（需单独安排）。

---

## 第二批：有基础、需补齐开发

### 4. 新版首页 HomeV2 全量上线 ✅ 2026-04-26 完成
- **现状**：`HomeScreen` 已直接渲染 `HomeScreenV2`，不再读取内测 flag；`ProfileScreen` 已移除"内部测试"卡片和 HomeV2 开关。
- **说明**：V2.0 新版首页默认对所有用户开放，不再按白名单或账号名分流。
- **后续**：继续按真机反馈打磨视觉和交互细节，但不再作为内测功能管理。
- **相关文件**
  - `frontend/src/features/meal/screens/HomeScreenV2.js`
  - `frontend/src/features/meal/screens/HomeScreen.js`
  - `frontend/src/features/profile/screens/ProfileScreen.js`

### 5. 短信验证码登录
- **现状**：后端接口已实现，生产 `APP_AUTH_SMS_PROVIDER=log`；前端 `LoginScreen` 有手机字段。
- **工作量**：接阿里云短信 provider，配生产模板，测试完整验证码登录流程。
- **相关文件**
  - `backend/.../platform/provider/sms/`
  - `frontend/src/features/auth/screens/LoginScreen.js`

### 6. 菜谱视频
- **现状**：后端 `videoStatus` 字段存在，provider 留位（`APP_MEAL_VIDEO_PROVIDER=disabled`）；前端结果页已解析 `videoStatus`。
- **工作量**：选定视频生成 provider 并接入，开启前端播放/展示入口。
- **相关文件**
  - `backend/src/main/resources/application.properties`（`app.meal.video.provider`）

---

## 第三批：全新开发

### 7. 菜谱搜索
- **现状**：`frontend/src/features/search/` 目录已建但为空；后端 Elasticsearch provider 已有，`database` fallback 可用。
- **工作量**：前端搜索页 + 搜索接口，后端确认 search provider 生产配置。

### 8. iPad 支持
- **现状**：1.0.x 临时改为 iPhone-only；iPad 布局未适配。
- **工作量**：响应式布局适配，重新开启 iPad target。

### 9. 推荐历史 → 再次生成
- **现状**：历史页展示后，支持点击一条历史记录直接复用或重新生成。
- **工作量**：历史详情页 → 结果页跳转逻辑。

### 10. 个人中心完善
- **现状**：头像上传接口已通（OSS）；昵称编辑 `EditProfileScreen` 已有但流程不完整。
- **工作量**：头像上传 UI + 昵称保存完整流程验收。

---

## 注意事项

- 第一批三项（购物清单、历史页、付费入口）代码几乎就绪，审核通过后可快速打包为 1.1.0 或 2.0.0 提交。
- 付费功能上线前需单独走一次 StoreKit 沙盒 → TestFlight → 生产的完整链路验证。
- 短信登录需要提前申请阿里云短信模板并备案，有外部依赖，建议提前启动。
