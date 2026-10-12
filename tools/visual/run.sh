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

# Anything that exits early must still leave the crash behind: without this the
# only evidence is a bare "never foregrounded" and the run is a dead end.
dump_diagnostics() {
  rc=$?
  if [ "$rc" -ne 0 ]; then
    mkdir -p "$OUT/shots"
    $ADB logcat -d > "$OUT/logcat-fail.txt" 2>&1 || true
    $ADB shell dumpsys activity activities > "$OUT/activity-fail.txt" 2>&1 || true
    $ADB exec-out screencap -p > "$OUT/shots/fail-diag.png" 2>/dev/null || true
    echo "--- crash lines ---" >&2
    grep -a -E "FATAL EXCEPTION|AndroidRuntime|UnsatisfiedLinkError|AuPlayer" \
      "$OUT/logcat-fail.txt" 2>/dev/null | head -40 >&2 || true
  fi
  exit $rc
}
trap dump_diagnostics EXIT

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
$ADB shell appops get "$PKG" || true

echo "== pre-seed prefs (skip first-run onboarding; SAF/permission steps can't run headless) =="
# Headless storage: point the Local source at an APP-OWNED dir. Real users pick
# a SAF tree in onboarding StorageStep; shared-/sdcard seeds are invisible to
# the app UID (scoped-storage ownership), and MANAGE_EXTERNAL_STORAGE does not
# apply to undeclared apps. Internal storage needs no permission at all.
SEEDBASE="/data/data/$PKG/files/citest"
$ADB shell "run-as $PKG mkdir -p $SEEDBASE/local/Demo"
$ADB shell "run-as $PKG mkdir -p /data/data/$PKG/shared_prefs"
printf '%s' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="__APP_STATE_onboarding_complete" value="true" /><string name="__APP_STATE_storage_dir">file://'"$SEEDBASE"'</string></map>' > "$OUT/prefs.xml"
PREFS_PATH="/data/data/$PKG/shared_prefs/${PKG}_preferences.xml"
cat "$OUT/prefs.xml" | $ADB shell "run-as $PKG sh -c 'cat > $PREFS_PATH'"
$ADB shell "run-as $PKG cat $PREFS_PATH"

launch_app() {
  # Explicit-component launches (-n $PKG/.X) resolve .X against the PACKAGE,
  # which is wrong when the activity lives in another namespace (Mihon) or
  # the package has a flavor suffix. Launcher-category launch always works.
  $ADB shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
}

