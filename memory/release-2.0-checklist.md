# iOS 2.0 发布计划 Checklist

更新时间：2026-05-05（2.0.1 第二轮拒审修复处理中）

## 发布范围

### 本次纳入

- [x] 新版首页 `HomeV2` 全量开放，不再走内测开关。
- [x] 首页 mood 场景卡从基础菜单 catalog 抽样，直接进入推荐结果。
- [x] 结果页新增食材采购清单，后端按采购区域与二级类别聚合，iOS 分享走 `.txt` 文件。
- [x] 推荐历史页进入底部 Tab，提供基础分页回看。
- [x] What To Eat Pro 入口、付费墙、Apple IAP 订阅与次数包链路纳入发布验收。
- [x] 免费/会员额度、订阅过期判断、付费墙与结果页额度展示口径对齐。
- [x] 新版 AppIcon 与 V2 Web 预览页已接入。
- [x] dishCount=5 被通用调味料去重挤成 4 道的问题已修。
- [x] 设置页邮箱更换、修改密码、账号注销继续作为发布前真机验证路径。

### 本次不纳入

- [ ] 短信验证码正式登录：当前生产仍是 `APP_AUTH_SMS_PROVIDER=log`，不写入 2.0 版本说明、不截图。
- [ ] 菜谱视频：当前前台不开放，`APP_MEAL_VIDEO_PROVIDER=disabled` 不作为 2.0 用户能力。
- [ ] iPad 支持：2.0 仍按 iPhone-only 发布。
- [ ] Android：与本次 iOS App Store 发版解耦，不作为阻断门。
- [ ] 用户侧菜谱搜索：当前没有前台搜索入口，不作为 2.0 承诺能力。

## Gate 0：代码与版本基线

- [x] 工作区保持干净后再开始发版改动。
- [x] `MARKETING_VERSION` 升至 `2.0.0`，后追加为 `2.0.1`。
- [x] `CURRENT_PROJECT_VERSION` 从 `1` 升至 `5`，2.0.1 重置为 build `1`。
- [x] 确认 App Store Connect 中 build `5` 未被占用；若已占用，继续递增 build。
- [x] 将 2.0 发版改动推送到 `origin/main` 或发布分支。
- [ ] 在最终候选 commit 上打 tag，建议 `ios-v2.0.1-build1`。

记录：2026-05-02 已推送 `16f71ed chore: prepare iOS 2.0 release checklist` 到 `origin/main`。
记录：2026-05-02 App Store Connect 中 iOS `2.0.0` 已创建并处于”准备提交”；build `2` 当时未被占用并已用于首个 TestFlight 上传。
记录：2026-05-02 build `2` 已上传并可安装 TestFlight；随后用户修复若干 bug，最终候选改为 build `3`。
记录：2026-05-02 TestFlight build `3` 安装后发现付费墙价格展示使用沙盒美元本地化值，且旧格式化逻辑会拼成 `¥$...元`；已修复为中国区上架价格文案并将最终候选递增为 build `4`。
记录：2026-05-02 用户完成 TestFlight 订阅相关流程复验，当前工程 build 已递增为 `5`。
记录：2026-05-02 版本升至 `2.0.1 / build 1`，修复若干前端 bug 后重新 Archive 并上传 TestFlight。

## Gate 1：本地静态质量门

- [x] `cd backend && mvn package -Dmaven.test.skip=true`
- [x] `cd frontend && npm run lint`
- [x] `cd frontend && npm test -- --runInBand`
- [x] 若 DTO 发生变化，执行 `./scripts/sync-models.sh` 并确认生成产物已提交。

记录：2026-05-02 后端打包通过，contract sync 输出 `contracts already up to date`；lint 通过但保留既有 `App.tsx` inline style warning；Jest 19 suites / 86 tests 全绿。

## Gate 2：生产后端与 Web

- [x] 确认 ECS `.env` 已配置 `APP_IAP_APPLE_SHARED_SECRET`。
- [x] 确认 SMTP 仍可用于找回密码与邮箱更换验证码。
- [x] 发布当前后端候选包到 ECS。
- [x] 运行 `./ops/scripts/smoke-api.sh https://eat.868299.com`。
- [x] 确认 `https://eat.868299.com/` 返回 200。
- [x] 确认 `https://eat.868299.com/v2/` 返回 200。
- [x] 确认 `https://eat.868299.com/support` 返回 200。
- [x] 确认 `https://eat.868299.com/privacy` 返回 200。
- [x] 决定 2.0 上线时是否将 V2 Web 预览页提升到根首页。
- [ ] 发布包含根首页 V2 切换的后端包到 ECS，并复查根首页、`/v2/`、支持页、隐私页均为 200。

