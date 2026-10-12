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
      if python3 - "$shot" <<'EOF'
import struct, sys, zlib
path = sys.argv[1]
data = open(path, "rb").read()
assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
pos, w, h, ctype, idat = 8, 0, 0, None, b""
while pos < len(data):
    (ln,) = struct.unpack(">I", data[pos:pos + 4])
    typ = data[pos + 4:pos + 8]
    chunk = data[pos + 8:pos + 8 + ln]
    if typ == b"IHDR":
        w, h, bitd, ctype, _, _, _ = struct.unpack(">IIBBBBB", chunk)
    elif typ == b"IDAT":
        idat += chunk
    pos += 12 + ln
print(f"png: {w}x{h} bitdepth={bitd} ctype={ctype}")
assert bitd == 8, f"unexpected bit depth {bitd}"
assert ctype in (2, 6), f"unexpected color type {ctype}"
raw = zlib.decompress(idat)
ch = 3 if ctype == 2 else 4
stride = w * ch + 1
n = len(raw) // stride
step = max(1, n // 20000)
px = [raw[i * stride + 1:i * stride + 1 + ch] for i in range(0, n, step)]
lum = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in px]
mean = sum(lum) / len(lum)
var = sum((v - mean) ** 2 for v in lum) / len(lum)
print(f"reader: {path} {w}x{h} mean={mean:.1f} var={var:.1f}")
assert mean > 40.0, "frame too dark"
assert var > 30.0, "frame near-uniform"
print("E: reader pixels ok")
EOF
      then
        PASS=1
      fi
    done
    [ "$PASS" -eq 1 ] || { echo "E: no good reader frame among shots"; exit 1; }
    ;;
esac

echo "== diagnostic shot (always, even on flow failure) =="

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
