# progress — evidence log (MS-001 owns this file's creation)

## MS-001 — Repo, Gradle KTS + version catalog, modules + flavors, SDK 36/26
- Status: DONE
- Sections used: §2, §4, §14, §15 (MS-001 row), §17.1 (T1)
- Placeholder identity (H4 undecided): AU Player / com.example.auplayer
- Flavors: direct (WEB_SOURCE_ENABLED=true), play (false)
- ABIs: release arm64-v8a + armeabi-v7a; debug adds x86_64 for CI emulator (§2, §17.2)
- Toolchain (verified in CI): AGP 8.13.2 + Gradle 8.13 + Kotlin 2.4.20 + JDK 17, compileSdk/targetSdk 36, minSdk 26
- CI: android-t1 green, run id 37686518140 (assembleDirectDebug, artifact direct-debug-apk)
- Fixes during task: preinstalled-runner-SDK install; kotlinOptions→compilerOptions (KGP 2.4); compose runtime added to :player/:reader/:library
- Tests added: none (scaffold only; JVM/unit gates land in MS-002)
- UNVERIFIED-on-device: everything (no device session yet; install + first T3 at Day-1 gate)
- Files changed: 34 scaffold files (settings, root build, version catalog, 7 module builds, manifest, MainActivity/AuApp/AppContainer/Nav/Tokens, 6 module stubs, AGENTS.md, MASTER_PLAN.md, docs, T1 workflow)

## MS-002 — CI gates: lint, detekt, ktlint, unit tests, license allowlist, 16KB, forbidden grep
- Status: DONE
- Sections used: §12.3, §15 (MS-002 row), §17.1 (T1), §17.5 (16KB minimal; full verifier in MS-062), §13 (allowlist basis)
- CI: android-t1 green, run id 37704945211 (2m3s): detekt + ktlintCheck + test + lint + assembleDirectDebug + 16KB all pass
- Fixes during task: forbidden/allowlist self-match exclusions; detekt.yml validity (buildUponDefaultConfig is Gradle-side); FunctionNaming @Composable ignore; MagicNumber named consts; SCREAMING consts; ELF header 13-field unpack
- detekt 1.23.8 (stable 1.x; 2.0 still pre-release) + ktlint-gradle 14.2.0, both version-pinned in catalog
- Tests added: 6 JVM unit tests (Diag, FakePlayerController, FakeEnhanceEngine, FakePageProvider, FakeWebSourceExtractor, FakeLibrarySource), all green; :app has no pure-logic unit yet (needs Context/Robolectric, T2)
- Tokens.kt renamed to §10 lowercase semantic names (also satisfies detekt TopLevelPropertyNaming); file-level MagicNumber suppress with rationale
- UNVERIFIED-on-device: everything (unchanged)

## MS-003 — feature-spec.md + decisions.md skeleton
- Status: DONE (partial: H2 screenshots missing, spec derived from plan text)
- Sections used: §5, §7.4, §8, §10, §15 (MS-003 row)
- `docs/feature-spec.md` lists every screen/control/setting with our own names; open checklist tracks H2 validation
- decisions.md skeleton already exists (MS-001); D1 audit itself is MS-004
- Tests added: none (docs only; forbidden-grep excludes docs/)
- UNVERIFIED-on-device: everything (unchanged); plus spec-vs-screenshots validation open

## MS-004 — D1 libmpv audit & integration decision
- Status: DONE (verdict: build ourselves, option 4)
- Sections used: §3.1, §3.2, §3.4, §13, §15 (MS-004 row)
- CI: d1-audit run 37710115959 green (report artifact); android-t1 green on same push
- **[VERIFY] results:** jdtech 1.0.0 REAL; mehiz 0.1.12 REAL; SnowMediaEnt DOES NOT EXIST
- **Facts:** both ship `--enable-gpl --enable-version3` FFmpeg, no `-Dgpl=false` evidence → not LGPL-clean; both misaligned on armeabi-v7a/x86 → fail 16KB; both have x86_64; JNI pkgs recorded for MS-053
- **Consequence:** no third-party AAR in `:player` (would violate §13 + 16KB gate); self-build is now the critical path (follow-up task); audit script reused for acceptance
- Tests added: none (report-only audit; gates unchanged and green)
- UNVERIFIED-on-device: everything (unchanged)

