#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
readonly artifact='.dependencies/tangram-android-0.10.1-16kb.aar'
readonly checksum='318cc7e61f598c1a63cf0b96378ddd09f871b76e79172945182c8367c1e37622'
readonly url='https://github.com/Mainkill1/TinyTravelTracker/releases/download/tangram-0.10.1-16kb-r1/tangram-android-0.10.1-16kb.aar'

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
