#!/bin/sh
# E1 scenario (MS-060): corpus → install → push clip → connectedCheck →
# screenshots/logcat gates. Runs INSIDE the emulator job (device up).
# Single-purpose file because the emulator action mangles multi-line
# inline scripts (each line via sh -c breaks functions and if-blocks).
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
adb kill-server || true
adb start-server
adb wait-for-device
gradle :app:installDirectDebug --stacktrace
  adb_retry adb push test-corpus/h264-720p.mp4 /data/local/tmp/e1.mp4
  adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e1.mp4 /data/data/com.example.auplayer/files/e1.mp4"
  adb_retry adb shell "run-as com.example.auplayer ls -l /data/data/com.example.auplayer/files/e1.mp4"
TEST_FAILED=0
gradle :app:connectedDirectDebugAndroidTest --stacktrace || TEST_FAILED=1
echo "=== androidTest results ==="
find app/build -name "*.xml" -path "*androidTest-results*" -exec grep -h "<testsuite " {} \; || true
adb_retry adb pull /sdcard/Android/data/com.example.auplayer/files/e1-shot-1.png "$GITHUB_WORKSPACE/e1-shot-1.png" || true
adb_retry adb pull /sdcard/Android/data/com.example.auplayer/files/e1-shot-2.png "$GITHUB_WORKSPACE/e1-shot-2.png" || true
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/logcat.txt"
if grep -aiE 'FATAL EXCEPTION|native crash|ANR in' "$GITHUB_WORKSPACE/logcat.txt"; then
  echo "E: crash/ANR in logcat" >&2
  exit 1
fi
echo "E: logcat clean"
exit $TEST_FAILED