## MS-005 — Design tokens, theme, base components
- Status: DONE (stacked; shared green run 37712790342)
- Sections used: §10, §15 (MS-005 row)
- Added Type.kt (12/14/16/20/28/36), Spacing.kt (4–32 + card 20/sheet 28), Shape.kt, AuComponents (AuChip, AuGlassPanel); AuTheme wires color/type/shapes
- Tests added: ThemeTokensTest (palette/spacing/type exactness, JVM)
- Screenshot gate: deferred to T2 per §17.1 (screenshot tool itself is [VERIFY] in MS-057); no screenshots in T1 by design
- T1 workflow: concurrency cancel-in-progress (sprint hygiene)

## MS-006 — Navigation shell, predictive back, destinations
- Status: DONE (stacked; shared green run 37712790342)
- Sections used: §2 (predictive back), §4, §15 (MS-006 row)
- NavDest sealed (home/player/reader/web/settings) + pure Navigator (JVM-tested back-stack) + NavHost wiring; predictive back via manifest flag
- Tests added: NavigatorTest (root/deep/unwind)

## MS-007 — PlayerEngine singleton + PlayerController + Fake
- Status: DONE (stacked; shared green run 37712790342)
- Sections used: §3.4 (singleton/dispatcher), §4 (interface+fake), §5.2 (events), §15 (MS-007 row)
- MpvBridge seam (init/options/command/events/attach/detach) + FakeMpvBridge; PlayerEngine (single-thread dispatcher, SharedFlow events, open/play/pause/seek/attach/detach/release); EnginePlayerController; AppContainer owns the single engine (Fake-backed until self-built AAR)
- Fixes: executor asCoroutineDispatcher; deterministic poll-based event test
- Tests added: PlayerEngineTest (5 tests, all green)

## MS-008 — mpv init options, CA/fonts/shader-cache paths, log to Diag
- Status: DONE (stacked; shared green run 37712790342)
- Sections used: §3.4, §15 (MS-008 row)
- MpvPaths + startupOptions (exact §3.4 map) + ensureMpvDirs + applyStartup (options in order, one Diag line); Diag gains bounded record/entries/clear (200)
- Fixes: MpvStartup.kt renamed MpvPaths.kt (MatchingDeclarationName)
- Tests added: MpvStartupTest (option exactness, dir creation, order+log, store bound)
- UNVERIFIED-on-device: everything (all playback/rendering needs device or self-built native + emulator suite)

## MS-009 + MS-010 — Surface lifecycle + source resolution (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.1, §5.2, §7.2 (intents), §15 (rows)
- MS-009: SurfaceHost guard (create→attach, destroy→detach, recreate→re-attach, null-safe); FakeMpvBridge attach/detach counters; SurfaceHostTest 6-case matrix
- MS-010: MediaSource sealed + PlayerError taxonomy + pure SourceResolver (split for complexity/return gates) + PlayerOpener; content:// fd resolution deferred to device testing; SourceResolverTest 10 tests

## MS-011 + MS-012 — State machine/UiState + controller UI logic (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.2, §15 (rows)
- MS-011: PlayerState enum + PlayerStateMachine (illegal throws) + immutable PlayerUiState; PlayerStateMachineTest incl. 8 illegal pairs
- MS-012: AutoHide + formatTime (pure, JVM-tested) + thin PlayerControls composable (T2 Compose tests deferred; text glyphs interim — icons land in MS-074); compose-icons dep added (allowlisted)

## MS-015 + MS-016 — Display modes + playback modes (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.1, §15 (rows)
- MS-015: AspectMode cycle/size, OrientationPref resolution, ZoomState; DisplayTest (fixed wrong width expectations)
- MS-016: speed presets/clamp/cycle, ABRepeat (smart-cast fix), LoopMode/EofAction, SleepTimer (injected clock); PlaybackModesTest

