#!/usr/bin/env python3
"""Apply AU LGPL patches to a pristine official buildscripts tree.

Each replacement must match EXACTLY ONCE — any drift fails loudly instead
of silently building a GPL binary. Usage: apply_patches.py <buildscripts-dir>
"""
import pathlib
import sys

PATCHES = [
    (
        "scripts/ffmpeg.sh",
        "--disable-static --enable-shared --enable-{gpl,version3}",
        "--disable-static --enable-shared --disable-gpl",
    ),
    (
        "scripts/mpv.sh",
        "-Dlibmpv=true -Dcplayer=false \\",
        "-Dlibmpv=true -Dcplayer=false -Dgpl=false \\",
    ),
    (
        "include/download-deps.sh",
        "[ ! -d dav1d ] && git clone https://github.com/videolan/dav1d",
        "[ ! -d dav1d ] && git clone --depth 1 -b 1.5.4 https://github.com/videolan/dav1d",
    ),
    (
        "include/download-deps.sh",
        "[ ! -d libass ] && git clone https://github.com/libass/libass",
        "[ ! -d libass ] && git clone --depth 1 -b 0.17.5 https://github.com/libass/libass",
    ),
    (
        "include/download-deps.sh",
        "[ ! -d libplacebo ] && git clone --recursive https://github.com/haasn/libplacebo",
        "[ ! -d libplacebo ] && git clone --depth 1 --recurse-submodules "
        "--shallow-submodules -b v7.360.1 https://github.com/haasn/libplacebo",
    ),
    (
        "include/download-deps.sh",
        "[ ! -d mpv ] && git clone https://github.com/mpv-player/mpv",
        "[ ! -d mpv ] && git clone --depth 1 -b v0.41.0 https://github.com/mpv-player/mpv",
    ),
]


def main() -> int:
    root = pathlib.Path(sys.argv[1])
    for rel, old, new in PATCHES:
        path = root / rel
        text = path.read_text(encoding="utf-8")
        count = text.count(old)
        if count != 1:
            print(f"PATCH DRIFT: {rel} has {count}x (want 1x): {old!r}", file=sys.stderr)
            return 1
        path.write_text(text.replace(old, new), encoding="utf-8")
        print(f"patched: {rel}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
