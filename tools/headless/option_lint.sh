#!/bin/sh
# H1 option lint (§17.4-1): every startup option in §3.4/§6 must be accepted
# by the pinned desktop mpv. Usage: option_lint.sh <mpv-binary>
set -eu
MPV="${1:?mpv binary required}"
fail=0
for opt in vo gpu-context opengl-es hwdec ao video-sync gpu-shader-cache-dir \
    tls-verify tls-ca-file sub-fonts-dir osd-fonts-dir cache demuxer-max-bytes \
    demuxer-max-back-bytes save-position-on-quit scale dscale \
    correct-downscaling linear-downscaling sigmoid-upscaling \
    tone-mapping hdr-compute-peak volume-max sub-codepage sub-auto \
    sub-delay audio-delay glsl-shaders; do
  # mpv --list-options format varies by version (leading dashes/spacing);
  # match the bare name as a token anywhere on the line.
  if "$MPV" --list-options 2>/dev/null | grep -qE "(^|[[:space:]])--?$opt([[:space:]=]|$)"; then
    echo "option ok: $opt"
  else
    echo "option_lint: unknown option $opt" >&2
    fail=1
  fi
done
exit $fail
