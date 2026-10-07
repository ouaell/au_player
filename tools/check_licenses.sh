#!/bin/sh
# MS-002 §12.3 license gate: every catalog library must be allowlisted, and no
# GPL/AGPL/non-commercial marker may appear in dependency declarations.
# (LGPL is allowed for native libs per §13; full POM audit lands in MS-064.)
set -eu
fail=0
aliases=$(grep -E '^[a-z0-9-]+ = \{ module' gradle/libs.versions.toml | cut -d' ' -f1)
for a in $aliases; do
  if grep -qE "^$a = " tools/licenses/allowlist.txt; then
    echo "allowlisted: $a"
  else
    echo "check_licenses: $a missing from tools/licenses/allowlist.txt" >&2
    fail=1
  fi
done
if grep -riE 'agpl|affero|nonfree|non-free|non-commercial' gradle/libs.versions.toml \
    app/build.gradle.kts core/build.gradle.kts player/build.gradle.kts \
    enhance/build.gradle.kts reader/build.gradle.kts websource/build.gradle.kts \
    library/build.gradle.kts tools/licenses/allowlist.txt; then
  echo "check_licenses: denylisted license marker found" >&2
  fail=1
fi
if grep -r 'GPL' gradle/libs.versions.toml app/build.gradle.kts core/build.gradle.kts \
    player/build.gradle.kts enhance/build.gradle.kts reader/build.gradle.kts \
    websource/build.gradle.kts library/build.gradle.kts tools/licenses/allowlist.txt \
    | grep -v 'LGPL'; then
  echo "check_licenses: non-LGPL GPL marker found" >&2
  fail=1
fi
echo "check_licenses: done (fail=$fail)"
exit $fail
