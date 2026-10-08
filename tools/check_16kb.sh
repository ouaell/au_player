#!/bin/sh
# MS-002 §17.5 + OPENCODE_COMMAND correction: the Play 16 KiB rule applies to
# 64-bit ABIs (arm64-v8a, x86_64) only. 32-bit libs are reported, never failed.
set -eu
if [ "$#" -eq 0 ]; then
  echo "usage: check_16kb.sh <apk|aab>..." >&2
  exit 2
fi
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
found=0
for art in "$@"; do
  if [ ! -f "$art" ]; then
    echo "check_16kb: artifact not found: $art" >&2
    exit 2
  fi
  unzip -o -q "$art" '*.so' -d "$tmp" || true
done
if [ -z "$(find "$tmp" -name '*.so' -print -quit)" ]; then
  echo "check_16kb: no native libs in artifacts (ok for MS-002 shell)"
  exit 0
fi
python3 - "$tmp" <<'EOF'
import os, struct, sys, glob
fails = []
for so in glob.glob(os.path.join(sys.argv[1], '**', '*.so'), recursive=True):
    with open(so, 'rb') as f:
        data = f.read(64)
    is64 = data[4] == 2
    le = data[5] == 1
    if not le or data[0:4] != b'\x7fELF':
        fails.append((so, 'not a little-endian ELF'))
        continue
    endian = '<' if le else '>'
    if is64:
        eh = struct.unpack(endian + 'HHIQQQIHHHHHH', data[16:64])
        fmt, size = endian + 'IIQQQQQQ', 56
    else:
        eh = struct.unpack(endian + 'HHIIIIIHHHHHH', data[16:52])
        fmt, size = endian + 'IIIIIIII', 32
    phoff, phentsize, phnum = eh[4], eh[8], eh[9]
    with open(so, 'rb') as f:
        for i in range(phnum):
            f.seek(phoff + i * phentsize)
            ph = struct.unpack(fmt, f.read(size))
            if ph[0] == 1 and ph[7] < 0x4000:
                if is64:
                    fails.append((so, 'LOAD align %#x < 0x4000' % ph[7]))
                    break
                print('INFO-32BIT: %s (LOAD align %#x, not required)' % (so, ph[7]))
                break
for so, why in fails:
    print('MISALIGNED: %s (%s)' % (so, why))
sys.exit(1 if fails else 0)
EOF
echo "check_16kb: all 64-bit native libs 16 KiB-aligned"
