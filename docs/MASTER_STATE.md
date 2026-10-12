# MASTER_STATE — single source of truth

## Clock
- Run start (T+0): 2026-10-11 ~03:45 UTC. Now: 2026-10-12 ~16:00 UTC (T+36h).

## Phase
- C DONE and proven (playback). D shipped and gated (shader chain reaches mpv).
- E/F/G NOT STARTED. The 6-hour target was not met; see "Honest status".

## Done
- Phase A gates: agents+skills, visual.yml, dossier + gap-matrix, recon.
- legacy-v1 tag; native artifacts present (no rebuild).
- mihon-line promoted to main. Workflows are ours only: mihon-ci (build+unit),
  mihon-visual, visual.yml, spotless-fix.
- Rebrand, tabs, local scan + chapter sync, dep ports, crash-text rebrand.
- PHASE C PORT: player/ + enhance/ carried forward from legacy-v1 into the Mihon
  tree, repackaged to app.auplayer, legacy :core folded into new :aucore module.
  JNI entry points renamed; Mihon convention plugins used throughout.
- Real Videos browser (MediaStore, thumbnails, inline permission) -> PlayerActivity.
- PLAYBACK PROVEN (mihon-visual 38223901993): libmpv v0.41.0 opened a MediaStore
  clip; burned-in timecode advanced 00:00:00.000 f0 -> 00:00:04.667 f112 and
  `compare -metric AE` proved the frames differ.
- PHASE D: Anime4K (Clamp/Restore/Upscale x2 M and S) + RAVU lite r4 fetched
  from pinned commit SHAs, patched deletions-only, gated by tools/check_shaders.sh,
  shipped as APK assets, staged to app storage by a hash-stamped ShaderStager.

## Bugs found by the loop (not by reading code)
- S3-001 UnsatisfiedLinkError: startup options applied before native load.
- S3-002 android_content app context unset: content:// URIs unresolvable.
- S3-003 shader path doubled its root segment: shaders never loaded.
- Controls auto-hid while paused/ended — a UX bug that also made the Enhance
  button unreachable; fixed and now covered by tests.

## CI run ids
- mihon-ci 38263924601 green (build + unit).
- mihon-visual 38272177419 SUCCESS, 5/5 flows, reader mean=119.0 std=88.3.
- Last unit total: 154 tests (was 147 before Phase D additions).

## Honest status
The 6-hour ask was not met — the work took roughly six times that in wall clock,
much of it spent waiting on CI (~8 min/visual run) and on repeated GitHub
network outages that had to be retried around.

What is genuinely done and proven: Mihon host, rebrand, tabs, local scan, reader,
MediaStore video browser, self-built LGPL libmpv playback, and an Anime4K/RAVU
enhancement chain that is selected, resolves real files and does not break
playback.

What is NOT done, and should not be claimed:
- Phases E, F, G entirely: PDF loader, Web browser/extractor, NCNN image
  enhancement, reader-side `:enhance` process isolation, settings consolidation,
  l10n ar/fr, tablet/RTL/a11y, self-test, ads shell, signed AAB.
- Enhancement QUALITY is unproven. The fixture is a synthetic test pattern and
  Anime4K targets line art; the gate proves wiring, not visual improvement.
- Hardware decode, Vulkan and real-device GPU behaviour are unproven: CI is
  swiftshader. Logcat shows h264_mediacodec failing before falling back.
- Landscape orientation in the player is still missing.
- Standard mode has exactly one rung (RAVU); FSR EASU/RCAS was rejected on
  licence grounds and is not implemented.
- S2-001 (WebViewActivity INTERNET crash) still present; its UI is unreachable.

## Next (in priority order)
1. Run a real anime clip on real hardware and compare enhanced vs raw frames.
2. Landscape orientation + a real settings surface for the player.
3. Phase E: PDF loader and Web tab.
4. Phase F/G: l10n, tablet/RTL/a11y, signed AAB, gap-matrix to zero.

## Open risks
- Emulator is swiftshader: hardware decode and Vulkan paths unproven.
- Reader window is FLAG_SECURE by design; adb screencap is black there.
- GitHub connectivity has been intermittent; CI dispatch needs retries.