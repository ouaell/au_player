#!/bin/sh
# MS-001 stub wrapper. CI uses system Gradle 8.13 (see .github/workflows).
# Full wrapper jar lands with supply-chain pinning in MS-075.
exec gradle "$@"
