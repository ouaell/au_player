#!/bin/sh
# H2 shader compile test (§17.4-2): every chain loads under Xvfb+Mesa with no
# GLSL errors. Usage: shader_compile.sh <mpv> <patched-dir> <clip> <outdir>
# llvmpipe proves correctness only, never performance.
set -eu
MPV="$1"
SHADERS="$2"
CLIP="$3"
OUT="$4"
mkdir -p "$OUT"
export DISPLAY="${DISPLAY:-:99}"
fail=0
for chain in "Anime4K_Clamp_Highlights Anime4K_Restore_CNN_M Anime4K_Upscale_CNN_x2_M" \
             "Anime4K_Clamp_Highlights Anime4K_Upscale_CNN_x2_S" \
             "ravu-lite-r4.hook"; do
  # shellcheck disable=SC2086
  set -- $chain
  name=$(echo "$chain" | tr ' ' '+')
  shaders=""
  for file in "$@"; do
    case "$file" in
      *.hook|*.glsl) shaders="$shaders --glsl-shader=$SHADERS/$file" ;;
      *) shaders="$shaders --glsl-shader=$SHADERS/$file.glsl" ;;
    esac
  done
  log="$OUT/$name.log"
  # shellcheck disable=SC2086
  xvfb-run -a "$MPV" --no-config --vo=gpu --gpu-context=x11egl --opengl-es=yes \
    --hwdec=no --ao=null $shaders --frames=3 -o "$OUT/$name.mkv" "$CLIP" \
    --msg-level=all=warn --log-file="$log" || true
  if grep -aiE 'shader.*(error|failed)|glsl.*error|link.*fail' "$log"; then
    echo "shader_compile: GLSL errors in $name" >&2
    fail=1
  elif [ ! -s "$OUT/$name.mkv" ]; then
    # Non-empty output proves frames actually rendered through the chain
    # (a crashed mpv leaves nothing; the || true above must not hide that).
    echo "shader_compile: empty/missing output for $name" >&2
    fail=1
  else
    echo "shader_compile: $name clean"
  fi
done
exit $fail
