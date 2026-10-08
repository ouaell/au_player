# Device test checklist (MS-055, ~30 min on any borrowed phone)

Source: MASTER_PLAN §12.6 + §17.8 Final Device Session (≤60 min total:
install release APK from CI, run self-test, share the report, then these).

1. Rotate ×3 during playback; lock/unlock; PiP in/out; background audio start/stop.
2. One real movie file: gestures (seek/brightness/volume), double-tap ±10 s, long-press 2×, pinch zoom.
3. One anime episode: Enhance Anime vs Off screenshots; then Standard vs Off.
4. Subtitles: SRT + ASS (incl. Arabic RTL fixture rendering check).
5. Headset unplug pauses; audio focus (play music over it, take a call).
6. One 100-page webtoon: webtoon mode, rapid scroll, rotate.
7. One 300-page PDF: paged mode, zoom tiles, password prompt if protected.
8. Web Source on 3 pages: scroll-load, preview grid untick/reorder, open in reader.
9. Share/Open-With: video → player; CBZ/PDF/image → reader; URL text → reader.
10. Airplane mode + low storage: sane errors, no crashes; Diagnostics → Share report.

Record: device model, Android version, pass/fail per item, screenshots,
Diagnostics report text. Agent fixes from the report only.
