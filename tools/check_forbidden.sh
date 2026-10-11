#!/bin/sh
# MS-002 §12.3 forbidden-string gate: no copied brand names in code/UI/strings.
# Excludes MASTER_PLAN.md + docs/ (plan and evidence legitimately discuss the
# reference app) and generated license notices (THIRD_PARTY_NOTICES/MS-051).
# Reader-scoped §7.1 check: no Mihon DB/extension/tracker remnants in :reader.
set -eu
fail=0
# (Self-excluded: this script necessarily names the patterns it searches for.)
if git grep -n -I -e 'MX Player' -e 'Mihon' -e 'Tachiyomi' -- . ':!MASTER_PLAN.md' ':!docs' ':!tools/check_forbidden.sh'; then
  echo "check_forbidden: brand string found outside allowed docs" >&2
  fail=1
else
  echo "check_forbidden: brand strings clean"
fi
if git grep -n -I -e 'tachiyomi\.source' -e 'DatabaseHandler' -- reader/; then
  echo "check_forbidden: reader remnant found" >&2
  fail=1
else
  echo "check_forbidden: reader remnants clean"
fi
exit $fail
