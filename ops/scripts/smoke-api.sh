#!/usr/bin/env bash

set -euo pipefail

# Auto-load local credentials file if present (gitignored, never committed).
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/.smoke.env" ]; then
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.smoke.env"
fi

BASE_URL="${1:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL%/}"

# Auth: provide SMOKE_AUTH_TOKEN (preferred) OR both SMOKE_AUTH_USERNAME + SMOKE_AUTH_PASSWORD.
# No demo defaults — callers must inject credentials explicitly.
SMOKE_AUTH_TOKEN="${SMOKE_AUTH_TOKEN:-}"
SMOKE_AUTH_USERNAME="${SMOKE_AUTH_USERNAME:-}"
SMOKE_AUTH_PASSWORD="${SMOKE_AUTH_PASSWORD:-}"

# Optional: path to an audio file for voice transcription smoke.
SMOKE_VOICE_FILE="${SMOKE_VOICE_FILE:-}"
SMOKE_VOICE_LOCALE="${SMOKE_VOICE_LOCALE:-zh-CN}"

log_phase() {
  echo "[smoke-api][$1] $2"
}

# skip_phase: print a skip notice and continue (not a failure).
skip_phase() {
  echo "[smoke-api][skip] $1"
}

json_get() {
  local path="$1"
  node -e '
const fs = require("fs");
const path = process.argv[1];
const input = fs.readFileSync(0, "utf8");
const segments = path.split(".");
try {
  let value = JSON.parse(input);
  for (const segment of segments) {
    if (value == null) process.exit(2);
    if (/^\d+$/.test(segment)) {
      value = value[Number(segment)];
    } else {
      value = value[segment];
    }
  }
  if (value === undefined || value === null) process.exit(2);
  process.stdout.write(typeof value === "string" ? value : JSON.stringify(value));
} catch {
  process.exit(1);
}
' "$path"
}

json_assert() {
  local path="$1"
  local expected="$2"
  local actual
  actual="$(json_get "$path")"
  if [ "$actual" != "$expected" ]; then
    echo "[smoke-api] assertion failed for $path: expected '$expected', got '$actual'"
    exit 1
  fi
}

api_get() {
  local path="$1"
  shift || true
  curl -fsS "$BASE_URL$path" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    "$@"
}

api_post_json() {
  local path="$1"
  local body="$2"
  curl -fsS -X POST "$BASE_URL$path" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "$body"
}

api_put_json() {
  local path="$1"
  local body="$2"
  curl -fsS -X PUT "$BASE_URL$path" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "$body"
}

login_and_get_token() {
  if [ -z "$SMOKE_AUTH_USERNAME" ] || [ -z "$SMOKE_AUTH_PASSWORD" ]; then
    echo "[smoke-api] credentials required: set SMOKE_AUTH_TOKEN, or both SMOKE_AUTH_USERNAME and SMOKE_AUTH_PASSWORD"
    exit 1
  fi

  local response token
  response="$(curl -fsS -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$(printf '{"username":"%s","password":"%s"}' "$SMOKE_AUTH_USERNAME" "$SMOKE_AUTH_PASSWORD")")"
  token="$(printf '%s' "$response" | json_get token)"
  if [ -z "$token" ]; then
    echo "[smoke-api] login succeeded but token is empty"
    exit 1
  fi
  printf '%s' "$token"
}

require_token() {
  if [ -n "$SMOKE_AUTH_TOKEN" ]; then
    printf '%s' "$SMOKE_AUTH_TOKEN"
    return 0
  fi
  login_and_get_token
}

assert_stream_contains() {
  local output="$1"
  local needle="$2"
  local label="$3"
  if ! printf '%s' "$output" | grep -q "$needle"; then
    echo "[smoke-api] missing $label in stream output"
    exit 1
  fi
}

AUTH_TOKEN="$(require_token)"

# ── auth ──────────────────────────────────────────────────────────────────────
log_phase "auth" "checking captcha and authenticated login"
curl -fsS "$BASE_URL/api/auth/captcha" >/dev/null

# ── intent ────────────────────────────────────────────────────────────────────
log_phase "intent" "verifying meal intent clarification/proceed path"
intent_response="$(api_post_json "/api/meals/intent" '{
  "sourceText": "辣",
  "locale": "zh-CN"
}')"
printf '%s' "$intent_response" | json_get decision >/dev/null

# ── speech-rules ──────────────────────────────────────────────────────────────
log_phase "speech-rules" "verifying OTA speech rules endpoint"
speech_rules_response="$(api_get "/api/meals/speech-rules")"
printf '%s' "$speech_rules_response" | json_get version >/dev/null
printf '%s' "$speech_rules_response" | json_get rules >/dev/null

# ── recommend-sync ────────────────────────────────────────────────────────────
log_phase "recommend-sync" "requesting synchronous meal recommendation"
recommendation_response="$(api_post_json "/api/meals/recommendations" '{
  "sourceText": "鸡胸肉、西兰花、米饭",
  "sourceMode": "TEXT",
  "dishCount": 2,
  "totalCalories": 900,
  "staple": "RICE",
  "locale": "zh-CN"
}')"

