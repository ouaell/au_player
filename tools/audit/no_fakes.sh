#!/bin/sh
# no-fakes-in-release gate: fakes/stubs/NoOps must not be reachable from
# release sources. The allowlist below is the CURRENT (bad) state; FX-01
# shrinks it. Any reference outside the allowlist, or any shrinkage of the
# allowlist file itself failing to match reality, fails the gate.
set -eu
cd "$(dirname "$0")/../.."
ACTUAL=$(mktemp)
ALLOW_SORTED=$(mktemp)
grep -rn --include='*.kt' -E 'Fake[A-Za-z]+|StubScreen|NoOp[A-Za-z]+|Stub[A-Za-z]+Manager' \
  app/src/main player/src/main enhance/src/main reader/src/main websource/src/main library/src/main core/src/main \
  | sed -E 's/^([^:]+):[0-9]+:[[:space:]]*/\1:/' | sort -u > "$ACTUAL" || true
ALLOW="tools/audit/fakes-allowlist.txt"
sort -u "$ALLOW" -o "$ALLOW_SORTED"
if ! diff -u "$ALLOW_SORTED" "$ACTUAL"; then
  echo "no-fakes-in-release: production fake references differ from allowlist (see diff above)"
  rm -f "$ACTUAL" "$ALLOW_SORTED"
  exit 1
fi
rm -f "$ACTUAL" "$ALLOW_SORTED"
echo "no-fakes-in-release: allowlist matches reality ($(wc -l < "$ALLOW") reference sites - all must go in FX-01)"
