# AUDIT — adversarial read-only audit (branch `audit/2026-10-10`, HEAD `d04e689`)

No product code changed. Tooling only: `tools/audit/`, `.github/workflows/audit-bughunt.yml`, `showcase.yml`.
Fresh CI re-runs were ATTEMPTED but the scheduler refused every job (account billing/spending-limit):
T1 `38070189258`, headless `38070192324`, smoke `38070197087`, emulator `38070199863`
(all `failure` in 3–6 s, annotation: "recent account payments have failed or your spending limit needs to be increased").
Native full rebuild was deliberately NOT re-run (multi-hour; see M).
Evidence below therefore = latest green runs on identical code HEAD (all 2026-10-10) + in-repo source.
`NO EVIDENCE` means exactly that.

Latest greens (identical HEAD): T1 `38021692373` (3m04s) · emulator `38017724352` (10/10) ·
headless `38021893683` (H1+H4) · smoke-release `38021902266` (minified R8).

## A. Owner requirements traceability

| Req | Implemented | Status | Evidence | Gap / Risk |
|---|---|---|---|---|
| R1 gestures seek | Partial (mapper pure, unwired) | UNIT-ONLY | `player/.../Gestures.kt:33-71` real; `PlayerScreen.kt:130-135` single-tap only; `GestureOverlay` defined `GestureOverlay.kt:43-50`, NO EVIDENCE of use | No brightness/volume/double-tap/long-press/pinch in player |
| R1 tracks + ext subs | Model only | UNIT-ONLY | `player/.../Tracks.kt:21-43`; NO UI, NO `aid/sid` send from UI | S2: cannot switch tracks |
| R1 sub style + delay | Props only | UNIT-ONLY | `player/.../SubtitleStyle.kt:15-38`; NO UI, NO `sub-*` send | S2: cannot style/delay subs |
| R1 audio delay/boost | Delay prop only | UNIT-ONLY | `audio-delay` prop exists; `volume-max=200` NO EVIDENCE in repo | Boost missing |
| R1 speed/A-B/sleep/loop | Logic only | UNIT-ONLY | `player/.../PlaybackModes.kt:5-80`; NO UI in `PlayerScreen.kt:139-183` | S2: unreachable |
| R1 lock/aspect/zoom/orient | Logic only | UNIT-ONLY | `Display.kt:3-70`, `PlayerState.kt:60`; NO buttons, NO `requestedOrientation`, NO zoom layer | Unreachable |
| R1 playlist/auto-next | Logic only | UNIT-ONLY | `player/.../Playlist.kt:10-47`; `PlayerScreen` opens single uri `:66-74` | No queue UI |
| R1 resume | Full | E2E-VERIFIED | E9, run `38017724352`: saved pos=1066, continue row shown | Row tap spawns new Activity (S4) |
| R1 folder browsing | None | MISSING | `library/.../Library.kt:8-9` `FakeLibrarySource.folders()=emptyList()`; "lands in MS-020" comment | No grid; SAF copy-to-inbox only |
| R1 PiP | Declarations only | UNVERIFIED | manifest `supportsPictureInPicture` `:23`; `PipCalc` + manual `enterAuPip()` `MainActivity.kt:60-67`; NO auto-enter/actions/callbacks | Behavior unproven |
| R1 background audio | None (stub SVC) | STUBBED | `AuPlaybackService.kt:11-12` `onGetSession()=null`; NO notification/FGS/focus/noisy wiring | S2: no background play |
| R1 sidecar subs | Matcher only | UNIT-ONLY | `player/.../Sidecar.kt:14-27`; NO MediaStore/SAF scan, NO `sub-codepage` | CP1256 etc. unhandled |
| R1 keep-screen-on | None | MISSING | pure `shouldKeepScreenOn()` only; NO `FLAG_KEEP_SCREEN_ON` call | Screen sleeps in player |
| R1 play/pause/seek/time | Full | E2E-VERIFIED | E8, run `38017724352`: freeze/resume proof, chrome diff 87806 | — |
| R1 lifecycle rotate/bg | Full | E2E-VERIFIED | E2 green same run | — |
| R1 controller a11y | Full | E2E-VERIFIED | E7 same run: 22+8 nodes, zero unlabeled | — |
| R2 Anime chain | Full (fixed) | E2E-VERIFIED | `enhance/.../ShaderChain.kt:29-33`; E3 green same run (real RAVU-class shader path) | llvmpipe≠phone GPU (see O) |
| R2 `//!WHEN` patch | Full | E2E-VERIFIED | `tools/patch_shaders.py:13,26-28` + `check_shaders.sh:19-32` gate in T1 | — |
| R2 supersampling runs | Partial | UNVERIFIED | Chain-list asserts only (`ShaderChainTest.kt:35-40`); NO render asserting 1080p→2160p internal | Behaves as designed, not pixel-proven |
| R3 Standard chain | Partial (RAVU, not FSRCNNX) | E2E-VERIFIED | `ShaderChain.kt:46` rung0=`ravu-lite-r4.hook`; E3 toggles it green | DEVIATION from plan (see M) |
| R3 license evidence | Partial | UNVERIFIED | `docs/assets-licenses.md:11-19` claims only; shader files gitignored, no SPDX in repo | In-repo proof NO EVIDENCE |
| R4 reader modes | Declared, paged-only wired | Partial | modes `core/.../Settings.kt:31-37`; `ReaderScreen.kt:80-86` uses ONLY `PagedReader`; `WebtoonReader`/`ZoomablePage` unwired | RTL/vertical/webtoon/double = S2 missing |
| R4 zoom/filters/crop | Logic only | UNIT-ONLY | `ReaderView.kt:59-79`, `ReaderUi.kt:40-67`; never applied in `ReaderScreen` | Unreachable |
| R4 progress memory | Model only | UNIT-ONLY | `library/.../ReaderHistory.kt:8-18`; `ReaderScreen` uses local `var current` | Lost on rotation |
| R4 chapters | Logic only | UNIT-ONLY | `reader/.../ChapterNav.kt:10-22`; never called | Missing |
| R4 CBZ/7z/folder render | Full (paged) | E2E-VERIFIED | E4 same run: pages=3, pixel proof | SAF-tree variant deferred |
| R5 PDF render | Basic full | E2E-VERIFIED | E4 same run: pages=1, white-fill, pixel proof | Tiles/cache: none (`size()=null`); password: NO dialog (throws); scanned detection: NO EVIDENCE |
| R6 inputs folders | Partial | Partial | SAF picker→inbox copy→VIEW `HomeScreen.kt:48-57`; FolderProvider(File) real; RAR omitted (per plan) | 7z real; content:// never passed to mpv (copied) |
| R6 intents | Full decl | E2E-VERIFIED | manifest filters `:29-58`; `IntentRouterTest`; VIEW flows exercised by E-suite | SEND flows: NO EVIDENCE exercised |
| R7 Web Source | Full-ish | E2E-VERIFIED | `WebScreen.kt:47-208`, extractor `websource/.../Extractor.kt:17-94`, downloader+cookies `WebSupport.kt:99-111`, hardening `WebPolicy.kt:9-33`; E6 green (ad-leak assert) | No reorder/advanced-selector UI; flag-gated |
| R8 image model | None (stub) | STUBBED | `enhance/.../Ncnn.kt:21-39` (`initVulkan()=false`, `inferTile` throws); NO weights in repo; `EnhanceService` `onBind()=null`; NO watchdog | Whole R8 unproven |
| R9 legal clean | Partial | Partial | LGPL recipe `native/apply_patches.py:13-23`; CI gates (`verify_native.sh`, allowlist, forbidden-grep) green in T1 `38021692373` | In-repo artifact proof NO EVIDENCE; notices JSON missing (see B) |
| R10 original design | Full | UNIT-ONLY | forbidden-grep clean (0 hits in `*/src/main`); tokens `§10` in theme | — |
| R11 l10n ar/fr/en | Skeleton | UNVERIFIED | 6/6/6 strings, 0 missing keys; `supportsRtl=true`; NO screenshot tests; 6 strings ≠ localized app | Barely started (M) |
| R12 settings list | Keys only | UNIT-ONLY | keys `core/.../Settings.kt:46-72` + DataStore; NO Settings screen (`Nav.kt:112` stub, unreachable) | S2: user cannot change anything |
| R13 monetization | Interface only | UNIT-ONLY | `AdProvider.kt:9-45` (no `nativeSlot()`); `NoOp` UNBOUND in release (`AppContainer` never refs it); NO ad SDK in gradle; consent stub | NoOp-by-absence, not by binding |
| R14 release ready | Partial | Partial | R8 on + keeps (`app/proguard-rules.pro`); targetSdk 36; 16 KB gated in CI; AAB task `release.yml:45`; signing debug-key default, NO keystore in repo | Signed AAB: NO EVIDENCE (release.yml never run); version 0.1.0-ms001 |
| R15 no content | Full | E2E-VERIFIED | 0 media/font/weight blobs in `*/src*`; corpus/fonts fetched, gitignored | — |

