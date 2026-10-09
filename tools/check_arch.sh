#!/bin/sh
# MS-064 architecture gate (§17.6, dependency-free Konsist fallback):
# - UI layer (*Screen.kt, ui/**, Nav.kt, MainActivity.kt) never imports JNI
#   mpv objects or NCNN internals (AppContainer as DI root is the exception);
# - no network client in :player main;
# - no Thread.sleep in main sources (use coroutines).
set -eu
fail=0
ui_files=$(git ls-files 'app/src/main/java/**/ui/**' 'app/src/main/java/**/*Screen.kt' \
  'app/src/main/java/**/Nav.kt' 'app/src/main/java/**/MainActivity.kt')
if [ -n "$ui_files" ]; then
  # shellcheck disable=SC2086
  if grep -n -e 'MpvNative' -e 'MpvRender' -e 'MpvEvents' -e 'MpvSurface' \
      -e 'NcnnEngine' -e 'enhance\.Ncnn[^a-zA-Z]' $ui_files; then
    echo "check_arch: native class imported in UI layer" >&2
    fail=1
  else
    echo "check_arch: UI layer native-free"
  fi
fi
if git grep -n -I -e 'okhttp3' -e 'OkHttpClient' -- player/src/main/; then
  echo "check_arch: network client in :player" >&2
  fail=1
else
  echo "check_arch: :player network-free"
fi
if git grep -n -I -e 'Thread\.sleep' -- '*/src/main/'; then
  echo "check_arch: Thread.sleep in main sources" >&2
  fail=1
else
  echo "check_arch: no Thread.sleep in main sources"
fi
exit $fail
