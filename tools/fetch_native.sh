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
# libc++_shared: required by libmpv.so/au_mpv.so DT_NEEDED but not built by
# us — copy from the PINNED NDK only (a runner-preinstalled older NDK has an
# incompatible libc++; `head -1` once shipped exactly that).
NDK_VERSION=$(grep '^NDK_VERSION=' "$ROOT/native/pins.txt" | cut -d= -f2)
NDK_DIR="${ANDROID_HOME:-}/ndk/$NDK_VERSION"
if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$NDK_DIR" ]; then
  echo "native: WARNING no pinned NDK ($NDK_VERSION); skipping libc++_shared" >&2
else
  for spec in "arm64-v8a:aarch64-linux-android" "armeabi-v7a:arm-linux-androideabi" "x86_64:x86_64-linux-android"; do
    abi=${spec%%:*}
    triple=${spec##*:}
    src="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/$triple/libc++_shared.so"
    if [ -f "$src" ]; then
      cp "$src" "$OUT_JNI/$abi/"
      echo "native: $abi <- NDK $NDK_VERSION libc++_shared.so"
    else
      echo "native: WARNING no libc++_shared for $abi" >&2
    fi
  done
fi
echo "native: staged jniLibs + headers"
