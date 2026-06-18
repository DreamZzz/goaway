# Backend Architecture

## Package Rules

- `com.quickstart.template.contexts`
  - Business contexts only.
  - Each context is organized as `api`, `application`, `domain`, `infrastructure`.
- `com.quickstart.template.platform`
  - Cross-cutting technical capabilities such as security, config, and external provider abstractions.
- `com.quickstart.template.shared`
  - Context-agnostic response models and shared helpers.

## Current Contexts

- `account`
  - Owns the `User` aggregate, authentication workflows, and profile management.
- `meal`
  - Core business context. Owns meal catalog, LLM-based recipe generation, SSE recipe delivery, async image enrichment, lazy step generation, favorites, shopping list extraction, and recommendation history.
- `media`
  - Owns file storage, upload APIs, and media storage/compression policies used by voice upload and recipe image persistence.
- `inventory`
  - Owns Pro-only fridge/pantry photo scans, AI-recognized ingredient candidates, user confirmation, and the current per-user ingredient inventory. It depends on `media` for image storage and `subscription` for Pro entitlement checks, but does not feed the meal recommendation path in v1.
- `subscription`
  - Owns `UserSubscription` (status: ACTIVE/EXPIRED/NONE), `QuotaService` (daily usage tracking with pessimistic locking), and `SubscriptionStatusDTO`. Enforces per-tier limits: free users get 3 dishes/request and 5 requests/day; premium (ACTIVE) users get 5 dishes and 15 requests. `MealController` delegates dish-count and quota checks to `QuotaService` before processing any recommendation request.

## Dependency Direction

- `api` -> `application`
- `application` -> `domain` and `infrastructure`
- `domain` must not depend on `api`
- `platform` can be used by any context for technical concerns, but business rules stay inside `contexts/*`

## API 向后兼容规则（App 已上线）

App 已在应用市场发布，线上存在多个客户端版本同时运行。所有后端改动必须遵守：

| 操作 | 规则 |
|---|---|
| DTO 字段 | 只增不删；新增字段必须有默认值，旧客户端不传时不能抛错 |
| 枚举值 | 只增不删不改名；前端对未知枚举值应能降级 |
| API 路径 | 已上线路径不可变更；需重构时保留旧路径代理到新实现 |
| 接口行为 | 修改已有接口逻辑前确认旧版客户端在新行为下不崩溃 |
| DB Schema | `ddl-auto=validate`，只能手动迁移；新增列必须 nullable 或有默认值，不可删除/重命名已有列 |

任何违反上述规则的改动视为 **breaking change**，必须在动手前与用户确认。

## Meal Context — Key Services

### 推荐路径记录（S1）
- **`PathLogService`** (`contexts/meal/application/`) — fire-and-forget 异步写入，每次推荐结束后从 `RecommendationTiming.annotations` 读 `path`、`ingredientKeyword`、`matchGap` 并写入 `recommendation_path_log` 表
- **`RecommendationPathLog`** (`contexts/meal/domain/`) — 记录 requestId、userId、sourceText、path（A_cache/B_broad/C_explicit/E_llm/F_catalog/F_hybrid 等）、食材关键词、matchGap
- **`RecommendationPathLogRepository`** — `JpaRepository<RecommendationPathLog, Long>`

### 食材分类树（S2-S3）
- **`MealCatalogTag`** 新增三列：`parent_tag_id BIGINT`、`tag_level SMALLINT DEFAULT 0`、`tag_source VARCHAR(20) DEFAULT 'MANUAL'`（随 S2 迁移脚本上线）
- **`TaxonomySeeder`** (`contexts/meal/application/`) — 应用启动时一次性初始化，调用 LLM（`LlmScene.TAXONOMY`，模型 deepseek-v4-pro，超时 120s）将现有 INGREDIENT 标签组织为三层分类树并以 `tagSource='TAXONOMY'` 写回 `meal_catalog_tags`；若已有 TAXONOMY 记录则跳过
- **`HierarchicalIngredientResolver`** (`contexts/meal/application/`) — 从 `tagSource='TAXONOMY'` 数据构建内存树，`resolve(sourceText)` 从叶到根搜索最佳匹配食材关键词，返回 `ResolutionResult(matchedKeyword, matchGap)`；`matchGap=0` 表示叶节点精确匹配，越大越泛化；`invalidateCache()` 使内存树失效
- **特性开关** `app.catalog.tag-resolver`：`legacy`（默认）= 旧 flat 解析路径；`hierarchical` = HierarchicalIngredientResolver

### LLM 场景
- `LlmScene.TAXONOMY` — 分类树生成专用场景，seed 默认配置：model=deepseek-v4-pro, timeout=120s，实际配置以 DB `llm_scene_config` 为准

### DB Schema 补充（S1-S3 手动迁移）
```sql
-- S1
ALTER TABLE meal_catalog_tags
  ADD COLUMN IF NOT EXISTS parent_tag_id BIGINT,
  ADD COLUMN IF NOT EXISTS tag_level SMALLINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tag_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- S2（同次迁移）
CREATE TABLE recommendation_path_log (
  id BIGSERIAL PRIMARY KEY,
  request_id VARCHAR(64) NOT NULL,
  user_id BIGINT,
  source_text TEXT NOT NULL,
  path VARCHAR(30) NOT NULL,
  ingredient_kw TEXT,
  match_gap SMALLINT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rpl_path_created ON recommendation_path_log(path, created_at);
CREATE INDEX idx_rpl_source_created ON recommendation_path_log(source_text, created_at);

-- llm_scene_config check constraint 需扩展以包含 TAXONOMY
ALTER TABLE llm_scene_config DROP CONSTRAINT llm_scene_config_scene_check;
ALTER TABLE llm_scene_config ADD CONSTRAINT llm_scene_config_scene_check
  CHECK (scene = ANY (ARRAY['RECOMMEND','STEPS','INTENT_REVIEW','TAXONOMY']));
```

## Notes

- `User` is no longer split across `identity` and `profile`. The aggregate now lives in `account`, and auth/profile are separate entrypoints over the same domain.
- Current public API remains on unversioned paths such as `/api/auth/**`, `/api/users/**`, `/api/meals/**`, `/api/voice/**`.
