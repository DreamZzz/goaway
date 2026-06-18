#!/bin/bash

set -euo pipefail

log() {
  echo "[xcode-cloud][pre-xcodebuild] $*"
}

fail() {
  echo "[xcode-cloud][pre-xcodebuild][error] $*" >&2
  exit 1
}

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_ROOT="$REPO_ROOT/frontend"
IOS_ROOT="$FRONTEND_ROOT/ios"
WORKSPACE_PATH="$IOS_ROOT/frontend.xcworkspace"
PODS_SUPPORT_DIR="$IOS_ROOT/Pods/Target Support Files/Pods-frontend"
RUNTIME_CONFIG_PATH="$FRONTEND_ROOT/src/app/config/runtime.generated.js"
RUNTIME_API_BASE_URL="${APP_REMOTE_API_BASE_URL:-https://eat.868299.com}"

require_file() {
  local path="$1"
  local message="$2"
  if [ ! -f "$path" ]; then
    fail "$message ($path)"
  fi
}

ensure_runtime_config() {
  log "Writing runtime.generated.js for remote (apiBaseUrl=$RUNTIME_API_BASE_URL)"
  (
    cd "$FRONTEND_ROOT"
    node scripts/write-runtime-config.js remote "$RUNTIME_API_BASE_URL" ""
  )
  require_file "$RUNTIME_CONFIG_PATH" "runtime.generated.js write failed"
  if ! grep -q "\"apiBaseUrl\": \"$RUNTIME_API_BASE_URL\"" "$RUNTIME_CONFIG_PATH"; then
    fail "runtime.generated.js does not contain expected apiBaseUrl ($RUNTIME_API_BASE_URL)"
  fi
}

main() {
  log "Validating Xcode Cloud build prerequisites"

  require_file "$WORKSPACE_PATH/contents.xcworkspacedata" "frontend.xcworkspace is missing"
  require_file "$PODS_SUPPORT_DIR/Pods-frontend.release.xcconfig" "Pods release xcconfig is missing; pod install likely did not run"
  require_file "$PODS_SUPPORT_DIR/Pods-frontend-frameworks-Release-output-files.xcfilelist" "Pods release xcfilelist is missing; pod install likely did not run"
  require_file "$IOS_ROOT/.xcode.env.local" "Pinned NODE_BINARY config is missing"

  ensure_runtime_config

  (
    cd "$IOS_ROOT"
    log "Listing Xcode workspace to fail fast if workspace wiring is broken"
    xcodebuild -list -workspace frontend.xcworkspace >/dev/null
  )

  log "Validated workspace, Pods support files, NODE_BINARY bootstrap, and runtime.generated.js"
}

main "$@"
