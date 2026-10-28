#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Compile the real activity helper; doubles replace only Android/app boundaries.
# No Gradle, Android SDK, or GPS regression fixtures are required (JDK 9+).
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/permissions -name '*.java' -print)
javac --release 8 -Xlint:-options -d "$classes" "${fixtures[@]}" \
  app/src/main/java/com/rareventure/gps2/{GTGActivityHelper,IGTGActivity}.java
java -cp "$classes" PermissionRegressionTest
