# visual-log (every entry from a screenshot I actually viewed)

## 2026-10-11 — legacy contact sheet (run 38120789006)
- Left: home screen = 3 default purple Material buttons on black ("Open file/URL/Web page").
  Verdict: textbook AI-slop; dies with the old UI (mihon-line replaces it). No fix (legacy).
- Right: test-pattern video frame rendering (color bars + circle). The engine pipeline works;
  the chrome around it (player UI) is legacy. No fix (legacy).

## 2026-10-11 — Mihon More tab (mihon-visual 00-diag)
- Real Mihon More screen with OUR bottom nav (Library/Videos/History/Web/More) — rebrand visible.
- Wrong: header still shows the Mihon み mark (LogoHeader uses ic_mihon). Fixed: new amber
  triangle mark (ic_au_mark) + LogoHeader swap.
- The screen is otherwise indistinguishable from Mihon (it is Mihon) — correct for Phase B.

## 2026-10-11 — Library with scanned Demo series (mihon-visual 38180934854)
- Library tab shows the "Demo" card (local-folder badge) after overflow → "Scan local folder".
  The scan action works end-to-end (seed CBZ → scan → library entry). Bottom nav rebranded.
- Earlier scan-assert failures were snackbar exact-text timing, not function (reader flow proved
  Demo appears).
- Earlier root causes, both fixed: (1) seed CBZ pushed to wrong dir (adb space-split; fixed +
  verified with ls); (2) correct local base is /sdcard/AU Player/local (uses app_name).

## 2026-10-12 — "0 chapters" mystery (mihon-visual 38182752074)
- MangaScreen for the scanned Demo shows title/author/Local-source/"In library" — but chapter list
  shows "0 chapters" at screenshot time. Gave the chapter assert a 60 s timeout (rejected: this
  Maestro version has no per-command timeout); re-enter strategy reads DB-cached chapters.

## 2026-10-12 — root cause: shell-pushed files invisible to the app (scoped storage)
- In-app File probe proved it: base dir lists [local, downloads], Demo/ exists and is a dir,
  but demoList=[] while adb-shell ls shows ch1.cbz. Dir metadata readable, children of a
  shell-owned file not — classic scoped-storage ownership. getChapterList silently [].
- MANAGE_EXTERNAL_STORAGE via appops does NOT apply (undeclared; absent from appops get).
- Product is correct as designed (real users pick a SAF tree in onboarding StorageStep).
  CI fix: storage_dir pref → app-owned internal dir (file://…/files/citest), CBZ via run-as
  base64-decode. No permission needed at all.
- Maestro shots now collected from $HOME/.maestro (numbered 1.png…); repo-bundled PNGs excluded.

## 2026-10-12 — REAL crash on reader open: stale subsampling decoder (mihon-visual 38196659076)
- Settled screenshot shows Mihon's crash screen: NoClassDefFoundError tachiyomi/decoder/ImageDecoder
  from subscaleview Decoder.init. Our imagedecoder port removed the old classes but the old
  tachiyomiorg subsampling fork still references them. Fixed the same way as upstream: moved to
  com.github.mihonapp:subsampling-scale-image-view@1b283e2bc4. Also rebranded the crash text
  (Discord → GitHub). The pixel gate correctly failed the dark frame (mean 23.6).

## 2026-10-12 — reader renders: test-pattern page + 1/3 indicator (mihon-visual 38197997050)
- Maestro reader-page.png: manga page renders (color bars), "1 / 3" indicator, Left/Right tap
  zones, fullscreen system hint overlay. The subsampling fork fix works; chapters sync works.
- The adb settled shot was BLACK by design: Mihon marks the reader FLAG_SECURE
  (registerSecureActivity), which adb screencap honors. Gate rewired to Maestro shots.
- All 3 flows passed in that run; only the (wrong) adb gate failed.

## 2026-10-12 — fullscreen scrim dims the page (mihon-visual 38199002354)
- Same reader-page content, but the system "Viewing full screen / Got it" overlay dims
  everything (mean crushed below the gate). Dismiss "Got it" in-flow (runFlow/when-visible)
  from now on. Full gate re-running as 38199758328.

## 2026-10-12 — my PNG parser lied, the app was fine (mihon-visual 38200667362)
- The "black" reader frames were Adam7-interlaced PNGs that my hand-rolled stdlib parser
  misdecoded (garbage mean/var). The actual pixels — viewed directly — show the rendered
  test-pattern page with 1/3 indicator. Gate rewritten on Pillow (handles interlace).
  Lesson: never trust a number you computed with an unverified tool; look at the image.

## 2026-10-12 — swiftshader wedges screenshot frames black (mihon-visual 38199758328)
- Same flow green twice, but one run's Maestro shot came back pure black (RGB mean 0.3).
  Composer flake ("Failed to find ColorBuffer" in logs), not app state. Gate now shoots 3×
  and passes on ANY good frame. The good frame from the prior run proves rendering.

## 2026-10-12 — Phase B GATE MET (mihon-visual 38201760941)
- All 3 flows green; pixel gate green on real numbers (mean=119.0 std=88.3 ×3).
- Contact sheet reviewed panel by panel: reader pages, Library Demo card + snackbar, series
  screen, History/Library empty states, More screen, Videos/Web placeholders. Real Mihon
  quality throughout, no black/empty/default screens. Promoting mihon-line to main.

## 2026-10-12 — PLAYBACK PROVEN (mihon-visual 38223901993, VISUAL PASS)
Viewed every frame. This is the Phase C gate: self-built LGPL libmpv v0.41.0
playing a MediaStore video inside the Mihon shell.

- `tab-videos.png` / `videos-library.png`: real platform thumbnail (testsrc2 bars),
  title "Sample Clip" with the extension stripped, "751 KB", "0:08" duration badge.
  Bottom nav reads Library/Videos/History/Web/More with Videos selected. Real
  content, no placeholder, no empty state.
- `player-controls.png`: player chrome over a live frame. Top bar has Back and
  "Enhance: OFF"; transport shows **Pause**, which is the honest label only while
  playing. Burned-in source timecode reads `00:00:00.000` frame 0.
- `player-frame-a.png`: controls auto-hidden, clean fullscreen. Source timecode
  reads `00:00:04.667` frame **112**. Against frame 0 in the earlier shot that is
  unambiguous motion — not a loaded first frame, not a black surface.
- Gate numbers: player frames `mean=56.3 std=103.7` (was 21.4/69.2 when the
  stream failed to open), and `compare -metric AE` proves the two frames differ.
- Reader untouched by all this: `mean=119.0 std=88.3`, unchanged from Phase B.

Defects seen in these frames, both still open:
- The seek slider bleeds a purple sliver past the left screen edge
  (`PlayerControls` has no horizontal inset). Cosmetic; Phase D polish.
- A 16:9 clip is letterboxed with large black bars in portrait. Correct scaling,
  but the player does not rotate to landscape — a real gap vs MX/REX.

Two crashes found by this loop, not by reading code:
- S3-001 `UnsatisfiedLinkError`: PlayerActivity applied mpv startup options
  before the native library was loaded and before a handle existed. The bridge
  already does this inside `init()`.
- S3-002 `android_content: application context is not set`: FFmpeg could not
  resolve the `content://` URI until `av_jni_set_android_app_ctx` was wired
  through a new `nativeSetAppContext` JNI entry point.
