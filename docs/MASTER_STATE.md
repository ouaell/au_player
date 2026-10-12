# MASTER_STATE — single source of truth

## Clock
- Run start (T+0): 2026-10-11 ~03:45 UTC. Now: 2026-10-12 ~07:05 UTC (T+27h30m).

## Phase
- B DONE and promoted to main. C IN PROGRESS (player shipped and building; visual proof running).

## Done
- Phase A gates: agents+skills, visual.yml, dossier + gap-matrix, recon.
- legacy-v1 tag; native artifacts present (no rebuild).
- mihon-line promoted to main. Upstream CI workflows removed; ours are
  mihon-ci (build + unit), mihon-visual, visual.yml, spotless-fix.
- Rebrand, tabs, local scan + chapter sync, dep ports, crash-text rebrand.
- PHASE C PORT: player/ + enhance/ carried forward from legacy-v1 into the
  Mihon tree as Gradle modules, repackaged com.example.auplayer -> app.auplayer,
  merged the legacy :core bits into a new :aucore module. JNI entry points
  renamed to match (Java_app_auplayer_player_*).
- Module wiring uses Mihon convention plugins (mihonx.plugins.android.library /
  compose / spotless); JUnit4 tests run via vintage engine under the platform.
- Real Videos browser: MediaStore query (:aucore/VideoRepository), thumbnail
  grid, inline READ_MEDIA_VIDEO ask, tap -> PlayerActivity.
- PlayerActivity owns one PlayerEngine per instance, applies startup options,
  persists resume positions, releases on destroy.
- Native libs staged from pinned release native-20261008 (3 ABIs + libc++).
- mihon-ci GREEN 38215246961: build=success, unit=success, 147 tests 0 failed.

## CI run ids
- mihon-ci 38215246961 green (build + unit). Earlier green: 38213864296.
- spotless-fix 38214578230 green, committed 49 reformatted files as 968bb34.
- mihon-visual 38215976112 running (videos flow + player pixel gate).

## Next
1. Land the visual gate result; view every screenshot and log it in
   docs/visual-log.md. A player frame must show real pixels, not black.
2. Phase D: wire enhancement controls into the real player (EnhanceController
   is ported but PlayerActivity passes an unconfigured controller), then the
   Anime4K/FSR shader pack + NCNN rung probe.
3. Phase E: PDF loader, Web tab, :enhance process isolation for reader.

## Open defects
- S2-001 (legacy WebView INTERNET crash) — still present in WebViewActivity.
- Browse/extension/tracker CODE still in tree (UI unreachable; deeper deletion later).
- PlayerActivity passes a bare EnhanceController: enhancement is inert in the
  app until wired (Phase D).

## Open risks
- Emulator is swiftshader: real hwdec/mediacodec and Vulkan paths stay
  unproven until a physical device run.
- Reader window is FLAG_SECURE by design; adb screencap is black there.