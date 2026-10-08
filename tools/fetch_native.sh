#!/bin/sh
# Stages pinned native libs into the app tree (no binaries in git).
# Usage: fetch_native.sh <release-tag>. Private repos need AU_GITHUB_TOKEN
# in the environment (CI provides GITHUB_TOKEN); public checkouts work bare.
# Layout: jniLibs/<abi>/*.so + cpp/thirdparty/include/**. Idempotent;
# Gradle/CI cache makes it a no-op.
set -eu
TAG="${1:?release tag required}"
ROOT=$(pwd)
OUT_JNI="$ROOT/player/src/main/jniLibs"
OUT_INC="$ROOT/player/src/main/cpp/thirdparty/include"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
fetch_one() {
  file="$1"
  if [ -n "${GH_TOKEN:-}" ] && command -v gh >/dev/null 2>&1; then
    gh release download "$TAG" --repo ouaell/au_player --pattern "$file" --output "$TMP/$file" --clobber
  elif [ -n "${AU_GITHUB_TOKEN:-}" ]; then
    curl -sfL --retry 3 -H "Authorization: Bearer $AU_GITHUB_TOKEN" -o "$TMP/$file" \
      "https://github.com/ouaell/au_player/releases/download/$TAG/$file"
  else
    curl -sfL --retry 3 -o "$TMP/$file" \
      "https://github.com/ouaell/au_player/releases/download/$TAG/$file"
  fi
}
for spec in "arm64-v8a:arm64" "armeabi-v7a:armv7l" "x86_64:x86_64"; do
  abi=${spec%%:*}
  build=${spec##*:}
  echo "native: $abi <- $TAG/au-mpv-$build.tgz"
  fetch_one "au-mpv-$build.tgz"
  mkdir -p "$TMP/$build"
  tar -xzf "$TMP/au-mpv-$build.tgz" -C "$TMP/$build"
  mkdir -p "$OUT_JNI/$abi"
  find "$TMP/$build" -name '*.so' -path '*lib*' -exec cp {} "$OUT_JNI/$abi/" \;
  inc=$(find "$TMP/$build" -type d -name include | head -1)
  if [ -n "$inc" ]; then
    mkdir -p "$OUT_INC"
    cp -rn "$inc"/. "$OUT_INC"/
  fi
done
echo "native: staged jniLibs + headers"