## B. Engine and native build

Pins (`native/pins.txt`): mpv `v0.41.0` (`:8`), FFmpeg `n9.0` (`:9`), dav1d `1.5.4` (`:10`),
libass `0.17.5` (`:11`), libplacebo `v7.360.1` (`:12`), NDK `30.0.16248370` (`:31`).

| Item | Finding |
|---|---|
| `.so` in repo per ABI | NONE — `player/src/main/jniLibs/` absent (gitignored `:18-19`); `*.so` repo-wide = 0 hits |
| FFmpeg `configuration:` verbatim | NO EVIDENCE (not recorded anywhere) |
| mpv `-Dgpl=false` / `--disable-gpl` | Recipe only: `native/apply_patches.py:13,16,20-23` + `native/README.md:16-19` + `docs/decisions.md:22,42,57-59` |
| Per-dep license table | `docs/native-licenses.md` (claims) + `docs/decisions.md` (rejected GPL AARs) |
| readelf ≥16 KB per ABI | Gated in CI (`check_16kb.sh`, `verify_native.sh:24-31`; T1 `38021692373` green) but artifacts are NOT committed → in-repo proof NO EVIDENCE |
| JNI binding | `MpvNative.load()` `player/.../MpvNative.kt:21-34` (`libmpv`+`au_mpv`); DI `AppContainer.kt:34-43`; R8 keeps `app/proguard-rules.pro:5,10-12,18-21` |
| Release loads real libs? | Only if staged: **else-branch binds `FakeMpvBridge()` in ALL variants** (`AppContainer.kt:41-42`) — STUBBED fallback path ships |
| Verdict | Engine REAL in CI (E1 green every emulator run); LGPL-clean PARTLY (recipe + CI string/alignment gates, no committed artifact proof, no FFmpeg config string) |

