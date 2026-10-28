#!/usr/bin/env bash
# Build the original Tangram JNI API with a current, checksum-pinned NDK.
set -euo pipefail
cd "$(dirname "$0")/.."
readonly root="$PWD"
readonly revision='3b0463a8bbbb3a3164593a93eeff5465d0b79360'
readonly official_sha='88ef5803f006bb5e1d84409f615dddb034bbd7afe8b9e5681053b43725439c14'
readonly ndk_sha='dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f'
readonly cache="${TANGRAM_CACHE_DIR:-${XDG_CACHE_HOME:-$HOME/.cache}/tiny-travel-tracker/tangram-16kb-r1}"
readonly source="$cache/source"
readonly output="$root/.dependencies/tangram-android-0.10.1-16kb-r2.aar"
readonly official="$cache/tangram-android-0.10.1.aar"

for tool in git curl cmake ninja python3 unzip sha256sum; do
    command -v "$tool" >/dev/null || { echo "Missing build tool: $tool" >&2; exit 1; }
done
[[ $(uname -s)-$(uname -m) == Linux-x86_64 ]] || { echo 'This pinned build supports Linux x86_64 hosts.' >&2; exit 1; }
mkdir -p "$cache" "$root/.dependencies"

download() {
    local url=$1 destination=$2 checksum=$3
    if [[ ! -f $destination ]] || ! printf '%s  %s\n' "$checksum" "$destination" | sha256sum --check --status; then
        curl --fail --location --retry 3 --proto '=https' --proto-redir '=https' "$url" -o "$destination.download"
        printf '%s  %s\n' "$checksum" "$destination.download" | sha256sum --check --status
        mv "$destination.download" "$destination"
    fi
}

download 'https://github.com/tangrams/tangram-es/releases/download/0.10.1/tangram-android-0.10.1.aar' "$official" "$official_sha"
readonly java_home="${TANGRAM_JAVA_HOME:-${JAVA_HOME:-}}"
readonly android_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n $java_home && -n $android_sdk ]] || { echo 'Set TANGRAM_JAVA_HOME to JDK17.0.17 and ANDROID_HOME to the SDK with platform36 revision2.' >&2; exit 1; }
download 'https://dl.google.com/dl/android/maven2/com/android/support/support-annotations/27.1.1/support-annotations-27.1.1.jar' \
    "$cache/support-annotations-27.1.1.jar" '3365960206c3d2b09e845f555e7f88f8effc8d2f00b369e66c4be384029299cf'
if [[ -n ${ANDROID_NDK_HOME:-} ]]; then
    ndk=$ANDROID_NDK_HOME
else
    ndk="$cache/android-ndk-r28c"
    if [[ ! -f $ndk/source.properties ]]; then
        download 'https://dl.google.com/android/repository/android-ndk-r28c-linux.zip' "$cache/android-ndk-r28c-linux.zip" "$ndk_sha"
        unzip -q "$cache/android-ndk-r28c-linux.zip" -d "$cache"
    fi
fi
ndk_version=$(sed -n 's/^Pkg.Revision = //p' "$ndk/source.properties")
[[ $ndk_version == 28.2.13676358 ]] || { echo 'Use NDK r28c (28.2.13676358).' >&2; exit 1; }
readonly toolchain="$ndk/toolchains/llvm/prebuilt/linux-x86_64"

if [[ ! -d $source/.git ]]; then
    git init -q "$source"
    git -C "$source" remote add origin https://github.com/tangrams/tangram-es.git
    git -C "$source" fetch --depth 1 origin "$revision"
    git -C "$source" checkout --detach FETCH_HEAD
fi
[[ $(git -C "$source" rev-parse HEAD) == "$revision" ]] || { echo 'Unexpected cached Tangram source revision.' >&2; exit 1; }
git -C "$source" submodule update --init --recursive core/deps
[[ -z $(git -C "$source" status --porcelain --untracked-files=no) ]] || { echo 'Cached source has tracked modifications.' >&2; exit 1; }

for abi in armeabi-v7a arm64-v8a x86 x86_64; do
    build="$cache/build/$abi"
    mkdir -p "$build"
    echo "Building Tangram for $abi (log: $build/build.log)"
    cmake -S "$source" -B "$build" -G Ninja \
        -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-21 -DANDROID_STL=c++_shared \
        -DCMAKE_BUILD_TYPE=Release -DTANGRAM_PLATFORM=android \
        -DCMAKE_CXX_STANDARD=14 -DCMAKE_CXX_STANDARD_REQUIRED=ON \
        -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
        "-DCMAKE_C_FLAGS=-g0 -ffile-prefix-map=$cache=. -ffile-prefix-map=$ndk=./ndk -ffile-prefix-map=$source=." \
        "-DCMAKE_CXX_FLAGS=-g0 -ffile-prefix-map=$cache=. -ffile-prefix-map=$ndk=./ndk -ffile-prefix-map=$source=." \
        '-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384' \
        > "$build/configure.log" 2>&1 || { cat "$build/configure.log" >&2; exit 1; }
    cmake --build "$build" --target tangram -j "${TANGRAM_JOBS:-4}" \
        > "$build/build.log" 2>&1 || { tail -80 "$build/build.log" >&2; exit 1; }
    "$toolchain/bin/llvm-strip" --strip-unneeded "$build/libtangram.so"
    case "$abi" in
        armeabi-v7a) triple=arm-linux-androideabi ;;
        arm64-v8a) triple=aarch64-linux-android ;;
        x86) triple=i686-linux-android ;;
        x86_64) triple=x86_64-linux-android ;;
    esac
    cp "$toolchain/sysroot/usr/lib/$triple/libc++_shared.so" "$build/libc++_shared.so"
    "$toolchain/bin/llvm-strip" --strip-unneeded "$build/libc++_shared.so"
done

python3 "$root/scripts/compile-tangram-font-parser.py" "$source" "$official" \
    "$cache/support-annotations-27.1.1.jar" "$android_sdk" "$java_home" "$cache/java-r2"
python3 "$root/scripts/package-tangram-16kb.py" "$source" "$ndk" "$cache/build" "$official" "$output" "$cache/java-r2"
python3 "$root/scripts/check-native-alignment.py" "$output"
sha256sum "$output"
