# Building Tiny Travel Tracker

Use Linux, Temurin **17.0.17+10**, the checked-in **Gradle 8.11.1** wrapper,
Android SDK **platforms;android-36**, **build-tools;35.0.0**, and platform-tools.
Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to the SDK directory, then accept
the SDK licenses with `sdkmanager --licenses`.

The app compiles and targets Android 16 (API 36), with minimum Android 5
(API 21). The build plugin is pinned to AGP **8.10.1**. Gradle verifies its
distribution checksum before running.

## Tangram provenance

`com.mapzen.tangram:tangram:0.10.1` was published to JCenter, as documented in
[upstream's versioned Android README](https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/README.md).
It no longer resolves from the project's repositories. We rebuild its native
libraries from the exact upstream source with NDK r28c for 16 KB page support,
retaining the renderer API/resources and original ABIs, and adding x86_64.
The Java font reader has a source patch for current Android variable-font XML.

- Source tag: `0.10.1`, commit `3b0463a8bbbb3a3164593a93eeff5465d0b79360`
- Rebuilt release: https://github.com/Mainkill1/TinyTravelTracker/releases/tag/tangram-0.10.1-16kb-r2
- Rebuilt AAR SHA-256: `a9037580d9284d89703ba6e038993ebe25fa5538aed1a0d672aa6c06cfb86516`

`scripts/fetch-tangram.sh` downloads the pinned rebuilt artifact into ignored
`.dependencies/` and verifies its checksum. Gradle independently verifies it.
See [native dependency instructions](docs/native-dependency.md) for exact source,
licenses, reproducible build steps, ELF checks and runtime verification limits.

A local AAR has no Maven transitive metadata. The two dependencies from
[upstream's tagged build](https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/tangram/build.gradle)
are therefore explicit: OkHttp **3.12.0** and AndroidX annotations **1.9.1** (Jetifier translates the upstream API).
Other existing direct dependency versions remain pinned. Google Maven,
Maven Central, and JitPack (Nammu 1.2.0) are still network dependencies. This is a
repeatable build with fixed tool/artifact versions, not a guarantee of byte-for-byte
identical signed APKs or permanent third-party artifact availability.

## Android and native limits

The Android platform migration includes typed foreground location service,
modern permission sequencing and document-based GPX import/export. Tracker
files remain in the existing app-specific external directory. The standalone
host checks exercise production components with Android test doubles; they
do not replace platform or physical-device testing.

Both APK variants can be built and checked with:

```sh
scripts/fetch-tangram.sh
./gradlew --no-daemon clean assembleDebug assembleRelease testDebugUnitTest testReleaseUnitTest
```

`assembleRelease` produces an unsigned release APK. A published prerelease must
be signed separately, verified with `apksigner verify`, and identify its signing
certificate. The development certificate used by alpha releases cannot update
an APK signed with another distributor's key.