## MS-019 + MS-020 + MS-021 — Resume, permissions, playlist (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.2, §5.4, §15 (rows)
- MS-019: ResumeStore + InMemoryResumeStore (continue-watching filter/sort/limit) + Room entity/DAO/database compile-checked (DAO tests = T2/Robolectric); HistoryTest
- MS-020: permission mapping (33+/≤32) + Android 14 partial flag; MediaStore/SAF bodies deferred to device; PermissionsTest
- MS-021: Playlist reusing afterEof (ALL wraps, ONE replays); PlaylistTest

## MS-024 + MS-025 — Settings + diagnostics (stacked, Day-1 close)
- Status: DONE (shared green run 37716823563)
- Sections used: §10 (final list), §15 (rows)
- MS-024: AppSettings (full final list, EnhanceMode OFF default) + SettingsStore/InMemory (JVM-tested) + DataStoreSettingsStore (all keys; persistence = T2); datastore dep added (allowlisted)
- MS-025: CrashLogger (install/onCrash/list) + buildDiagnosticsReport + DiagnosticsScreen (share wiring; reactive entries + self-test = MS-065); CrashLoggerTest

## Sprint close-out — Day-2 + Day-3 stacked green (run 37786144798)
- ~199 JVM tests green (×debug/release variants), detekt + ktlint + lint + assemble + 16KB + verify_native + corpus all pass
- Notable fixes en route: enhance/player cycle broken via function injection (MpvCommandException→core); KDoc nested-comment parse break; xz runtime for 7z; protocol-relative slashes; BROWSABLE + lowercase MIME (lint); aapt enum-hex grep; Standard/FSR files PENDING license (documented §16 fallback, chain ready)
- QUEUED (owner/device-blocked, not attempted): MS-054/MS-061 FTL (needs Firebase H6), MS-056 buffer process, MS-057-T2/T3 (Robolectric/screenshots/emulator), MS-059 desktop-mpv suite, MS-060 emulator suite, MS-063 release smoke (needs emulator), MS-064 fuzz/prop/arch gates, MS-067 font files + visual goldens, MS-068-full screenshots, MS-074 automated a11y, native self-build (critical path, MS-004 verdict)
- Partial completions recorded per-entry above (Standard rung, DAO/Robolectric, Compose interaction, PdfRenderer binding, session binding, FGS rules, font files, ar screenshots)

## P1.1 Standard shaders — RESOLVED (RAVU, FSRCNNX rejected)
- FSRCNNX rejected: igv/FSRCNN-TensorFlow is GPL-3.0; dotfile copies are not license evidence
- Standard top rung = RAVU Lite R4 (bjin/mpv-prescalers @b3f0a59, LGPL-3.0 verified in-file + LICENSE; self-contained LUT; .hook loads via glsl-shaders)
- FSR rung: AMD source MIT verified, no mpv-ready port (agyild missing) → follow-up own port; ladder is RAVU → Off
- Pipeline extended to .hook (fetch/patch/check); chain + controller tests updated

## P0 native self-build (OPENCODE_COMMAND §3) — recipe landed, first build running

## P0 milestone — all 3 ABIs green (run 37825642552)
- arm64 8m1s, armv7l 12m0s, x86_64 14m23s; in-workflow verify (64-bit 16KB + GPL-string scan) passed per ABI
- Publish flaked on missing git context (fixed via GH_REPO); release hosting next
- Next: app consumes versioned tarballs (jniLibs + headers) → own JNI wrapper → E1
- native/README.md + pins.txt (mpv v0.41.0, ffmpeg n9.0, libplacebo v7.360.1, dav1d 1.5.4, libass 0.17.5, NDK r30, scripts @c2e02fe) + apply_patches.py (exact-once: --disable-gpl, -Dgpl=false, tag pins) + native.yml (arm64 first; armv7l+x86_64 after green) + docs/native-licenses.md
- Key evidence: official ffmpeg.sh uses --enable-gpl (confirmed), mpv.sh passes no -Dgpl (defaults GPL), loadarch already sets 16KB max-page-size, buildall.sh builds per-target/per-arch (mpv without app)
- check_16kb.sh now takes raw .so files too (64-bit rule preserved)

