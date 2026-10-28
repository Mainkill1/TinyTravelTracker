#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
readonly artifact='.dependencies/tangram-android-0.10.1.aar'
readonly checksum='88ef5803f006bb5e1d84409f615dddb034bbd7afe8b9e5681053b43725439c14'
readonly url='https://github.com/tangrams/tangram-es/releases/download/0.10.1/tangram-android-0.10.1.aar'

verify() {
    printf '%s  %s\n' "$checksum" "$1" | sha256sum --check --status
}

if [[ -f "$artifact" ]]; then
    verify "$artifact" || { echo "Tangram checksum mismatch; remove $artifact and retry." >&2; exit 1; }
    echo "Verified $artifact"
    exit 0
fi

mkdir -p .dependencies
artifact_tmp=$(mktemp .dependencies/tangram-download.XXXXXX)
trap 'rm -f "$artifact_tmp"' EXIT
curl --fail --location --retry 3 --proto '=https' --proto-redir '=https' "$url" --output "$artifact_tmp"
verify "$artifact_tmp" || { echo 'Downloaded Tangram checksum mismatch.' >&2; exit 1; }
mv "$artifact_tmp" "$artifact"
echo "Verified $artifact"
