#!/bin/bash

set -euo pipefail

trap 'echo "[xcode-cloud][shim][error] failed at line $LINENO: $BASH_COMMAND" >&2' ERR

echo "[xcode-cloud][shim] BASH_SOURCE=${BASH_SOURCE[0]}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "[xcode-cloud][shim] SCRIPT_DIR=$SCRIPT_DIR"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
echo "[xcode-cloud][shim] REPO_ROOT=$REPO_ROOT"
echo "[xcode-cloud][shim] target=$REPO_ROOT/ci_scripts/ci_post_clone.sh"

if [ ! -f "$REPO_ROOT/ci_scripts/ci_post_clone.sh" ]; then
  echo "[xcode-cloud][shim][error] main script not found at $REPO_ROOT/ci_scripts/ci_post_clone.sh" >&2
  ls "$REPO_ROOT/" >&2 || true
  exit 1
fi

exec "$REPO_ROOT/ci_scripts/ci_post_clone.sh" "$@"
