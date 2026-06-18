#!/bin/bash

set -euo pipefail

trap 'echo "[xcode-cloud][post-clone][error] command failed at line $LINENO: $BASH_COMMAND" >&2' ERR

log() {
  echo "[xcode-cloud][post-clone] $*"
}

fail() {
  echo "[xcode-cloud][post-clone][error] $*" >&2
  exit 1
}

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_ROOT="$REPO_ROOT/frontend"
IOS_ROOT="$FRONTEND_ROOT/ios"
REQUIRED_NODE_MAJOR=18

ensure_node() {
  if command -v node >/dev/null 2>&1; then
    local current_major
    current_major="$(node -e 'process.stdout.write(process.versions.node.split(".")[0])')"
    if [ "$current_major" -ge "$REQUIRED_NODE_MAJOR" ]; then
      log "Using preinstalled Node $(node --version)"
      return 0
    fi
    log "Preinstalled Node $(node --version) is below required major $REQUIRED_NODE_MAJOR"
  else
    log "Node is not installed yet"
  fi

  if ! command -v brew >/dev/null 2>&1; then
    fail "Homebrew is unavailable, cannot install Node"
  fi

  log "Installing node@20 with Homebrew (LTS)"
  HOMEBREW_NO_AUTO_UPDATE=1 brew install node@20
  export PATH="$(brew --prefix node@20)/bin:$PATH"

  if ! command -v node >/dev/null 2>&1; then
    fail "Node installation did not expose a node binary"
  fi

  log "Installed Node $(node --version)"
}

# Xcode Cloud ships Ruby 2.6 which is too old for Bundler 4.x.
# Install a modern Ruby via Homebrew so gem/bundle operations work.
ensure_ruby() {
  local ruby_major
  ruby_major="$(ruby -e 'print RUBY_VERSION.split(".").first.to_i' 2>/dev/null || echo 0)"
  if [ "$ruby_major" -ge 3 ]; then
    log "Using system Ruby $(ruby --version)"
    return 0
  fi

  log "System Ruby $(ruby --version 2>/dev/null || echo 'unknown') is too old (need >= 3), installing via Homebrew"
  if ! command -v brew >/dev/null 2>&1; then
    fail "Homebrew is unavailable, cannot install Ruby"
  fi

  HOMEBREW_NO_AUTO_UPDATE=1 brew install ruby
  export PATH="$(brew --prefix ruby)/bin:$PATH"
  log "Homebrew Ruby $(ruby --version) ready"
}

ensure_bundler() {
  local required_version
  required_version="$(grep -A1 'BUNDLED WITH' "$FRONTEND_ROOT/Gemfile.lock" | tail -1 | tr -d ' ')"
  log "Installing Bundler $required_version"
  gem install bundler:"$required_version" --no-document
}

write_xcode_env_local() {
  local node_binary
  node_binary="$(command -v node)"
  cat > "$IOS_ROOT/.xcode.env.local" <<EOF
export NODE_BINARY=$node_binary
EOF
  log "Pinned NODE_BINARY for Xcode script phases: $node_binary"
}

install_javascript_dependencies() {
  log "Installing frontend npm dependencies"
  cd "$FRONTEND_ROOT"
  npm ci
}

install_cocoapods_dependencies() {
  cd "$IOS_ROOT"

  if [ -n "${CI_XCODE_CLOUD:-}" ]; then
    log "Xcode Cloud detected — using bundle exec pod install for pinned CocoaPods version"
    cd "$FRONTEND_ROOT"
    bundle config set --local path vendor/bundle
    bundle install --jobs 4 --retry 3
    cd "$IOS_ROOT"
    # Use --no-repo-update instead of --deployment so hermes-engine prebuilt
    # checksum drift on the CDN does not fail the build.
    bundle exec pod install --no-repo-update
    return 0
  fi

  if command -v pod >/dev/null 2>&1; then
    log "Running pod install with CocoaPods $(pod --version)"
    pod install --no-repo-update
    return 0
  fi

  if command -v bundle >/dev/null 2>&1; then
    log "System CocoaPods unavailable, falling back to bundle exec pod install"
    cd "$FRONTEND_ROOT"
    bundle config set --local path vendor/bundle
    bundle install --jobs 4 --retry 3
    cd "$IOS_ROOT"
    bundle exec pod install --no-repo-update
    return 0
  fi

  fail "Neither CocoaPods nor Bundler is available"
}

main() {
  log "Repository root: $REPO_ROOT"
  log "Node: $(node --version 2>/dev/null || echo 'not found')"
  log "Ruby: $(ruby --version 2>/dev/null || echo 'not found')"
  log "Bundler: $(bundle --version 2>/dev/null || echo 'not found')"
  log "CocoaPods: $(pod --version 2>/dev/null || echo 'not found')"

  ensure_node
  write_xcode_env_local
  install_javascript_dependencies

  if [ -n "${CI_XCODE_CLOUD:-}" ]; then
    ensure_ruby
    ensure_bundler
  fi

  install_cocoapods_dependencies
  log "Xcode Cloud post-clone bootstrap completed"
}

main "$@"
