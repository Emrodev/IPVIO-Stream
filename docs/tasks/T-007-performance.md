# T-007 Performance

Status: implemented on branch `perf/T-007`. Debug build and unit tests run in CI. Device test PENDING.

## Goal

The app feels slow and jerky (user report, 2026-11-11). Reduce work per frame and turn the release optimizer back on, without visible changes.

## Changes

| File | Change | Reason |
|---|---|---|
| `app/build.gradle.kts` | `optimization.enable = true`, scope unchanged (`androidx.**`, `kotlin.**`, `kotlinx.**`) | Compose ran unoptimized in release since 1.2. See ADR 0004 |
| `app/src/main/keepRules/reflection.keep` | new: keep `androidx.media3.decoder.ffmpeg.**` | FFmpeg decoder is loaded by reflection and called from native code |
| `app/src/main/baseline-prof.txt` | new: wildcard profile for `com/ipvio/stream/**` | precompile the app's own code after installation |
| `LiveNow.kt` | `LiveBadge`: dot pulses in 100 ms steps instead of an infinite per-frame animation; no animation at all with motion "Off" | the badge made the whole Live TV screen redraw on every frame, also while scrolling the channel list |
| `LiveNow.kt` | `LiveBackdrop` uses `CompositingStrategy.Offscreen` | the full-screen background (6 fills) is drawn once and copied per frame |
| `HomeScreen.kt` | `BillboardLayer`: backdrop with `CompositingStrategy.Offscreen` | same as above for the home billboard |
| `HomeScreen.kt` | `Hero`: slideshow progress in 40 steps instead of a per-frame animation | the home screen redrew on every frame while the big picture was visible |
| `Billboard.kt` | `billboardFocus`: `onPlaced` instead of `onGloballyPositioned` | the callback ran for every card on every scroll frame; the coordinates object is the same |
| `HomeParts.kt` | `RowPosterCard`: store coordinates with `onPlaced`, compute the position once when the card expands | same reason |

Not changed: player, playlist sources, EPG, storage, UI strings, app code R8 scope.

## Checks

- Brace and parenthesis balance of every changed Kotlin file (strings and comments removed): unchanged.
- CI (`testDebugUnitTest`, `assembleDebug`) on the branch: see the GitHub Actions run.
- Not run: release build, R8, device test.

## Test steps (maintainer)

1. Build the **release** APK (`gradlew.bat assembleRelease`) and install it. Judge speed only on the release build; the debug build ("IPVIO Stream" with app ID `.test`) is always much slower.
2. App starts? If not: `adb logcat -d > crash.txt` and send it; quick rollback is `enable = false` in `app/build.gradle.kts`.
3. Live TV: hold Down in a long channel list; the LIVE dot on the right still pulses (a little less smooth).
4. Home: scroll the rows fast; the big picture changes and the progress dot fills up; a card in the middle of a row still expands to both sides after a short wait, at the left edge to the right, at the right edge to the left.
5. Movie with AC3/E-AC3 sound over Bluetooth headphones (software audio) plays with sound.
6. Settings > View > Animations "Off": LIVE dot stays still.

## Risks

- Release build does not start (as in 1.2). Rollback in one line, see above.
- Offscreen layers cost about 8 MB GPU memory each while visible (one full-screen texture). Only one is visible at a time.
- Baseline profile increases the compiled size on the device (precompiled code).
