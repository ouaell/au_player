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
