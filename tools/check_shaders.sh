#!/bin/sh
# MS-026 static gate (§6.2): every patched file has no OUTPUT-gated //!WHEN
# line, keeps its HOOK/BIND/DESC header, and is a deletions-only derivative
# of its upstream source.
set -eu
ROOT=$(pwd)
UP="$ROOT/enhance/src/main/assets/shaders/upstream"
PD="$ROOT/enhance/src/main/assets/shaders/patched"
fail=0
for patched in "$PD"/*.glsl "$PD"/*.hook; do
  [ -f "$patched" ] || continue
  base=$(basename "$patched")
  upstream="$UP/$base"
  if [ ! -f "$upstream" ]; then
    echo "check_shaders: missing upstream for $base" >&2
    fail=1
    continue
  fi
  if grep -q '^//!WHEN.*OUTPUT' "$patched"; then
    echo "check_shaders: $base still has OUTPUT-gated WHEN" >&2
    fail=1
  fi
  for tag in '//!HOOK' '//!BIND' '//!DESC'; do
    if ! grep -q "$tag" "$patched"; then
      echo "check_shaders: $base lost $tag header" >&2
      fail=1
    fi
  done
  if diff "$upstream" "$patched" | grep -q '^>'; then
    echo "check_shaders: $base has additions (must be deletions-only)" >&2
    fail=1
  fi
  echo "check_shaders: $base ok"
done
if [ "$fail" -ne 0 ]; then exit 1; fi
echo "check_shaders: all patched shaders pass"
