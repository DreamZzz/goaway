#!/usr/bin/env bash

set -euo pipefail

PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-https://eat.868299.com}"
LOCAL_BASE_URL="${LOCAL_BASE_URL:-http://127.0.0.1:8081}"
SERVICE_NAME="${SERVICE_NAME:-what-to-eat-backend}"

assert_status() {
  local url="$1"
  local expected="$2"
  local label="$3"
  local status_code

  status_code="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time 20 "$url" || true)"
  if [ "$status_code" != "$expected" ]; then
    echo "[verify-backend] ${label} expected ${expected}, got ${status_code:-curl-error}" >&2
    exit 1
  fi
  echo "[verify-backend] ${label} => ${status_code}"
}

systemctl is-active --quiet "$SERVICE_NAME"

assert_status "${LOCAL_BASE_URL}/api/auth/captcha" "200" "local captcha"
assert_status "${PUBLIC_BASE_URL}/api/auth/captcha" "200" "public captcha"
assert_status "${PUBLIC_BASE_URL}/api/meals/catalog" "401" "public protected endpoint"

echo "[verify-backend] success"
