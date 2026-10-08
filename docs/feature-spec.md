# feature-spec — AU Player 1.0 (MS-003)

> Source: MASTER_PLAN.md §5, §7.4, §8, §10. Owner screenshots (H2,
> `docs/reference/`) are NOT yet available, so this spec is derived from the
> plan text and must be validated screen-by-screen when H2 lands. All names
> below are our own; the reference app is used only for *what* exists.

## Naming rules (binding)

- No "MX", "Mihon", "Tachiyomi" in UI strings, package, or resources.
- Original app name, icon, colors, layout. Feature parity only.

## Home (library home)

- Continue-watching row with progress bars.
- Folders grid (MediaStore folders).
- Recent reader items row.
- Quick actions: Open file, Open URL, Web page.

## Player

### Playback

Local files, folders, URLs (http/https/HLS/DASH), playlist + auto-next,
resume, loop (one/all), A–B repeat, sleep timer, speed 0.25–4×, frame
screenshot, video info sheet.

### Controller layout (auto-hide after 3 s, never while scrubbing)

- Top bar: back, title + codec/resolution chip, audio, subtitles, more.
- Center: prev, large play-pause, next.
- Bottom: thick scrubber with thumbnail bubble, elapsed/remaining, chip row:
  speed, aspect, rotate, lock, Enhance, PiP.
- Glass panels (blur API 31+, flat translucent below).
- Enhance chip shows live state ("Anime", "Standard", "Anime · lowered").

### Gestures

| Gesture | Action |
|---|---|
| Horizontal drag | Seek |
| Left vertical drag | Brightness |
| Right vertical drag | Volume |
| Double-tap left/right | −10 s / +10 s |
| Double-tap center | Play/pause |
| Long-press | 2× while held |
| Pinch | Zoom |
| Single tap | Toggle controls |

Ignored inside system-gesture insets and while locked. Screen lock control.

### Tracks

Audio/subtitle switching, external subtitles (SRT/ASS/VTT), subtitle style
(size, color, outline, position, font), subtitle delay, audio delay, audio
boost to 200%.

### Display

Aspect modes fit/fill/stretch/16:9/4:3/custom; orientation auto/landscape/
portrait/follow-video; brightness restored on exit.

### System

Background audio with media notification, PiP, audio focus, noisy-audio
pause, hardware keys, resume after process death. HDR on SDR screens tone
mapped; Dolby Vision profile 5 shows a warning (renders incorrectly).

## Enhance sheet

Exactly three rows: Off, Anime, Standard. Initial Off; last choice remembered
app-wide (one stored value). Disabled with "Already high resolution" when
source height > 1440. First activation shows "Preparing enhancement…".

## Reader

- Modes: RTL, LTR, vertical paged, webtoon, double-page (landscape).
- Zoom, double-tap zoom, configurable tap zones, keep-screen-on, orientation
  lock, brightness, filters (grayscale, invert, sepia), crop borders.
- Thin progress bar, page slider, per-series mode memory.
- Long-press page menu: share page, save page, enhance page.
- "Resume where you left" on open; next/previous chapter for siblings.

## Web Source

- Browser screen (user browses; app never automates challenges) + "Read this
  page" action.
- Preview grid: untick/reorder/remove, then "Open in reader".
- Advanced CSS selector field.
- First-use notice: no content shipped; user responsible for pages/rights.
- "Clear browsing data" button.

## Settings (FINAL LIST — nothing else in 1.0)

Playback (resume, auto-next, background play, PiP); Gestures (seek /
brightness / volume, double-tap seconds); Subtitles (size, color, outline,
position, font, default delay); Audio (boost, preferred language, downmix);
Display (orientation, default aspect); Decoder (Hardware/Software); Reader
(mode, tap zones, orientation, keep screen on, filter, crop borders);
Storage (caches + clear); About (licenses, privacy, terms, diagnostics).

## Open validation (H2)

- [ ] Compare every section above against `docs/reference/` screenshots.
- [ ] Record deviations in `docs/progress.md` under MS-003.
