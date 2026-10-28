#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Compile the real receiver with Android/app boundaries; JDK 9+ is sufficient.
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/boot -name '*.java' -print)
javac --release 8 -Xlint:-options -d "$classes" "${fixtures[@]}" \
  app/src/main/java/com/rareventure/gps2/bootup/GpsTrailerReceiver.java
java -cp "$classes" BootRecoveryRegressionTest
