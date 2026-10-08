#!/bin/sh
# MS-058 test media corpus: synthetic clips only (testsrc+sine, no
# third-party content). Fast presets, tiny durations — keeps T1 under budget.
# Outputs dir + manifest with sha256; uploaded as a CI artifact for T2/T3.
set -eu
OUT="${1:-test-corpus}"
mkdir -p "$OUT"
make_clip() {
  name="$1"; shift
  if [ ! -f "$OUT/$name" ]; then
    # shellcheck disable=SC2086
    ffmpeg -hide_banner -loglevel error -y $* "$OUT/$name"
  fi
  echo "corpus: $name"
}
# 480p/720p/1080p H.264 + AAC in MP4
make_clip h264-480p.mp4 -f lavfi -i "testsrc=size=854x480:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac -shortest
make_clip h264-720p.mp4 -f lavfi -i "testsrc=size=1280x720:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac -shortest
make_clip h264-1080p.mp4 -f lavfi -i "testsrc=size=1920x1080:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac -shortest
# HEVC 10-bit + VP9 in MKV/WebM
make_clip hevc-1080p-10bit.mkv -f lavfi -i "testsrc=size=1920x1080:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libx265 -preset ultrafast -pix_fmt yuv420p10le -c:a aac -shortest
make_clip vp9-720p.webm -f lavfi -i "testsrc=size=1280x720:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libvpx-vp9 -b:v 1M -c:a libopus -shortest
# AV1 is T2-only (libaom is too slow for the T1 budget even at 2 s 480p).
# make_clip av1-480p.mp4 -f lavfi -i "testsrc=size=854x480:rate=24:duration=2" -c:v libaom-av1 -crf 30 -b:v 0 -cpu-used 8 -shortest
# HDR10-tagged clip (tags only; tone-mapping path exercise)
make_clip hdr-tagged.mp4 -f lavfi -i "testsrc=size=1280x720:rate=30:duration=2" -c:v libx264 -preset ultrafast -pix_fmt yuv420p -color_primaries bt2020 -color_trc smpte2084 -colorspace bt2020nc
(cd "$OUT" && sha256sum ./* > MANIFEST.sha256)
echo "corpus: $(ls "$OUT" | wc -l) files in $OUT"
