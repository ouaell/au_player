# status — truth table (OPENCODE_COMMAND §1)

One status per feature. `E2E-VERIFIED` needs a run id + artifact.
Nothing below claims to work without evidence.

## Engine / playback

| feature | status | evidence / note |
|---|---|---|
| libmpv presence (any ABI) | E2E-VERIFIED | emulator run 38017724352 (10/10 green): in-app dlopen, `mpv v0.41.0` init, VO [gpu] 720p + AO [audiotrack] + mediacodec-copy |
| open + time-pos events + clean EOF | E2E-VERIFIED | same run: E1 loadfile plays (time-pos samples, span >2 s, eof=True), shot variance delta, logcat clean |
| play/pause/seek commands | E2E-VERIFIED | same run: E8 pause-freezes-trace + play-resumes, E5 seek storm green |
| surface attach/detach | E2E-VERIFIED | same run: wid plumbing, vo-owned surface, real frames in E1/E8 screenshots |
| activity lifecycle (rotate/bg) | E2E-VERIFIED | same run: E2 lifecycle green |
| state machine/transitions | UNIT-ONLY | PlayerStateMachineTest (+illegal pairs) |
| controller UI/auto-hide/chrome | E2E-VERIFIED | run 38017724352: E8 shown-vs-hidden pixel diff 87806, 3 s auto-hide; E7 zero unlabeled clickables (22 player + 8 reader nodes) |
| gestures mapping | UNIT-ONLY | GesturesTest table (+locked matrix); tap-to-show proven via E8, drag paths need device |
| aspect/orientation/zoom math | UNIT-ONLY | DisplayTest |
| speed/A-B/loop/sleep math | UNIT-ONLY | PlaybackModesTest |
| tracks/subtitle props mapping | UNIT-ONLY | TracksTest, SubtitleStyleTest (mpv color order verified in §17.4 goldens) |
| subtitle rendering | E2E-VERIFIED | headless H4 goldens SSIM 1.0 (MS-059/MS-067 runs) |
| resume/continue-watching | E2E-VERIFIED | run 38017724352: E9 saved pos=1066 dur=3000, continue row shown; HistoryTest for policy |
| playlist/focus/PiP math | UNIT-ONLY | PlaylistTest, AudioFocusTest, PipCalcTest (behavior = device) |
| MediaSession service/notification | UNVERIFIED | skeleton compiles; needs device |
| source resolution/routing | UNIT-ONLY | SourceResolverTest, IntentRouterTest (fd:// + SAF = device) |

## Enhancement (video)

| feature | status | evidence / note |
|---|---|---|
| Anime chain definition | UNIT-ONLY | ShaderChainTest exact lists; patched files pass static check in T1 |
| Standard chain (FSRCNNX/FSR files) | UNVERIFIED | no license-verified source; degrades to Off |
| apply/clear/failover/governor/compare | UNIT-ONLY | EnhanceControllerTest, GovernorTest, CompareHolderTest |
| shader compile on GPU | E2E-VERIFIED | run 38017724352: E3 enhance toggle green with real RAVU shader |
| probe-first rung selection | UNVERIFIED | not implemented (MS-028 used top rung) |

## Reader / web source / image model

| feature | status | evidence / note |
|---|---|---|
| archive/folder/URL providers | UNIT-ONLY | ArchiveProvidersTest (zip/7z/order/caps), UrlListTest (MockWebServer), FolderProviderTest |
| archive rendering (CBZ) | E2E-VERIFIED | run 38017724352: E4 pages=3, pixel mean/var proof |
| PDF rendering | E2E-VERIFIED | same run: E4 pages=1, white-fill fix, pixel mean/var proof |
| SAF tree, BitmapFactory sizing | UNVERIFIED | Android-only paths, needs device/SAF |
| natural sort, tiling, page model | UNIT-ONLY | property-style unit tests |
| viewers/zoom/webtoon/RTL | UNVERIFIED | composables need device gesture proof |
| reader UI logic (tap/progress/filters/matrix) | UNIT-ONLY | ReaderUiTest |
| memory budget math | UNIT-ONLY | MemoryTest (release paths = device) |
| web extraction pipeline | E2E-VERIFIED | run 38017724352: E6 collector proof on emulator (+ExtractorTest fixtures UNIT) |
| web downloader | UNIT-ONLY | PageDownloaderTest (MockWebServer) |
| NCNN inference/tiler/queue/cache | UNIT-ONLY | policy + stub-golden tests; real inference UNVERIFIED (no device) |
| model weights | UNVERIFIED | not downloaded, license unverified |

## App shell / release

| feature | status | evidence / note |
|---|---|---|
| theme tokens/components | UNIT-ONLY | ThemeTokensTest (screenshots = device) |
| nav back-stack | UNIT-ONLY | NavigatorTest (NavHost back = device) |
| settings model/store | UNIT-ONLY | SettingsTest (DataStore persistence = device) |
| diagnostics/crash/self-test framework | UNIT-ONLY | CrashLoggerTest, SelfTestTest (device probes PENDING) |
| ads/monetization | UNIT-ONLY | NoOp tests (network impl behind flag, correctly absent) |
| licenses screen/notices | UNIT-ONLY | NoticesTest + check_notices.sh gate (render = device) |
| locales ar/fr | UNVERIFIED | strings exist; screenshots = device |
| manifest/aapt2/16KB/license gates | E2E-VERIFIED | T1 (e.g. run 38017466797 green; APK, corpus, reports artifacts) |
| minified release smoke (emulator half) | E2E-VERIFIED | MS-063 run 37953004587: R8 minified VO/reader asserts green |
| release R8/AAB/signed signing | UNVERIFIED | unsigned/debug-key only; signed needs keystore H8 (owner) |
| FTL Robo + pre-launch report path | UNVERIFIED | needs Play account + Firebase subscription (owner) |
| CI tiers/caching/KVM/artifacts | E2E-VERIFIED | MS-057: T1 3m32s/3m56s (< 8 min), emulator KVM step, dispatchable T2/T3, 7-day artifacts |

## Counts

- E2E-VERIFIED: 15
- UNIT-ONLY: 21
- UNVERIFIED: 9