## E1 round 37877271123 — loadfile ACCEPTED, then silence (mpv muted)
- No `command dropped` in logcat: loadfile returned 0; still zero post-open events (no time-pos, no end-file) → core stuck pre-start-file (demux/vo/hwdec) with mpv's internal log never requested
- Fix: au_mpv.cpp requests `info` log messages at pump start + forwards MPV_EVENT_LOG_MESSAGE to logcat as `mpv [prefix/level]` (native-only, zero Kotlin/test impact)
- Next: read mpv's own error lines (vo/gpu EGL? mediacodec-copy? demux?) then fix the cause, not the plumbing

## E1 test timing bug — shots fired before open, close killed EOF
- E1PlayerTest took shots at +0.8s/+2.0s but last round's `open` landed at +3s (dlopen+init on slow emulator) → both shots pre-playback (black + chrome = identical); scenario.close then destroyed the engine before the 3s clip could EOF
- Fix: poll app-side trace (same external files dir both processes) for `time=` before shot1, and for `eof` before close (30s timeouts); removed fixed STARTUP_MS
- T1 must go green on this before the next emulator dispatch

## E1 round 37875455035 — pump alive, render ctx ok, silence after open
- Logcat: route viewUri ok → event pump started → libmpv v0.41.0 → pump alive first event id=22 (PROPERTY_CHANGE, pre-open = initial observe notification, time-pos null → correctly silent) → render start code=0 → open logged — then ZERO further events, no trace, identical screenshots (non-blank = black surface + system chrome)
- Native pump audited clean: NewGlobalRef callback, Attach/DetachCurrentThread, mpv_event_name strings (no hardcoded ABI numbers); event mapping sound (trace iff real time-pos values)
- Conclusion: loadfile result was invisible (MpvCommandException → Diag only); mpv usually accepts loadfile async, so silence implies synchronous throw OR async failure with no end-file — need the error text

## E1 fix — loud command failures + Log.e on drop path
- PlayerEngine.cmd catch now Log.e("AuPlayer", "command dropped: ...") (JVM-safe: executes only on real failures, never in unit tests); LibmpvBridge.command already includes mpv_error_string; JNI nativeErrorString exists (au_mpv.cpp)
- T1 green run 37876551255; emulator redispatched run 37877271123 — verdict hinges on `command dropped:` line in logcat

## E1 root cause — commands raced mpv_initialize
- `MpvCommandException: command failed: core not initialized`: nobody called init before open
- Fix: engine auto-inits in constructor; all commands await readiness (specific catches only); LibmpvBridge.init idempotent

## E1 blocker #2 — wrong NDK's libc++_shared (DT_NEEDED proof)
- `ls ndk/* | head -1` picked the runner-preinstalled older NDK; its libc++ lacks `__from_chars_floating_point` that our r30-built libmpv.so needs → dlopen "cannot locate symbol"
- Fix: NDK_VERSION pinned in pins.txt; fetch copies libc++_shared ONLY from it (loud warning otherwise); cache key already covers pins.txt

## E1 blocker found — missing libc++_shared.so (DT_NEEDED proof)
- libmpv.so needs libc++_shared.so (T1 linkage manifest); APK never packaged it → dlopen failed in-app
- Fix: fetch_native.sh copies it per-ABI from the NDK (+ ANDROID_HOME on Gates step); T1 cache key unchanged (pins hash — hmm, script changed but cache key is pins-only, so stale cache without libc++ persists! FIX: include script hash in key)

## E1 status — engine built, playback unproven (several emulator rounds)
- 3-ABI LGPL release published; JNI compiles+links; LibmpvBridge/EglHost/RenderLoop/PlayerScreen/VIEW routing/T1 all green
- Emulator rounds: KVM fixed, run-as paths fixed, logcat gate scoped, E1 split-brain fixed via trace file; current blocker is in-app dlopen (test-process Fake explained; app-process reason next via Log.e)

## E1 split-brain fix — trace file crosses the process boundary
- Test-process engine is always Fake (no .so there); app-process events are proven by e1-trace.txt (workflow awk gate: span ≥ 2s + eof) while the test proves native-load + motion pixels + no crash
- LibmpvBridge writes trace lines; AppContainer passes external files dir; logcat gate scoped to our package

## E1 dispatched — first real-engine playback (run 37845440428)
- API 30 x86_64 emulator, corpus clip via run-as, VIEW intent, time-pos + EOF + screenshot + logcat gates

