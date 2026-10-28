#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/regression -name '*.java' -print)
javac --release 8 -Xlint:-options -d "$classes" "${fixtures[@]}" \
  app/src/main/java/com/rareventure/gps2/GpsTrailerGpsStrategy.java \
  app/src/main/java/com/rareventure/gps2/BatteryTrackingController.java
java -cp "$classes" com.rareventure.gps2.GpsStrategyRegressionTest
java -cp "$classes" com.rareventure.gps2.BatteryTrackingRegressionTest