## C. Video enhancement

- Shipped files per mode: Anime rung0 = `Anime4K_Clamp_Highlights` → `Restore_CNN_M` → `Upscale_CNN_x2_M`;
  rung1 = Clamp + `Upscale_CNN_x2_S`; Standard rung0 = `ravu-lite-r4.hook` (`ShaderChain.kt:27-48`).
  Files are CI-fetched (gitignored); in-repo: manifest pins only (`tools/shaders/manifest.txt:4-8`;
  FSRCNNX REJECTED `:9`, FSR PENDING `:11`). SPDX in repo: NO EVIDENCE.
- Probe-first: math exists (`RungProbe.kt:11-31`) but ZERO production callers → only reactive governor
  (`Governor.kt:57-59`: ≥3 drops × 3 samples OR thermal ≥ MODERATE(2); never steps up).
- Compile-error handling: rung-fail persist/skip is policy-level; shader-log error scan on device: NO EVIDENCE.
- Hold-to-compare: `CompareHolder.kt:13-26` (snapshot/clear/restore; mid-hold change ignored). UI wiring: enhance sheet exists.
- Emulator proved: chains load + playback survives with shader active (E3; mpv-log GLSL-error scan on emulator: NO EVIDENCE).
  Desktop llvmpipe proved: option lint (H1), all-chain clean compile (H2), subtitle goldens (H4). Neither proves phone-GPU cost or visual quality delta.
- Golden provenance: bootstrapped BY the same `mpv --vo=image` pipeline they gate
  (`headless.yml` H4 bootstrap branch; `tools/headless/README.md`). `check_golden.sh` default 0.98, H4 invoked at 0.90.
  Human review: one progress note ("eyeballed RTL/bold", `docs/progress.md:137`). A same-renderer golden proves
  stability, NOT correctness — undisclosed in `docs/status.md`. Images: `tools/headless/goldens/sub-{latin,arabic,cjk,styled}.png`.

