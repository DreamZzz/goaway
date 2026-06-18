# Recommendation Regression Baseline

本文档记录推荐核心链路上线前回归测试基线。目标是把线上已有入口形态固化为可重复执行的测试资产，避免后续优化食材意图、catalog 匹配、preload 或 LLM prompt 时破坏既有场景。

## 覆盖范围

| 场景 | 覆盖测试 | 说明 |
| --- | --- | --- |
| 文字输入推荐 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverTextAndVoiceInputs(TEXT)` | 走 `recommendRecipes` fresh LLM path，确认 `sourceMode=TEXT`、结果保存和响应形态稳定 |
| 语音输入推荐 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverTextAndVoiceInputs(VOICE)` | 语音转写后的推荐入口仍走同一推荐链路，确认 `sourceMode=VOICE` 不被改写 |
| 1-5 道菜推荐 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverDishCountsOneToFive` | 覆盖普通用户/Pro 可能出现的 1 到 5 道菜响应数量，不允许链路误过滤 |
| 缓存命中 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverCacheHitPath` | 完整缓存批次直接返回 `provider=database`，不调用 LLM |
| LLM 生成 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverTextAndVoiceInputs` / `ShouldCoverDishCountsOneToFive` | cache miss 后调用 provider 生成并保存 |
| 单关键词食材意图 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverSingleIngredientKeywordIntent` | 例如 `猪蹄`，应识别为 ingredient intent，保留 core ingredient，且多菜食材请求跳过缓存 |
| 多关键词意图 | `MealRecommendationCoreRegressionTest.recommendRecipes_ShouldCoverMultiKeywordIntentProfile` | 例如 `鸡蛋，番茄，土豆，想吃热乎的，别太复杂`，保留全部食材与偏好 |
| 游客来点灵感：随机 | `MealRecommendationCoreRegressionTest.recommendGuestInspirations_ShouldCoverGuestRecommendationPaths('', TEXT)` | 空输入时返回基础菜单随机 3 道 |
| 游客来点灵感：强关键词 | `MealRecommendationCoreRegressionTest.recommendGuestInspirations_ShouldCoverGuestRecommendationPaths(虾, TEXT)` | 强 catalog 命中直接返回基础菜单，不调用 LLM |
| 游客来点灵感：弱关键词 | `MealRecommendationCoreRegressionTest.recommendGuestInspirations_ShouldCoverGuestRecommendationPaths(龙虾, TEXT/VOICE)` | 弱匹配或无匹配走游客 1 道 LLM 试吃 |
| 首页 6 个灵感场景 | `frontend/__tests__/features/meal/catalog.test.js` 的 `meal catalog mood regression baseline` | 覆盖 `没想法 / 想吃辣 / 清淡点 / 高蛋白 / 一人食 / 宴客菜` 6 个 mood 选菜逻辑 |

## 推荐执行命令

上线前最小推荐回归：

```bash
cd backend && mvn test -Dtest=MealRecommendationCoreRegressionTest
cd frontend && npm test -- __tests__/features/meal/catalog.test.js --runInBand
```

推荐链路相关完整回归：

```bash
cd backend && mvn test -Dtest="MealRecommendationCoreRegressionTest,MealIntentServiceTest,MealRecommendationQualityGateTest,OpenAiCompatibleMealGenerationProviderTest,MealServiceTest,MealControllerTest"
cd frontend && npm test -- __tests__/features/meal/catalog.test.js __tests__/screens/HomeScreen.test.js __tests__/screens/MealResultsScreen.test.js --runInBand
```

发布前全量基线仍以仓库默认命令为准：

```bash
cd backend && mvn test
cd backend && mvn package -Dmaven.test.skip=true
cd frontend && npm run lint
cd frontend && npm test -- --runInBand
```

## 后续扩展方向

- 每次新增推荐入口、mood 场景、quota 分支或 provider fallback，都应先补本基线中的测试矩阵。
- 生产 smoke 可以继续覆盖真实 API 与账号状态；本基线侧重本地可重复、无外部依赖的行为回归。
- 如果后续把库存接入推荐主链路，应在本文件新增“库存输入推荐”分组，单独覆盖库存强命中、库存弱命中、库存为空和 Pro 权限分支。
