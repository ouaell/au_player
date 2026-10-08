# asset licenses (MS-026 owns the shader section; MS-046 adds model weights,
# MS-067 adds fonts). Code license ≠ weights license: both recorded.

## Video shaders (MIT, fetched pinned — see tools/shaders/manifest.txt)

- Anime4K (bloc97/Anime4K @ 7684e9586f8dcc738af08a1cdceb024cc184f426, MIT):
  - `Anime4K_Clamp_Highlights.glsl` (glsl/Restore)
  - `Anime4K_Restore_CNN_M.glsl` (glsl/Restore)
  - `Anime4K_Upscale_CNN_x2_M.glsl` (glsl/Upscale)
  - `Anime4K_Upscale_CNN_x2_S.glsl` (glsl/Upscale, governor degrade rung)
- Standard chain files (`FSRCNNX_x2_8-0-4-1`, FSR EASU/RCAS-style): wanted
  filenames only — NO source with a verified permissive license found yet
  (dotfile copies are not license evidence). Standard mode degrades to Off
  until a follow-up verifies and pins sources. This is the §16 fallback for
  the [VERIFY] in §6.3, not a silent swap: the chain definition already
  references the exact wanted filenames.
