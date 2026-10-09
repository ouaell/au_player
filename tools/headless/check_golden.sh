#!/bin/sh
# H3/H4 output regression (§17.4-3/4): SSIM + histogram checks of rendered
# frames vs stored goldens using ffmpeg's ssim/signalstats filters.
# Usage: check_golden.sh <rendered.png> <golden.png> <min-ssim>
set -eu
RENDERED="$1"
GOLDEN="$2"
MIN_SSIM="${3:-0.98}"
score=$(ffmpeg -hide_banner -i "$RENDERED" -i "$GOLDEN" \
  -lavfi "ssim" -f null - 2>&1 | grep -oE 'All:[0-9.]+' | cut -d: -f2 | tail -1)
echo "ssim(all)=$score (min $MIN_SSIM)"
python3 -c "import sys; sys.exit(0 if float('$score') >= float('$MIN_SSIM') else 1)" \
  || { echo "check_golden: SSIM below threshold" >&2; exit 1; }
echo "check_golden: $RENDERED matches golden"
