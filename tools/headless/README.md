# headless harness (H1–H4, §17.4) — scripts ready, execution queued

Runs on Linux CI under Xvfb + Mesa llvmpipe with a desktop mpv built from
the SAME pinned source as the Android build (mpv v0.41.0, `native/pins.txt`).
Correctness only — llvmpipe numbers never feed performance decisions.

- `option_lint.sh <mpv>` — H1: every §3.4/§6 option accepted.
- `shader_compile.sh <mpv> <patched-dir> <clip> <outdir>` — H2: each chain
  loads with no GLSL errors; `.hook` and `.glsl` handled.
- `check_golden.sh <rendered> <golden> [min-ssim]` — H3/H4: SSIM gate for
  enhancement outputs and subtitle renders (Latin, Arabic RTL, CJK, ASS).

Pending: a `headless.yml` job that builds desktop mpv v0.41.0 (meson +
system deps) and runs H1–H4 against `test-corpus/` + subtitle fixtures.
Exact mpv flags (`--gpu-context=x11egl` vs alternatives) are confirmed on
first execution; llvmpipe limits are stated, not discovered twice.
