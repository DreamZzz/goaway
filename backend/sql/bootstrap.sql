-- What To Eat baseline schema
--
-- Maintenance rule:
-- - `bootstrap.sql` is the primary schema baseline for local/bootstrap environments.
-- - When a table structure, relation, column meaning, or migration strategy changes,
--   update the nearby comments in this file in the same change so the schema notes stay accurate.

-- users:
-- - Account and identity root table.
-- - Stores login credentials, profile fields, region, and failed-login counters.
-- - Referenced by authentication, profile, favorites, and generated recipe ownership.
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(255),
    bio VARCHAR(200),
    gender VARCHAR(20),
    birthday DATE,
    region VARCHAR(100),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- guest_profiles:
-- - Guest-mode profile table keyed by hashed installation id and linked 1:1 to an internal guest user row.
-- - Persists guest trial usage, first/last seen hashed IPs, and the current anti-abuse block window if needed.
-- - Only hashed installation/IP fingerprints are stored or logged; raw device identifiers must never be persisted.
CREATE TABLE IF NOT EXISTS guest_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    installation_hash VARCHAR(64) NOT NULL UNIQUE,
    trial_max_count INTEGER NOT NULL DEFAULT 3,
    trial_used_count INTEGER NOT NULL DEFAULT 0,
    first_seen_ip_hash VARCHAR(64),
    last_seen_ip_hash VARCHAR(64),
    last_auth_at TIMESTAMP,
    last_seen_at TIMESTAMP,
    blocked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- meal_catalog_datasets:
