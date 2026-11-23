# T-051: Sound of the previous channel keeps playing

Status: implemented in the branch (23 Nov 2026), not built, not device tested. Cause not confirmed. Test steps below.
Source: user issue, Fire TV, Live TV.

## Goal

After leaving the player and starting another channel, the sound of the previous channel must not continue in the background.

## Reported behaviour

"Sometimes I can go out of a channel, play another and still have the sound from the previous channel playing in the background."

## Analysis (read only, 23 Nov 2026)

- The player is created once per `PlayerScreen` (`PlayerScreen.kt:161-180`). Switching channels inside the player reuses it (`openStream`, `PlayerScreen.kt:1928-1931`).
- Leaving the player: `onDispose` calls `player.release()` (`PlayerScreen.kt:789-797`). A new channel from the Live tab creates a new `PlayerScreen` through `key(p)` (`MainActivity.kt:467`), so the old one is disposed.
- Lifecycle: `ON_PAUSE` pauses the player (`PlayerScreen.kt:805-830`). Only the `ON_PAUSE` path is covered, not a release.
- Other players that can make sound: multi view (`DualView.kt:69-84`, released in `onDispose`; `audioOn` decides which tile has sound). Not yet checked where this composable is called from.
- Live buffer: `LiveTimeshift.stop()` is called on dispose (`PlayerScreen.kt:799`). The release is asynchronous (`LiveTap.close`, `LiveTap.kt:194`), but it does not feed audio.

**Not confirmed.** I found no path where a released player keeps its audio session. Most likely candidates, in order:
1. Exit path that does not run `onDispose` (e.g. Home key or task switch while the player stays composed in the background). Needs a device check.
2. Multi view player (`DualView`) still alive after the player closes. Needs a check of its call site.
3. Race between the old and the new `PlayerScreen` during `key(p)` change (`MainActivity.kt:467`).

## Affected files (for a later fix)

- `app/src/main/java/com/ipvio/stream/PlayerScreen.kt` (protected, section 5: needs an approved plan)
- `app/src/main/java/com/ipvio/stream/MainActivity.kt` (around line 467)
- `app/src/main/java/com/ipvio/stream/DualView.kt` (call site to be checked)

## Acceptance criteria

- After leaving a channel (Back, Home, starting another channel), no audio of the old channel is audible.
- Zapping inside the player keeps working as before.
- No change to the stored settings or keys.

## Risks

- Player core is protected. Any fix must be small and reviewed.
- A fix that releases too early may break the "resume after Home" behaviour (`PlayerScreen.kt:805-830`).

## Test steps (maintainer, device)

1. Play live channel A. Press Back to the Live tab. Start channel B from the list. Listen: no sound of A.
2. Play A. Press Home. Open the app again and start B from the Live tab. Listen for A.
3. Play A inside the player, zap to B with the remote, then leave with Back. Listen for A and B.
4. Repeat 1 to 3 five times each. Note which step leaves sound behind.
5. Open the multi view (split screen) and leave the player. Listen for the second tile.

## Documentation

- `CHANGELOG.md` entry only with the fix. Nothing for this analysis step.

## Test results

- Static read of the code only. No Gradle build, no device test.

## Depends on

- T-054 (freeze) may share the same live-buffer code. Analyse together, fix separately.
