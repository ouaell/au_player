# status — truth table (OPENCODE_COMMAND §1)

One status per feature. `E2E-VERIFIED` needs a run id + artifact.
Nothing below claims to work without аварий evidence.

## Engine / playback

| feature | status | evidence / note |
|---|---|---|
| libmpv presence (any ABI) | UNVERIFIED | no native lib in repo; P0 self-build not started |
| open/play/pause/seek/events | UNVERIFIED | FakeMpvBridge only; needs E1 |
| surface attach/detach | UNVERIFIED | logic UNIT-ONLY (SurfaceHostTest); needs E4 |
| state machine/transitions | UNIT-ONLY | PlayerStateMachineTest (+illegal pairs) |
| controller UI/auto-hide/time | UNVERIFIED | AutoHide/formatTime UNIT-ONLY; composables need T2 |
| gestures mapping | UNIT-ONLY | GesturesTest table (+locked matrix) |
| aspect/orientation/zoom math | UNIT-ONLY | DisplayTest |
| speed/A-B/loop/sleep math | UNIT-ONLY | PlaybackModesTest |
| tracks/subtitle props mapping | UNIT-ONLY | TracksTest, SubtitleStyleTest (mpv color order still [VERIFY]) |
| resume/history/playlist/focus/PiP math | UNIT-ONLY | HistoryTest, PlaylistTest, AudioFocusTest, PipCalcTest |
| MediaSession service/notification | UNVERIFIED | skeleton compiles; needs device |
| source resolution/routing | UNIT-ONLY | SourceResolverTest, IntentRouterTest (fd:// + SAF = device) |

## Enhancement (video)

| feature | status | evidence / note |
|---|---|---|
| Anime chain definition | UNIT-ONLY | ShaderChainTest exact lists; patched files pass static check in CI |
| Standard chain (FSRCNNX/FSR files) | UNVERIFIED | no license-verified source; degrades to Off |
| apply/clear/failover/governor/compare | UNIT-ONLY | EnhanceControllerTest, GovernorTest, CompareHolderTest |
| shader compile on GPU | UNVERIFIED | needs H2/E3 (headless + emulator) |
| probe-first rung selection | UNVERIFIED | not implemented (MS-028 used top rung) |

## Reader / web source / image model

| feature | status | evidence / note |
|---|---|---|
| archive/folder/URL providers | UNIT-ONLY | ArchiveProvidersTest (zip/7z/order/caps), UrlListTest (MockWebServer), FolderProviderTest |
| natural sort, tiling, page model | UNIT-ONLY | property-style unit tests |
| PDF rendering, SAF tree, BitmapFactory sizing | UNVERIFIED | Android-only paths |
| viewers/zoom/webtoon/RTL | UNVERIFIED | composables need T2 gesture tests (E5) |
| reader UI logic (tap/progress/filters/matrix) | UNIT-ONLY | ReaderUiTest |
| memory budget math | UNIT-ONLY | MemoryTest (release paths = device) |
| web extraction pipeline | UNIT-ONLY | ExtractorTest fixtures (WebView JS = instrumented T2) |
| web downloader | UNIT-ONLY | PageDownloaderTest (MockWebServer) |
| NCNN inference/tiler/queue/cache | UNIT-ONLY | policy + stub-golden tests; real inference UNVERIFIED (E6) |
| model weights | UNVERIFIED | not downloaded, license unverified |

## App shell / release

| feature | status | evidence / note |
|---|---|---|
| theme tokens/components | UNIT-ONLY | ThemeTokensTest (screenshots = T2) |
| nav back-stack | UNIT-ONLY | NavigatorTest (NavHost back = T2) |
| settings model/store | UNIT-ONLY | SettingsTest (DataStore persistence = T2) |
| diagnostics/crash/self-test framework | UNIT-ONLY | CrashLoggerTest, SelfTestTest (device probes PENDING) |
| ads/monetization | UNIT-ONLY | NoOp tests (network impl behind flag, correctly absent) |
| licenses screen/notices | UNIT-ONLY | NoticesTest + check_notices.sh gate (render = T2) |
| locales ar/fr | UNVERIFIED | strings exist; screenshots = T2 |
| manifest/aapt2/16KB/license gates | E2E-VERIFIED | CI run 37786798915 (T1 green; artifacts: APK, corpus, reports) |
| release R8/AAB/signing | UNVERIFIED | workflow exists, never dispatched (needs keystore H8 for signed) |
| pre-launch report path | UNVERIFIED | needs Play account + service secret (owner) |

## Counts

- E2E-VERIFIED: 1 (CI gate stack — builds/tests/static checks only)
- UNIT-ONLY: 26
- UNVERIFIED: 21