-- - Version registry for seeded base menu datasets.
-- - One row represents one imported menu source, such as `cn-home-menu-v1`.
-- - Used to support idempotent imports and online dataset migrations/cutovers.
-- - Chinese base-menu expansion creates immutable `cn-home-menu-vN` snapshots; rollback
--   is implemented by switching the active flag among these versions, not by deleting rows.
CREATE TABLE IF NOT EXISTS meal_catalog_datasets (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    source_file VARCHAR(200) NOT NULL,
    source_checksum VARCHAR(64) NOT NULL,
    total_items INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Compatibility backfills for databases created before `source_checksum` and `active`
-- existed on `meal_catalog_datasets`.
ALTER TABLE meal_catalog_datasets
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(64);
ALTER TABLE meal_catalog_datasets
    ADD COLUMN IF NOT EXISTS active BOOLEAN;
ALTER TABLE meal_catalog_datasets
    ALTER COLUMN source_file TYPE VARCHAR(200);
ALTER TABLE meal_catalog_datasets
    ALTER COLUMN title TYPE VARCHAR(200);

UPDATE meal_catalog_datasets
SET source_checksum = COALESCE(
        source_checksum,
        md5(CONCAT(version, ':', source_file, ':', total_items))
)
WHERE source_checksum IS NULL;

UPDATE meal_catalog_datasets
SET active = COALESCE(active, TRUE)
WHERE active IS NULL;

ALTER TABLE meal_catalog_datasets
    ALTER COLUMN source_checksum SET NOT NULL;
ALTER TABLE meal_catalog_datasets
    ALTER COLUMN active SET NOT NULL;
ALTER TABLE meal_catalog_datasets
    ALTER COLUMN active SET DEFAULT TRUE;

-- meal_catalog_tags:
-- - Shared tag dictionary for the base menu.
-- - Holds reusable labels across multiple dimensions:
--   CATEGORY / SUBCATEGORY / COOKING_METHOD / FLAVOR / FEATURE / INGREDIENT.
CREATE TABLE IF NOT EXISTS meal_catalog_tags (
    id BIGSERIAL PRIMARY KEY,
    tag_type VARCHAR(40) NOT NULL,
    tag_key VARCHAR(120) NOT NULL,
    tag_label VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_meal_catalog_tag_type_key UNIQUE (tag_type, tag_key)
);

-- meal_catalog_items:
-- - Canonical base-menu dish table. One row equals one source dish.
-- - Stores dish naming, classification, cooking method, flavor-tag summary, source order,
--   and the dataset version it belongs to.
-- - "来点灵感" and future precise recommendation logic draw candidates from here.
CREATE TABLE IF NOT EXISTS meal_catalog_items (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES meal_catalog_datasets(id) ON DELETE CASCADE,
    dataset_version VARCHAR(80) NOT NULL,
    source_index INTEGER NOT NULL,
    code VARCHAR(80) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    subcategory VARCHAR(80) NOT NULL,
    cooking_method VARCHAR(80) NOT NULL,
    raw_flavor_text VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_meal_catalog_item_dataset_code UNIQUE (dataset_id, code)
);

-- Compatibility backfills for databases created before `dataset_version` and `slug`
-- existed on `meal_catalog_items`.
ALTER TABLE meal_catalog_items
    ADD COLUMN IF NOT EXISTS dataset_version VARCHAR(80);
ALTER TABLE meal_catalog_items
    ADD COLUMN IF NOT EXISTS slug VARCHAR(120);
ALTER TABLE meal_catalog_items
    ALTER COLUMN code TYPE VARCHAR(80);
ALTER TABLE meal_catalog_items
    ALTER COLUMN name TYPE VARCHAR(120);

UPDATE meal_catalog_items item
SET dataset_version = dataset.version
FROM meal_catalog_datasets dataset
WHERE item.dataset_id = dataset.id
  AND item.dataset_version IS NULL;

UPDATE meal_catalog_items
SET slug = COALESCE(code, 'catalog-' || LPAD(source_index::text, 3, '0'))
WHERE slug IS NULL;

ALTER TABLE meal_catalog_items
    ALTER COLUMN dataset_version SET NOT NULL;
ALTER TABLE meal_catalog_items
    ALTER COLUMN slug SET NOT NULL;

-- meal_catalog_item_tags:
-- - Many-to-many relation between base-menu dishes and reusable tags.
-- - This is the core association table for recommendation features, including ingredient tags.
CREATE TABLE IF NOT EXISTS meal_catalog_item_tags (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES meal_catalog_items(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES meal_catalog_tags(id) ON DELETE CASCADE,
    CONSTRAINT uk_meal_catalog_item_tag UNIQUE (item_id, tag_id)
);

-- meal_image_assets:
-- - Dish-image cache table keyed by normalized dish name.
-- - Stores the original公网图片地址 and the persisted local/OSS image URL so repeated
--   generations can reuse an existing dish image instead of searching again.
-- - This is the persistence bridge for the async recipe-image flow:
--   `POST /api/meals/recipes/{id}/image` -> search/download -> local or OSS storage -> cache hit reuse.
CREATE TABLE IF NOT EXISTS meal_image_assets (
    id BIGSERIAL PRIMARY KEY,
    dish_name VARCHAR(200) NOT NULL,
    normalized_dish_name VARCHAR(200) NOT NULL,
    source_image_url VARCHAR(1000) NOT NULL,
    source_page_url VARCHAR(1000),
    storage_key VARCHAR(255) NOT NULL,
    public_image_url VARCHAR(1000) NOT NULL,
    source_provider VARCHAR(80) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_meal_image_asset_dish_source UNIQUE (normalized_dish_name, source_image_url)
);

-- meal_recipes:
-- - Generated recipe/result table for actual user requests.
-- - Stores the request context, model/provider output, generated steps/ingredients JSON,
--   optional image info, and the user's preference (`LIKE` / `DISLIKE`).
-- - `catalog_item_id` links a generated result back to the base-menu candidate that inspired it.
-- - `image_status` and `steps_status` support the two-phase UX:
--   first return recipe cards quickly, then lazily补图 / 补做法.
CREATE TABLE IF NOT EXISTS meal_recipes (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    catalog_item_id BIGINT,
    catalog_item_code VARCHAR(80),
    source_text VARCHAR(1000) NOT NULL,
    source_mode VARCHAR(20) NOT NULL,
    dish_count INTEGER NOT NULL,
    total_calories INTEGER,
    staple VARCHAR(40),
    locale VARCHAR(20),
    provider VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(1000),
    estimated_calories INTEGER,
    ingredients_json TEXT,
    seasonings_json TEXT,
    steps_json TEXT,
    image_url VARCHAR(500),
    image_status VARCHAR(20) NOT NULL DEFAULT 'OMITTED',
    preference VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Video generation columns (added after initial schema)
ALTER TABLE meal_recipes ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);
ALTER TABLE meal_recipes ADD COLUMN IF NOT EXISTS video_status VARCHAR(20) NOT NULL DEFAULT 'OMITTED';

-- Compatibility backfills for earlier recipe rows that were created before
-- the base-menu linkage columns were introduced.
ALTER TABLE meal_recipes
    ADD COLUMN IF NOT EXISTS catalog_item_id BIGINT;
ALTER TABLE meal_recipes
    ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);
ALTER TABLE meal_recipes
    DROP COLUMN IF EXISTS flavor;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_meal_recipes_catalog_item'
          AND table_name = 'meal_recipes'
    ) THEN
        ALTER TABLE meal_recipes
            ADD CONSTRAINT fk_meal_recipes_catalog_item
            FOREIGN KEY (catalog_item_id) REFERENCES meal_catalog_items(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_meal_catalog_items_dataset_id ON meal_catalog_items(dataset_id);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_items_dataset_version ON meal_catalog_items(dataset_version);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_items_source_index ON meal_catalog_items(source_index);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_item_tags_item_id ON meal_catalog_item_tags(item_id);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_item_tags_tag_id ON meal_catalog_item_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_meal_image_assets_normalized_dish_name ON meal_image_assets(normalized_dish_name);
CREATE INDEX IF NOT EXISTS idx_guest_profiles_user_id ON guest_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_recipes_user_id ON meal_recipes(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_recipes_catalog_item_id ON meal_recipes(catalog_item_id);
CREATE INDEX IF NOT EXISTS idx_meal_recipes_request_id ON meal_recipes(request_id);
CREATE INDEX IF NOT EXISTS idx_meal_recipes_preference ON meal_recipes(preference);
CREATE INDEX IF NOT EXISTS idx_meal_recipes_updated_at ON meal_recipes(updated_at DESC);

-- user_subscriptions:
-- - One row per user. Tracks subscription status (ACTIVE / EXPIRED / NONE) and consumable credit balance.
-- - status ACTIVE means an auto-renewable subscription is in effect; expiresAt is the end of the paid period.
-- - credit_balance counts remaining one-off generation credits purchased via consumable IAP.
-- - purchased_at records when the latest subscription period was purchased or renewed (IAP or admin grant).
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    product_id               VARCHAR(120),
    original_transaction_id  VARCHAR(100),
    latest_transaction_id    VARCHAR(100),
    status                   VARCHAR(20) NOT NULL DEFAULT 'NONE',
    expires_at               TIMESTAMP,
    purchased_at             TIMESTAMP,
    credit_balance           INTEGER NOT NULL DEFAULT 0,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS purchased_at TIMESTAMP;

-- user_daily_quotas:
-- - Tracks how many meal-generation requests a user has made per calendar day.
-- - Used to enforce the free daily quota (configurable via app.meal.free-daily-quota).
-- - One row per (user_id, quota_date); used_count is incremented atomically on each generation.
CREATE TABLE IF NOT EXISTS user_daily_quotas (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quota_date  DATE NOT NULL,
    used_count  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_daily_quota UNIQUE (user_id, quota_date)
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_daily_quotas_user_date ON user_daily_quotas(user_id, quota_date);

-- subscription_periods:
-- - One row per subscription billing period (initial purchase or renewal).
-- - Immutable audit trail: records when each period started and ended.
-- - Inserted whenever verifyAppleReceipt processes a new subscription transaction,
--   or when admin grants membership.
CREATE TABLE IF NOT EXISTS subscription_periods (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id               VARCHAR(120),
    original_transaction_id  VARCHAR(100),
    transaction_id           VARCHAR(100) NOT NULL,
    period_start             TIMESTAMP NOT NULL,
    period_end               TIMESTAMP NOT NULL,
    source                   VARCHAR(40) NOT NULL,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscription_periods_user_id ON subscription_periods(user_id);
CREATE INDEX IF NOT EXISTS idx_subscription_periods_created_at ON subscription_periods(created_at DESC);

-- meal_recommendation_records:
-- - One row per recommendation requestId, used for recommendation-quality review and operational tracing.
-- - Stores request shape, provider/cache metadata, prompt and batch snapshots, result payloads,
--   final status, and the user's satisfaction feedback when available.
-- - This table is populated asynchronously via RabbitMQ consumers; if MQ is unavailable the
--   recommendation flow degrades to log-only and the table may temporarily miss records.
CREATE TABLE IF NOT EXISTS meal_recommendation_records (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_text VARCHAR(1000) NOT NULL,
    source_mode VARCHAR(20) NOT NULL,
    dish_count INTEGER NOT NULL,
    total_calories INTEGER,
    staple VARCHAR(40),
    locale VARCHAR(20),
    provider VARCHAR(80),
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    feedback_status VARCHAR(20),
    feedback_at TIMESTAMP,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    execution_rounds INTEGER NOT NULL DEFAULT 0,
    request_payload_json TEXT,
    prompt_json TEXT,
    batch_metrics_json TEXT,
    result_payload_json TEXT,
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_meal_recommendation_records_user_id ON meal_recommendation_records(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_recommendation_records_status ON meal_recommendation_records(status);
CREATE INDEX IF NOT EXISTS idx_meal_recommendation_records_feedback_status ON meal_recommendation_records(feedback_status);
CREATE INDEX IF NOT EXISTS idx_meal_recommendation_records_cache_hit ON meal_recommendation_records(cache_hit);
CREATE INDEX IF NOT EXISTS idx_meal_recommendation_records_created_at ON meal_recommendation_records(created_at DESC);

-- meal_catalog_expansion_runs:
-- - Manual admin-console analysis jobs that scan recent LLM-generated recipes missing
--   the active base catalog and group them into expansion candidates.
CREATE TABLE IF NOT EXISTS meal_catalog_expansion_runs (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    base_dataset_version VARCHAR(80) NOT NULL,
    sample_since TIMESTAMP NOT NULL,
    sample_until TIMESTAMP NOT NULL,
    sample_limit INTEGER NOT NULL,
    sample_count INTEGER NOT NULL DEFAULT 0,
    candidate_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- meal_catalog_expansion_candidates:
-- - Reviewable candidate dishes produced from expansion runs.
-- - Approved candidates are ingested by copying the current active dataset into a new
--   `cn-home-menu-vN` snapshot and appending the selected candidates there.
CREATE TABLE IF NOT EXISTS meal_catalog_expansion_candidates (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT REFERENCES meal_catalog_expansion_runs(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    dish_name VARCHAR(200) NOT NULL,
    normalized_dish_name VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    subcategory VARCHAR(80) NOT NULL,
    cooking_method VARCHAR(80) NOT NULL,
    raw_flavor_text VARCHAR(200) NOT NULL,
    cuisine VARCHAR(20) NOT NULL DEFAULT 'CHINESE',
    flavor_tags_json TEXT,
    feature_tags_json TEXT,
    ingredient_tags_json TEXT,
    confidence DOUBLE PRECISION,
    reason VARCHAR(1000),
    sample_count INTEGER NOT NULL DEFAULT 0,
    source_recipe_ids_json TEXT,
    source_request_ids_json TEXT,
    source_texts_json TEXT,
    base_dataset_version VARCHAR(80) NOT NULL,
    ingested_dataset_version VARCHAR(80),
    ingested_catalog_item_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_meal_catalog_expansion_candidate_base_name UNIQUE (base_dataset_version, normalized_dish_name)
);

CREATE INDEX IF NOT EXISTS idx_meal_catalog_expansion_runs_created_at ON meal_catalog_expansion_runs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_expansion_candidates_status ON meal_catalog_expansion_candidates(status);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_expansion_candidates_base ON meal_catalog_expansion_candidates(base_dataset_version);
CREATE INDEX IF NOT EXISTS idx_meal_catalog_expansion_candidates_sample_count ON meal_catalog_expansion_candidates(sample_count DESC);

-- inventory_scans:
-- - Pro account feature for fridge/pantry photo recognition.
-- - Stores the uploaded compressed image reference, provider/model metadata, raw recognition
--   response, status, and a short image retention deadline. After cleanup, image_url/storage_key
--   may be nulled while keeping recognition history and confirmed inventory intact.
CREATE TABLE IF NOT EXISTS inventory_scans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(1000),
    storage_key VARCHAR(500),
    content_type VARCHAR(100),
    image_size_bytes BIGINT,
    provider VARCHAR(80),
    model VARCHAR(128),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    raw_response_json TEXT,
    error_message TEXT,
    image_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- inventory_scan_items:
-- - AI-generated candidate items from a scan; users must review/correct these before saving.
-- - quantity_level is intentionally coarse: LOW/MEDIUM/HIGH/UNKNOWN.
CREATE TABLE IF NOT EXISTS inventory_scan_items (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT NOT NULL REFERENCES inventory_scans(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    category VARCHAR(80),
    quantity_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    amount_text VARCHAR(120),
    confidence DOUBLE PRECISION,
    location_hint VARCHAR(120),
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- inventory_items:
-- - User-confirmed current pantry/fridge inventory.
-- - Same normalized ingredient name is upserted per user; scans do not delete old inventory.
-- - expires_at is a user-editable coarse freshness date. Expired items stay visible until
--   the user clears them, but they are excluded from recommendation context and treated as
--   unavailable in shopping-list inventory matching.
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    category VARCHAR(80),
    quantity_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    amount_text VARCHAR(120),
    source_scan_id BIGINT REFERENCES inventory_scans(id) ON DELETE SET NULL,
    last_seen_at TIMESTAMP,
    expires_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_item_user_normalized_name UNIQUE (user_id, normalized_name)
);

-- inventory_recipe_consumptions:
-- - Idempotency ledger for "cooked this recipe" inventory deductions.
-- - client_action_id is generated by the client for one confirm action; repeating it must not
--   decrement inventory twice.
CREATE TABLE IF NOT EXISTS inventory_recipe_consumptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipe_id BIGINT NOT NULL REFERENCES meal_recipes(id) ON DELETE CASCADE,
    client_action_id VARCHAR(120) NOT NULL,
    details_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_recipe_consumption_user_action UNIQUE (user_id, client_action_id)
);

ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS expires_at DATE;

CREATE INDEX IF NOT EXISTS idx_inventory_scans_user_created ON inventory_scans(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_scans_image_expires ON inventory_scans(image_expires_at);
CREATE INDEX IF NOT EXISTS idx_inventory_scan_items_scan_position ON inventory_scan_items(scan_id, position);
CREATE INDEX IF NOT EXISTS idx_inventory_items_user_updated ON inventory_items(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_items_user_expires ON inventory_items(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_inventory_recipe_consumptions_user_created ON inventory_recipe_consumptions(user_id, created_at DESC);

-- Migrate meal_recipes_id_seq from BIGSERIAL (INCREMENT BY 1) to Hibernate SEQUENCE strategy
-- (INCREMENT BY 50) to enable batch INSERT support.
-- Idempotent: skips if the sequence already has INCREMENT BY 50.
DO $$
DECLARE
    current_increment BIGINT;
    max_id            BIGINT;
    new_start         BIGINT;
BEGIN
    SELECT increment_by INTO current_increment
    FROM pg_sequences
    WHERE sequencename = 'meal_recipes_id_seq';

    IF current_increment IS DISTINCT FROM 50 THEN
        SELECT COALESCE(MAX(id), 0) INTO max_id FROM meal_recipes;
        -- Hibernate's pooled optimizer treats nextval() as the HIGH end of the range:
        -- it allocates IDs [nextval - allocationSize + 1, nextval].
        -- Setting new_start = max_id + allocationSize guarantees the range
        -- starts at max_id + 1, safely after all existing rows.
        new_start := max_id + 50;
        ALTER SEQUENCE meal_recipes_id_seq INCREMENT BY 50;
        PERFORM setval('meal_recipes_id_seq', new_start, false);
        RAISE NOTICE 'meal_recipes_id_seq migrated: INCREMENT=50, next_val=%', new_start;
    ELSE
        RAISE NOTICE 'meal_recipes_id_seq already at INCREMENT=50, skipping.';
    END IF;
END $$;