记录：2026-05-02 通过 `curl -L -I` 确认根首页、V2 预览页、支持页、隐私页均为 HTTP 200。
记录：2026-05-02 用户确认邮箱找回密码可正常发送到腾讯企业邮箱；用户用最新 App 连接生产环境并通过沙盒 Apple ID 走通月度会员和 10 次包，说明生产 receipt 校验配置可用。
记录：2026-05-02 创建生产 smoke 账号 `smoketest20260502a`，并以该账号运行 `./ops/scripts/smoke-api.sh https://eat.868299.com` 成功；脚本覆盖登录、intent、同步/流式推荐、详情、补图、步骤流、收藏、订阅状态，语音按默认跳过。
记录：2026-05-02 用户确认本轮暂不重发后端；本次 2.0 准备未改后端运行时代码，生产现状已通过 smoke。
记录：2026-05-02 历史决策：用户曾确认 2.0 提交前暂不提升 V2 Web 到根首页；该决策已被下一条“根域名切 2.0 新版介绍页”覆盖。
记录：2026-05-02 用户更新决策：根域名切换到 2.0 新版介绍页；`/v2/` 继续作为同版兼容地址保留。该切换需要发布包含新版静态资源的后端包后在线上生效。
记录：2026-05-02 已在代码中将根路径转发到 `static/web/index.html`，并将 `static/web/index.html` 替换为 2.0 新版介绍页；后端打包通过，等待发布到 ECS 后生效。

## Gate 3：IAP 与订阅专项

- [x] App Store Connect 中月订阅 `com.868299.eat.subscription.monthly` 已创建、可提交审核。
- [x] App Store Connect 中次数包 `com.868299.eat.credits.10` 已创建、可提交审核。
- [x] 月订阅在单一订阅组内，展示名、描述、价格、审核截图完整。
- [x] 首月 `¥8` 推介促销价已生效，常规价格为 `¥28/月`。
- [x] 使用全新沙盒账号验证首月价展示、购买、后端 receipt 校验、Pro 状态生效。
- [x] 验证恢复购买。
- [x] 验证次数包购买后到账并可在额度用尽后抵扣。
- [x] TestFlight 包重复验证沙盒购买链路。

记录：2026-05-02 用户确认最新 App 连接生产环境时，沙盒 Apple ID 可走通 App Store Connect 中配置的月度会员与内购 10 次包，首月促销可正常触发。
记录：2026-05-02 用户确认恢复购买尚未验证；当前 IAP 验证基于真机连接 ECS 的开发/本地包，尚未发布 TestFlight 包复验。
记录：2026-05-02 用户确认恢复购买验证成功。
记录：2026-05-02 用户确认 TestFlight 中订阅相关流程验证完成。
记录：2026-05-02 用户确认月订阅位于单一订阅组内，展示名、描述、价格和审核截图完整；Gate 3 订阅专项全部完成。

## Gate 4：iOS 真机主链路

- [x] 冷启动未登录进入首页。
- [x] 游客点击“来点灵感”可完成一次低成本推荐。
- [x] 游客第 4 次灵感推荐被正确拦截并引导登录。
- [x] 登录成功。
- [x] 新首页 mood 场景卡可进入结果页。
- [x] 文字输入低相关词会先澄清，确认后进入偏好页。
- [x] 语音输入可转文字并进入偏好页。
- [x] 偏好页可选择菜数、总热量、主食。
- [x] 流式结果页显示推荐理由与菜谱卡片。
- [x] 图片与做法可补齐。
- [x] 收藏后可在收藏页读回，收藏卡片可进入详情页。
- [x] 购物清单可生成、展示、分享。
- [x] 推荐历史页可打开并分页加载。
- [x] 非会员访问 Pro 功能时展示付费墙。
- [x] Pro 用户可看到会员状态，相关权益生效。
- [x] 设置页邮箱更换验证码闭环通过。
- [x] 设置页修改密码闭环通过。
- [x] 账号注销路径仍可用。

记录：2026-05-02 用户确认 Gate 4 第一批真机主链路验证正常：冷启动游客首页、游客灵感推荐、游客额度拦截与登录成功均通过。
记录：2026-05-02 用户发现“文字输入低相关词会先澄清，确认后进入偏好页”当前存在问题，正在修复中；该项保持未完成，先继续验证后续链路。
记录：2026-05-02 用户确认 Gate 4 第二批真机主链路验证通过：mood 场景、语音输入、偏好页、流式结果、图片/做法补齐、收藏读回均正常。
记录：2026-05-02 用户确认 Gate 4 剩余真机主链路验证通过：关键词澄清复验、购物清单、推荐历史、Pro 付费墙/会员状态、邮箱更换、修改密码、账号注销均正常；Gate 4 全部完成。

## Gate 5：App Store Connect 元数据

- [x] 版本号创建为 `2.0.1`，选择 build `1`。
- [x] 上传新版截图：新首页、偏好页、结果页、购物清单、推荐历史、Pro 权益。
- [x] 更新 What’s New 文案。
- [x] 检查描述、关键词、宣传语是否与 2.0 能力一致。
- [x] App Review Notes 填写测试账号、guest 路径、Pro 入口、购买测试方式、账号注销路径。
- [x] 隐私政策 URL、支持 URL、营销 URL 均可访问。
- [x] IAP 商品（月订阅 + 10次包）与版本一并提交审核。
- [x] 发布方式选择：`Manual Release`，审核通过后人工放量。

