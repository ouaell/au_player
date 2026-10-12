# MASTER_STATE — single source of truth

## Clock
- Run start (T+0): 2026-10-11 ~03:45 UTC. Now: 2026-10-12 ~09:40 UTC.

## Phase
- C DONE and proven (playback). D IN PROGRESS (shader pack shipped; enhance flow proving).

## Done
- Phase A gates: agents+skills, visual.yml, dossier + gap-matrix, recon.
- legacy-v1 tag; native artifacts present (no rebuild).
- mihon-line promoted to main. Workflows are ours only: mihon-ci (build+unit),
  mihon-visual, visual.yml, spotless-fix.
- Rebrand, tabs, local scan + chapter sync, dep ports, crash-text rebrand.
- PHASE C PORT: player/ + enhance/ carried forward from legacy-v1 into the Mihon
  tree, repackaged to app.auplayer, legacy :core folded into new :aucore module.
  JNI entry points renamed; convention plugins used throughout.
- Real Videos browser (MediaStore, thumbnails, inline permission) -> PlayerActivity.
- PlayerActivity owns one PlayerEngine, persists resume points, releases on destroy.
- mihon-ci green: build + unit, **147 tests, 0 failed** (38215246961).
- **PLAYBACK PROVEN** on mihon-visual 38223901993 (VISUAL PASS): libmpv v0.41.0
  opened a MediaStore clip, source timecode advanced 00:00:00.000 f0 ->
  00:00:04.667 f112 between shots, frames provably differ (compare -metric AE).
- Two crashes found by the loop, not by reading code:
  S3-001 UnsatisfiedLinkError (startup options applied before native load),
  S3-002 android_content app context never set (content:// unresolvable).
- PHASE D: Anime4K x2 (M/S) + RAVU lite r4 fetched from pinned commit SHAs,
  patched deletions-only, gated by tools/check_shaders.sh; shipped as APK assets
  and staged to app-private storage by ShaderStager (hash-stamped, idempotent).

## CI run ids
- mihon-ci 38228175228 green. mihon-visual 38223901993 VISUAL PASS.
- mihon-visual 38228724592: 01-04 passed, 05 failed on flow order (fixed 599faf1).

## Next
1. Land 05-enhance green; view enhance screenshots; log in docs/visual-log.md.
2. Player does not rotate to landscape for 16:9 — real gap vs MX/REX.
3. Phase E: PDF loader, Web tab, NCNN image model, :enhance process isolation.
4. Standard mode rung 1 (FSR EASU/RCAS) still unimplemented — governor steps to Off.

## Open defects
- S2-001 WebViewActivity INTERNET crash — still present, UI not reachable.
- Browse/extension/tracker CODE still in tree (UI unreachable).
- Landscape orientation in the player.
- Standard-mode enhancement has only one rung (RAVU).

## Open risks
- Emulator is swiftshader: real hwdec/mediacodec and Vulkan paths unproven on
  hardware. Logcat already shows `h264_mediacodec: Both surface and native_window
  are NULL` before falling back to gpu+mediacodec-copy.
- Reader window is FLAG_SECURE by design; adb screencap is black there.
- GitHub API connectivity has been intermittent; CI dispatch may need retries.