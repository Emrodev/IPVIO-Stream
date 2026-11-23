# T-054: Fire TV froze while cycling through channel history

Status: implemented in the branch (23 Nov 2026), not built, not device tested. Cause not confirmed on a device.
Source: user report. "While cycling through my channel history channels, the app froze my entire Fire TV. I couldn't even force reboot. I had to power cycle it. That's never happened before."

## Goal

Rapid channel changes must not freeze the device. Fix only after the maintainer confirms the test steps below reproduce it (section 14: no guessing).

## Analysis (read only)

Each channel change with the on-device buffer ("Live-Puffer", timeshift) active:

1. `PlayerScreen` → `LaunchedEffect(cur.url)` → `LiveTimeshift.start` (`PlayerScreen.kt:857-885`, `LiveTimeshift.kt:170-180`).
2. `LiveTaps.open` (`LiveTap.kt:216-232`) creates a **new** `LiveTap` with its own `OkHttpClient` (`LiveTap.kt:41`) and a new download thread (`begin`), and a new folder in `cacheDir/livetap`.
3. The old tap is released only after the new one is open (`old?.release()`, `LiveTimeshift.kt:175`). `close()` interrupts the thread and deletes the folder in a background thread after up to 3 s (`LiveTap.kt:194-205`).
4. Every tap writes at stream bitrate into chunk files, and `trim()` deletes old chunks (`LiveTap.kt:145`).
5. Default buffer size: **512 MB per channel** (`LiveTimeshift.kt:26`). Budget = min(setting, usable space − 400 MB reserve) (`TapLogic.kt:28-32`).

Effect of fast zapping: several taps can be alive at the same time (old one still closing for up to 3 s, new one filling). Each can hold up to 512 MB of chunk files, so deletions and writes overlap on the flash storage. Combined with the player and the Fire TV's small RAM, this can exhaust I/O and memory.

**Not confirmed on a device.** Also not checked: whether `LiveTaps.open` (synchronized, called from the UI thread via `LaunchedEffect`) ever blocks for long (e.g. `root.deleteRecursively()` when the tap map is empty, `LiveTap.kt:220`).

## Plan

Step 1, measurement only (no behaviour change):
- Add nothing yet. Use the existing diagnostics log (`Diag.kt`, last 25 lines) and `DiagLogic`.
- Maintainer reproduces with the buffer on and off, to see whether the buffer is the trigger.

Step 2, if confirmed, a small fix (candidates, pick after step 1):
- a) Cap the buffer for zapping: smaller default (e.g. 64 MB) when zapping quickly; decision for the maintainer (visible change, section 6).
- b) Debounce `LiveTimeshift.start`: start the tap only after the channel has been on screen for about 1 s. Pure logic in `TapLogic` (`TapLogicTest`), no Android import.
- c) Close the old tap before the new one is opened (not after), so only one buffer exists at a time. Needs a check that the recording (`Recorder`) which shares taps is not broken.
- d) Reuse one `OkHttpClient` for all taps instead of building one per tap.

Recommended first: b) and c) together, smallest change, no visible setting change.

## Affected files (later)

- `app/src/main/java/com/ipvio/stream/LiveTimeshift.kt` (start/stop, default size)
- `app/src/main/java/com/ipvio/stream/LiveTap.kt` (open/close, client, thread)
- `app/src/main/java/com/ipvio/stream/TapLogic.kt` (pure logic, tests)
- `app/src/main/java/com/ipvio/stream/PlayerScreen.kt` (only if the debounce goes into the player; protected)

## Acceptance criteria

- 40 channel changes in a row with the buffer on: no freeze, no reboot, the picture comes back within a few seconds each time.
- Recording of a channel still works while zapping.
- No change to stored settings or keys (`LiveSettings` prefs stay as they are).

## Risks

- A freeze is hard to reproduce and may involve other apps or the system. The fix may not be the only cause.
- Smaller buffer = less rewind time (visible to users; decision needed).
- Crash/restart data: `LiveTaps.open` removes leftovers after a crash (`root.deleteRecursively()`), no change planned.

## Test steps (maintainer, Fire TV, record each result)

Record: device model, free storage (Settings → Device → Storage), app version, buffer setting (Live TV settings), timeshift on/off.

1. Timeshift **off**. Play live. Zap up and down 40 times with the remote as fast as possible (about 1 per second). Result: freeze yes/no.
2. Timeshift **on**, default 512 MB. Same 40 zaps. Result: freeze yes/no, time until the picture returns.
3. Timeshift on, buffer set to the smallest value. Same 40 zaps. Result.
4. After a freeze (if it happens): open the Diagnostics log after restart and copy the last lines.
5. Check free storage before and after the test.

Interpretation: if only step 2 freezes, the buffer is the trigger → plan step 2 (b and c). If step 1 also freezes, the trigger is elsewhere (player or network) → new analysis.

## Documentation

- `CHANGELOG.md` with the fix. Known limits in the release notes if the buffer default changes.

## Test results

- Static read of the code only. No device reproduction, no Gradle build.
