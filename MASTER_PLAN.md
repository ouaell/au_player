# MASTER_PLAN.md — Video Player + Reader (Android) — complete build plan

This file REPLACES AGENTS.md, TECH_SPEC.md and TECH_QUALITY.md. Keep only this file plus the 15-line AGENTS.md pointer in §14.
`[VERIFY]` = from memory or partial research. The agent must confirm it in the real source/docs, then write the result in `docs/progress.md`. A failed verification triggers the fallback written next to it, never a silent workaround.

Honesty note: this plan gives *gates* (tests, CI checks, checklists), not guarantees. Nothing is "perfect" until it has run on real devices; the plan is built so that real-device sessions find problems fast and precisely.

---
## 1. Owner decisions (fixed — do not re-open)
1. **Enhancement = two user-chosen modes + Off.** The player has an Enhance button → sheet with exactly: `Off`, `Anime`, `Standard`. No content detection, no auto-profile, no sub-presets, no per-folder logic. Initial value is `Off`; the last choice is remembered app-wide (one stored value).
2. Each mode has ONE fixed chain (§6). No Mode A/B/C picker, no tiers exposed to the user.
3. An internal safety governor exists (§6.4). It is NOT a user setting: it only lowers a chain when the device cannot sustain it, and never raises it again during the same video.
4. **Generic Web Source stays**: the user enters/browses a page URL and the app extracts the images into the reader (§8). Implemented as a normal in-app browser + extractor, with a compile-time flag so a store build can omit it.
5. Reader inputs: local files/folders, PDF, direct image URLs and URL lists, Share/Open-With from other apps, and Web Source. No built-in catalog, no extensions, no content shipped.
6. Mihon: reuse ONLY the reader viewers (Apache-2.0). No trackers, no library DB, no extension system in 1.0.
7. Original name, icon, colors and layout. MX Player is a *feature reference* (what exists, how settings are grouped); nothing visual or textual is copied.
8. Phones/tablets only in 1.0. Android TV boxes are an unsupported device class (see §3.3).
9. Monetization is isolated behind an `AdProvider` interface (§11) so it can never destabilize the core.

## 2. Targets and environment
- minSdk 26, **compileSdk/targetSdk 36** (Google Play requires new apps and updates to target API 36 since 31 Aug 2026; existing apps need ≥ 35). On API 36, `onBackPressed()`/`KEYCODE_BACK` are not delivered: use `BackHandler`/predictive back and `android:enableOnBackInvokedCallback="true"` from day one.
- Kotlin 2.x, Jetpack Compose + Material3 (heavily themed), single-activity, Coroutines/Flow, Room (history/resume), DataStore (settings), OkHttp, Coil. **Manual DI** (a small `AppContainer`): no Hilt/Koin, to keep CI builds fast and agent mistakes low.
- ABIs: arm64-v8a + armeabi-v7a for release; **x86_64 is required for debug/CI builds** (needed for the CI emulator suite, §17.2). If the chosen libmpv AAR lacks x86_64, build libmpv ourselves.
- Owner machine is weak (i3/4 GB): **never run Gradle locally**. Build/test only in GitHub Actions. opencode edits, pushes, reads CI logs (`gh run view --log-failed`).
- The agent's model must be able to read images (for `docs/reference/` screenshots). If it can't, tell the owner to produce `docs/feature-spec.md` with a vision-capable model first.

## 3. Engine decisions
### 3.1 libmpv source — Decision D1 (first engineering task, time-box 60 min)
Candidates, in order (confirm each is real before using):
1. `dev.jdtech.mpv:libmpv` (github.com/jarnedemeulemeester/libmpv-android, MIT wrapper) [VERIFY]
2. `io.github.abdallahmehiz:mpv-android-lib` [VERIFY]
3. A repository named `SnowMediaEnt/libmpv-android-lgpl` was proposed by another plan; a web search did not find it. Verify it exists and inspect it before trusting any of its claims [VERIFY].
4. Build ourselves with the official mpv-android `buildscripts` in GitHub Actions (cache the output). Note: official mpv-android is an app, not an importable AAR; use its MPVLib/MPVView/native code as the reference.
Audit checklist for the chosen artifact (write results to `docs/decisions.md`): mpv built with `-Dgpl=false`? FFmpeg without `--enable-gpl/--enable-nonfree`? libass/fribidi/harfbuzz/freetype licenses? ABIs? JNI package name (for R8 keep rules)? **`.so` ELF segment alignment ≥ 16 KB** (`readelf -lW lib.so | grep LOAD`, align must be 0x4000 or larger; Play requires 16 KB support for apps targeting Android 15+ on newer devices) [VERIFY current rule].
If no candidate is LGPL-clean and 16 KB-aligned → build ourselves (option 4). If that fails within 4 h → STOP and report to the owner (alternatives: Media3 without Anime4K, or GPL release).

