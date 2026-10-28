#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT
mapfile -t stubs < <(find tests/map-overlay-pause/stubs -name '*.java' | sort)
javac --release 8 -Xlint:-options -d "$build_dir" "${stubs[@]}" \
 app/src/main/java/com/rareventure/android/SuperThread.java \
 app/src/main/java/com/rareventure/android/SuperThreadManager.java \
 app/src/main/java/com/rareventure/android/SortedBestOfIntArray.java \
 app/src/main/java/com/rareventure/util/MultiValueHashMap.java \
 app/src/main/java/com/rareventure/util/ReadWriteThreadManager.java \
 app/src/main/java/com/rareventure/gps2/reviewer/map/GpsOverlay.java \
 app/src/main/java/com/rareventure/gps2/reviewer/map/GpsTrailerOverlay.java \
 tests/map-overlay-pause/MapOverlayPauseRegression.java
java -cp "$build_dir" com.rareventure.gps2.reviewer.map.MapOverlayPauseRegression
