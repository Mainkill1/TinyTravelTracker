#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
artifact="${1:-.dependencies/tangram-android-0.10.1-16kb-r2.aar}"
cache="${XDG_CACHE_HOME:-$HOME/.cache}/tiny-travel-tracker/font-tests"
mkdir -p "$cache"
kxml="$cache/kxml2-2.3.0.jar"
checksum='f264dd9f79a1fde10ce5ecc53221eff24be4c9331c830b7d52f2f08a7b633de2'
if [[ ! -f $kxml ]] || ! printf '%s  %s\n' "$checksum" "$kxml" | sha256sum --check --status; then
    curl --fail --location --retry 3 --proto '=https' --proto-redir '=https' \
        https://repo.maven.apache.org/maven2/net/sf/kxml/kxml2/2.3.0/kxml2-2.3.0.jar -o "$kxml.download"
    printf '%s  %s\n' "$checksum" "$kxml.download" | sha256sum --check --status
    mv "$kxml.download" "$kxml"
fi
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
unzip -p "$artifact" classes.jar > "$classes/tangram.jar"
mapfile -t fixtures < <(find tests/tangram-fonts -name '*.java' -print)
javac --release 8 -Xlint:-options -cp "$classes/tangram.jar:$kxml" -d "$classes" "${fixtures[@]}"
java -cp "$classes:$classes/tangram.jar:$kxml" com.mapzen.tangram.FontParserRegressionTest