recipe_id="$(printf '%s' "$recommendation_response" | json_get items.0.id)"
request_id="$(printf '%s' "$recommendation_response" | json_get requestId)"
printf '%s' "$recommendation_response" | json_get items.0.title >/dev/null

# ── recommend-feedback ────────────────────────────────────────────────────────
log_phase "recommend-feedback" "submitting thumbs-up feedback for requestId $request_id"
feedback_response="$(api_post_json "/api/meals/recommendations/$request_id/feedback" '{"feedbackStatus":"SATISFIED"}')"
printf '%s' "$feedback_response" | json_get feedbackStatus >/dev/null

# ── recommend-stream ──────────────────────────────────────────────────────────
log_phase "recommend-stream" "verifying streaming summary / recipe / done events"
stream_output="$(curl -fsS -N -X POST "$BASE_URL/api/meals/recommendations/stream" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceText": "家常菜",
    "sourceMode": "TEXT",
    "dishCount": 1,
    "totalCalories": 800,
    "staple": "RICE",
    "locale": "zh-CN"
  }')"
assert_stream_contains "$stream_output" 'event:summary' 'summary event'
assert_stream_contains "$stream_output" 'event:recipe' 'recipe event'
assert_stream_contains "$stream_output" 'event:done' 'done event'

# ── recipe-detail ─────────────────────────────────────────────────────────────
log_phase "recipe-detail" "verifying recipe detail endpoint"
recipe_detail_response="$(api_get "/api/meals/recipes/$recipe_id")"
printf '%s' "$recipe_detail_response" | json_get id >/dev/null

# ── recipe-image ──────────────────────────────────────────────────────────────
log_phase "recipe-image" "triggering image enrichment"
image_response="$(api_post_json "/api/meals/recipes/$recipe_id/image" '{}')"
printf '%s' "$image_response" | json_get recipeId >/dev/null

# ── recipe-steps ──────────────────────────────────────────────────────────────
log_phase "recipe-steps" "verifying steps stream emits token/step or cached step and done"
steps_output="$(curl -fsS -N -X POST "$BASE_URL/api/meals/recipes/$recipe_id/steps/stream?locale=zh-CN" \
  -H "Authorization: Bearer $AUTH_TOKEN")"
if ! printf '%s' "$steps_output" | grep -q 'event:token' && ! printf '%s' "$steps_output" | grep -q 'event:step'; then
  echo "[smoke-api] missing token/step event in recipe steps stream"
  exit 1
fi
assert_stream_contains "$steps_output" 'event:done' 'steps done event'

# ── favorites ─────────────────────────────────────────────────────────────────
log_phase "favorites" "liking a recipe and reading it back from favorites"
preference_response="$(api_put_json "/api/meals/recipes/$recipe_id/preference" '{"preference":"LIKE"}')"
printf '%s' "$preference_response" | json_assert preference LIKE
favorites_response="$(api_get "/api/meals/favorites?page=0&size=10")"
printf '%s' "$favorites_response" | json_assert retrieval.scene favorites
printf '%s' "$favorites_response" | json_get items.0.id >/dev/null

# ── history ───────────────────────────────────────────────────────────────────
# History is subscription-only; 402 means the gate is working, 200 means subscribed account.
log_phase "history" "reading recommendation history list (200=ok, 402=gate ok)"
history_status="$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/api/meals/history?page=0&size=5" \
  -H "Authorization: Bearer $AUTH_TOKEN")"
if [ "$history_status" != "200" ] && [ "$history_status" != "402" ]; then
  echo "[smoke-api] unexpected status $history_status for /api/meals/history"
  exit 1
fi

# ── history-detail ────────────────────────────────────────────────────────────
# history-detail is subscription-only; same gate check as above.
log_phase "history-detail" "fetching recipes for requestId $request_id (200=ok, 402=gate ok)"
history_detail_status="$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/api/meals/requests/$request_id/recipes" \
  -H "Authorization: Bearer $AUTH_TOKEN")"
if [ "$history_detail_status" != "200" ] && [ "$history_detail_status" != "402" ]; then
  echo "[smoke-api] unexpected status $history_detail_status for /api/meals/requests/{id}/recipes"
  exit 1
fi

# ── subscription ──────────────────────────────────────────────────────────────
log_phase "subscription" "reading current subscription / quota status"
subscription_response="$(api_get "/api/subscription/status")"
printf '%s' "$subscription_response" | json_get subscriptionStatus >/dev/null
printf '%s' "$subscription_response" | json_get canGenerate >/dev/null

# ── voice (optional) ──────────────────────────────────────────────────────────
if [ -n "$SMOKE_VOICE_FILE" ]; then
  log_phase "voice" "testing multipart voice transcription upload"
  if [ ! -f "$SMOKE_VOICE_FILE" ]; then
    echo "[smoke-api] SMOKE_VOICE_FILE not found: $SMOKE_VOICE_FILE"
    exit 1
  fi
  curl -fsS -X POST "$BASE_URL/api/voice/transcriptions" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -F "audio=@${SMOKE_VOICE_FILE}" \
    -F "locale=${SMOKE_VOICE_LOCALE}" >/dev/null
else
  skip_phase "voice transcription smoke; set SMOKE_VOICE_FILE=<path> to enable"
fi

echo "[smoke-api] all checks passed for $BASE_URL"