## Push B3 — PlayerScreen + E1 harness (E-verification pending)
- Status: in review (pushed; CI pending)
- PlayerScreen (SurfaceView → SurfaceHost + RenderLoop), MainActivity VIEW routing (+ test-only EXTRA_DEBUG_PATH), engine.bridge accessor
- E1PlayerTest (time-pos span + clean EOF + non-blank differing screenshots) + emulator.yml (corpus, run-as clip push, logcat FATAL gate, shot artifacts)
- Per §8.2: nothing is "player complete" before E1 is green

## P0 app integration B2 — real bridge + EGL + render loop
- Status: in review (pushed; CI pending)
- EglHost (app-owned EGL14 display/context/window, GLES3, fail-closed), LibmpvBridge (startup map, string props/events, render helpers, assume-guarded test), RenderLoop (redraw-on-update thread), AppContainer picks real bridge when native loads
- E1 (emulator, real engine plays corpus) = next gate; nothing is "player complete" before it

## P0 app integration B1 — own JNI layer (compiles in T1, runs on emulator)
- Status: in review (pushed; CI pending)
- MpvNative (load guarded, header-symbol-only bindings, string events) + au_mpv.cpp (create/init/options/commands/props/observe/render/events/destroy) + CMake (pinned NDK r30) ; NDK+cmake in T1 SDK step
- EGL host + LibmpvBridge + PlayerScreen wiring = Push B2 (after B1 compiles)

## P0 app integration A — staged native libs
- Status: in review (pushed; CI pending)
- tools/fetch_native.sh (pinned release tag → jniLibs/<abi> + cpp headers) + :player fetchNativeLibs task (preBuild, up-to-date aware) + T1 cache + pins NATIVE_RELEASE_TAG; nothing committed (gitignored)

## P1 batch — Standard resolved, probe, E-scaffold, headless, pre-launch
- Status: in review (pushed; CI pending)
- P1.1: RAVU Lite R4 wired (manifest/patch/check/.hook, chain + controller tests); FSRCNNX rejected (igv GPL-3.0); FSR port = follow-up
- P1.2: RungProbe.choose + tests (vo-passes orchestration = E3)
- MS-060 core: emulator.yml + SmokeTest + androidTest deps (E1–E9 scenarios land with native engine)
- Headless H1–H4 scripts ready (option lint, shader compile, SSIM golden); desktop-mpv build job queued
- MS-061': play-upload job (secret-gated) + docs/play-upload.md (manual ≤6 clicks)

## E-scaffold (MS-060 core) + P1.2 probe
- Status: in review (pushed; CI pending)
- emulator.yml (manual, KVM runner, swiftshader-capable matrix hook) + SmokeTest (boots without crash); androidTest deps allowlisted + notices; testInstrumentationRunner set
- P1.2: RungProbe.choose math + tests (vo-passes orchestration = E3)

## P1.2 probe-first rung selection
- Status: in review (pushed; CI pending)
- RungProbe.choose (55% budget, 35% at ≥50 fps, null → Off); RungProbeTest incl. boundaries; vo-passes reading + pause/redraw/resume orchestration = E3 device work

## MS-068 + MS-073 — Locales + isolation (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §15/§17.9 rows, §17.7
- MS-068-part: en/ar/fr strings.xml (enhance + share strings) + DiagnosticsScreen wired; full externalization + ar screenshots = T2
- MS-073-part: IsolationPolicy (2 crashes → disabled) + :enhance process service skeleton/manifest; binding + watchdog = device work

## MS-065 + MS-066 + MS-069 + MS-070 + MS-072 + MS-075 — Small pure tasks (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §15/§17.9 rows
- MS-065: SelfTest JVM probes + reportJson schema (device probes PENDING); SelfTestTest
- MS-066: matchSidecars (stem+lang+ext gate); SidecarTest (encodings = device work)
- MS-069: windowClass compact/medium/expanded; MS-070: shouldKeepScreenOn; WindowTest
- MS-072: ChapterNav siblings/prompt; ChapterNavTest (format matrix in code note)
- MS-075-part: UpdateChecker tag parse + numeric compare; UpdateCheckerTest (verification-metadata needs a trusted CI run — queued)

