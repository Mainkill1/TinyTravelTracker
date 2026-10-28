# Tangram native dependency

The app preserves Tangram ES 0.10.1's Java API and rebuilds its JNI libraries with
Android NDK r28c. The official 2018 AAR bundles an ARM64 C++ runtime whose RELRO
protection overlaps writable data on a 16 KB page. Its large LOAD alignment alone
does not establish compatibility.

## Rebuilding

On Linux x86_64, install Git, curl, CMake, Ninja, Python 3, unzip, and sha256sum, then
run:

```sh
scripts/build-tangram-16kb.sh
```

The script downloads checksum-pinned official inputs, checks out Tangram commit
`3b0463a8bbbb3a3164593a93eeff5465d0b79360` (tag `0.10.1`), and initializes Android
core submodules at their recorded commits. No upstream source patches are needed.
The NDK archive is r28c / `28.2.13676358`, SHA-256
`dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f`.
The original AAR SHA-256 is
`88ef5803f006bb5e1d84409f615dddb034bbd7afe8b9e5681053b43725439c14`.

Builds use Android API 21, C++14 (required by the original dependencies), PIC, and
both `-z,max-page-size=16384` and `-z,common-page-size=16384`. Debug information is
disabled before linking and source/toolchain paths are mapped to stable relative
paths. The original `armeabi-v7a`, `arm64-v8a` and `x86` ABIs are retained; `x86_64` is added for modern emulator testing.
The rebuilt `libtangram.so` and NDK `libc++_shared.so` are stripped of unnecessary
symbols; no ELF headers or segment contents are patched.

Output: `.dependencies/tangram-android-0.10.1-16kb.aar`. The script prints its
SHA-256. Java classes, resources, manifest, and consumer ProGuard rules remain
byte-identical to the original AAR. The ZIP uses sorted entries, fixed timestamps,
fixed permissions, and stored compression so packaging is independent of zlib.

Build cache defaults to
`~/.cache/tiny-travel-tracker/tangram-16kb-r1` (several GB). Optional environment
variables:

- `TANGRAM_CACHE_DIR`: cache/source/build location.
- `TANGRAM_JOBS`: parallel compiler jobs, default 4.
- `ANDROID_NDK_HOME`: an existing NDK r28c installation; otherwise the script
  downloads the pinned Linux archive into its cache.

The verified build used CMake 3.31.6, Ninja 1.12.1, and Python 3.13.5. Keep these
versions when reproducing a published artifact checksum; a different host tool
version can change generated binaries even when the inputs and behavior match.
Rebuilding ARM64 in a second build directory produced a byte-identical stripped
library; repeating the complete packaging also produced an identical AAR.

## Verification

```sh
python3 scripts/check-native-alignment.py .dependencies/tangram-android-0.10.1-16kb.aar
python3 scripts/check-native-alignment.py app/build/outputs/apk/debug/app-debug.apk
```

The check examines every shared library. For 64-bit libraries it requires LOAD
alignment of at least 16 KB and rejects RELRO protection that would cover mutable
data in the same 16 KB page. A RELRO segment in its own LOAD segment can be safe
even if its end is not aligned, provided no mutable data shares the rounded page.
32-bit libraries are reported separately because 16 KB Android devices use
64-bit ABIs.

The official AAR fails this check; the rebuilt AAR passes. The 59 exported JNI
functions match the original library for all three original ABIs. Packaging checks also
verify all eight expected native libraries, ZIP integrity, and unchanged upstream
Java/resources.

The final APK still requires AGP packaging that aligns uncompressed native
libraries. Check it with SDK Build Tools 35 or newer:

```sh
zipalign -c -P 16 4 app/build/outputs/apk/debug/app-debug.apk
```

Run the app on a 16 KB device or emulator (`adb shell getconf PAGE_SIZE` must print
`16384`) and exercise map creation, rendering, panning, zooming, and tile loading.
ELF inspection establishes binary layout compatibility; it does not replace this
runtime check.

## Attribution and release

Each rebuilt AAR includes `META-INF/tangram-rebuild.json` with exact source and
submodule revisions, toolchain version, source AAR checksum, ABIs, and API level.
Upstream licenses and NDK notices are included under `META-INF/tangram-licenses`.
The omitted FreeType 2.7 license and required binary attribution are supplied in
[`third_party/tangram`](../third_party/tangram/NOTICE.txt).

Publish the verified AAR with the matching source commit/build instructions and
pin its SHA-256 in `scripts/fetch-tangram.sh`. Consumers should fetch that immutable
artifact; rebuilding the native dependency is an explicit maintenance operation.

References:

- [Tangram ES 0.10.1 source](https://github.com/tangrams/tangram-es/tree/3b0463a8bbbb3a3164593a93eeff5465d0b79360)
- [Android 16 KB page-size guidance, including RELRO](https://developer.android.com/guide/practices/page-sizes)
- [NDK r28 release notes](https://github.com/android/ndk/wiki/Changelog-r28)
