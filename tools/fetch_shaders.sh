#!/bin/sh
# MS-026: fetches pinned shader sources into enhance/src/main/assets/shaders/upstream/.
# Refuses PENDING (commented) entries and any non-200 response. Deterministic:
# every URL pins an upstream commit SHA.
set -eu
ROOT=$(pwd)
OUT="$ROOT/enhance/src/main/assets/shaders/upstream"
mkdir -p "$OUT"
count=0
while IFS='|' read -r name url; do
  case "$name" in
    ''|\#*) continue ;;
  esac
  echo "fetch: $name"
  curl -sfL --retry 3 -o "$OUT/$name.glsl" "$url"
  count=$((count + 1))
done < "$ROOT/tools/shaders/manifest.txt"
if [ "$count" -eq 0 ]; then
  echo "fetch_shaders: manifest produced zero files" >&2
  exit 1
fi
echo "fetch_shaders: $count files in $OUT"
