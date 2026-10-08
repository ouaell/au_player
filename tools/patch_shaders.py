#!/usr/bin/env python3
"""MS-026 (§6.2): produce assets/shaders/patched/* from upstream sources.

CNN luma doublers run at SOURCE resolution and must not be skipped when the
screen matches the source size, so the //!WHEN lines that gate on OUTPUT size
are removed. Everything else is copied byte-identically (static test in
tools/check_shaders.sh enforces deletions-only + intact HOOK/BIND/DESC).
"""
import pathlib
import re
import sys

WHEN_OUTPUT = re.compile(r"^//!WHEN.*OUTPUT")

UPSTREAM = pathlib.Path("enhance/src/main/assets/shaders/upstream")
PATCHED = pathlib.Path("enhance/src/main/assets/shaders/patched")


def patch_one(src: pathlib.Path, dst: pathlib.Path) -> tuple[int, int]:
    kept = 0
    dropped = 0
    with src.open("r", encoding="utf-8") as handle:
        lines = handle.readlines()
    out = []
    for line in lines:
        if WHEN_OUTPUT.match(line):
            dropped += 1
            continue
        out.append(line)
        kept += 1
    dst.parent.mkdir(parents=True, exist_ok=True)
    with dst.open("w", encoding="utf-8", newline="") as handle:
        handle.writelines(out)
    return kept, dropped


def main() -> int:
    sources = sorted(UPSTREAM.glob("*.glsl"))
    if not sources:
        print("patch_shaders: no upstream files (run fetch_shaders.sh first)", file=sys.stderr)
        return 1
    for src in sources:
        dst = PATCHED / src.name
        kept, dropped = patch_one(src, dst)
        print(f"patch: {src.name} kept={kept} dropped_when={dropped}")
        if dropped == 0:
            print(f"patch: WARNING {src.name} had no OUTPUT-gated WHEN lines", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