## D. Player system behavior

| Item | Tested how |
|---|---|
| Surface lifecycle (attach/detach, rotate, PiP-shaped) | E2E-VERIFIED (E2 rotate/bg + E1/E8 real frames, run `38017724352`); `wid` plumbing in code |
| PiP enter/leave | UNVERIFIED (manual `enterAuPip()` only; no test, no callback handling) |
| MediaSession/FGS + notification | STUBBED (`onGetSession()=null`); never started; permission declared |
| Audio focus / noisy-unplug | Pure policy UNIT-ONLY (`AudioFocus.kt:27-67`); NO `AudioManager`/receiver wiring |
| Process-death restore | NO EVIDENCE (no test; `save-position-on-quit=no`, resume is Room-based on dispose only) |
| configChanges (no recreate) | Declared (`AndroidManifest.xml:22`); behaviorally UNVERIFIED (E2 rotates — playback survived, activity path unknown) |
| Large-screen/multi-window/foldable | UNVERIFIED — MS-069 tagged but ZERO window-size tests in repo |
| Keep-screen-on / wake locks | MISSING — MS-070 tagged but no code and no tests |

## E. Reader internals

Own Compose viewers (Mihon extraction: did it happen? `tachiyomi.source|DatabaseHandler` grep clean per
`check_forbidden.sh`; viewer provenance: own `ReaderView.kt` — extraction-or-fallback outcome NOT recorded, see M).
`PagedReader` (HorizontalPager) + `WebtoonReader` (LazyColumn) + `ZoomablePage` (transformable + 2× double-tap) exist,
but `ReaderScreen` wires ONLY `PagedReader`. Gesture/zoom/scroll case tests: pure `ReaderUiTest` (tap zones, progress,
matrix) UNIT-ONLY. Memory: `MemoryBudget`/`Tiling` math UNIT-ONLY; 100-page strip / 300-page PDF harness: NO EVIDENCE run.
Known visual limits: no tiles (full-page 1080 px bitmaps), no LRU bitmap cache, filters/crop/modes unwired,
progress lost on rotation, no long-press menu, no slider, no chapter UI.

## F. NCNN

Stub. `JniNcnnBridge.init()` tries `loadLibrary("ncnn_jni")` else false; `initVulkan()=false` hardcoded;
`inferTile` throws (`Ncnn.kt:21-39`). Weights: none in repo, none fetched (no manifest entry).
No model family licensed/recorded beyond `MODEL_ID` const. Vulkan path: UNVERIFIED (hard-false).
CPU path: UNVERIFIED (throws). Isolation: manifest `:enhance` process declared, service body null;
`IsolationPolicy` counts (pure) but NO watchdog timer, NO timeout, NO crash hook. Time/page on emulator CPU:
NO EVIDENCE (never ran — nothing to run). MS-073 tag overstates (no forced-crash test).

## G. Web Source

`ExtractorTest` fixtures cover: lazy images, srcset, background-image, relative URLs, ad containers
(`ExtractorTest`, `CollectorParse` + E6 ad-leak assert). NOT covered: infinite-scroll SPAs beyond 15 s budget,
auth-walled pages, `picture>` exotic variants, non-UTF8 pages, JS-rendered-after-load galleries, 500-URL lists,
20%-failure resilience (downloader retries unit-tested via MockWebServer: redirects/403/wrong-type/timeouts).
Hardening checklist: `allowFileAccess=false` ✓, `allowContentAccess=false` ✓, mixed-content blocked ✓,
Safe Browsing ✓, popups blocked ✓, no JS interface ✓ (`WebPolicy.kt:24-33`, `WebSupport.kt:32-62`),
`intent:`/unknown schemes blocked ✓, first-use notice ✓, clear-data ✓. Cleartext: blocked by policy (http rejected).
`CookieManager` third-party: defaults (not asserted). `play` flavor exclusion: declared (`build.gradle.kts:26-33`).

## H. Ads

