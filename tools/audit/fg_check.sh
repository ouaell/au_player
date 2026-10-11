#!/bin/sh
# Anti-no-op guard (shared): after WE launch the app, the foreground window
# must belong to our package and its process must be alive. Fails otherwise.
# Usage: . tools/audit/fg_check.sh
#        assert_foreground com.example.auplayer
assert_foreground() {
  pkg="$1"
  focus=$(adb shell dumpsys window 2>/dev/null | grep -a mCurrentFocus | head -1 || true)
  echo "foreground: $focus"
  case "$focus" in
    *"$pkg"*) ;;
    *) echo "E: foreground window is not $pkg (app never came up?)" >&2; return 1 ;;
  esac
  pid=$(adb shell pidof "$pkg" 2>/dev/null | tr -d '\r' || true)
  if [ -z "$pid" ]; then
    echo "E: no live process for $pkg" >&2
    return 1
  fi
  echo "process: $pkg pid=$pid"
  return 0
}

# Polling variant for slow cold starts (first launch + onboarding on
# swiftshader can take a minute). Fails after $2 seconds (default 30).
wait_foreground() {
  pkg="$1"
  timeout_s="${2:-30}"
  waited=0
  while [ "$waited" -lt "$timeout_s" ]; do
    if assert_foreground "$pkg" >/dev/null 2>&1; then
      assert_foreground "$pkg"
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done
  echo "E: $pkg never foregrounded within ${timeout_s}s" >&2
  adb shell dumpsys window 2>/dev/null | grep -a mCurrentFocus | head -1 || true
  return 1
}

# Lighter variant for hostile inputs (system chooser/error UI may own the
# foreground): the process must still be alive.
assert_process() {
  pkg="$1"
  pid=$(adb shell pidof "$pkg" 2>/dev/null | tr -d '\r' || true)
  if [ -z "$pid" ]; then
    echo "E: no live process for $pkg" >&2
    return 1
  fi
  echo "process: $pkg pid=$pid"
  return 0
}
