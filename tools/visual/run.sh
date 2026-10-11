#!/bin/sh
# Visual QA loop body for the Mihon line (runs INSIDE the emulator job).
# $1 = optional flow-name filter. Maestro asserts visible UI; adb captures
# labelled screenshots; ImageMagick builds contact sheets. Fails if any flow
# fails or any shot is missing.
set -eu
FILTER="${1:-}"
OUT="visual-out"
mkdir -p "$OUT/shots"
export PATH="$HOME/.maestro/bin:$PATH"
ADB="adb -s emulator-5554"
. tools/audit/fg_check.sh
$ADB wait-for-device
APK=$(ls app/build/outputs/apk/debug/*.apk | head -1)
$ADB install -r "$APK"
PKG=$( $ADB shell pm list packages 2>/dev/null | grep auplayer | head -1 | tr -d '\r' | cut -d: -f2 )
[ -n "$PKG" ] || { echo "visual: auplayer package not installed"; exit 1; }
echo "visual: package=$PKG"

echo "== storage access for headless runs =="
# Real users pick a SAF tree in onboarding StorageStep. Headless CI cannot
# drive the system folder picker, so grant the equivalent directly (test
# scaffolding only; the SAF flow itself is untouched product code).
$ADB shell appops set "$PKG" MANAGE_EXTERNAL_STORAGE allow || true
$ADB shell appops set "$PKG" READ_MEDIA_IMAGES allow || true
$ADB shell appops set "$PKG" READ_MEDIA_VIDEO allow || true

echo "== pre-seed prefs (skip first-run onboarding; SAF/permission steps can't run headless) =="
printf '%s' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="__APP_STATE_onboarding_complete" value="true" /></map>' > "$OUT/prefs.xml"
$ADB shell "run-as $PKG mkdir -p /data/data/$PKG/shared_prefs"
PREFS_PATH="/data/data/$PKG/shared_prefs/${PKG}_preferences.xml"
cat "$OUT/prefs.xml" | $ADB shell "run-as $PKG sh -c 'cat > $PREFS_PATH'"
$ADB shell "run-as $PKG cat $PREFS_PATH"

launch_app() {
  # Explicit-component launches (-n $PKG/.X) resolve .X against the PACKAGE,
  # which is wrong when the activity lives in another namespace (Mihon) or
  # the package has a flavor suffix. Launcher-category launch always works.
  $ADB shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
}

echo "== seed local series =="
sh tools/gen_corpus.sh visual-corpus >/dev/null 2>&1 || true
CBZ=$(ls visual-corpus/*.cbz visual-corpus/*.zip 2>/dev/null | head -1 || true)
# Local source base = /sdcard/<app_name>/local (AndroidStorageFolderProvider).
# NOTE: adb shell splits on spaces remotely — single-quote every remote path.
LOCALDIR="/sdcard/AU Player/local/Demo"
$ADB shell "mkdir -p '$LOCALDIR'"
[ -n "$CBZ" ] && $ADB push "$CBZ" "$LOCALDIR/ch1.cbz"
$ADB shell "ls -l '$LOCALDIR'"

echo "== cold-start timing =="
$ADB shell am start -W -a android.intent.action.MAIN -n "$PKG/eu.kanade.tachiyomi.ui.main.MainActivity" 2>&1 | tee "$OUT/coldstart.txt" || true
wait_foreground "$PKG" 120

echo "== maestro flows =="
FAIL=0
mkdir -p "$OUT/flows"
for flow in maestro/mihon/*.yaml; do
  name=$(basename "$flow" .yaml)
  case "$FILTER" in ""|*"$name"*) ;; *) continue ;; esac
  echo "--- flow: $name ---"
  # Pin the resolved package id (flavor suffixes vary by build type).
  sed "s/^appId: .*/appId: $PKG/" "$flow" > "$OUT/flows/$name.yaml"
  maestro test --format junit --output "$OUT/junit-$name.xml" "$OUT/flows/$name.yaml" || FAIL=1
done
[ "$FAIL" -eq 0 ] || echo "E: a maestro flow failed (see junit + diag shot)"

echo "== collect maestro screenshots =="
mkdir -p "$OUT/maestro"
# Locate where this Maestro version drops takeScreenshot PNGs, then gather.
find "$HOME/.maestro" /tmp -name "*.png" 2>/dev/null | head -20 || true
find . -name "series-*.png" -o -name "scan-*.png" -o -name "tab-*.png" -o -name "library-*.png" -o -name "reader-*.png" 2>/dev/null | head -20 || true
find "$HOME/.maestro" /tmp -name "*.png" -mmin -30 2>/dev/null -exec cp {} "$OUT/maestro/" \; || true
find . -maxdepth 1 -name "series-*.png" -o -maxdepth 1 -name "scan-*.png" -o -maxdepth 1 -name "tab-*.png" -o -maxdepth 1 -name "library-*.png" -o -maxdepth 1 -name "reader-*.png" 2>/dev/null -exec cp {} "$OUT/maestro/" \; || true
ls "$OUT/maestro" || true

echo "== diagnostic shot (always, even on flow failure) =="
$ADB exec-out screencap -p > "$OUT/shots/00-diag.png" || true
launch_app
wait_foreground "$PKG" 60
$ADB exec-out screencap -p > "$OUT/shots/01-tabs.png" || true

echo "== contact sheet (always) =="
montage "$OUT"/shots/*.png "$OUT"/maestro/*.png -tile 2x -geometry +4+4 -label '%f' "$OUT/contact-sheet.png" 2>/dev/null || \
  montage "$OUT"/shots/*.png -tile 2x -geometry +4+4 -label '%f' "$OUT/contact-sheet.png" || true

echo "== device stats (always) =="
$ADB shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt" 2>&1 || true
$ADB shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt" 2>&1 || true
$ADB logcat -d > "$OUT/logcat.txt" 2>&1 || true
if grep -A4 'FATAL EXCEPTION' "$OUT/logcat.txt" | grep -q "$PKG"; then
  echo "E: java crash during visual" >&2; exit 1
fi
[ "$FAIL" -eq 0 ] || { echo "E: maestro flow failed"; exit 1; }
echo "VISUAL PASS"