记录：2026-05-02 已将 6 张 2.0 截图处理为 ASC 可接受规格：`frontend/app-store-assets/release-2.0/6.9/` 为 1290×2796，`frontend/app-store-assets/release-2.0/6.5/` 为 1242×2688；ASC 上传动作仍待人工完成。
记录：2026-05-02 复查 URL：`https://eat.868299.com/support`、`https://eat.868299.com/privacy`、`https://eat.868299.com/v2/` 均返回 HTTP 200；用户随后决定将 2.0 页面提升到根域名，营销 URL 改为 `https://eat.868299.com`。
记录：2026-05-02 iOS App `2.0.1 (build 1)` 及应用内购两个产品（月订阅 `com.868299.eat.subscription.monthly`、10次包 `com.868299.eat.credits.10`）已一并提交 App Store 审核。Gate 5 全部完成。
记录：2026-05-04 Apple 拒绝 iOS App `2.0.1 (2)`，Submission ID `55a1b8f5-b780-4b42-ba3c-fb5ce4852a5d`。拒绝原因：Guideline 3.1.2(c) 要求 App Store 元数据补 Terms of Use (EULA) 链接；Guideline 2.1(b) 认为 App 引用了 promo monthly membership，但对应 IAP 产品未完成随版本审核提交。Apple 明确要求补齐 IAP 审核材料并上传新 binary。

## Gate 5B：2.0.1 拒审修复

- [x] App Description 末尾补充 Terms of Use (EULA)：`https://www.apple.com/legal/internet-services/itunes/dev/stdeula/`
- [x] App Review Notes 补充 Terms of Use (EULA) 链接。
- [x] 确认月订阅 `com.868299.eat.subscription.monthly` 元数据、价格、审核截图完整，并处于 `Waiting for Review`。
- [x] 确认次数包 `com.868299.eat.credits.10` 元数据、价格、审核截图完整，并处于 `Waiting for Review`。
- [x] 录制并上传付费墙视频/截图：展示月订阅、10 次包、隐私政策链接、EULA 链接可点击访问。
- [x] 递增 build 后上传新 binary；本次重新提交为 `2.0.1 (3)`。
- [x] 在 Resolution Center 回复 Apple，并说明已补 EULA、两个 IAP 均已提交审核、新 binary 已上传。

记录：2026-05-05 用户已重新提交 iOS App `2.0.1 (3)`；App Description 已补 EULA 和隐私政策链接，两个 IAP（月订阅与 10 次包）在各自页面处于 `Waiting for Review`，并在审核备注中向 Apple 说明。

## Gate 5C：2.0.1 第二轮拒审修复

- [x] 记录 Apple 拒绝信息：Submission ID `fdf19a73-d456-4a3c-b825-b45069984889`，审核日期 2026-05-05，设备 iPad Air 11-inch (M3)，版本 `2.0.1 (3)`。
- [x] 确认拒绝原因：Guideline 5.1.1(v)，Apple 认为首页“文字”和“语音”入口不应要求用户先注册或登录。
- [x] 调整游客文字入口：未登录用户可进入偏好页，锁定只生成 1 道菜。
- [x] 调整游客语音入口：未登录用户可使用语音转文字，后续同样锁定只生成 1 道菜。
- [x] 后端游客推荐保持边界：有 `sourceText` 时先按基础菜单 catalog 强命中；弱匹配或无匹配时仅调用 LLM 生成 1 道试吃菜谱，不走 SSE、推荐记录、收藏或反馈链路。
- [x] 账号型能力继续要求登录：收藏、推荐反馈、购物清单、推荐历史、Pro 权益、订阅与购买。
- [ ] 上传新 binary，建议 `2.0.1 (4)`。
- [ ] App Review Notes / Resolution Center 说明：Text 与 Voice 已支持免注册试吃，游客只返回 1 道菜；强 catalog 命中走基础菜单，弱/无匹配走 1 道 LLM 试吃，账号型能力仍要求登录。

记录：2026-05-05 已按 App Review 要求扩展 guest 路径；2026-05-06 调整文字/语音游客入口匹配规则：强 catalog 命中直接返回基础菜单，弱匹配或无匹配时按用户实际输入意图生成 1 道 LLM 试吃菜谱。

## Gate 6：发布后观察

- [ ] App Store 状态进入 `Ready for Sale` 后，生产购买链路用真实环境小额验证。
- [ ] 观察 ECS 日志、推荐成功率、Apple receipt 校验错误、登录/邮箱验证码失败率。
- [ ] 观察 App Store 崩溃、TestFlight/用户反馈、订阅转化与退款异常。
- [ ] 根首页切换到 2.0 新版介绍页后，继续观察 Web、支持页、隐私页可访问性。
