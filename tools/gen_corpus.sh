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
# T1 corpus is H.264-only (fast). HEVC-10, VP9 and HDR tags move to the T2
# corpus job where minutes don't block every push (MS-060).
# make_clip hevc-1080p-10bit.mkv -f lavfi -i "testsrc=size=1920x1080:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libx265 -preset ultrafast -pix_fmt yuv420p10le -c:a aac -shortest
# make_clip vp9-720p.webm -f lavfi -i "testsrc=size=1280x720:rate=30:duration=3" -f lavfi -i "sine=frequency=440:duration=3" -c:v libvpx-vp9 -b:v 1M -c:a libopus -shortest
# make_clip hdr-tagged.mp4 -f lavfi -i "testsrc=size=1280x720:rate=30:duration=2" -c:v libx264 -preset ultrafast -pix_fmt yuv420p -color_primaries bt2020 -color_trc smpte2084 -colorspace bt2020nc
(cd "$OUT" && sha256sum ./* > MANIFEST.sha256)
# Reader fixtures (E4): 3-page CBZ (testsrc PNGs, varied sizes) + minimal
# 1-page PDF (hand-built 1.4, Helvetica — valid xref, PdfRenderer opens it).
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "testsrc=size=800x1200:rate=1:duration=1" -frames:v 1 "$OUT/e4-p1.png"
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "testsrc=size=800x1200:rate=1:duration=2" -frames:v 1 "$OUT/e4-p2.png"
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "testsrc=size=1200x800:rate=1:duration=3" -frames:v 1 "$OUT/e4-p3.png"
python3 - "$OUT" <<'EOF'
import sys
import zipfile
out = sys.argv[1]
with zipfile.ZipFile(f"{out}/e4-pages.cbz", "w", zipfile.ZIP_DEFLATED) as archive:
    for page in ("e4-p1.png", "e4-p2.png", "e4-p3.png"):
        archive.write(f"{out}/{page}", page)
objects = []
objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")
objects.append(b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
objects.append(b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
               b"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>")
objects.append(b"<< /Length 45 >>\nstream\nBT /F1 24 Tf 100 700 Td (E4 test page) Tj ET\nendstream")
objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
pdf = bytearray(b"%PDF-1.4\n")
offsets = []
for number, body in enumerate(objects, start=1):
    offsets.append(len(pdf))
    pdf += f"{number} 0 obj\n".encode() + body + b"\nendobj\n"
xref_at = len(pdf)
pdf += f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n".encode()
for offset in offsets:
    pdf += f"{offset:010d} 00000 n \n".encode()
pdf += f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_at}\n%%EOF".encode()
with open(f"{out}/e4-doc.pdf", "wb") as handle:
    handle.write(bytes(pdf))
print("corpus: e4-pages.cbz + e4-doc.pdf")
EOF
(cd "$OUT" && sha256sum ./* > MANIFEST.sha256)
echo "corpus: $(ls "$OUT" | wc -l) files in $OUT"
