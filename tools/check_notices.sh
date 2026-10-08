#!/bin/sh
# MS-051 CI check: every dependency-allowlist module must be covered by a
# THIRD_PARTY_NOTICES entry (matched by module path in Notices.kt).
set -eu
NOTICES=core/src/main/java/com/example/auplayer/core/LicenseEntry.kt
token_for() {
  case "$1" in
    core-ktx|activity-compose) echo "AndroidX" ;;
    compose-*) echo "Compose" ;;
    lifecycle-*) echo "Lifecycle" ;;
    navigation-*) echo "Navigation" ;;
    room-*) echo "Room" ;;
    datastore) echo "DataStore" ;;
    coroutines-*) echo "Coroutines" ;;
    okhttp|mockwebserver) echo "OkHttp" ;;
    coil-*) echo "Coil" ;;
    media3-*) echo "Media3" ;;
    commons-compress) echo "Commons Compress" ;;
    junit) echo "JUnit" ;;
    *) echo "" ;;
  esac
}
fail=0
aliases=$(grep -E '^[a-z0-9-]+ = \{ module' gradle/libs.versions.toml | cut -d' ' -f1)
for a in $aliases; do
  token=$(token_for "$a")
  if [ -z "$token" ]; then
    echo "check_notices: no mapping for $a" >&2
    fail=1
    continue
  fi
  if grep -q "$token" "$NOTICES"; then
    echo "notices cover: $a"
  else
    echo "check_notices: token '$token' missing for $a" >&2
    fail=1
  fi
done
exit $fail
