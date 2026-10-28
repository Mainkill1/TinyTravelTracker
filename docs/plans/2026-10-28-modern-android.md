# Modern Android and GPS delivery

The OnePlus 15 reports no fixes with location permissions granted. This is still an unresolved device report: service startup, request registration and receipt of a fix must be independently observable. The previous Android 8 target and host tests do not establish modern-device support.

## Implementation

1. Upgrade to a pinned SDK 36 / AGP 8.10.1 / Gradle 8.11.1 toolchain and AndroidX, keeping minSdk 21, package identity and existing tracker data. Build debug and release variants in CI.
2. Register explicitly with the GNSS provider, set registration state only after success, and test failed registration / permission races. Update foreground location declarations, runtime permission flow, pending intents and legal background recovery. Expose waiting-for-fix versus successful delivery.
3. Migrate GPX backup and restore to Android's document picker. Preserve encrypted ZIP and stream cancellation/error semantics. Leave app-specific tracker databases in place.
4. Rebuild the pinned map native dependency for modern 64-bit / 16 KB devices if possible. Do not call a ZIP alignment change a native compatibility fix.
5. Run the complete host suite, build both APK variants, and test startup, location delivery and screen-off operation on an API 36 emulator. Review focused changes before merging. Publish a signed prerelease APK with exact limitations and checksum.

## Verification focus

- Fresh install with no grants must reach onboarding without a foreground-service crash.
- Precise permission, approximate-only permission, denial, and revocation have distinct safe outcomes.
- Background recovery obeys current foreground-service start restrictions; disabled recording stays disabled.
- No fix yet is never reported as a received GPS signal; all timestamps and intervals remain 64 bit.
- Document picker cancellation and failed output do not destroy an existing backup or database.
- Existing alpha data and certificate identity remain upgrade compatible.
- Emulator injected fixes establish Android integration, not OnePlus hardware reception or OxygenOS power policy.

Native implementation is used for the build migration, with independent work on location, document storage and native dependency compatibility. This follows the user's existing request to fix, review, merge and publish, without an additional approval gate.
