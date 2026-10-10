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
sh tools/fetch_shaders.sh
sh tools/fetch_fonts.sh
python3 tools/patch_shaders.py
adb kill-server || true
adb start-server
adb wait-for-device
gradle :app:installDirectDebug --stacktrace
adb_retry adb push test-corpus/h264-720p.mp4 /data/local/tmp/e1.mp4
adb_retry adb push enhance/src/main/assets/shaders/patched/ravu-lite-r4.hook /data/local/tmp/e3-ravu.hook
adb_retry adb push test-corpus/e4-pages.cbz /data/local/tmp/e4-pages.cbz
adb_retry adb push test-corpus/e4-doc.pdf /data/local/tmp/e4-doc.pdf
adb_retry adb shell "run-as com.example.auplayer mkdir -p /data/data/com.example.auplayer/files"
adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e1.mp4 /data/data/com.example.auplayer/files/e1.mp4"
adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e3-ravu.hook /data/data/com.example.auplayer/files/e3-ravu.hook"
adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e4-pages.cbz /data/data/com.example.auplayer/files/e4-pages.cbz"
adb_retry adb shell "run-as com.example.auplayer cp /data/local/tmp/e4-doc.pdf /data/data/com.example.auplayer/files/e4-doc.pdf"
adb_retry adb shell "run-as com.example.auplayer ls -l /data/data/com.example.auplayer/files/e1.mp4"
TEST_FAILED=0
gradle :app:connectedDirectDebugAndroidTest --stacktrace || TEST_FAILED=1
echo "=== APK native libs ==="
unzip -l app/build/outputs/apk/direct/debug/*.apk | grep '\.so' || true
echo "=== androidTest results ==="
find app/build -name "*.xml" -path "*androidTest-results*" -exec grep -h "<testsuite " {} \; || true
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/logcat.txt"
# API 30+ scoped storage hides /sdcard/Android/data/<pkg>/ from adb shell
# ("No such file"), and `adb exec-out run-as` fails with "unknown package",
# while `adb shell "run-as ..."` works in this same script. So: shell-form
# run-as for everything; text trace redirects directly (line parsing is
# whitespace-tolerant), PNGs round-trip via base64 (shell mangles binary).
# App + test use internal filesDir (same UID, run-as readable).
# run-as CWD is not the data dir, so relative files/... misses (0-byte pulls);
# use absolute /data/data/... paths like the working push/clip steps above.
adb_retry adb shell "run-as com.example.auplayer cat /data/data/com.example.auplayer/files/e1-trace.txt" > "$GITHUB_WORKSPACE/e1-trace.txt" || true
adb_retry adb shell "run-as com.example.auplayer cat /data/data/com.example.auplayer/files/e1-shot-1.png | base64" > "$GITHUB_WORKSPACE/shot1.b64" || true
adb_retry adb shell "run-as com.example.auplayer cat /data/data/com.example.auplayer/files/e1-shot-2.png | base64" > "$GITHUB_WORKSPACE/shot2.b64" || true
python3 - "$GITHUB_WORKSPACE" <<'EOF'
import base64
import os
import sys
work = sys.argv[1]
for shot in ("e1-shot-1.png", "e1-shot-2.png"):
    src = os.path.join(work, "shot1.b64" if shot == "e1-shot-1.png" else "shot2.b64")
    try:
        with open(src, "rb") as handle:
            raw = b"".join(handle.read().split())
        with open(os.path.join(work, shot), "wb") as handle:
            handle.write(base64.b64decode(raw))
        print(f"decoded {shot}: {len(raw)} b64 chars")
    except Exception as error:  # missing/empty when cat failed; gate decides
        print(f"no {shot}: {error}")
EOF
echo "=== E1 trace gate (time-pos span + clean EOF, via logcat) ==="
echo "=== (device file pulls are best-effort: post-test run-as is unreliable;"
echo "===  the test dumps the same evidence to logcat under E1GATE) ==="
python3 - "$GITHUB_WORKSPACE/logcat.txt" <<'EOF'
import re
import sys
times = []
eof = False
shots = {}
with open(sys.argv[1], errors="replace") as handle:
    for raw in handle:
        line = raw.strip()
        if "E1GATE" not in line:
            continue
        time = re.search(r"trace time=([0-9.]+)", line)
        if time:
            try:
                times.append(float(time.group(1)))
            except ValueError:
                pass  # partial line from logcat rotation; ignore
        if re.search(r"trace eof\b", line):
            eof = True
        shot = re.search(r"shot([12]) mean=([0-9.\-eE]+) var=([0-9.\-eE]+)", line)
        if shot:
            try:
                shots[shot.group(1)] = (float(shot.group(2)), float(shot.group(3)))
            except ValueError:
                pass
assert times, "no time-pos in E1GATE logcat dump"
span = max(times) - min(times)
print(f"samples={len(times)} span={span:.2f}s eof={eof}")
assert span >= 2.0, f"span too small: {span}"
assert eof, "no clean EOF"
assert "1" in shots and "2" in shots, f"shot stats missing: {sorted(shots)}"
mean_delta = abs(shots["1"][0] - shots["2"][0])
var_delta = abs(shots["1"][1] - shots["2"][1])
print(f"shot deltas: mean={mean_delta:.3f} var={var_delta:.3f}")
assert mean_delta > 0.5 or var_delta > 0.5, "screenshots identical (frozen video?)"
print("E: trace gate pass")
EOF
adb_retry adb logcat -d > "$GITHUB_WORKSPACE/logcat.txt" || echo "logcat unavailable" > "$GITHUB_WORKSPACE/logcat.txt"
echo "=== E-evidence digest (artifact-free verification; full logcat.txt is best-effort) ==="
grep -a 'E[0-9]GATE' "$GITHUB_WORKSPACE/logcat.txt" | tail -80 || true
echo "--- player/mpv lines ---"
grep -a ' I AuPlayer: ' "$GITHUB_WORKSPACE/logcat.txt" | tail -40 || true
grep -a 'mpv \[' "$GITHUB_WORKSPACE/logcat.txt" | tail -30 || true
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
