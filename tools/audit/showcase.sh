#!/bin/sh
# Showcase capture (owner has no device): scripted intents + screencaps +
# one screen recording on the CI emulator. Best-effort per shot; a missing
# shot is recorded in showcase/STATUS.txt, never fatal.
set -eu
PKG="${PKG:-com.example.au_player}"
OUT="${OUT:-showcase}"
mkdir -p "$OUT"
ADB="adb -s emulator-5554"
shot() { # $1=name; best-effort screencap pull
  if $ADB exec-out screencap -p > "$OUT/$1.png" 2>/dev/null && [ -s "$OUT/$1.png" ]; then
    echo "OK $1" >> "$OUT/STATUS.txt"
  else
    echo "MISS $1" >> "$OUT/STATUS.txt"
    rm -f "$OUT/$1.png"
  fi
}
: > "$OUT/STATUS.txt"
echo "== waiting for device =="
$ADB wait-for-device
$ADB shell input keyevent 82 || true
APK=$(ls app/build/outputs/apk/direct/debug/*.apk | head -1)
$ADB install -r "$APK"
sh tools/gen_corpus.sh showcase-corpus >/dev/null 2>&1 || true
CLIP=$(ls showcase-corpus/*.mp4 2>/dev/null | head -1 || true)
CBZ=$(ls showcase-corpus/*.cbz showcase-corpus/*.zip 2>/dev/null | head -1 || true)
[ -n "$CLIP" ] && $ADB push "$CLIP" /sdcard/showcase.mp4 || true
[ -n "$CBZ" ] && $ADB push "$CBZ" /sdcard/showcase.cbz || true

echo "== home (en, dark) =="
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/.MainActivity"
sleep 4
shot home-en-dark

echo "== player with controls (tap center to show) =="
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/showcase.mp4 -t video/mp4 "$PKG/.MainActivity"
sleep 6
$ADB shell input tap 720 1280
sleep 1
shot player-controls-en-dark

echo "== screen recording: playback ~25 s =="
$ADB shell screenrecord --time-limit 25 /sdcard/showcase-play.mp4 &
REC=$!
sleep 27
wait $REC || true
$ADB pull /sdcard/showcase-play.mp4 "$OUT/showcase-play.mp4" || echo "MISS showcase-play" >> "$OUT/STATUS.txt"

echo "== reader: cbz + pdf =="
$ADB shell am start -a android.intent.action.VIEW -d file:///sdcard/showcase.cbz -t application/x-cbz "$PKG/.MainActivity"
sleep 5
shot reader-cbz-en-dark
$ADB shell input keyevent 4
sleep 1

echo "== web source =="
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/.MainActivity"
sleep 3
shot home-2-en-dark

echo "== arabic locale (best-effort, needs adb root) =="
$ADB root || true
$ADB shell "setprop persist.sys.locale ar-EG; stop; start" || true
sleep 15
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/.MainActivity"
sleep 4
shot home-ar-dark
$ADB shell "setprop persist.sys.locale en-US; stop; start" || true
sleep 15

echo "== self-test report (best-effort) =="
$ADB shell run-as "$PKG" cat files/self-test.json > "$OUT/self-test.json" 2>/dev/null \
  || echo "MISS self-test.json (no in-app self-test UI at this HEAD)" >> "$OUT/STATUS.txt"
cat "$OUT/STATUS.txt"
