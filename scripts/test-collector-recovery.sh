#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/boot/stubs tests/collector -name '*.java' -print)
javac --release 8 -Xlint:-options -d "$classes" "${fixtures[@]}" \
  app/src/main/java/com/rareventure/gps2/bootup/GpsTrailerReceiver.java \
  app/src/main/java/com/rareventure/gps2/CollectorServicePolicy.java \
  app/src/main/java/com/rareventure/gps2/JobSchedulerRestarterService.java
java -cp "$classes" CollectorRecoveryRegressionTest
