#!/bin/sh
# Audit bug-hunt pass (read-only against the app; fails the job if a
# FATAL EXCEPTION / native crash / ANR implicating our package appears).
# Runs inside the audit-bughunt workflow on the CI emulator.
set -eu
PKG="${PKG:-com.example.auplayer}"
OUT="${OUT:-bughunt-out}"
mkdir -p "$OUT"
ADB="adb -s emulator-5554"
. tools/audit/fg_check.sh
echo "== waiting for device =="
$ADB wait-for-device
$ADB shell input keyevent 82 || true

echo "== installing debug APK =="
APK=$(ls app/build/outputs/apk/direct/debug/*.apk | head -1)
$ADB install -r "$APK"
# Resolve the real package id (flavors may suffix it); fail fast if absent.
PKG=$( $ADB shell pm list packages 2>/dev/null | grep auplayer | head -1 | tr -d '\r' | cut -d: -f2 )
if [ -z "$PKG" ]; then echo "bughunt: auplayer package not installed"; exit 1; fi
echo "bughunt: package=$PKG"

echo "== pushing corpus =="
sh tools/gen_corpus.sh audit-corpus >/dev/null 2>&1 || true
CLIP=$(ls audit-corpus/*.mp4 2>/dev/null | head -1 || true)
[ -n "$CLIP" ] && $ADB push "$CLIP" /sdcard/bughunt.mp4 || true

echo "== clearing logcat =="
$ADB logcat -c

# Crash verdict comes from the package-scoped logcat gate at the end (stronger
# than monkey's exit code), so monkey must survive SYSTEM-component ANRs
# (launcher/searchbox routinely ANR under swiftshader load) and keep probing.
MONKEY_FLAGS="--ignore-crashes --ignore-timeouts --ignore-security-exceptions"

echo "== monkey: home + launcher (20000 events) =="
$ADB shell monkey -p "$PKG" $MONKEY_FLAGS --throttle 50 -s 42 20000 > "$OUT/monkey-home.txt" 2>&1 || true
grep -q "Events injected: 20000" "$OUT/monkey-home.txt" || { echo "E: home monkey did not inject 20000 events" >&2; exit 1; }

echo "== monkey: player (VIEW intent, then events) =="
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/bughunt.mp4 -t video/mp4 "$PKG/.MainActivity"
sleep 5
assert_foreground "$PKG"
$ADB shell monkey -p "$PKG" $MONKEY_FLAGS --throttle 50 -s 7 20000 > "$OUT/monkey-player.txt" 2>&1 || true
grep -q "Events injected: 20000" "$OUT/monkey-player.txt" || { echo "E: player monkey did not inject 20000 events" >&2; exit 1; }

echo "== negative inputs =="
$ADB shell "head -c 0 /dev/null > /sdcard/zero.mp4" || : > /sdcard/zero.mp4
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/zero.mp4 -t video/mp4 "$PKG/.MainActivity" || true
sleep 5
assert_process "$PKG"
$ADB shell "head -c 1024 /dev/urandom > /sdcard/corrupt.mp4" || true
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/corrupt.mp4 -t video/mp4 "$PKG/.MainActivity" || true
sleep 5
assert_process "$PKG"
printf 'not a zip' > "$OUT/evil.cbz"
$ADB push "$OUT/evil.cbz" /sdcard/evil.cbz || true
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/evil.cbz -t application/x-cbz "$PKG/.MainActivity" || true
sleep 5
assert_process "$PKG"

echo "== lifecycle/system =="
$ADB shell am send-trim-memory "$PKG" RUNNING_CRITICAL || true
sleep 2
$ADB shell settings put system font_scale 2.0 || true
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/.MainActivity"
sleep 5
assert_foreground "$PKG"
$ADB shell settings put system font_scale 1.0 || true
$ADB shell am kill "$PKG" || true
sleep 2
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/.MainActivity"
sleep 5
assert_foreground "$PKG"

echo "== meminfo/gfxinfo =="
$ADB shell dumpsys meminfo "$PKG" > "$OUT/meminfo-after.txt" 2>&1 || true
$ADB shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt" 2>&1 || true

echo "== fatal/ANR scan (package-scoped) =="
$ADB logcat -d > "$OUT/logcat.txt" 2>&1 || true
grep -E "FATAL EXCEPTION|ANR in $PKG|Build fingerprint|signal .* \(SIG" "$OUT/logcat.txt" > "$OUT/fatals.txt" || true
if grep -q "$PKG" "$OUT/fatals.txt"; then
  echo "BUGHUNT FAIL: fatals implicating $PKG (see bughunt-out/fatals.txt)"
  cat "$OUT/fatals.txt"
  exit 1
fi
echo "BUGHUNT PASS: no package-scoped fatals/ANRs in this pass"
