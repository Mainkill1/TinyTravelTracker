#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT
mapfile -t stubs < <(find tests/map-gestures/stubs -name '*.java' | sort)
javac --release 8 -Xlint:-options -d "$build_dir" "${stubs[@]}" \
 app/src/main/java/com/rareventure/gps2/reviewer/map/MyMapController.java \
 app/src/main/java/com/rareventure/gps2/reviewer/map/MyTouchInput.java \
 tests/map-gestures/MapGesturesRegression.java
java -cp "$build_dir" com.rareventure.gps2.reviewer.map.MapGesturesRegression
