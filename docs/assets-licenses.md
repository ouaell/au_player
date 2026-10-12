# asset licenses (MS-026 owns the shader section; MS-046 adds model weights,
# MS-067 adds fonts). Code license â‰  weights license: both recorded.

## Video shaders (MIT, fetched pinned â€” see tools/shaders/manifest.txt)

- Anime4K (bloc97/Anime4K @ 7684e9586f8dcc738af08a1cdceb024cc184f426, MIT):
  - `Anime4K_Clamp_Highlights.glsl` (glsl/Restore)
  - `Anime4K_Restore_CNN_M.glsl` (glsl/Restore)
  - `Anime4K_Upscale_CNN_x2_M.glsl` (glsl/Upscale)
  - `Anime4K_Upscale_CNN_x2_S.glsl` (glsl/Upscale, governor degrade rung)
- Standard chain (P1.1 verdict, FSRCNNX rejected):
  - `ravu-lite-r4.hook` â€” bjin/mpv-prescalers @ b3f0a59 (LGPL-3.0, license
    text verified in-file + repo LICENSE; compute-capable GLES path,
    device-verified in H2/E3 with error fallback).
  - FSRCNNX: REJECTED â€” only traceable source (igv/FSRCNN-TensorFlow) is
    GPL-3.0; circulating .glsl provenance untraceable.
  - FSR1 EASU/RCAS rung: AMD FidelityFX-FSR source is MIT (verified), but no
    mpv-ready port found (agyild/FSR does not exist). Follow-up: write our
    own thin port from ffx_fsr1.h; until then Standard ladder is RAVU â†’ Off.

## Subtitle fonts (OFL, fetched pinned — see tools/fonts/manifest.txt)

- NotoSans.ttf — google/fonts NotoSans[wdth,wght].ttf @ 2984c57 (OFL, variable font; Latin coverage device-verified in H4 golden).
- NotoNaskhArabic.ttf — google/fonts NotoNaskhArabic[wght].ttf @ 35ddfc5 (OFL; Arabic RTL shaping device-verified in H4 golden).
- CJK: intentionally NOT bundled (Noto CJK ~16 MB/weight). Strategy: Android system fallback (present on all GMS devices). Headless runner has no CJK font, so the H4 CJK golden shows .notdef boxes — correct given the runner, not a product gap.
