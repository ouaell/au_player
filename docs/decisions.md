# decisions — skeleton (D1 libmpv audit lands in MS-004)

- D1 candidates (§3.1): dev.jdtech.mpv:libmpv [VERIFY], io.github.abdallahmehiz:mpv-android-lib [VERIFY], SnowMediaEnt/libmpv-android-lgpl [VERIFY existence], self-build via mpv-android buildscripts.
- Pending checks: -Dgpl=false, FFmpeg nonfree flags, libass/fribidi/harfbuzz/freetype licenses, ABIs incl. x86_64 for CI, JNI package (R8 keeps), .so 16KB alignment.