### 3.2 Render/decoder path (single pipeline)
- Always `vo=gpu`, `gpu-context=android`, `opengl-es=yes`, `hwdec=mediacodec-copy` (HW), automatic fallback to `hwdec=no` (SW) when the HW decoder errors.
- **Do not use `vo=mediacodec_embed`.** It renders straight to a Surface (no GL post-processing, so no user shaders), cannot show software-decoded formats, and because mpv chooses the VO before the decoder, a VO that cannot handle fallback formats breaks the chain when HW decoding fails (mpv maintainers' discussion). Also verify whether it can draw libass subtitles at all [VERIFY]; if not, that alone rules it out for an MX-class player. One pipeline = fewer bugs and shaders work in every mode.
- Cost accepted: CPU copy of decoded frames. Mitigation: enhancement only for sources ≤ 1440p (§6.1). 2160p/HDR sources play without neural passes.

### 3.3 Known device-class limitation
mpv-android issue reports show green frames with `mediacodec-copy` on some Android TV-box SoCs (e.g., RK3588) while `mediacodec_embed` worked there. Phones are the target; TV boxes are unsupported in 1.0. Provide ONE escape hatch in Settings → Decoder: `Hardware (default) / Software`. Diagnostics shows `hwdec-current`.

### 3.4 mpv startup options (verify each against the chosen wrapper's source) 
```
vo=gpu  gpu-context=android  opengl-es=yes  hwdec=mediacodec-copy
ao=audiotrack,opensles      video-sync=audio
gpu-shader-cache-dir=<cacheDir>/mpv-shaders      # avoid recompiling shaders each launch
tls-verify=yes  tls-ca-file=<filesDir>/cacert.pem # bundle a CA file and copy on first run (HTTPS streaming)
sub-fonts-dir=<filesDir>/fonts  osd-fonts-dir=<filesDir>/fonts   # bundle one OFL-licensed font
cache=yes  demuxer-max-bytes=32MiB  demuxer-max-back-bytes=16MiB
save-position-on-quit=no                          # we manage resume ourselves
scale=bilinear dscale=bilinear                    # Enhance=Off (cheap)
```
When Enhance ≠ Off set: `scale=lanczos dscale=mitchell correct-downscaling=yes linear-downscaling=no` (and `sigmoid-upscaling=no`). [VERIFY option names on the bundled mpv version.]
- mpv's wrapper is a **process-wide singleton** (static `MPVLib`). So: one `PlayerEngine` owned by the Application, surface attach/detach on demand, reused across videos and by the background service. Never create two instances.

---
## 4. Architecture
```
:app        entry, navigation, AppContainer, flavors (play/direct), manifest
:core       theme/design system, DataStore settings, diagnostics, crash log, licenses, utils
:player     PlayerEngine, PlayerController(+Fake), UiState, gestures, tracks, session service, PiP
:enhance    ShaderAssets, ShaderChainBuilder, EnhanceController, Governor (video) ; NcnnEngine, Tiler, Queue, Cache (images)
:reader     PageProvider(s), viewers, reader UI, PDF, history
:websource  WebView screen, extractor, downloader   (excluded from builds when flag off)
:library    MediaStore/SAF browsing, history, continue-watching
```
Rules: UI talks only to interfaces (`PlayerController`, `EnhanceEngine`, `PageProvider`); every interface has a fake for unit tests; no file > 600 lines (split earlier when it improves cohesion); pure logic (gestures, chain builder, governor, tiling, sorting) lives in plain Kotlin and has JVM unit tests.
Flavors: `play` (store build) and `direct` (GitHub/APK). `BuildConfig.WEB_SOURCE_ENABLED` defaults true in `direct`; the owner may set it false for `play`.

---
## 5. Player specification
### 5.1 Features (parity list; reference app is for *what* exists; derive details from `docs/feature-spec.md`)
Playback: local files, folders, URLs (http/https/HLS/DASH), playlist + auto-next, resume, loop (one/all), A–B repeat, sleep timer, speed 0.25–4×, frame screenshot, video info sheet.
Gestures: horizontal seek, left vertical brightness, right vertical volume, double-tap ±10 s (left/right) and pause (center), long-press 2× while held, pinch zoom, single tap toggles controls, screen lock. Ignore gestures inside system-gesture insets and while locked.
Tracks: audio/subtitle switching, external subtitle files (SRT/ASS/VTT), subtitle style (size, color, outline, position, font), subtitle delay, audio delay, audio boost to 200% (`volume-max=200`).
Display: aspect modes (fit/fill/stretch/16:9/4:3/custom), orientation (auto/landscape/portrait/follow video), brightness restore on exit.
System: background audio with media notification, PiP, audio focus, noisy-audio pause, hardware keys, resume after process death.
HDR on SDR screens: tone mapping on (`tone-mapping=bt.2390` or the closest available, `hdr-compute-peak=no`); Dolby Vision profile 5 is detected and a warning shown (renders incorrectly) [VERIFY].

### 5.2 Threading & state
- All mpv commands on one single-thread dispatcher; mpv events → `SharedFlow`; UI reads one immutable `PlayerUiState` via `StateFlow`.
- State machine: `Idle → Loading → Ready → Playing ⇄ Paused → Buffering → Error → Released`; illegal transitions throw in tests.
- Observe: `time-pos, duration, pause, paused-for-cache, track-list, video-params, speed, volume, aid, sid, hwdec-current, frame-drop-count, decoder-frame-drop-count, vo-delayed-frame-count`.
- Surface lifecycle: attach on `surfaceCreated`, detach on `surfaceDestroyed` (must not crash or leave a black frame after PiP/background/rotation/lock-screen). Write the lifecycle test matrix first.
- content:// URIs: open via file descriptor/`fd://` path as in the wrapper, never assume file paths.

### 5.3 Background / PiP / session
- `MediaSessionService` (Media3 session or androidx.media) with notification controls; `foregroundServiceType="mediaPlayback"` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission; start the service only from a visible Activity (Android 14/15 FGS rules).
- Audio focus: pause on LOSS, pause/duck on TRANSIENT (resume on GAIN if we paused), `ACTION_AUDIO_BECOMING_NOISY` pause.
- PiP: `setAutoEnterEnabled` where available, source-rect + aspect from `video-params`, actions (play/pause, ±10 s), hide controls in PiP.
- Permissions: `READ_MEDIA_VIDEO` (33+; handle Android 14 partial-access state), `READ_EXTERNAL_STORAGE` (≤ 32), `POST_NOTIFICATIONS` (33+). Prefer SAF folder grants with persisted URI permissions; never request all-files access.

### 5.4 Library (player side)
Folders-by-MediaStore grid, SAF tree picker, "Open URL", continue-watching row (progress bars), recents. Thumbnails: Coil video-frame decoder or `MediaMetadataRetriever` (bounded LRU, local files only). Seek-bar thumbnails: closest-sync frames via `MediaMetadataRetriever`, throttled, local files only.

---
## 6. Video enhancement (the two modes)
### 6.1 Applicability
Enhancement is available when the source height ≤ 1440. For larger sources the button is disabled with the text "Already high resolution". Fixed rule: **exactly one 2× neural luma pass**, then mpv scales to the screen. (Two neural passes are out of 1.0: more cost, more bugs.)

### 6.2 Why the shaders need patching (supersampling)
These CNN shaders are luma doublers that run at SOURCE resolution; mpv then scales the doubled image to the display, so a 1080p source on a 1080p screen is effectively processed at 2160p and downscaled (this is what gives the cleaner picture the owner wants). But the shader files contain `//!WHEN` conditions that skip the pass unless the output is larger than the source by a margin (about 1.2×), so on equal-size screens they would never run. The build step `tools/patch_shaders.py` produces `assets/shaders/patched/*` with those `//!WHEN` lines removed, while app logic decides when to load them. Static test: every patched file has no `//!WHEN` line that references OUTPUT size, and every `//!HOOK/BIND/DESC` header block is intact. [VERIFY exact lines per file]

### 6.3 Fixed chains (file names from each project's own instructions [VERIFY])
**Anime** (Anime4K, MIT): `Anime4K_Clamp_Highlights` → `Anime4K_Restore_CNN_M` → `Anime4K_Upscale_CNN_x2_M` (patched). Degrade ladder (governor only): `Clamp_Highlights → Upscale_CNN_x2_S` → Off.
**Standard** (live action): `FSRCNNX_x2_8-0-4-1` (patched). The 16-0-4-1 variant is not used (too heavy for phones, small gain). Degrade ladder: `FSR` EASU/RCAS-style shader (MIT) → Off. [VERIFY FSRCNNX and FSR file licenses]
Common: after a chain loads set `scale=lanczos dscale=mitchell correct-downscaling=yes linear-downscaling=no`. Apply with `change-list glsl-shaders set "<p1>:<p2>"` (or `glsl-shaders-clr` + appends) [VERIFY separator and command on the wrapper's mpv version].
First activation compiles shaders (can stall for seconds on mobile): run it while showing "Preparing enhancement…" over the paused frame, keep `gpu-shader-cache-dir` set, then resume.

### 6.4 Governor (internal, deterministic, unit-tested)
- Start every video at the mode's top rung. Every 2 s sample: `frame-drop-count` delta, `decoder-frame-drop-count` delta, `PowerManager.currentThermalStatus` (API 29+; treat as NONE below).
- Step DOWN one rung if (drops ≥ 3 in a sample for 3 consecutive samples) OR thermal ≥ MODERATE. Show a small toast: "Enhancement lowered to protect your device".
- Never step up within the same video. New video → top rung again. Battery Saver on → start one rung lower.
- If shader compile/link errors appear in mpv's log (lines containing "shader" at error level) → mark that rung failed, go down, and persist failure per (device model, rung) to skip it next time.
- Rationale in code comments: no flapping, no hidden state, easy to test with simulated inputs.

### 6.5 Hold-to-compare
Press-and-hold on the Enhance chip → temporarily clear shaders and restore previous list on release (snapshot/restore). Ignore if a rung change happens during the hold.

### 6.6 Defaults for non-enhanced playback
No sharpening, no debanding by default. Keep Off cheap (bilinear). Expose nothing else.

---
## 7. Reader specification
### 7.1 Viewers
Extract from Mihon (Apache-2.0) only: pager viewer, webtoon viewer, zoom/gesture/transition code and the minimal model (`ReaderPage`-like) they need [VERIFY paths in current repo: `.../ui/reader/viewer/`]. Replace DB/source/view-model dependencies with our `ReaderState` + `PageProvider`. Keep license headers; rename packages; `grep` in CI fails if "tachiyomi.source", "Extension", "Tracker" or "DatabaseHandler" remain in `:reader`.
**Time-box 3 h. Fallback (pre-approved):** own viewers — `HorizontalPager` (paged), `LazyColumn` + `FillWidth` (webtoon, no gaps), zoom layer (a vetted Apache-2.0 Compose zoom lib [VERIFY] or custom `transformable`), `BitmapRegionDecoder` for tall images.
Modes: RTL, LTR, vertical paged, webtoon (continuous), double-page (landscape). Zoom, double-tap zoom, configurable tap zones, keep-screen-on, orientation lock, brightness, filters (grayscale, invert, sepia), crop borders, progress memory, page slider, "split wide pages" option only if time remains (cut otherwise).

### 7.2 PageProvider
```kotlin
interface PageProvider {
  val title: String
  val pageCount: Int
  suspend fun open(index: Int): InputStream
  suspend fun size(index: Int): Pair<Int, Int>?
  fun close()
}
```
Implementations:
- `ArchiveProvider`: CBZ/ZIP and 7z via commons-compress using `SeekableByteChannel` over a `ParcelFileDescriptor` (no full copy) [VERIFY]; RAR only if a permissively-licensed library is confirmed (otherwise omit RAR in 1.0). Natural sort (`1,2,10`), skip non-images, `__MACOSX`, hidden files; zip-slip impossible because we never extract to paths (stream by entry); entry-size and entry-count caps (zip-bomb defense).
- `FolderProvider`: SAF `DocumentFile`, natural sort.
- `PdfProvider`: `PdfRenderer`; single-thread dispatcher + mutex (one page open at a time); render at display width × density; tiles via `render(bitmap, clip, matrix, mode)` for zoom; LRU bitmap cache bounded by memory class; password prompt on `SecurityException`; text-heavy PDFs get higher render DPI instead of neural enhancement.
- `UrlListProvider`: OkHttp, per-request `Referer`/`User-Agent`, timeouts, retries (3, backoff), content-type must be `image/*`, size cap per image, disk cache.
- `WebSourceProvider`: wraps `UrlListProvider` with the cookies/headers captured from the WebView (§8).
Intents handled: `ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE` for `image/*`, `application/pdf`, `application/zip`, `application/x-cbz`, `application/vnd.comicbook+zip`, `text/plain` (URLs). Routing: video MIME → player, images/archives/PDF → reader.

### 7.3 Memory rules
Downsample decode to screen size; cap decoded bitmaps ≈ 25% of `memoryClass`; slice tall webtoon images ≤ 2048 px per tile; release on `onTrimMemory`; test with a synthetic 100-page 1500×22000 px strip and a 300-page PDF.

### 7.4 Reader UX
Immersive; thin progress bar; tap-zone preview in settings; per-series mode memory; smooth paged animations; long-press page menu (share page, save page, enhance page); "Resume where you left" on open.

---
## 8. Web Source (in-app browser + extractor)
Design: the user opens a **browser screen** (WebView) at a URL they type/paste, browses normally (the site's own checks, cookies and JavaScript work as in any browser; the app does not solve or automate challenges), then taps **"Read this page"**. The app extracts the images the page actually loaded.
Extraction steps:
1. Run JS (`evaluateJavascript`) that first auto-scrolls to the bottom in steps to trigger lazy loading (cap time 15 s, stop when height stops growing).
2. Collect candidates from `img` (`currentSrc`, `src`, `data-src`, `data-original`, `data-lazy-src`, largest `srcset`), `picture > source`, and CSS `background-image`; resolve to absolute URLs; keep DOM order.
3. Filter: natural size ≥ 300×300 (configurable constant), skip data-URI icons, tracking pixels, sprites, obvious ad containers (`iframe`, elements inside `[class*=ad]`/`[id*=ad]` heuristics), duplicates.
4. Show a **preview grid**: user can untick/reorder/remove pages, then "Open in reader". (This also makes failures understandable instead of silent.)
5. Download with OkHttp using the WebView's cookies (`CookieManager`), `Referer` = page URL, same `User-Agent`; cache to app cache; retries; per-page error placeholders.
6. User-adjustable CSS selector field ("Advanced") when auto extraction finds nothing.
Hardening: `setAllowFileAccess(false)`, no file/content URL schemes loaded, block `intent:` and unknown schemes (ask before opening external apps), popups blocked, Safe Browsing on, no JavaScript interface exposed to pages, `evaluateJavascript` results parsed strictly as JSON, mixed content blocked, third-party cookies per `CookieManager` defaults, "Clear browsing data" button, cleartext HTTP warned.
Notice screen on first use: "The app contains no content. You are responsible for the pages you open and the rights to read them."
Tests: extraction JS against local HTML fixtures (lazy images, srcset, background images, relative URLs) using Robolectric/WebView-free DOM parsing for logic + one instrumented smoke test; downloader tests with MockWebServer (redirects, 403, wrong content-type, timeouts).
Store note: store review of reader apps that fetch web images is discretionary; this is why the module sits behind the flavor flag.

---
## 9. Image enhancement model (reader)
- Engine: NCNN prebuilt Android+Vulkan release (BSD-3) [VERIFY] via a small JNI wrapper; Vulkan init failure → CPU path only for small pages, otherwise feature disabled with a message.
- Model: ONE small 2× SR model shipped inside the APK if ≤ ~10 MB, chosen ONLY from the Real-ESRGAN family (realesr-animevideov3 and realesr-general-x4v3-class compact models; same repository/license, verify the weights license in the release notes); other families are excluded from 1.0. **Code license ≠ weights license:** record both in `docs/assets-licenses.md`; reject non-commercial weights. Pick by running the candidates on 5 bundled test pages (B/W screentone, colour manhwa, webtoon strip, low-res scan, text page) and keeping screentone intact [VERIFY outputs visually via CI-generated side-by-side images attached as artifacts].
- Pipeline: decide (upscale only if shorter edge < 1.5× display width px) → overlapping tiles (128–192 px, 8–16 px overlap, linear blend) → infer → stitch → WebP lossless/high-quality into disk cache keyed `(contentHash, page, modelId, scale)`; LRU cache cap (default 500 MB) + "Clear cache".
- Queue: priority = current page, +1, +2, +3; one inference at a time; cancel stale jobs on fast scroll; pause when thermal ≥ MODERATE or Battery Saver; webtoon strips processed in segments.
- UI: reader toggle "Enhance pages" (default Off), per-page "enhancing…" indicator, hold-to-compare.
- PDF: enhancement offered only for scanned/image-only pages.
- Tests (no device): tile-grid math, overlap blending, cache key/eviction, queue priority/cancel, and a pipeline golden test with a stub "model" (nearest ×2).

---
## 10. Design system (original identity)
- Dark-first, AMOLED base. Tokens (can be tuned, but keep semantic names): `bg #000000`, `surface #101015`, `surfaceHigh #1A1A22`, `outline #2A2A35`, `onSurface #ECECF2`, `muted #9A9AAB`, `accent #7C5CFF`, `accent2 #00D1FF` (gradient only on primary actions/progress), `danger #FF5C6C`, `success #3DDC97`. Light theme supported via the same tokens.
- Shape: 20–28 dp radii for sheets/cards, pill chips; spacing scale 4/8/12/16/24/32; type: one variable sans (OFL), sizes 12/14/16/20/28/36.
- Icons: Material Symbols Rounded (Apache-2.0) [VERIFY], one stroke weight everywhere.
- Motion: 150–250 ms ease-out fades, springy sheets, predictive-back, no layout jumps; haptic ticks on lock/unlock and seek snaps.
- Player controller: top bar (back, title + codec/resolution chip, audio, subtitles, more) / center (prev, large play-pause, next) / bottom (thick scrubber with thumbnail bubble, elapsed/remaining, chip row: speed, aspect, rotate, lock, **Enhance**, PiP). Controls auto-hide after 3 s, never while scrubbing; glass panels (blur on API 31+, translucent flat below).
- Enhance chip shows live state ("Anime", "Standard", or "Anime · lowered"). 
- Home: continue-watching, folders grid, recent reader items, quick actions (Open file / Open URL / Web page).
- Settings (FINAL LIST, nothing else in 1.0): Playback (resume, auto-next, background play, PiP), Gestures (enable seek / brightness / volume, double-tap seconds), Subtitles (size, color, outline, position, font, default delay), Audio (boost, preferred language, downmix), Display (orientation, default aspect), Decoder (Hardware/Software), Reader (mode, tap zones, orientation, keep screen on, filter, crop borders), Storage (caches + clear), About (licenses, privacy, terms, diagnostics).

---
## 11. Monetization (isolated, last)
- `interface AdProvider { suspend fun init(); fun nativeSlot(); fun showInterstitial(...) ; fun showRewarded(...) }` with a `NoOpAdProvider` default. Real network (AdMob or AppLovin MAX) implemented behind a build flag. **Day-0 check:** confirm the chosen network and Play Console can pay out in the owner's country; confirm Play developer registration is possible [VERIFY].
- Consent: Google UMP flow for EEA/UK before the first ad request; not child-directed.
- Placements: native slots in library lists (every ~10 items), interstitial at most once per ~15 min and only on leaving the player/reader, optional rewarded action. **Never** during playback or reading, never on app open, never mid-gesture.
- Ads are implemented only after Day-3 core gates pass; the core never depends on them.

---
## 12. Test & verification strategy (no personal phone required) — SUPERSEDED IN DETAIL BY §17
1. JVM unit tests: gestures→actions, state machine, shader chain builder + patched-shader static checks, governor with simulated drops/thermal, PageProvider sorting/limits, tiling/cache/queue, URL/extractor logic, settings migrations.
2. Robolectric/Compose tests: navigation, predictive back, settings persistence, reader mode switching with `FakePageProvider`, intent routing.
3. CI static checks: Android Lint, detekt, ktlint; dependency license allowlist (fail on GPL/AGPL/non-commercial); 16 KB alignment check on all `.so`; `grep` for forbidden strings ("MX Player", "Mihon", "Tachiyomi" outside notices); shader header validity.
4. Emulator smoke test (x86_64, only if native libs include x86_64): app starts, opens bundled CBZ and PDF, no crash. mpv GPU rendering is NOT expected to work there; test only graceful failure.
5. Firebase Test Lab Robo on real devices (manual workflow dispatch; free daily quota [VERIFY limits]); download crash logs/videos as CI artifacts.
6. Human device sessions (use `docs/device-test-checklist.md`; ~30 min each on any borrowed phone): playback matrix (H.264/HEVC/VP9/AV1, 480p/720p/1080p, 10-bit), subtitles (SRT/ASS), gestures, rotate/lock/PiP/background/headset unplug, enhancement Anime/Standard with screenshots ON/OFF, 100-page webtoon, 300-page PDF, Web Source on 3 pages, airplane-mode failures, low storage. The owner pastes the Diagnostics report back; the agent fixes by report.
7. Evidence rule: a task is DONE only with a green CI run id recorded in `docs/progress.md`. Anything verifiable only on a device goes into the checklist as "UNVERIFIED".

---
## 13. Legal gates (single place; they must not block quality work — if unclear, swap to the nearest permissive equivalent and continue)
- Native libs: LGPL-clean mpv/FFmpeg per D1; dynamic linking only; ship notices; offer relinking info/source links in-app; keep the build recipe in the repo.
- `THIRD_PARTY_NOTICES.md` + in-app Licenses screen generated from `licenses.json` (name, license, URL, text): mpv/FFmpeg/libass family, Anime4K (MIT), FSRCNNX/FSR/SSim* (verify each), NCNN (BSD-3), model weights (verify), Mihon code (Apache-2.0), OkHttp/Coil/commons-compress/etc., fonts and icons.
- No "MX", "Mihon", "Tachiyomi" in UI/strings/package. Original icon/name.
- Codec patents (HEVC etc.): prefer platform hardware decoders; do not bundle extra codec encoders.
- Store: privacy policy page (GitHub Pages), Data Safety form, foreground-service declaration (media playback), photo/video permission declaration if requested [VERIFY], content rating, ads declaration.
- Play account: a new personal developer account may be required to run a closed test with a minimum number of testers for a minimum period before production access [VERIFY current numbers]; start the account on Day 0. This sets the real store-launch date regardless of build speed.

---
## 14. opencode operating protocol
**15-line `AGENTS.md` (put in repo root):**
```
Read MASTER_PLAN.md before any task. One task (MS id) per session; start fresh.
Build and test ONLY through GitHub Actions (never run Gradle locally).
Read the task row in §15 + the sections it cites; do nothing outside it.
No file > 600 lines. Pure logic goes in plain Kotlin with JVM tests.
Never use vo=mediacodec_embed. Never use GPL/AGPL code or non-commercial model weights.
[VERIFY] items must be confirmed in real source first; record results in docs/progress.md.
Task done = green CI + entry in docs/progress.md (CI run id, tests added, UNVERIFIED-on-device list).
If blocked or time-box exceeded: use the written fallback, note it, continue.
Commit "MS-xxx: summary" and tag MS-xxx-done. Every 3 tasks: run the full test suite and re-read the plan.
```
**Per-task prompt template:** "Execute MS-xxx from MASTER_PLAN.md §15. Cite the sections used. Write tests first where the task row says so. Report: files changed, CI run id, UNVERIFIED items."

---
## 15. Schedule and tasks (each ≤ 45 min human-equivalent; one fresh session each)
Time frame: 3 build days + 2 explicit buffer days (see §17.10). The CI round-trip is the bottleneck: use the CI tiers in §17.1 (T1 per push, T2 at gates, T3 manual).

### Day 0 — owner actions (before the agent starts)
H1 GitHub repo + Actions on; H2 take MX Player screenshots (every screen, settings page, player overlays, menus) into `docs/reference/`; H3 install opencode, pick a vision-capable model; H4 decide app name + package id; H5 create Play Console account now (§13); H6 Firebase project for Test Lab (service-account secret in GitHub); H7 arrange ONE phone session (~60 min) for the very end (§17.8); H8 generate/upload keystore secrets (the agent writes the script).

### Day 1 — foundation + player
| ID | Task | Needs | Done when |
|---|---|---|---|
| MS-001 | Repo, Gradle KTS + version catalog, modules + flavors, SDK 36/26 | H1 | CI-built debug APK installs target |
| MS-002 | CI: build, lint, detekt, ktlint, unit tests, license allowlist, 16 KB check, forbidden-string grep | 001 | CI green on shell |
| MS-003 | `docs/feature-spec.md` from screenshots + `docs/decisions.md` skeleton | H2 | every reference screen/control/setting listed with our own names |
| MS-004 | D1 libmpv audit & integration decision | 001 | decisions.md has license/ABI/alignment results; AAR in build |
| MS-005 | Design tokens, theme, base components | 001 | preview screenshots (Paparazzi/Compose test) |
| MS-006 | Navigation shell, predictive back, destinations | 005 | back works in tests |
| MS-007 | `PlayerEngine` singleton + `PlayerController` + Fake | 004 | unit tests with Fake |
| MS-008 | mpv init options, CA file, fonts, shader-cache dir, log→Diagnostics | 007 | startup test passes, options logged |
| MS-009 | Surface host + attach/detach lifecycle | 008 | lifecycle matrix tests |
| MS-010 | Open file/URL/intent → play; error taxonomy | 009 | tests for each error type |
| MS-011 | `PlayerUiState` + state machine | 007 | illegal-transition tests |
| MS-012 | Controller UI (play/pause/seek/time/auto-hide) | 011,005 | Compose tests |
| MS-013 | Gesture engine (pure) | 011 | table-driven tests |
| MS-014 | Gesture overlay UI (brightness/volume/seek/double-tap/long-press/pinch) | 013,012 | Compose tests; indicators |
| MS-015 | Lock, orientation, aspect, zoom | 012 | tests |
| MS-016 | Speed, A–B, sleep timer, loop | 011 | tests |
| MS-017 | Audio/subtitle track sheet + external subtitles | 011 | tests with fake track-list |
| MS-018 | Subtitle style + delays → mpv props | 017 | prop-mapping tests |
| MS-019 | Resume (Room) + continue-watching data | 011 | DAO tests |
| MS-020 | Library: MediaStore folders, permissions (13/14), SAF tree | 006 | Robolectric permission-state tests |
| MS-021 | Playlist/auto-next | 019,020 | tests |
| MS-022 | MediaSession service, notification, audio focus, noisy | 011 | tests with fake focus |
| MS-023 | PiP | 022 | tests |
| MS-024 | Settings (final list) with DataStore | 005 | persistence tests |
| MS-025 | Diagnostics screen + crash logger + share | 008 | tests |
**Day-1 gate:** CI green; FTL Robo run; T2+T3 harness run #1 (playback, subtitles, gestures, background, PiP).

### Day 2 — enhancement + reader + web source
| ID | Task | Needs | Done when |
|---|---|---|---|
| MS-026 | Shader assets + `patch_shaders.py` + static check test | 004 | patched files pass checks; licenses recorded |
| MS-027 | `ShaderChainBuilder` (pure) | 026 | tests: (mode, rung, source height) → exact list/opts |
| MS-028 | `EnhanceController` (apply/clear, async prep, cache dir, error fallback) | 027,008 | tests with Fake engine |
| MS-029 | Enhance sheet UI + 1440p availability rule | 028,012 | Compose tests |
| MS-030 | Governor | 028 | simulated-input tests incl. no step-up |
| MS-031 | Hold-to-compare | 028 | tests |
| MS-032 | Enhance edge cases: surface loss, seek, rotate, decoder fallback | 028,009 | lifecycle tests |
| MS-033 | `PageProvider` + models + fakes | 001 | tests |
| MS-034 | Archive + Folder providers, natural sort, limits | 033 | zip-bomb/sort tests |
| MS-035 | PDF provider (+password, tiles, cache) | 033 | tests with sample PDFs |
| MS-036 | UrlList provider | 033 | MockWebServer tests |
| MS-037 | Mihon viewers extraction (3 h box) or fallback viewers | 033 | forbidden-symbol grep passes; viewer tests |
| MS-038 | Reader UI: modes, settings, tap zones, progress, filters | 037,005 | Compose tests |
| MS-039 | Reader memory management + synthetic strip/PDF tests | 038 | no OOM in test harness |
| MS-040 | Intent routing (Share/Open With) | 034,035,036,010 | routing tests |
| MS-041 | Reader history/progress (Room) | 038 | DAO tests |
| MS-042 | Web Source: browser screen + hardening | 006 | hardening tests/config checks |
| MS-043 | Web Source: extraction JS + scroll-load + filters + preview grid | 042 | HTML-fixture tests |
| MS-044 | Web Source: downloader with cookies/referer + errors | 043,036 | MockWebServer tests |
**Day-2 gate:** CI green; FTL Robo; T2+T3 harness run #2 (Anime/Standard on real clips with ON/OFF screenshots, reader modes, PDF, Web Source on 3 pages).

### Day 3 — image model, release, monetization shell
| ID | Task | Needs | Done when |
|---|---|---|---|
| MS-045 | NCNN integration (prebuilt libs, JNI, Vulkan/CPU) | 001 | init test + graceful failure |
| MS-046 | Model packaging, candidate comparison artifacts, license record | 045 | side-by-side images in CI artifacts; chosen model recorded |
| MS-047 | Tiler/stitcher + stub-model golden test | 045 | seam-free test |
| MS-048 | Queue/prefetch/cancel/cache | 047 | priority/cancel/eviction tests |
| MS-049 | Reader enhance UI + hold-to-compare + thermal/battery guard | 048,038 | Compose tests |
| MS-050 | `AdProvider` + NoOp + consent flow scaffold (network impl behind flag) | 024 | tests; no ad code path in NoOp |
| MS-051 | Licenses screen + notices generator + CI check | 002 | screen renders all entries |
| MS-052 | Privacy/Terms pages, store listing text (honest claims), Data Safety notes | — | docs ready |
| MS-053 | R8/shrink rules (mpv/NCNN JNI), release signing in CI, AAB + APK artifacts | 004,045 | release build passes smoke test |
| MS-054 | Full FTL Robo run + fix crashes | all | no unresolved crash |
| MS-055 | `docs/device-test-checklist.md`, `docs/tuning.md`, final `docs/progress.md` | all | complete |
| MS-056 | Buffer: fixes from harness run #3 and the Final Device Session (§17.8) | 055 | checklist items closed |

Out of 1.0 (explicit): trackers, backup/restore, Chromecast, SMB/FTP/WebDAV, subtitle download, equalizer UI, TV UI, content auto-detection, per-folder memory, page-level instant GPU sharpen, RAR (unless license confirmed), EPUB.

---
## 16. Risk register (with pre-approved fallbacks)
| Risk | Response |
|---|---|
| No LGPL-clean libmpv artifact | Build via mpv-android buildscripts in CI; if > 4 h, stop and report |
| `.so` not 16 KB-aligned | Rebuild with correct linker flags or pick another artifact |
| Shader passes too heavy on some phones | Governor ladder; if a rung fails compile, persisted skip |
| Shader compile stalls | Async "Preparing enhancement…" + shader cache dir |
| Green frames on some SoCs | Decoder → Software escape hatch; TV boxes unsupported |
| Mihon extraction drags on | Fallback viewers after 3 h |
| WebView extraction breaks on a site | Preview grid + manual selector + user can add URL lists instead |
| Store rejects Web Source | Build `play` flavor with the flag off; ship `direct` flavor with it |
| Play account hurdles (country/testing requirement) | Start Day 0; ship GitHub builds meanwhile |
| Ads hurt stability/review | Isolated provider; NoOp default; enable last |
| Unverified [VERIFY] item fails | Use the written fallback, never improvise silently |


---
# 17. AMENDMENTS v2 (authoritative: where this section conflicts with earlier text, this section wins)

## 17.0 Owner constraint and what it implies
No personal phone and no local emulator. A phone is used ONCE, at the very end (Final Device Session, §17.8). Therefore everything below exists so that, by that session, bugs are already found and located, and the session only confirms and tunes. Evidence that a task is done = green CI run id, and for device-bound behavior = a T2/T3 harness result (not a guess).

## 17.1 CI tiers (keeps the 10–20 min CI round-trip from eating the schedule)
- **T1 – every push (target < 8 min):** compile changed modules, JVM unit tests of changed modules, detekt, ktlint, forbidden-string grep. Gradle build cache + configuration cache, one ABI.
- **T2 – at each gate and nightly:** everything in T1 for all modules + Robolectric + screenshot tests (Roborazzi or Paparazzi [VERIFY]) + CI emulator instrumented suite (§17.2) + headless desktop-mpv suite (§17.4) + native verifiers (§17.5) + fuzz/property tests (§17.6) + license/lint/manifest checks.
- **T3 – manual dispatch, at most twice per day:** Firebase Test Lab real-device runs (§17.3).
Rule: agents never wait for T2/T3 after every task; they run at gates (Day 1, 2, 3 and buffer days).

## 17.2 CI emulator suite (the "emulator" lives in GitHub Actions, not on the owner's laptop)
- Use `reactivecircus/android-emulator-runner` on Linux runners with KVM enabled (add the udev KVM permission step); hardware acceleration works on 2-vCPU and larger Linux runners, and is free for public repos [VERIFY current private-repo terms].
- Native libs must include **x86_64** for debug/CI builds (require this in D1; if the chosen AAR lacks it, build libmpv ourselves with x86_64, or test on an API 30+ x86 image with ARM translation [VERIFY]).
- Start the emulator with a software GL backend (`-gpu swiftshader_indirect`) [VERIFY mpv's `gpu-context=android` initializes on it]. If it does not, mark all render tests UNVERIFIED-in-CI and rely on §17.3; the non-render suites still run.
- Instrumented scenarios (UiAutomator + androidTest), each asserting: no `FATAL EXCEPTION`/native crash in logcat, no ANR, `time-pos` advances, and the captured screenshot is not blank (luminance variance above a threshold):
  1. play synthetic clip → rotate ×3 → home → return → lock/unlock → PiP in/out → background audio start/stop;
  2. toggle Enhance Off/Anime/Standard 20× while playing, including during seek;
  3. seek storm (200 random seeks), 30 sequential videos (memory growth check), force-stop and restore;
  4. reader: open generated CBZ/PDF/7z/webtoon strip, switch all modes, rapid page turns, rotate;
  5. Web Source against a local test server (lazy images, srcset, background-image, cookie/referer check);
  6. permission states (granted, denied, Android 14 partial).
- Use StrictMode in debug, and LeakCanary instrumentation for leak assertions [VERIFY artifact name].

## 17.3 Firebase Test Lab (real devices, no ownership needed)
- Budget (verify on the official page when starting): Spark plan ≈ 15 runs/day total (10 virtual, 5 physical); Blaze begins with a no-cost allowance (~30 min/day physical, ~60 min/day virtual) then $5/h physical and $1/h virtual; a physical run is capped at 45 min.
- Plan the matrix once, reuse it: (a) low-end 2–3 GB RAM device, Android 10–11; (b) mid-range Android 13/14; (c) recent Android 16; (d) one tablet ≥ 600 dp. Max 4 devices per run.
- Run the SAME androidTest suite from §17.2 on physical devices (instrumentation, not only Robo), against BOTH the debug build and the **minified release-like variant** (R8 on, debuggable off).
- The suite writes `perf.json` per device (shader-chain pass times from `vo-passes`, frame-drop counts, thermal status, decoder in use, time-to-first-frame, NCNN time per tile/page, peak memory) into the pulled-directories folder; a script merges them into `docs/tuning.md` automatically. This gives real device performance data without owning a phone.
- Also run Robo for crash discovery; download videos/logcat as CI artifacts.

## 17.4 Headless desktop-mpv suite (Linux CI; no phone, no emulator)
Run desktop mpv (version as close as possible to the one inside the AAR; ideally built from the same source in CI) under Xvfb with Mesa software GL (llvmpipe), the same technique other projects use for headless GL screenshot tests [VERIFY exact mpv flags: e.g., `--vo=gpu --gpu-context=x11egl --opengl-es=yes` or `--vo=gpu-next --gpu-api=vulkan` with lavapipe].
1. **Option lint:** every startup option/property used in §3.4 and §6 is accepted by that mpv version (catches typos and renamed options before they break init on devices).
2. **Shader compile test:** every chain (Anime/Standard, each rung) loads; mpv log contains no shader/GLSL error lines; patched shaders are valid.
3. **Output regression:** render synthetic test patterns and one sample frame through each chain; compare with stored goldens using SSIM/PSNR tolerance; assert enhanced ≠ plain-scaled, no NaN/clipping (histogram checks), and no halos on the line-art pattern beyond a threshold.
4. **Subtitle rendering:** render ASS and SRT fixtures (Latin, Arabic with shaping/RTL, CJK, styled ASS) using the exact bundled fonts dir; compare with goldens to prove glyph coverage and RTL correctness.
Limits (state them in the test README): llvmpipe is not a mobile GPU; use it for correctness only, never for performance numbers.

## 17.5 Native and release verifiers (no device)
`tools/verify_native.sh` runs on every build:
- every `.so`: ELF LOAD segment alignment ≥ 16 KB, ABI present, size budget, no GPL/AGPL/"nonfree" license strings (`strings`), expected exported symbols;
- JNI vs R8: list `Java_*` exports (`nm -D`) and class/method names that native code looks up (`strings` for class paths) and assert they exist, unrenamed and un-removed in the release `mapping.txt`; keep-rules cover native→Java callbacks (not only `native` methods);
- `bundletool validate` on the AAB; `aapt2 dump badging/xmltree` checks: permissions allowlist, exported components only where intended, intent filters as in §17.7, FGS type declared, targetSdk 36, `enableOnBackInvokedCallback`.

## 17.6 Fuzz, property and architecture tests
- Jazzer (JUnit integration) [VERIFY] on: archive providers (zip/7z), natural-sort, HTML/URL extractor, subtitle-file sniffing/encoding detection, URL list parser — time-boxed 60–120 s per target in T2.
- Property tests (Kotest property [VERIFY]): tiler (any size/overlap/tile → stitched size exact, no gaps), gesture mapper (no illegal action, bounds), governor (random sample streams → never steps up, never below Off, deterministic), page-index math.
- Architecture rules (Konsist [VERIFY]): UI never imports mpv/NCNN classes; module boundaries; no `Thread.sleep` in main-thread code; no network in `:player` UI layer.
- Coverage (Kover): ≥ 80% on pure-logic packages; CI fails below.
- Static: Android Lint (`checkReleaseBuilds`), detekt, ktlint, Semgrep mobile rules, OWASP dependency-check, license allowlist, Compose accessibility checks, pseudo-locales + `ar`/`fr`/`en` screenshot tests with RTL.

## 17.7 Gaps found in review → new requirements
1. **Native crash isolation:** NCNN/Vulkan inference runs in a separate Android process (`android:process=":enhance"`) behind a bound service; image data via shared memory/file; watchdog + timeout; after 2 native crashes on a device persist "enhancement disabled on this device" and show a message. Vulkan driver crashes must never kill the reader.
2. **Test media corpus:** generated in CI with ffmpeg from synthetic sources (`testsrc`, sine; no third-party content): containers MP4/MKV/TS/AVI/WebM/FLV, codecs H.264/HEVC/VP9/AV1 (8/10-bit, 480p/720p/1080p/1440p), audio AAC/AC3/EAC3/DTS/FLAC/Opus/MP3, multi-audio, multi-subtitle (SRT/ASS/VTT/PGS where possible), 24/30/60 fps, an HDR10-tagged clip; plus generated CBZ/ZIP/7z/PDF/webtoon-strip. This is also the legal-clean test set.
3. **Subtitles done properly:** (a) sidecar subtitle discovery under scoped storage (MediaStore name/relative-path match or SAF tree), because mpv cannot see siblings of a `content://` URI; (b) legacy encodings (e.g., Windows-1256 Arabic, CP1252, CP936/932/949) via charset detection and mpv `sub-codepage` [VERIFY]; (c) bundled OFL fonts with Latin, **Arabic** and CJK coverage (CJK may be large; ship a subset or on-demand download of a font pack [VERIFY size]) — proven by §17.4.
4. **App localization:** Arabic + French + English strings, `supportsRtl=true`, mirrored layouts, bidi-safe filenames/titles in lists and the player title bar; screenshot tests for `ar`.
5. **Android 16 large screens:** for apps targeting API 36, orientation locks and `resizeableActivity=false` are ignored on displays ≥ 600 dp; an opt-out property exists for 36 but disappears for API 37, which Play is expected to require in August 2027. Do not rely on the opt-out: the player and reader must handle resize, multi-window, foldables and tablet landscape/portrait; our own orientation settings apply on phones only. Add window-size tests (compact/medium/expanded). Edge-to-edge is enforced.
6. **Player system details:** keep-screen-on while playing (and release on pause/PiP/background), partial wake lock only for background audio, `configChanges` for orientation/size/uiMode so rotation does not recreate the player, `supportsPictureInPicture`, multi-window handling.
7. **Manifest intents:** `ACTION_VIEW` for `video/*`, `audio/*`, `application/x-mpegURL`, `application/vnd.apple.mpegurl`, `application/dash+xml` over `content`, `file`, `http`, `https`; `ACTION_SEND` for URLs and files; reader types per §7.2. Default-app behavior tested via aapt2 dump.
8. **Reader navigation:** next/previous chapter for local archives/folders (sibling files in natural order), "continue to next" prompt at the last page; image format matrix and fallbacks (WebP, AVIF where platform supports it, HEIC, GIF/APNG static first frame, JPEG XL unsupported → message).
9. **Self-test mode and Final Device Session (§17.8).**
10. **Supply chain and keys:** pinned dependency versions, Gradle dependency verification metadata, keystore generated once, backed up (encrypted offline copy + CI secret); with Play App Signing use an upload key; direct-APK users need the same key forever.
11. **Direct flavor updates:** "Check for updates" that reads the latest GitHub release and opens the download page (no silent install); `play` flavor uses the platform's in-app update API.
12. **Release observability:** local crash log + "Share report"; optional opt-in crash reporting SDK only if the owner approves (privacy policy must match); Play Console vitals are free for Play builds.
13. **Accessibility:** content descriptions, ≥ 48 dp targets, contrast, TalkBack order in the controller and reader menus (automated checks + Play pre-launch report when available [VERIFY]).
14. **Model weights:** restrict candidates to the Real-ESRGAN family (same repository and license; verify the weights license in the release notes) and compare two models only; other families are out of 1.0.
15. **File-size rule:** no file > 600 lines (split earlier when it improves cohesion).
16. **Cleartext:** `network_security_config` blocks cleartext for OkHttp/WebView; HTTP pages are unsupported in Web Source 1.0 (mpv uses its own network stack, so `http://` streams still play).

## 17.8 Self-test mode and the single Final Device Session
- Settings → Diagnostics → **Run self-test** (no adb needed): plays the bundled synthetic clips for ~20 s each through hardware and software decoders and through Off/Anime/Standard, records dropped frames, shader pass times, thermal status, time-to-first-frame; runs the reader on generated archives/PDF; runs the NCNN model on a test image in the isolated process; verifies subtitle rendering of the Arabic/CJK fixtures (renders to an off-screen bitmap and reports pass/fail by luminance checks); checks audio focus and PiP availability; writes ONE shareable report (text + JSON).
- Final Device Session (≤ 60 min, any phone): (1) install the release-variant APK from CI; (2) run self-test; (3) share the report; (4) 10 manual checks from `docs/device-test-checklist.md` (rotate/PiP/background, one real movie, one anime episode with Anime/Standard ON vs OFF screenshots, one webtoon, one PDF, one Web Source page, headset unplug). The agent then fixes from the report; only known, located issues remain.
- If NO phone can be found at all: ship as a clearly labelled **Beta** on GitHub, include an in-app "Send diagnostics" button, treat the first testers as the device lab, and fix from their reports.

## 17.9 New tasks (add to §15; same rules: ≤ 45 min each, fresh session, tests first)
| ID | Task | Needs | Done when |
|---|---|---|---|
| MS-057 | CI tiers T1/T2/T3 workflows, caching, path filters, KVM step, artifacts | 002 | T1 < 8 min on a trivial change; T2/T3 dispatchable |
| MS-058 | Test media corpus generator (ffmpeg synthetic) + generated CBZ/ZIP/7z/PDF/webtoon strip | 057 | corpus produced as CI artifact with checksums; no third-party content |
| MS-059 | Headless desktop-mpv suite (option lint, shader compile, golden SSIM, subtitle render) | 026,058 | passes in T2; README states llvmpipe limits |
| MS-060 | CI emulator instrumented suite (lifecycle, toggles, seek storm, reader, web source) | 009,058,057 | scenarios in §17.2 green or marked UNVERIFIED with reason |
| MS-061 | FTL harness (instrumentation + Robo, debug + minified), `perf.json` → `docs/tuning.md` generator | 060 | one successful matrix run with merged table |
| MS-062 | `tools/verify_native.sh` (alignment, GPL strings, JNI↔R8 mapping, bundletool, aapt2 checks) | 004,053 | fails on a deliberately broken fixture |
| MS-063 | Minified release-variant instrumented smoke (play one clip, open one CBZ, one NCNN call) | 053,060 | green on emulator and FTL |
| MS-064 | Fuzz + property + architecture tests (Jazzer, Kotest, Konsist, Kover gates) | 034,043,047 | thresholds enforced in T2 |
| MS-065 | In-app self-test mode + report | 025,058 | report generated on emulator; JSON schema tested |
| MS-066 | Sidecar subtitle discovery (MediaStore/SAF) + encoding handling | 017,020 | tests with Arabic CP1256 and UTF-8 fixtures |
| MS-067 | Subtitle fonts (Latin/Arabic/CJK strategy) + visual tests | 008,059 | goldens pass for Arabic RTL and CJK |
| MS-068 | Localization ar/fr/en + RTL + locale screenshot tests | 005 | screenshots for `ar` show mirrored layouts |
| MS-069 | Large-screen/resizable/multi-window/foldable handling + window-size tests | 012 | compact/medium/expanded tests green |
| MS-070 | Keep-screen-on, wake locks, `configChanges`, FGS rule tests | 022 | lifecycle tests green |
| MS-071 | Manifest intent filters + aapt2-based tests | 010,040 | dump matches spec |
| MS-072 | Reader chapter navigation + image-format matrix | 034,038 | tests with sibling archives and each format fixture |
| MS-073 | NCNN isolated process (`:enhance`), watchdog, crash-count disable | 045 | forced native crash does not kill app; auto-disable after 2 crashes |
| MS-074 | Accessibility pass + automated checks | 012,038 | checks green in T2 |
| MS-075 | Supply chain (pinning, dependency verification), keystore backup doc, direct-flavor update checker | 053 | verification metadata committed; doc present |
Placement: Day 1 early: 057, 058, 068; with player: 066, 067, 069, 070, 071; Day 2: 059, 060, 064, 072, 074; Day 3: 061, 062, 063, 065, 073, 075.

## 17.10 Honest schedule
- **3 build days + 2 explicit buffer days.** Build days implement MS-001…MS-075 in the order above; Day 4–5 are for CI failures, T3 reruns (quota-limited), fixes from harness results, the Final Device Session, and release prep. Reason: CI round-trips and the first real-device-class results (T3) will surface issues that cannot be known in advance.
- Gate results replace the old "borrowed-phone sessions": Day-1/2/3 gates run T2 and one T3.
- Store launch time is separate from build time (Play account/testing requirements, §13).
