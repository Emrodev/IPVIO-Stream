# ADR 0004: R8 for the libraries again

## Status

Accepted (T-007). Supersedes [ADR 0001](0001-no-r8-for-now.md). Needs a device test of the release build.

## Context

- Releases 1.0 and 1.1 were built with `optimization { enable = true; packageScope = androidx.**, kotlin.**, kotlinx.** }` (Git history, `app/build.gradle.kts`). 1.1 was published and used on devices.
- During 1.2 the scope was extended to `com.ipvio.**` (project notes, round 27b). That release build (12.7 MB) did not start on the device. R8 was then turned off completely. The cause was never found.
- Without R8, Compose runs unoptimized in the release build. This is a known, large cost for Compose apps and matches the user report "slow and jerky".
- The FFmpeg audio decoder (`org.jellyfin.media3:media3-ffmpeg-decoder`) lives in `androidx.media3.decoder.ffmpeg`, which is inside the scope. It is loaded by reflection (`SoftAudio.kt:24`, Media3 `DefaultRenderersFactory`) and its native code calls back into Java by name.
- Reflection search in `app/src/main` (`Class.forName`, `getMethod`, `newInstance`, `@Keep`, `external fun`): only `SoftAudio.kt`.

## Decision

- R8 on again with the scope of 1.0/1.1 (libraries only). The app's own code stays out of scope until the 1.2 crash is understood.
- Keep rule `-keep class androidx.media3.decoder.ffmpeg.** { *; }` in `app/src/main/keepRules/reflection.keep`.
- Baseline profile `app/src/main/baseline-prof.txt` with a wildcard rule for `com/ipvio/stream/**`, so the app's own (unoptimized) code is precompiled after installation.

## Consequences

- Faster UI in release builds (not measured; expected from the known effect of R8 on Compose).
- Debug builds are unchanged and stay much slower than release builds. Speed must be judged on a release build.
- Risk: the release build might not start, as in 1.2. Rollback: set `enable = false` again (one line). If it fails, the crash log (`adb logcat`) is needed to find the missing keep rule.
- Software audio (headphones, Bluetooth, AC3/E-AC3) must be tested on a device with the release build.
- Unverified: that `androidx.profileinstaller` is on the classpath (expected as a transitive dependency of Compose/Activity). Without it the baseline profile is only used on installs that support APK profiles.
