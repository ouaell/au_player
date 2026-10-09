#!/bin/sh
# MS-067: fetches pinned OFL fonts into app/src/main/assets/fonts/ (bundled
# into the APK; installed to filesDir/fonts at first run for mpv).
# Refuses empty manifests and any non-200 response. Deterministic: every URL
# pins an upstream commit SHA.
set -eu
ROOT=$(pwd)
OUT="$ROOT/app/src/main/assets/fonts"
mkdir -p "$OUT"
count=0
while IFS='|' read -r name url; do
  case "$name" in
    ''|\#*) continue ;;
  esac
  case "$name" in
    *.ttf|*.otf) ;;
    *) echo "fetch_fonts: unexpected extension for $name" >&2; exit 1 ;;
  esac
  if [ -f "$OUT/$name" ]; then
    echo "fetch_fonts: cached $name"
  else
    curl -sfL --retry 3 -o "$OUT/$name" "$url"
    echo "fetch: $name"
  fi
  count=$((count + 1))
done < "$ROOT/tools/fonts/manifest.txt"
if [ "$count" -eq 0 ]; then
  echo "fetch_fonts: manifest produced zero files" >&2
  exit 1
fi
echo "fetch_fonts: $count files in $OUT"