## MS-058 + MS-062 + MS-071 — Corpus + native verifier + intent filters (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §15/§17.9 (rows), §17.5, §17.7, §17.2
- MS-058: gen_corpus.sh (H.264 480/720/1080, HEVC-10, VP9, AV1-tiny, HDR tags; sha manifest; T1 artifact)
- MS-062: verify_native.sh (16KB reuse, GPL-strings, JNI/R8 + bundletool degrade-to-SKIP honestly, aapt2 targetSdk/perms/back/PiP/VIEW checks) wired into T1
- MS-071: VIEW/SEND filters for player + reader mimes (in-app IntentRouter decides)

## MS-053 + MS-055 — Release hardening + gate docs (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §13, §15 (rows), §17.7-10 (signing), §17.8 (session)
- MS-053: R8 minify+shrink on, JNI keep rules, conditional release signing (debug key without secrets), manual release.yml (AAB+APK+16KB); docs/signing.md (H8 recipe)
- MS-054: BLOCKED on owner (Firebase service account H6 + device session H7) — recorded, not attempted
- MS-055: device-test-checklist (10 checks), tuning.md (perf table shell for MS-061), this entry

## MS-026 + MS-027 — Shader assets + chain builder (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §6.2, §6.3, §15 (rows)
- MS-026: fetch (pinned SHA manifest) + patch (drop OUTPUT-gated WHEN) + static check (no OUTPUT-WHEN, headers intact, deletions-only); Anime4K MIT verified (bloc97 @7684e95); Standard files PENDING license (documented §16 fallback); assets-licenses.md created; CI step added
- MS-027: ShaderChainBuilder (Anime 2 rungs, Standard top rung, >1440p/Off → empty); ShaderChainTest exact-list

## MS-050 + MS-051 + MS-052 — Monetization shell + licenses + legal docs (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §11, §13, §15 (rows)
- MS-050: AdProvider/NoOpAdProvider (isReady=false, all shows Unavailable) + ConsentManager stub; AdProviderTest
- MS-051: THIRD_PARTY_NOTICES curated list + LicensesScreen + check_notices.sh alias→token CI gate (wired into T1); NoticesTest
- MS-052: privacy/terms/store-listing (honest claims)/data-safety docs

## MS-046 + MS-048 + MS-049 — Model select + queue/cache + reader enhance UI (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §9, §15 (rows)
- MS-046: ModelSelect (compact realesr 2×; weights packaging/verification = follow-up with real NCNN); ModelSelectTest 1.5× rule
- MS-048: PrefetchOrder (current+3, stale-drop) + PageCache (keyed files, oldest-first eviction, 500MB); QueueCacheTest
- MS-049: ReaderEnhanceState + shouldPauseEnhance guard; ReaderEnhanceTest (hold-compare reuses MS-031 pattern)

## MS-045 + MS-047 — NCNN seam + tiler golden (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §9, §15 (rows)
- MS-045: NcnnBridge/JniNcnnBridge (loadLibrary try/catch)/FakeNcnnBridge + NcnnEngine backend policy (Vulkan→small-CPU→disabled); NcnnTest incl. graceful-missing-native
- MS-047: Tiler split/stitch over shared Tiling + nearest stub; stub-model golden (stitched == direct); seam-free by construction

## MS-040 + MS-041 — Intent routing + reader history (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §7.2, §17.7 (intents), §15 (rows)
- MS-040: Incoming/IntentRouter (VIEW video→player, images/archive/pdf/text-urls→reader); IntentRouterTest matrix
- MS-041: ReaderHistory over shared ResumeStore (no second schema); ReaderHistoryTest

## MS-042 + MS-043 + MS-044 — Web source (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §8, §15 (rows)
- MS-042: WebPolicy (https-only, scheme blocks) + locked WebSettings; WebView screen = T2/instrumented; WebPolicyTest
- MS-043: ImagePicker pipeline (order/dedupe/300px/ad/data-URI) + JsCollector (scroll step + required selectors); ExtractorTest fixtures
- MS-044: PageDownloader over UrlListProvider (cookies/Referer, cache files, Failed placeholders); MockWebServer tests

