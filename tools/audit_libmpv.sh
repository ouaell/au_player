#!/bin/sh
# MS-004 D1 audit (REPORT-ONLY, §3.1): prints license/ABI/alignment/JNI/build-flag
# facts for both libmpv candidates. The verdict is recorded by a human in
# docs/decisions.md — this script never decides, it only evidences.
# Runs in CI (.github/workflows/d1-audit.yml); needs curl/unzip/git/strings.
set -eu
ROOT=$(pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
cd "$WORK"

JD_URL="https://repo1.maven.org/maven2/dev/jdtech/mpv/libmpv/1.0.0/libmpv-1.0.0.aar"
MH_URL="https://repo1.maven.org/maven2/io/github/abdallahmehiz/mpv-android-lib/0.1.12/mpv-android-lib-0.1.12.aar"
curl -sSL -o jdtech.aar "$JD_URL"
curl -sSL -o mehiz.aar "$MH_URL"
ls -la jdtech.aar mehiz.aar

for a in jdtech mehiz; do
  echo "===== $a: jni ABIs ====="
  unzip -l "$a.aar" | grep 'jni/' | awk '{print $4}' | cut -d/ -f2 | sort -u
  echo "===== $a: .so inventory ====="
  unzip -l "$a.aar" | grep '\.so$' | awk '{print $4, $1}'
  rm -rf "so-$a" "cj-$a"
  mkdir -p "so-$a" "cj-$a"
  unzip -o -q "$a.aar" 'jni/*' -d "so-$a"
  unzip -o -q "$a.aar" 'classes.jar' -d "cj-$a" || true
  if [ -f "cj-$a/classes.jar" ]; then
    echo "===== $a: wrapper classes (mpv/jni) ====="
    unzip -l "cj-$a/classes.jar" | grep -iE 'mpv|MPV' | awk '{print $4}' | head -40
  fi
  echo "===== $a: 16KB alignment ====="
  sh "$ROOT/tools/check_16kb.sh" "$a.aar" || echo "AUDIT: $a MISALIGNED (see above)"
  echo "===== $a: license/version strings in native libs ====="
  find "so-$a" -name '*.so' -exec sh -c 'strings "$1" | grep -aiE "general public license|lesser general|nonfree|enable-gpl|FFmpeg version|mpv [0-9]+\.[0-9]+" | sort | uniq -c | sort -rn | head -25' _ {} \;
  echo "===== $a: exported JNI symbols ====="
  find "so-$a" -name 'libmpv.so' | head -2 | xargs -I{} sh -c 'nm -D --defined-only "{}" 2>/dev/null | grep " Java_" | head -20 || echo "(nm unavailable)"'
done

echo "===== build-flag sources (shallow clones) ====="
git clone --depth 1 -q https://github.com/jarnedemeulemeester/libmpv-android jdtech-src
git clone --depth 1 -q --branch library https://github.com/abdallahmehiz/mpv-android mehiz-src
for s in jdtech-src mehiz-src; do
  echo "----- $s: gpl/nonfree/x26x/openssl flags -----"
  grep -rniE '\-Dgpl|enable-gpl|enable-nonfree|enable-libx26[45]|enable-openssl|openssl|mbedtls|gmp|libutvideo' "$s/buildscripts" | head -40 || true
  echo "----- $s: ass/fribidi/harfbuzz/freetype refs -----"
  grep -rniE 'libass|fribidi|harfbuzz|freetype|fontconfig' "$s/buildscripts" | head -20 || true
done
echo "AUDIT COMPLETE"
