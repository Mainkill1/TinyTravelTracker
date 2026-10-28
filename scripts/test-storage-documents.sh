#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -t fixtures < <(find tests/storage/stubs -name '*.java' -print)
sources=(
  "${fixtures[@]}"
  tests/storage/GpxValidationTest.java
  app/src/main/java/com/rareventure/gps2/gpx/GpxReader.java
  tests/storage/RestoreCollectorRestartTest.java
  app/src/main/java/com/rareventure/gps2/gpx/RestoreCollectorRestart.java
  tests/storage/BackupImportCancellationTest.java
  tests/storage/BackupCancellationTest.java
  app/src/main/java/com/rareventure/gps2/gpx/BackupCancellation.java
  tests/storage/PendingBackupRequestTest.java
  app/src/main/java/com/rareventure/gps2/gpx/PendingBackupRequest.java
  tests/storage/BackupDocumentStreamsTest.java
  tests/storage/BackupDocumentsTest.java
  tests/storage/BackupArchiveRoundTripTest.java
  app/src/main/java/com/rareventure/gps2/gpx/BackupDocumentStreams.java
  app/src/main/java/com/rareventure/gps2/gpx/BackupDocuments.java
)
javac --release 8 -Xlint:-options -sourcepath app/src/main/java -d "$classes" "${sources[@]}"
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.BackupDocumentStreamsTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.BackupDocumentsTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.BackupArchiveRoundTripTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.PendingBackupRequestTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.BackupCancellationTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.RestoreCollectorRestartTest
java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.GpxValidationTest

java -cp "$classes:app/src/main/resources" com.rareventure.gps2.gpx.BackupImportCancellationTest
