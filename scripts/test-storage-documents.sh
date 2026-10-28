#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(rg --files tests/storage/stubs -g '*.java')
sources=(
  "${fixtures[@]}"
  tests/storage/BackupDocumentStreamsTest.java
  tests/storage/BackupDocumentsTest.java
  tests/storage/BackupArchiveRoundTripTest.java
  app/src/main/java/com/rareventure/gps2/gpx/BackupDocumentStreams.java
  app/src/main/java/com/rareventure/gps2/gpx/BackupDocuments.java
)
javac --release 8 -Xlint:-options -sourcepath app/src/main/java -d "$classes" "${sources[@]}"
java -cp "$classes" com.rareventure.gps2.gpx.BackupDocumentStreamsTest
java -cp "$classes" com.rareventure.gps2.gpx.BackupDocumentsTest
java -cp "$classes" com.rareventure.gps2.gpx.BackupArchiveRoundTripTest