Linked SDK: NONE (no admob/applovin/ump in any gradle catalog — E2E-VERIFIED absent by `check_licenses` + inspection).
Flags: none (no build flag; module always compiled). Consent: `ConsentStatus` + `StubConsentManager→UNKNOWN` only.
`NoOpAdProvider` exists but is NOT bound anywhere in release DI — "NoOp default" is true by absence of any ad code,
not by binding. No ad code path can execute (nothing to call it).

## I. Manifest / permissions / intents

`app/src/main/AndroidManifest.xml`: permissions `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE(maxSdk32)`,
`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (`:5-11`).
`MainActivity exported=true` (`:21`); `AuPlaybackService exported=false` + `mediaPlayback` type + `MediaSessionService`
action (`:60-67`). Player VIEW filters: content/file/http/https for video/audio + HLS + DASH (`:29-45`).
Reader VIEW/SEND/SEND_MULTIPLE: image/*, zip/x-cbz/comicbook+zip, pdf, text/plain (`:47-58`).
`configChanges=orientation|screenSize|screenLayout|uiMode|smallestScreenSize|density|keyboardHidden` (`:22`),
`supportsPictureInPicture=true` (`:23`), `supportsRtl=true` (`:17`), `enableOnBackInvokedCallback=true` (`:18`).
Backup/`dataExtractionRules`: NO EVIDENCE (defaults apply — S3). `aapt2 dump` output itself: NO EVIDENCE
(never pasted; values above read from the manifest source, targetSdk 36 from `app/build.gradle.kts:15`).

## J. Build outputs

APK/AAB sizes per variant: NO EVIDENCE (never recorded; artifacts expire after 7 days).
ABI splits: NO EVIDENCE of splits config (single APK with staged `.so`). R8: on for `release` + `smoke`
(`app/build.gradle.kts:43-63`); release signing = debug key unless `AU_KEYSTORE_*` set (`:50-76`) —
NO keystore in repo (`docs/signing.md` = H8 owner recipe). versionCode 1 / versionName `0.1.0-ms001`.
`release.yml` (AAB + Play upload tasks) exists but was NEVER dispatched → signed AAB: NO EVIDENCE.
Download: CI artifacts of green runs (7-day retention; quota droughts observed).

## K. Test inventory

66 files / 221 JVM unit tests + 10 files / 10 instrumented tests. `@Ignore/@Disabled`: 0.
Kover/Jacoco: none. Jazzer/Kotest/Konsist: none in deps (MS-064 tag rests on hand-rolled equivalents —
`docs/progress.md:145` admits the fallback). Static in CI: Lint + detekt + ktlint only
(no CodeQL/Semgrep/dependency-check). StrictMode/LeakCanary: none in app code.

| Suite | Time (last green) | What it asserts (strongest assertion quoted) |
|---|---|---|
| T1 JVM+lint+gates | ~3–5 min (`38021692373`: 3m04s) | gates + 221 unit tests |
| Emulator E1–E9+Smoke | ~10–13 min (`38017724352`) | see rows below |
| Headless H1/H4 | ~1 min (`38021893683`) | option lint; SSIM 1.0 goldens |
| smoke-release | ~8 min (`38021902266`) | minified VO/reader asserts |
| E1 | — | `assertTrue("screenshots identical (frozen video?)", changed)` (`E1PlayerTest.kt:113`) |
| E2 | — | `assertTrue("playback died across rotate", waitForFreshPlayback(launchTime))` (`E2LifecycleTest.kt:48`) |
| E3 | — | `assertTrue("playback died with shader", waitForFreshPlayback(launchTime))` (`E3EnhanceTest.kt:51`) |
| E4 | — | `assertTrue("$what screen flat (no pages?)", variance > MIN_VARIANCE)` (`E4ReaderTest.kt:75`) |
| E5 | — | `assertTrue("playback died in seek storm", waitForFreshPlayback(launchTime))` (`E5SeekTest.kt:44`) |
| E6 | — | `assertTrue("ad pixel leaked in: $picked", picked.none { it.contains("ads.example") })` (`E6WebTest.kt:79`) |
| E7 | — | `assertTrue("unlabeled clickables on $screen: ...", unlabeled.isEmpty())` (`E7A11yTest.kt:125-127`) |
| E8 | — | `assertTrue("controls chrome did not render", diff > MIN_DIFF)` (`E8PlayerControlsTest.kt:135`) + `assertEquals("time advanced while paused", frozen, traceLines())` (`:53`) |
| E9 | — | `assertTrue("continue row missing", shown)` (`E9ResumeTest.kt:66`) |
| Smoke | — | `assertTrue(true)` (`SmokeTest.kt:19`) — trivial, asserts nothing (S3) |

Flaky-noted: E9 (fixed: app-scope save + seek-to-1.5s); UiAutomator dump wedging (worked around, dump-free E7/E8);
artifact-quota droughts (worked around, continue-on-error). E-tests bypass in-app Nav (all use `EXTRA_DEBUG_PATH`
or direct intents) → real user navigation paths (Home→Player via VIEW re-entry, Web→Reader) are NOT E-covered.

## L. Hidden stubs

Release-shipping stubs (severity): `AppContainer.kt:41-42` `FakeMpvBridge()` fallback — HIGH (silent fake engine
if `.so` missing; no user-visible error); `Nav.kt:93,99,109,112` dead `StubScreen`s — LOW (unreached);
`AuPlaybackService.kt:11-12` null session — HIGH if background play claimed (it isn't in UI);
`EnhanceService.kt:8-9` null binder — MEDIUM (R8 dead); `Ncnn.kt:32-36` hard-false/throw — MEDIUM (feature-gated off? NO gate — page-enhance UI path check needed);
`NoOpAdProvider`/`StubConsentManager` unbound — LOW (no callers). Test-only fakes (by design, §4): `MpvBridge.kt:31`,
`PlayerController.kt:10`, `EnhanceEngine.kt:9`, `Ncnn.kt:42`, `PageProvider.kt:16`, `WebSource.kt:8`, `Library.kt:8`.
Suspicious swallows: `ReaderScreen.kt:47` empty catch on `provider.close()`; `Nav.kt:76-77` sick-DB→empty list (by design,
logged? NO — silent); `DataStoreSettings.kt:120` bad-enum→null (silent). `TODO/FIXME/HACK`: 0 hits.

## M. Deviations and decisions

1. Standard = RAVU Lite R4, not plan's FSRCNNX (license-rejected: igv GPL-3.0, untraceable) — documented
   (`docs/assets-licenses.md`, manifest). Downgrade in ambition, honest in record. FSR "ladder" still pending.
2. `RungProbe` unwired (governor-only) vs `docs/progress.md` MS-028 DONE + `docs/status.md` now honest (UNVERIFIED).
3. MS-068 tagged with 6 strings/locale and ZERO screenshot tests. MS-069 tagged with ZERO window tests.
   MS-070 tagged with ZERO wakelock code/tests. MS-073 tagged with NO crash test. MS-065 tagged with NO self-test
   UI and NO emulator report (schema test only). Each tag's progress entry describes partial work — tags overstate.
4. Mihon-viewer extraction outcome (used vs fallback) NOT recorded anywhere (grep-clean only proves no tracker symbols).
5. `[VERIFY]` ledger: D1 prebuilts rejected ✓ (docs/decisions.md); mediacodec_embed avoided ✓; FSRCNNX rejected ✓;
   FSR port missing ✓ (pending); RAR omitted ✓; CJK font strategy (subset/on-demand) OPEN; sub-codepage OPEN;
   Play/Firebase country+quota numbers OPEN (now billing-blocked); Paparazzi/Roborazzi never adopted (no screenshot tests).
6. Audit's own deviations: no `MS-xxx` commit tag (not an MS task; branch `audit/2026-10-10` instead);
   native full rebuild NOT re-run (hours-long; verifier step re-run attempted via T1, billing-blocked);
   fresh-run rule complied as far as billing allows (attempt ids recorded); AUDIT.md kept ≤600 lines by compression.

## N. Known bugs, limitations, risks

| ID | Sev | One-line | Evidence |
|---|---|---|---|
| N1 | S2 | Settings/Diagnostics/Licenses unreachable from UI (stubs or dead defs) | `Nav.kt:93-113`, zero call-sites for the two screens |
| N2 | S2 | Track select, speed, A-B, sleep, sub-style, aspect, lock, PiP actions, background play: logic exists, NO UI | §A rows; `PlayerScreen.kt:139-183` has only play/seek/enhance/back |
| N3 | S2 | Reader modes/filters/zoom/chapters/progress: unwired (paged-only) | `ReaderScreen.kt:80-86` |
| N4 | S2 | Release binds Fake engine when `.so` absent, silently | `AppContainer.kt:41-42`; no error UI path |
| N5 | S3 | `SmokeTest` asserts nothing (`assertTrue(true)`) | `SmokeTest.kt:19` |
| N6 | S3 | Same-renderer subtitle goldens (stability ≠ correctness) | §C; `headless.yml` bootstrap branch |
| N7 | S3 | No backup rules; versionName `0.1.0-ms001`; sick-DB silent-empty | manifest grep NO EVIDENCE; `Nav.kt:76-77` |
| N8 | S3 | Artifact quota droughts; 7-day expiry (evidence rots) | observed CI behavior |
| N9 | S4 | Continue-row re-entry spawns new Activity; `Uri.fromFile` self-URI | `Nav.kt:85-90`, `HomeScreen.kt:54` |
| N10 | S4 | E-suite bypasses Nav (dead stubs never executed — lucky, not coverage) | all E-tests use debug/direct intents |
| Risks | — | Phone-GPU shader cost/thermal unknown; vendor decoder quirks; Vulkan driver crashes (no watchdog); WebView site breakage; store review of Web Source; HEVC patent exposure via bundled DAV1D? (dav1d is AV1-BSD; HEVC via platform — ok) | see O |

No S1 (crash/data-loss) is KNOWN — but monkey/negative/lifecycle passes have NEVER run (tooling added this audit;
runs billing-blocked), so "zero S1" is absence of evidence, not evidence of absence.

## O. Unverifiable without hardware

| Item | Emulator blind spot | Runtime mitigation present? |
|---|---|---|
| Shader cost/thermal on mobile GPU | swiftshader ≠ Adreno/Mali; llvmpipe correctness-only | YES: governor step-down + persisted rung-skip + 1440p gate |
| Vendor mediacodec quirks/green frames | google_apis image only | YES: `decoder=SOFTWARE` setting key + `EnhanceEdgeCases` fallback helper (wiring partial) |
| Vulkan driver crashes (NCNN) | no Vulkan on CI emulator path | PARTIAL: `:enhance` process declared; NO watchdog/timeout/crash-hook |
| Real touch feel/haptics | UiAutomator taps only | NO (no haptic code found) |
| Headset unplug / calls | no audio routing test | NO (no noisy receiver) |
| Doze/standby + background audio | no N/A (no background audio) | N/A |
| Per-device shader compile stalls | compile measured nowhere | YES: async "Preparing…" + shader cache dir (paths set; stall UX untested) |
| CJK glyph coverage on device | goldens use fetched Noto subset? CJK font NOT bundled | NO (CJK ships nothing; Arabic/Latin fetched OFL) |

## P. Go/No-Go

| # | Criterion | Verdict | Remains |
|---|---|---|---|
| 1 | Real engine LGPL-clean, E1 green on release variant | PARTIAL | E1 green on debug only; release-variant E1 never run; FFmpeg config string unrecorded; `.so` not in repo (CI-staged) |
| 2 | Anime AND Standard visibly differ, no GLSL errors | PARTIAL | H2 (run `38021893683`): Anime M-rung, S-rung, `ravu-lite-r4.hook` all "clean", "compiled without GLSL errors"; E3 proves both load + play on emulator; off-vs-enhanced visible-difference assert: NO EVIDENCE |
| 3 | Zero S1/S2 open | FAIL | S2s N1–N4 open; S1 unknown (no monkey) |
| 4 | No STUBBED in release | FAIL | Fake-engine fallback, null session/binder, NCNN throw-path, dead stubs |
| 5 | Signed AAB produced | FAIL | `release.yml` never dispatched; no keystore (H8) |
| 6 | Pre-launch report path ready | FAIL | No Play account/Firebase (billing failing); no device checklist run |

OVERALL: **NO-GO** for 1.0 store release. Shippable as a clearly-labelled GitHub **Beta** (per §17.8 fallback)
once N1 (settings reachable) is fixed OR settings scope is cut explicitly — provided CI billing is restored and
the audit bug-hunt + showcase jobs run green.
