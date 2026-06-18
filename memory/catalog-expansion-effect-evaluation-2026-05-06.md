# 基础菜单扩容一次性效果评估（2026-05-06）

## 定位

- 本文件是本次“基础菜单版本化扩容”技术方案的一次性离线评估口径。
- 不落库，不新增 API，不进入 admin console，不作为后续产品功能承诺。
- 当前仓库环境没有生产库只读连接和供应商真实单价，因此本文件先固定可复跑 SQL 与输出字段；真实数值需在生产只读窗口执行后填入。

## 评估窗口

- 时间范围：执行时刻向前 30 天。
- 基础库：执行时当前 `active=true` 的中文主菜单版本。
- 样本来源：`meal_recipes` 中 `catalog_item_id IS NULL` 的 LLM 新菜品。

## 样本 SQL

```sql
WITH active_dataset AS (
    SELECT version
    FROM meal_catalog_datasets
    WHERE active = TRUE
      AND version LIKE 'cn-home-menu-v%'
    ORDER BY imported_at DESC
    LIMIT 1
),
active_titles AS (
    SELECT lower(regexp_replace(name, '[[:space:][:punct:]]+', '', 'g')) AS normalized_title
    FROM meal_catalog_items
    WHERE dataset_version = (SELECT version FROM active_dataset)
      AND enabled = TRUE
),
samples AS (
    SELECT
        r.id,
        r.request_id,
        r.title,
        lower(regexp_replace(trim(r.title), '[[:space:][:punct:]]+', '', 'g')) AS normalized_title,
        r.source_text,
        r.provider,
        r.image_status,
        r.created_at
    FROM meal_recipes r
    WHERE r.created_at >= now() - interval '30 days'
      AND r.catalog_item_id IS NULL
      AND r.title IS NOT NULL
      AND trim(r.title) <> ''
      AND lower(coalesce(r.provider, '')) NOT LIKE 'catalog%'
),
deduped AS (
    SELECT s.*
    FROM samples s
    LEFT JOIN active_titles t ON t.normalized_title = s.normalized_title
    WHERE t.normalized_title IS NULL
)
SELECT
    normalized_title,
    min(title) AS candidate_title,
    count(*) AS recipe_count,
    count(DISTINCT request_id) AS request_count,
    array_agg(DISTINCT request_id) FILTER (WHERE request_id IS NOT NULL) AS request_ids,
    array_agg(DISTINCT source_text) FILTER (WHERE source_text IS NOT NULL) AS source_texts,
    array_agg(DISTINCT provider) FILTER (WHERE provider IS NOT NULL) AS providers,
    jsonb_object_agg(image_status, image_status_count) AS image_status_counts
FROM (
    SELECT
        d.*,
        count(*) OVER (PARTITION BY normalized_title, image_status) AS image_status_count
    FROM deduped d
) x
GROUP BY normalized_title
ORDER BY request_count DESC, recipe_count DESC
LIMIT 100;
```

## Deepseek 调用节省估算

- 预计减少 Deepseek 调用数 = 可被新增 catalog 命中的 request 数 x 平均每次推荐文本调用数。
- 若 `meal_recommendation_records.batch_metrics_json` 能推断调用轮次，优先用真实 `execution_rounds` 或 batch metrics 修正；否则默认按 `1` 次/请求保守估算。

```sql
SELECT
    count(DISTINCT r.request_id) AS candidate_request_count,
    coalesce(avg(nullif(rr.execution_rounds, 0)), 1) AS avg_text_calls_per_request,
    count(DISTINCT r.request_id) * coalesce(avg(nullif(rr.execution_rounds, 0)), 1) AS estimated_deepseek_calls_saved
FROM meal_recipes r
LEFT JOIN meal_recommendation_records rr ON rr.request_id = r.request_id
WHERE r.created_at >= now() - interval '30 days'
  AND r.catalog_item_id IS NULL
  AND r.title IS NOT NULL
  AND trim(r.title) <> ''
  AND lower(coalesce(r.provider, '')) NOT LIKE 'catalog%';
```

## Jimeng 调用节省估算

- 预计减少 Jimeng 调用数 = 候选菜品历史图片生成次数 - 已有图片资产命中次数。
- 若生产使用 `web-search` 或 disabled 图片 provider，本项只输出可复用图片资产缺口，不折算 Jimeng 成本。

```sql
WITH candidate_titles AS (
    SELECT DISTINCT lower(regexp_replace(trim(title), '[[:space:][:punct:]]+', '', 'g')) AS normalized_title,
           min(title) AS title
    FROM meal_recipes
    WHERE created_at >= now() - interval '30 days'
      AND catalog_item_id IS NULL
      AND title IS NOT NULL
      AND trim(title) <> ''
      AND lower(coalesce(provider, '')) NOT LIKE 'catalog%'
    GROUP BY lower(regexp_replace(trim(title), '[[:space:][:punct:]]+', '', 'g'))
)
SELECT
    c.title,
    count(r.id) FILTER (WHERE r.image_status IN ('GENERATED', 'FAILED', 'PENDING')) AS historical_image_attempts,
    count(a.id) AS reusable_asset_hits,
    greatest(count(r.id) FILTER (WHERE r.image_status IN ('GENERATED', 'FAILED', 'PENDING')) - count(a.id), 0) AS estimated_jimeng_calls_saved
FROM candidate_titles c
JOIN meal_recipes r ON lower(regexp_replace(trim(r.title), '[[:space:][:punct:]]+', '', 'g')) = c.normalized_title
LEFT JOIN meal_image_assets a ON lower(regexp_replace(trim(a.dish_name), '[[:space:][:punct:]]+', '', 'g')) = c.normalized_title
WHERE r.created_at >= now() - interval '30 days'
GROUP BY c.title
ORDER BY estimated_jimeng_calls_saved DESC, historical_image_attempts DESC
LIMIT 100;
```

## 输出表

| 指标 | 结果 |
|------|------|
| 评估窗口 | 过去 30 天 |
| 当前 active dataset | 待生产只读执行填入 |
| 可新增候选菜品数 | 待填 |
| 可被 catalog 命中的 request 数 | 待填 |
| 预计减少 Deepseek 调用数 | 待填 |
| 预计减少 Jimeng 调用数 | 待填 |
| Deepseek 单次推荐文本调用均价 | 待填单价 |
| Jimeng 单张图片生成均价 | 待填单价 |
| 预计月节省成本区间 | 待填单价后计算 |

## Top 候选贡献

| 候选菜名 | request 数 | 历史图片生成次数 | 已有图片资产命中 | 预计节省说明 |
|----------|------------|------------------|------------------|--------------|
| 待生产只读执行填入 | 待填 | 待填 | 待填 | 待填 |
