# decisions

## D1 libmpv audit (MS-004) — DECIDED: build ourselves (option 4)

Evidence: `d1-audit` CI run 37710115959 (report artifact `d1-audit-report.txt`),
AARs inspected 2026-10-08. Audit method: `tools/audit_libmpv.sh` (report-only).

### Candidate existence ([VERIFY] results)

- `dev.jdtech.mpv:libmpv` 1.0.0 — REAL (Maven Central, MIT wrapper,
  jarnedemeulemeester/libmpv-android, based on mpv-android, kotlin-stdlib 2.2.10).
- `io.github.abdallahmehiz:mpv-android-lib` 0.1.12 — REAL (Maven Central, MIT,
  fork of official mpv-android `library` branch; multi-instance, DASH,
  mpv_node; transitively adds appcompat 1.7.0).
- `SnowMediaEnt/libmpv-android-lgpl` — DOES NOT EXIST (search finds only
  unrelated companies). ELIMINATED without further work.

### Audit facts (both AARs)

- ABIs: arm64-v8a, armeabi-v7a, x86, x86_64 in both (x86_64 satisfies §17.2).
- FFmpeg: jdtech n8.1 / mehiz N-122742, BOTH configured with
  `--enable-gpl --enable-version3` (from binary `strings`; no
  `--enable-nonfree`/x264/x265 present; encoders/muxers/filters mostly
  disabled). mpv `-Dgpl=false`: NO evidence in either buildscripts clone
  (flag absent; mpv defaults to GPL) → treat as GPL builds.
- Gate §3.1 requires mpv `-Dgpl=false` + FFmpeg without `--enable-gpl`:
  BOTH FAIL → not LGPL-clean.
- 16KB: both MISALIGNED on armeabi-v7a + x86 (`libplayer.so`,
  `libc++_shared.so` LOAD @0x1000); arm64-v8a/x86_64 aligned. Our release
  ships armeabi-v7a → integrating either breaks our 16KB gate + Play rule.
- JNI packages (for R8 keeps in MS-053): jdtech `dev.jdtech.mpv.MPVLib`
  (singleton-style, matches §3.4 one-PlayerEngine); mehiz `is.xyz.mpv.MPV`
  (+ `BaseMPVView`, Kotlin Flow accessors; multi-instance claim noted, our
  single-engine design holds regardless).
- Permissive sub-deps in both: mbedtls (Apache-2.0), dav1d (BSD), libxml2
  (MIT), libass/fribidi/harfbuzz/freetype/fontconfig present.

### Decision

Neither candidate is LGPL-clean and 16KB-aligned → per §3.1 BUILD OURSELVES
(option 4): official mpv-android buildscripts in GitHub Actions with
LGPL-clean flags (`-Dgpl=false`, no `--enable-gpl/--enable-nonfree`), all 4
ABIs incl. x86_64, 16KB-aligned link, cached output. Reference:
ouaell/au-player-native-build (arm64-only pinned artifacts today; useful
reference, not directly consumable).

### Integration consequences

- NO third-party AAR added to `:player` (deliberate: would violate §13 and
  break the 16KB gate). Fakes carry MS-007+ unit tests until the self-built
  AAR lands; every self-built AAR must pass `tools/audit_libmpv.sh` before
  acceptance (follow-up native-build task).
