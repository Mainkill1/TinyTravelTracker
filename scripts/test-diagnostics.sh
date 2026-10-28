#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/diagnostics -name '*.java' -print)
javac --release 8 -Xlint:-options -d "$classes" "${fixtures[@]}" \
  tests/regression/stubs/android/os/SystemClock.java \
  tests/regression/stubs/com/rareventure/android/{AndroidPreferenceSet,GpsReader,IntentTimer}.java \
  app/src/main/java/com/rareventure/android/{TestUtil,WriteConstants}.java \
  app/src/main/java/com/rareventure/util/DebugLogFile.java \
  app/src/main/java/com/rareventure/gps2/GpsTrailerGpsStrategy.java
java -cp "$classes" com.rareventure.gps2.DiagnosticsRegressionTest
