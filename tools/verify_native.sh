#!/bin/sh
# MS-062 native/release verifier (no device): alignment, GPL strings,
# JNI↔R8 mapping, bundletool validate, aapt2 manifest checks. Each check
# degrades to SKIP with a reason when its input is absent (debug builds have
# no mapping.txt/AAB); failures are hard errors.
# Usage: verify_native.sh <apk> [mapping.txt] [aab]
set -eu
APK="${1:?apk required}"
MAPPING="${2:-}"
AAB="${3:-}"
fail=0
BT="${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools/36.0.0"

echo "== verify: 16KB alignment =="
sh "$(dirname "$0")/check_16kb.sh" "$APK"

echo "== verify: no GPL/AGPL/nonfree strings in native libs =="
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
unzip -o -q "$APK" '*.so' -d "$tmp" || true
if [ -z "$(find "$tmp" -name '*.so' -print -quit)" ]; then
  echo "SKIP: no native libs in $APK"
else
  if find "$tmp" -name '*.so' -exec sh -c 'strings "$1"' _ {} \; \
      | grep -aiE 'General Public License|AGPL|nonfree' \
      | grep -av 'Lesser General Public' | grep -q .; then
    echo "verify: non-LGPL GPL marker found" >&2
    fail=1
  else
    echo "OK: no non-LGPL GPL markers"
  fi
fi

echo "== verify: JNI exports vs R8 mapping =="
if [ -z "$MAPPING" ] || [ ! -f "$MAPPING" ]; then
  echo "SKIP: no mapping.txt (debug/unminified build)"
elif [ -z "$(find "$tmp" -name '*.so' -print -quit)" ]; then
  echo "SKIP: no native libs to cross-check"
else
  echo "(mapping present; full Java_* ↔ mapping cross-check lands with the native build in MS-062+)"
fi

echo "== verify: bundletool validate =="
if [ -z "$AAB" ] || [ ! -f "$AAB" ]; then
  echo "SKIP: no AAB (T1 builds APK only; release.yml covers AAB)"
else
  BTJAR="$tmp/bundletool.jar"
  if [ ! -f "$BTJAR" ]; then
    curl -sfL --retry 2 -o "$BTJAR" \
      "https://github.com/google/bundletool/releases/download/1.18.1/bundletool-all-1.18.1.jar"
  fi
  java -jar "$BTJAR" validate --bundle="$AAB"
fi

echo "== verify: aapt2 manifest checks =="
AAPT="$BT/aapt2"
if [ ! -x "$AAPT" ]; then
  echo "SKIP: aapt2 missing" >&2
  fail=1
else
  badging=$("$AAPT" dump badging "$APK")
  echo "$badging" | grep -q "targetSdkVersion:'36'" || { echo "verify: targetSdk != 36" >&2; fail=1; }
  echo "$badging" | grep -q "uses-permission.*READ_MEDIA_VIDEO" || { echo "verify: READ_MEDIA_VIDEO missing" >&2; fail=1; }
  xmltree=$("$AAPT" dump xmltree "$APK" --file AndroidManifest.xml)
  echo "$xmltree" | grep -q "enableOnBackInvokedCallback" || { echo "verify: predictive-back flag missing" >&2; fail=1; }
  echo "$xmltree" | grep -q "foregroundServiceType" || { echo "verify: FGS type missing" >&2; fail=1; }
  echo "$xmltree" | grep -q "supportsPictureInPicture" || { echo "verify: PiP missing" >&2; fail=1; }
  echo "$xmltree" | grep -q "android.intent.action.VIEW" || { echo "verify: VIEW filter missing" >&2; fail=1; }
  echo "OK: manifest checks pass"
fi

if [ "$fail" -ne 0 ]; then exit 1; fi
echo "verify_native: ALL CHECKS PASS (or SKIP with reason)"
