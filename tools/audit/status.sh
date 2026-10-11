#!/bin/sh
# claims-check helper: every claim in docs/claims.json must reference an
# existing test_id (repo-relative path) and a non-empty run_id.
# Usage: sh tools/audit/status.sh [--check]
set -eu
cd "$(dirname "$0")/../.."
python3 - <<'EOF'
import json, os, sys
claims = json.load(open('docs/claims.json'))['claims']
bad = 0
print(f"{'feature':<22} {'test_id exists':<13} run_id")
for c in claims:
    ok = os.path.exists(c['test_id'])
    print(f"{c['feature']:<22} {str(ok):<13} {c['run_id']}")
    if not ok or not c['run_id']:
        bad += 1
        print(f"  FAIL: {c['feature']}: missing test file or run id", file=sys.stderr)
print(f"{len(claims)} claims, {bad} bad")
sys.exit(1 if bad else 0)
EOF
