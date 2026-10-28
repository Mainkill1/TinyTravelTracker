#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" tests/cache-scheduling/CacheSchedulingRegression.java
java -cp "$classes" CacheSchedulingRegression \
  app/src/main/java/com/rareventure/gps2/database/cachecreator/GpsTrailerCacheCreator.java \
  tests/cache-scheduling/CacheWorker.java.fixture "$classes"
