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

launch_app() {
  # Explicit-component launches (-n $PKG/.X) resolve .X against the PACKAGE,
  # which is wrong when the activity lives in another namespace (Mihon) or
  # the package has a flavor suffix. Launcher-category launch always works.
  $ADB shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
}

echo "== seed local series =="
sh tools/gen_corpus.sh visual-corpus >/dev/null 2>&1 || true
CBZ=$(ls visual-corpus/*.cbz visual-corpus/*.zip 2>/dev/null | head -1 || true)
LOCALDIR="/sdcard/Android/data/$PKG/files/local/Demo"
$ADB shell mkdir -p "$LOCALDIR" || true
[ -n "$CBZ" ] && $ADB push "$CBZ" "$LOCALDIR/ch1.cbz" || true

echo "== cold-start timing =="
$ADB shell am start -W -a android.intent.action.MAIN -n "$PKG/eu.kanade.tachiyomi.ui.main.MainActivity" 2>&1 | tee "$OUT/coldstart.txt" || true
sleep 3
assert_foreground "$PKG"

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
[ "$FAIL" -eq 0 ] || { echo "E: maestro flow failed"; exit 1; }

echo "== labelled screenshots =="
launch_app
sleep 3
assert_foreground "$PKG"
$ADB exec-out screencap -p > "$OUT/shots/01-tabs.png"
for shot in "$OUT"/shots/*.png; do
  [ -s "$shot" ] || { echo "E: missing shot $shot"; exit 1; }
done

echo "== contact sheet =="
montage "$OUT"/shots/*.png "$OUT"/maestro/*.png -tile 2x -geometry +4+4 -label '%f' "$OUT/contact-sheet.png" 2>/dev/null || \
  montage "$OUT"/shots/*.png -tile 2x -geometry +4+4 -label '%f' "$OUT/contact-sheet.png"

echo "== device stats =="
$ADB shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt" 2>&1 || true
$ADB shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt" 2>&1 || true
$ADB logcat -d > "$OUT/logcat.txt" 2>&1 || true
if grep -A4 'FATAL EXCEPTION' "$OUT/logcat.txt" | grep -q "$PKG\|app.auplayer"; then
  echo "E: java crash during visual" >&2; exit 1
fi
echo "VISUAL PASS"