## MS-037 + MS-038 + MS-039 — Reader viewers + UI logic + memory (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §7.1, §7.3, §7.4, §15 (rows)
- MS-037: fallback decision (own Compose viewers; Mihon is View-system) — PagedReader/WebtoonReader/ZoomablePage; interaction = T2; compose-foundation added (allowlisted)
- MS-038: TapZoneConfig/actionFor, readerProgress, filterMatrix, isPagedMode; ReaderUiTest
- MS-039: MemoryBudget (25% cap, inSampleSize, webtoon slices via shared Tiling); device release paths noted; MemoryTest

## MS-035 + MS-036 — PDF tiling + URL provider (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §7.2, §15 (rows)
- MS-035: Tiling grid (row-major, edge-clamped, webtoon column) shared by PDF zoom + NCNN; PdfRenderer binding = device work; TilingTest incl. 1500×22000 strip
- MS-036: UrlListProvider (headers, 3 tries, image/* gate, size cap) + MockWebServer tests (headers/redirect/403/type/cap/timeout); mockwebserver dep added (allowlisted, test-only)

## MS-028 — EnhanceController (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §6.3, §6.4 (failure signal), §15 (row)
- Applies scale opts + change-list set (shapes per §6.3; native proof = MS-059), clear restores bilinear, failures clear + Diag + Failed(rung); shader cache via MS-008, Preparing UI in MS-029
- Tests added: EnhanceControllerTest (exact command sequence, clear, unavailable, throwing-bridge, async)

## MS-033 + MS-034 — Page model + archive/folder providers (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §7.2, §15 (rows)
- MS-033: PageRef + validated ref(); PageRefTest
- MS-034: NaturalOrder (zero-pad aware), ArchiveLimits caps, ZipProvider/SevenZProvider (stream-by-entry, SAF variant deferred), FolderProvider (local; SAF tree = device work), SUPPORTED_IMAGE_EXTS + __MACOSX/hidden filters; size() needs BitmapFactory (device); ArchiveProvidersTest incl. 7z round-trip

## MS-029 + MS-030 — Enhance sheet + governor (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §6.1, §6.4, §10, §15 (rows)
- MS-029: EnhanceSheet (Off/Anime/Standard, 1440p rule with message) + enhanceLabel incl. lowered state; interaction = T2; :player now depends on :enhance
- MS-030: EnhanceGovernor (3-strike drops, immediate thermal, no step-up, battery start-lower, per-video reset); GovernorTest incl. never-steps-up stream

## MS-031 + MS-032 — Hold-to-compare + edge cases (stacked)
- Status: DONE (shared green run 37786144798)
- Sections used: §3.3, §6.5, §15 (rows)
- MS-031: CompareHolder (snapshot/clear/restore, mid-hold rung change ignored); CompareHolderTest
- MS-032: EnhanceEdgeCases (surface-loss re-attach, safe seek, hwdec software fallback + Diag); EnhanceEdgeCasesTest

## MS-022 + MS-023 — Session/focus + PiP (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.2, §5.3, §15 (rows)
- MS-022: AudioFocusHandler pure policy (loss/transient/duck/gain) + AudioFocusTest matrix; AuPlaybackService skeleton + FGS permissions/manifest (session binding + notification = device work)
- MS-023: PipCalc aspect reduction + MainActivity.enterAuPip; behavior device-verified later; PipCalcTest

## MS-017 + MS-018 — Tracks + subtitle style props (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.1, §15 (rows)
- MS-017: Track/TrackKind/TrackList (aid/sid/sub-add mapping, srt/ass/vtt gate); engine routes via send(List) (consolidated for TooManyFunctions); TracksTest incl. engine commands
- MS-018: SubtitleStyle→props, mpvColor #AARRGGBB ([VERIFY] order in §17.4 goldens), delayProps; SubtitleStyleTest prop-mapping

## MS-013 + MS-014 — Gesture engine + overlay indicators (stacked)
- Status: DONE (shared green run 37716823563)
- Sections used: §5.1, §15 (rows)
- MS-013: TapZone/GestureAction/GestureConfig + pure GestureMapper (locked→Ignored); GesturesTest table-driven incl. full locked matrix
- MS-014: GestureIndicator + indicatorText (pure, JVM-tested) + thin GestureOverlay (T2-tested); :player uses own Surface (no upward dep on :app)
