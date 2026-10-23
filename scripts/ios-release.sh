#!/usr/bin/env bash
set -euo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TASK="${1:-help}"

if [ "$TASK" = "help" ] || [ "$TASK" = "--help" ]; then
    cat <<'EOF'
Usage: ./scripts/ios-release.sh <command> [option:value ...]

  setup                          Install the pinned Fastlane dependencies.
  status                         Check API authentication, builds, and App Store versions.
  archive version:X.Y.Z build:N   Create and export a signed Release IPA locally.
  export archive:/absolute/path   Export an existing xcarchive locally.
  upload ipa:/absolute/path       Upload an IPA to App Store Connect; do not submit review.
  deploy ipa:/path sha256:HASH    Upload, wait for processing, submit and verify review.
  review version:X.Y.Z build:N    Wait for the uploaded build, submit and verify review.
                                 notes:/path supplies approved Korean release notes.
                                 exempt_encryption:true requires an approved declaration.

API key configuration: ~/.config/ramap/appstore-connect/config.json
Override its location with RAMAP_ASC_CONFIG. Keep the .p8 key outside the repository.
deploy completes App Store review submission. upload is the TestFlight-only option.
review resumes after an upload timeout without uploading again. Public release stays manual.
EOF
    exit 0
fi

case "$TASK" in
    setup|status|archive|export|upload|deploy|review) ;;
    *) echo "Unknown command: $TASK" >&2; exit 1 ;;
esac
shift

if command -v brew >/dev/null 2>&1; then
    RUBY_PREFIX="$(brew --prefix ruby 2>/dev/null || true)"
    if [ -x "$RUBY_PREFIX/bin/ruby" ]; then
        export PATH="$RUBY_PREFIX/bin:$PATH"
    fi
fi

ruby -e 'abort "Ruby 3.2 or later is required (brew install ruby)." if Gem::Version.new(RUBY_VERSION) < Gem::Version.new("3.2")'

export BUNDLE_GEMFILE="$ROOT_DIR/iosApp/Gemfile"
export BUNDLE_PATH="${RAMAP_IOS_GEMS:-$HOME/.local/share/ramap/ios-deploy/gems}"
export BUNDLE_APP_CONFIG="$HOME/.local/share/ramap/ios-deploy/bundle"
export FASTLANE_SKIP_UPDATE_CHECK=1
export FASTLANE_HIDE_CHANGELOG=1
export FASTLANE_HIDE_GITHUB_ISSUES=1
export FASTLANE_SKIP_DOCS=1
export FASTLANE_OPT_OUT_USAGE=1
export FASTLANE_XCODEBUILD_SETTINGS_TIMEOUT=30

if [ "$TASK" = "setup" ]; then
    CONFIG_DIR="$(dirname "${RAMAP_ASC_CONFIG:-$HOME/.config/ramap/appstore-connect/config.json}")"
    install -d -m 700 "$CONFIG_DIR"
    CONFIG_PATH="${RAMAP_ASC_CONFIG:-$CONFIG_DIR/config.json}"
    if [ ! -e "$CONFIG_PATH" ]; then
        (umask 077; cp "$ROOT_DIR/iosApp/fastlane/api-key.example.json" "$CONFIG_PATH")
    fi
    bundle install
    exit 0
fi

bundle check >/dev/null 2>&1 || {
    echo "Run ./scripts/ios-release.sh setup first." >&2
    exit 1
}
cd "$ROOT_DIR/iosApp"
exec bundle exec fastlane ios "$TASK" "$@"
