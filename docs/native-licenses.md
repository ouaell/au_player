# native licenses (P0, OPENCODE_COMMAND §3.4)

Self-built with `--disable-gpl` (FFmpeg) + `-Dgpl=false` (mpv). SPDX below
is per each upstream LICENSE file; full-text audit is a release gate
(MS-062+). Any GPL-only dependency is REMOVED, never shipped.

| dep | version | license | source |
|---|---|---|---|
| mpv | v0.41.0 | LGPL-2.1+ (gpl=false; project dual GPL2+/LGPL2.1+) | github.com/mpv-player/mpv |
| ffmpeg | n9.0 | LGPL-2.1+ (`--disable-gpl`, no x264/x265/nonfree) | github.com/FFmpeg/FFmpeg |
| libass | 0.17.5 | ISC | github.com/libass/libass |
| dav1d | 1.5.4 | BSD-2-Clause | github.com/videolan/dav1d |
| libplacebo | v7.360.1 | LGPL-2.1+ (no vulkan in this build) | github.com/haasn/libplacebo |
| mbedtls | 3.6.7 | Apache-2.0 | github.com/Mbed-TLS/mbedtls |
| fribidi | 1.0.17 | LGPL-2.1+ | github.com/fribidi/fribidi |
| harfbuzz | 14.5.0 | MIT (see its COPYING) | github.com/harfbuzz/harfbuzz |
| freetype | 2.14.3 | FTL (dual FTL/GPLv2 — FTL chosen) | gitlab.freedesktop.org/freetype/freetype |
| fontconfig | 2.18.3 | MIT | gitlab.freedesktop.org/fontconfig/fontconfig |
| libxml2 | 2.15.4 | MIT | gitlab.gnome.org/GNOME/libxml2 |
| unibreak | 8.0 | zlib | github.com/adah1972/libunibreak |
| lua | 5.2.4 | MIT | lua.org |
| curl | 8.22.0 | curl (MIT/X derivative) | curl.se |
| NDK | r30 | Apache-2.0 (toolchain; not shipped) | android.googlesource.com |

Wrapper (MPVLib equivalent) and JNI glue are OUR code (Apache-2.0, this
repo) — the upstream app/JNI sources are GPL and are never copied.
Notices for all of the above land in THIRD_PARTY_NOTICES (MS-051+) with the
release that first ships the self-built `.so` files.
