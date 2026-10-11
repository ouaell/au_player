#!/bin/sh
# MS-063 release smoke (minified R8 build, debug-signed): install, play a
# clip + open a CBZ via VIEW intents, assert via logcat. No instrumentation:
# logcat needs no run-as, so scoped-storage/run-as flakiness cannot apply.
set -eu
adb_retry() {
  tries=0
  while [ $tries -lt 5 ]; do
    if "$@"; then return 0; fi
    tries=$((tries + 1))
    sleep 5
    adb kill-server || true
    adb start-server || true
    adb wait-for-device || true
  done
  return 1
}
sudo apt-get update -qq
sudo apt-get install -y -qq ffmpeg
sh tools/gen_corpus.sh test-corpus
python3 - test-corpus <<'EOF'
import sys
import zipfile
out = sys.argv[1]
with zipfile.ZipFile(f"{out}/r-pages.cbz", "w", zipfile.ZIP_DEFLATED) as archive:
    archive.write(f"{out}/e4-p1.png", "p1.png")
print("smoke: r-pages.cbz")
EOF
gradle :app:assembleDirectSmoke --stacktrace
APK=$(ls app/build/outputs/apk/direct/smoke/*.apk | head -1)
echo "smoke: release APK $APK"
. tools/audit/fg_check.sh
adb_retry adb install -r "$APK"
adb_retry adb push test-corpus/h264-720p.mp4 /data/local/tmp/r.mp4
adb_retry adb push test-corpus/r-pages.cbz /data/local/tmp/r-pages.cbz
adb logcat -c
adb shell am start -a android.intent.action.VIEW \
  -d file:///data/local/tmp/r.mp4 -t video/mp4 \
  -n com.example.auplayer/.MainActivity
sleep 12
assert_foreground com.example.auplayer
adb shell am start -a android.intent.action.VIEW \
  -d file:///data/local/tmp/r-pages.cbz -t application/vnd.comicbook+zip \
  -n com.example.auplayer/.MainActivity
sleep 8
assert_foreground com.example.auplayer
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/r-logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/r-logcat.txt"
echo "=== MS-063 release smoke gate ==="
grep -a 'AuPlayer' "$GITHUB_WORKSPACE/r-logcat.txt" | tail -20 || true
if ! grep -aq 'VO: \[gpu\]' "$GITHUB_WORKSPACE/r-logcat.txt"; then
  echo "E: no VO init in minified build (R8 broke mpv path?)" >&2
  exit 1
fi
if ! grep -aq 'E4GATE.*reader pages=' "$GITHUB_WORKSPACE/r-logcat.txt"; then
  echo "E: reader never rendered in minified build" >&2
  exit 1
fi
if grep -A4 'FATAL EXCEPTION' "$GITHUB_WORKSPACE/r-logcat.txt" | grep -q 'com.example.auplayer'; then
  echo "E: java crash in minified build" >&2
  exit 1
fi
if grep -B15 '>>> com.example.auplayer <<<' "$GITHUB_WORKSPACE/r-logcat.txt" | grep -aiqE 'signal [0-9]+|abort'; then
  echo "E: native crash in minified build" >&2
  exit 1
fi
echo "E: release smoke pass"
