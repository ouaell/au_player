#!/bin/sh
# E1 scenario (MS-060): corpus → install → push clip → connectedCheck →
# trace/screenshot/logcat gates. Runs INSIDE the emulator job (device up).
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
adb_retry adb shell "run-as com.example.auplayer mkdir -p /data/data/com.example.auplayer/files"
adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e1.mp4 /data/data/com.example.auplayer/files/e1.mp4"
adb_retry adb shell "run-as com.example.auplayer ls -l /data/data/com.example.auplayer/files/e1.mp4"
TEST_FAILED=0
gradle :app:connectedDirectDebugAndroidTest --stacktrace || TEST_FAILED=1
echo "=== APK native libs ==="
unzip -l app/build/outputs/apk/direct/debug/*.apk | grep '\.so' || true
echo "=== androidTest results ==="
find app/build -name "*.xml" -path "*androidTest-results*" -exec grep -h "<testsuite " {} \; || true
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/logcat.txt"
adb_retry adb pull /sdcard/Android/data/com.example.auplayer/files/e1-shot-1.png "$GITHUB_WORKSPACE/e1-shot-1.png" || true
adb_retry adb pull /sdcard/Android/data/com.example.auplayer/files/e1-shot-2.png "$GITHUB_WORKSPACE/e1-shot-2.png" || true
adb_retry adb pull /sdcard/Android/data/com.example.auplayer/files/e1-trace.txt "$GITHUB_WORKSPACE/e1-trace.txt" || true
echo "=== E1 trace gate (time-pos span + clean EOF) ==="
if [ ! -f "$GITHUB_WORKSPACE/e1-trace.txt" ]; then
  echo "E: no trace file — engine produced zero events" >&2
  exit 1
fi
python3 - "$GITHUB_WORKSPACE/e1-trace.txt" <<'EOF'
import sys
times = []
eof = False
with open(sys.argv[1]) as handle:
    for line in handle:
        line = line.strip()
        if line.startswith('time='):
            times.append(float(line[5:]))
        elif line == 'eof':
            eof = True
assert times, 'no time-pos in trace'
span = max(times) - min(times)
print(f'samples={len(times)} span={span:.2f}s eof={eof}')
assert span >= 2.0, f'span too small: {span}'
assert eof, 'no clean EOF'
print('E: trace gate pass')
EOF
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/logcat.txt"
# Package-scoped crash gate (unscoped greps catch GMS/system noise).
CRASHED=0
if grep -A4 'FATAL EXCEPTION' "$GITHUB_WORKSPACE/logcat.txt" | grep -q 'com.example.auplayer'; then
  echo "E: java crash in our process" >&2
  CRASHED=1
fi
if grep -q 'ANR in com.example.auplayer' "$GITHUB_WORKSPACE/logcat.txt"; then
  echo "E: ANR in our process" >&2
  CRASHED=1
fi
if grep -B15 '>>> com.example.auplayer <<<' "$GITHUB_WORKSPACE/logcat.txt" | grep -aiqE 'signal [0-9]+|abort'; then
  echo "E: native crash in our process" >&2
  CRASHED=1
fi
if [ "$CRASHED" -ne 0 ]; then exit 1; fi
echo "E: logcat clean"
exit $TEST_FAILED
