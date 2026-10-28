# Android build

This restores the existing SDK 26 application build. Use Linux, Temurin
**8.0.504+1**, the checked-in **Gradle 5.1.1** wrapper, Android SDK
**platforms;android-26**, **build-tools;28.0.3**, and platform-tools. Accept the
Android SDK licenses with `sdkmanager --licenses` using a recent JDK before
switching `JAVA_HOME` to JDK 8. Set `ANDROID_HOME` to the SDK directory.
AGP remains **3.4.0**; its [documented defaults](https://developer.android.com/build/releases/agp-3-4-0-release-notes)
are Gradle 5.1.1 and Build Tools 28.0.3.

From the repository root:

```sh
scripts/fetch-tangram.sh
./gradlew --no-daemon clean assembleDebug testDebugUnitTest
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. The workflow
`.github/workflows/android-build.yml` runs this actual APK build and uploads it.
The debug APK uses a development signing key and is not a store release.
The existing unit suite currently contains only an example arithmetic test;
this build does not replace device testing of recording or map rendering.

## Tangram provenance

`com.mapzen.tangram:tangram:0.10.1` was published to JCenter, as documented in
[upstream's versioned Android README](https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/README.md).
It no longer resolves from the project's repositories. We use the **same version's
official Android release asset**, without changing the renderer API:

- Release: https://github.com/tangrams/tangram-es/releases/tag/0.10.1
- Tag commit: `3b0463a8bbbb3a3164593a93eeff5465d0b79360`
- Asset: https://github.com/tangrams/tangram-es/releases/download/0.10.1/tangram-android-0.10.1.aar
- SHA-256: `88ef5803f006bb5e1d84409f615dddb034bbd7afe8b9e5681053b43725439c14`

The fetch script downloads over HTTPS into ignored `.dependencies/`, verifies
SHA-256 before installing the file, and rejects a changed cached artifact.
Gradle independently verifies the file before configuration. The checksum pins
bytes obtained from the official release; it is not an upstream signature or a
claim that the binary was independently rebuilt from source. If the upstream
asset disappears, restore these exact bytes from a trusted local archive rather
than substituting an unreviewed mirror.

A local AAR has no Maven transitive metadata. The two dependencies from
[upstream's tagged build](https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/tangram/build.gradle)
are therefore explicit: OkHttp **3.12.0** and support-annotations **27.1.1**.
Other existing direct dependency versions remain pinned. Google Maven,
Maven Central, and JitPack (Nammu 1.2.0) are still network dependencies. This is a
repeatable build with fixed tool/artifact versions, not a guarantee of byte-for-byte
identical signed APKs or permanent third-party artifact availability.

## Android and native limits

Compile and target SDK remain **26**, minimum SDK **21**. Raising the target
SDK and validating current Android behavior are separate changes.
The official AAR includes `libtangram.so` and `libc++_shared.so` for
**armeabi-v7a**, **arm64-v8a**, and **x86**, with no **x86_64** binaries.
These are prebuilt native libraries; this build does not compile native code.
Inspection with `readelf -Wl` found 64 KB (`0x10000`) LOAD alignment for the
arm64 libraries, and 4 KB (`0x1000`) for the ARMv7 and x86 libraries. However,
arm64 `libc++_shared.so` has a GNU_RELRO end at `0x111000`, which is not a
multiple of 16 KB. It fails the current documented RELRO alignment condition.
The arm64 Tangram library's RELRO ends at `0x64c000`.
Do **not** treat this artifact as 16 KB compatible. A native dependency rebuild
or upgrade and device verification are still needed. No 16 KB page-size device
or current Android runtime has been tested here.
See [Android's 16 KB guidance](https://developer.android.com/guide/practices/page-sizes)
before treating this legacy APK as compatible with 16 KB devices or store rules.