echo "== seed local series (app-owned, via run-as base64) =="
sh tools/gen_corpus.sh visual-corpus >/dev/null 2>&1 || true
CBZ=$(ls visual-corpus/*.cbz visual-corpus/*.zip 2>/dev/null | head -1 || true)
if [ -n "$CBZ" ]; then
  base64 -w0 "$CBZ" > "$OUT/ch1.b64"
  $ADB shell "run-as $PKG sh -c 'base64 -d > $SEEDBASE/local/Demo/ch1.cbz'" < "$OUT/ch1.b64"
fi
$ADB shell "run-as $PKG sh -c 'ls -l $SEEDBASE/local/Demo'"

echo "== seed a MediaStore video (the emulator ships with none) =="
# A generated clip, not a checked-in binary: keeps the repo small and makes the
# fixture reproducible. Needs a real H.264 stream for hwdec/mediacodec to be
# exercised at all.
ffmpeg -y -f lavfi -i "testsrc2=size=640x360:rate=24:duration=8" \
  -f lavfi -i "sine=frequency=440:duration=8" \
  -c:v libx264 -pix_fmt yuv420p -preset veryfast -c:a aac -shortest \
  "$OUT/Sample Clip.mp4" >/dev/null 2>&1 || {
    echo "E: ffmpeg could not generate the sample clip"; exit 1; }
$ADB push "$OUT/Sample Clip.mp4" /sdcard/Movies/ >/dev/null
# The scanner indexes on its own schedule; nudge it and wait for the row.
$ADB shell "content call --uri content://media/external/video/media --method scan_volume --arg external" >/dev/null 2>&1 || true
$ADB shell am broadcast -a android.intent.action.MEDIA_MOUNTED \
  -d file:///sdcard >/dev/null 2>&1 || true
i=0
until $ADB shell content query --uri content://media/external/video/media \
  --projection _display_name 2>/dev/null | grep -q "Sample Clip"; do
  i=$((i + 1))
  [ "$i" -lt 30 ] || { echo "E: sample video never entered MediaStore"; exit 1; }
  sleep 2
done
echo "visual: MediaStore seeded with Sample Clip"

echo "== cold-start timing =="
$ADB shell am start -W -a android.intent.action.MAIN -n "$PKG/eu.kanade.tachiyomi.ui.main.MainActivity" 2>&1 | tee "$OUT/coldstart.txt" || true
wait_foreground "$PKG" 180

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
# Maestro drops takeScreenshot PNGs under $HOME/.maestro (deep). The repo itself
# lives under $HOME/work — exclude it (fastlane art, corpus fixtures).
find "$HOME/.maestro" /tmp -name "*.png" -mmin -30 -not -path "*/work/*" \
  -exec cp {} "$OUT/maestro/" \; 2>/dev/null || true
ls "$OUT/maestro" || true

echo "== reader render proof (Maestro shots + pixel variance) =="
case "$FILTER" in
  ""|*reader*)
    # NOTE: Mihon's reader window is FLAG_SECURE (registerSecureActivity), so
    # `adb exec-out screencap` returns black by design. Maestro/UiAutomator
    # screenshots capture the page fine — gate on those instead.
    # The swiftshader composer occasionally wedges a frame black, so shoot
    # three times and pass on ANY good frame.
    READER_SHOTS=$(ls -t "$OUT"/maestro/reader-page*.png 2>/dev/null || true)
    [ -n "$READER_SHOTS" ] || { echo "E: no reader-page Maestro shots collected"; exit 1; }
    echo "$READER_SHOTS"
    PASS=0
    for shot in $READER_SHOTS; do
      # Pillow decodes all PNG variants (incl. interlaced) correctly; the
      # hand-rolled parser misreads Adam7 files, so it is retired.
      if python3 - "$shot" <<'EOF'
import sys
try:
    from PIL import Image, ImageStat
except ImportError:
    print("Pillow missing")
    sys.exit(2)
img = Image.open(sys.argv[1]).convert("RGB")
st = ImageStat.Stat(img)
mean = sum(st.mean) / 3
std = (sum(v * v for v in st.stddev) / 3) ** 0.5
print(f"reader: {sys.argv[1]} {img.size[0]}x{img.size[1]} mean={mean:.1f} std={std:.1f}")
sys.exit(0 if (mean > 40.0 and std > 15.0) else 1)
EOF
      then
        PASS=1
      fi
    done
    [ "$PASS" -eq 1 ] || { echo "E: no good reader frame among shots"; exit 1; }
    ;;
esac

echo "== player render proof (video frame, not a black surface) =="
case "$FILTER" in
  ""|*videos*)
    PLAYER_SHOTS=$(ls -t "$OUT"/maestro/player-frame*.png 2>/dev/null || true)
    [ -n "$PLAYER_SHOTS" ] || { echo "E: no player-frame Maestro shots collected"; exit 1; }
    PASS=0
    for shot in $PLAYER_SHOTS; do
      # A black player is the failure mode that matters: mpv can come up,
      # accept the loadfile and still present nothing. Requiring real pixel
      # variance catches that where a UI assert cannot.
      if python3 - "$shot" <<'EOF'
import sys
try:
    from PIL import Image, ImageStat
except ImportError:
    print("Pillow missing")
    sys.exit(2)
img = Image.open(sys.argv[1]).convert("RGB")
st = ImageStat.Stat(img)
mean = sum(st.mean) / 3
std = (sum(v * v for v in st.stddev) / 3) ** 0.5
print(f"player: {sys.argv[1]} {img.size[0]}x{img.size[1]} mean={mean:.1f} std={std:.1f}")
sys.exit(0 if (mean > 25.0 and std > 20.0) else 1)
EOF
      then
        PASS=1
      fi
    done
    [ "$PASS" -eq 1 ] || { echo "E: player surface is black in every frame"; exit 1; }
    # Non-black is not playback. Compare two frames taken seconds apart: a
    # paused or wedged surface renders identically forever, so a real diff is
    # what separates "loaded" from "playing". testsrc2 moves constantly.
    A=$(ls -t "$OUT"/maestro/player-frame-a.png 2>/dev/null | head -1)
    B=$(ls -t "$OUT"/maestro/player-frame-b.png 2>/dev/null | head -1)
    if [ -n "$A" ] && [ -n "$B" ]; then
      if ! compare -metric AE "$A" "$B" null: 2>&1 | awk '{exit ($1 > 5000) ? 0 : 1}'; then
        echo "E: player frames are identical — surface is not advancing"; exit 1
      fi
      echo "player: frames differ (surface is advancing)"
    else
      echo "E: need two player frames to prove playback advances"; exit 1
    fi
    # Enhancement must not black out the picture it is applied to: same
    # variance bar as plain playback, after the Anime chain is pushed.
    ENC_SHOTS=$(ls -t "$OUT"/maestro/enhance-applied*.png 2>/dev/null || true)
    if [ -n "$ENC_SHOTS" ]; then
      ENC_PASS=0
      for shot in $ENC_SHOTS; do
        if python3 - "$shot" <<'EOF'
import sys
try:
    from PIL import Image, ImageStat
except ImportError:
    print("Pillow missing")
    sys.exit(2)
img = Image.open(sys.argv[1]).convert("RGB")
st = ImageStat.Stat(img)
mean = sum(st.mean) / 3
std = (sum(v * v for v in st.stddev) / 3) ** 0.5
print(f"enhance: {sys.argv[1]} mean={mean:.1f} std={std:.1f}")
sys.exit(0 if (mean > 25.0 and std > 20.0) else 1)
EOF
        then
          ENC_PASS=1
        fi
      done
      [ "$ENC_PASS" -eq 1 ] || { echo "E: enhancement blacked out the surface"; exit 1; }
    fi
    ;;
esac

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
echo "== db ground truth (chapters+mangas, best-effort) =="
$ADB shell "run-as $PKG ls /data/data/$PKG/databases/" || true
for suffix in "" "-shm" "-wal"; do
  $ADB shell "run-as $PKG cat /data/data/$PKG/databases/tachiyomi.db$suffix" > "$OUT/tachiyomi.db$suffix" 2>/dev/null || true
done
python3 - "$OUT/tachiyomi.db" <<'EOF' || true
import sqlite3, sys
try:
    db = sqlite3.connect(sys.argv[1])
    print("mangas:", db.execute("SELECT _id, title, favorite, source FROM mangas").fetchall())
    print("chapters:", db.execute("SELECT _id, manga_id, name, read FROM chapters").fetchall())
except Exception as e:
    print("db unreadable:", e)
EOF
if grep -A4 'FATAL EXCEPTION' "$OUT/logcat.txt" | grep -q "$PKG"; then
  echo "E: java crash during visual" >&2; exit 1
fi
[ "$FAIL" -eq 0 ] || { echo "E: maestro flow failed"; exit 1; }
echo "VISUAL PASS"
