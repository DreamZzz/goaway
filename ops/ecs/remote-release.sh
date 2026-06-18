#!/usr/bin/env bash

set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/what-to-eat/backend}"
CURRENT_DIR="${CURRENT_DIR:-$APP_ROOT/current}"
RELEASES_DIR="${RELEASES_DIR:-$APP_ROOT/releases}"
STATE_DIR="${STATE_DIR:-$APP_ROOT/shared/releases}"
SERVICE_NAME="${SERVICE_NAME:-what-to-eat-backend}"

usage() {
  cat <<'EOF'
Usage:
  remote-release.sh deploy <uploaded_jar> <release_id> <commit_sha> <jar_sha256>
  remote-release.sh rollback <release_id|previous>
EOF
}

require_file() {
  local path="$1"
  if [ ! -f "$path" ]; then
    echo "Required file not found: $path" >&2
    exit 1
  fi
}

record_active_release() {
  local release_id="$1"
  printf '%s\n' "$release_id" > "$STATE_DIR/current-release.txt"
}

active_release() {
  if [ -f "$STATE_DIR/current-release.txt" ]; then
    cat "$STATE_DIR/current-release.txt"
  fi
}

previous_release() {
  local current
  current="$(active_release)"
  if [ -z "$current" ]; then
    return 1
  fi

  find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' \
    | sort -r \
    | awk -v current="$current" '$0 != current { print; exit }'
}

activate_release() {
  local release_id="$1"
  local release_dir="$RELEASES_DIR/$release_id"
  require_file "$release_dir/app.jar"

  mkdir -p "$CURRENT_DIR" "$STATE_DIR"
  cp "$release_dir/app.jar" "$CURRENT_DIR/app.jar"
  chown deploy:deploy "$CURRENT_DIR/app.jar" || true
  if [ -f "$release_dir/release.env" ]; then
    cp "$release_dir/release.env" "$CURRENT_DIR/release.env"
    chown deploy:deploy "$CURRENT_DIR/release.env" || true
  fi
  record_active_release "$release_id"

  systemctl restart "$SERVICE_NAME"
  systemctl status "$SERVICE_NAME" --no-pager
}

deploy() {
  local uploaded_jar="$1"
  local release_id="$2"
  local commit_sha="$3"
  local jar_sha="$4"
  local release_dir="$RELEASES_DIR/$release_id"

  require_file "$uploaded_jar"
  mkdir -p "$release_dir" "$STATE_DIR" "$CURRENT_DIR"

  mv "$uploaded_jar" "$release_dir/app.jar"
  chown deploy:deploy "$release_dir/app.jar" || true

  cat > "$release_dir/release.env" <<EOF
RELEASE_ID=$release_id
COMMIT_SHA=$commit_sha
JAR_SHA256=$jar_sha
DEPLOYED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF
  chown deploy:deploy "$release_dir/release.env" || true

  activate_release "$release_id"
}

rollback() {
  local requested="${1:-previous}"
  local release_id="$requested"

  if [ "$requested" = "previous" ]; then
    release_id="$(previous_release || true)"
    if [ -z "$release_id" ]; then
      echo "No previous release available to roll back to." >&2
      exit 1
    fi
  fi

  if [ ! -d "$RELEASES_DIR/$release_id" ]; then
    echo "Release not found: $release_id" >&2
    echo "Available releases:" >&2
    find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -r >&2 || true
    exit 1
  fi

  activate_release "$release_id"
}

main() {
  if [ "$#" -lt 1 ]; then
    usage
    exit 1
  fi

  local command="$1"
  shift

  case "$command" in
    deploy)
      if [ "$#" -ne 4 ]; then
        usage
        exit 1
      fi
      deploy "$@"
      ;;
    rollback)
      if [ "$#" -ne 1 ]; then
        usage
        exit 1
      fi
      rollback "$1"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
