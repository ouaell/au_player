# native/ — self-built LGPL libmpv (P0, OPENCODE_COMMAND §3)

We do NOT vendor upstream scripts (their repo is GPL-3.0): CI fetches the
official mpv-android `buildscripts` at a pinned commit, then applies OUR
patches (`apply_patches.py`, asserted exactly-once). The recipe (pins +
patches + workflow) is the build record required by §13.

## Pins (`pins.txt`)

mpv v0.41.0 + FFmpeg n9.0 + libplacebo v7.360.1 (needs ≥6.338.2 per mpv
meson) + dav1d 1.5.4 + libass 0.17.5 + official tarball versions for the
rest. NDK r30 (official `download.sh`), SDK platform 36.

## LGPL deltas vs upstream

1. FFmpeg: `--enable-{gpl,version3}` → `--disable-gpl` (no GPL-only
   external libs present: mjpeg/png encoders + mov/matroska/mpegts muxers
   only; x264/x265 absent).
2. mpv: add `-Dgpl=false` (mpv `gpl` meson option; disables cdda/dvdnav/
   jack/oss/caca/direct3d/x11 — none needed on Android; lua stays, MIT).
3. Sources pinned to tags (upstream clones master unpinned).
4. 16KB: inherited from upstream `loadarch`
   (`-Wl,-z,max-page-size=16384`); verified per-ABI in CI.

## Layout

- `pins.txt` — every version + upstream commit.
- `apply_patches.py` — exact-match patch applier (fails loudly on drift).
- `../.github/workflows/native.yml` — matrix build (arm64 first, then
  armv7l + x86_64), prefix caching, verify, artifacts → GitHub release.
- `../docs/native-licenses.md` — dependency license table.
