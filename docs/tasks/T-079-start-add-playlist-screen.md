# T-079: Add playlist screen opens at start with a saved playlist

Status: analysed (27 Nov 2026), no code changed. Waiting for the maintainer's approval (CLAUDE.md section 6: startup flow is user-visible).

## Goal

After an update, the app sometimes opens on the "Loading playlist" screen and then on the "Add playlist" screen, although the playlists are still there. Cancel or "Continue without playlist" leads to the normal app. The screen should only appear when there is really no playlist to use.

## Reported by

User, Fire TV Stick 4K Max, a few hours after the update.

## Cause (from code reading, not reproduced)

- Before a playlist is downloaded, `PlaylistStore.writePending` stores a record with state `run` (`LoadLogic.Pending`, `PlaylistStore.kt:407`).
- If the app is closed while loading (Fire OS stops background apps, update restart, crash), the record stays.
- At the next start, `MainActivity.kt:237` reads `resumeAdd = pending.autoResume` (state `run` and fewer than `MAX_AUTO` = 3 attempts). `MainActivity.kt:238` then starts with `adding = true`, whether or not a playlist is saved.
- `AddPlaylistScreen.kt:133` continues the load automatically, so the user sees "Loading playlist".
- Cancel and "Continue without playlist" clear the record (`AddPlaylistScreen.kt:127, 253, 256`). The screen only comes back when the app is closed again during a load.

Unverified: whether the update itself interrupted a load in the user's case. The diagnostics log (Settings > Diagnostics > Log) should show a load that did not finish.

## Affected files (planned)

- `app/src/main/java/com/ipvio/stream/MainActivity.kt` (start: `resumeAdd` only when no playlist is saved, or continue silently in the background)
- `app/src/main/java/com/ipvio/stream/AddFlowLogic.kt` (pure function for the start decision, testable on the JVM)
- `app/src/test/java/com/ipvio/stream/` (new JVM test for the decision)
- `docs/BEHAVIOUR-REGISTER.md` (entry for the start behaviour)
- `CHANGELOG.md`

## Acceptance criteria

1. A saved playlist and a leftover `run` record: the app opens the home screen, no Add playlist screen.
2. No saved playlist and a `run` record: the add flow continues as today.
3. Cancel and "Continue without playlist" behave as today.
4. The interrupted load is still retried, or its failure is shown, but not as a blocking screen.

## Risks

- Removing the resume for a saved playlist must not lose a half-downloaded list. The `.part` file logic (`PlaylistStore`, resumable download) stays unchanged.
- Start flow is user-visible: needs approval before implementation.

## Test steps for the maintainer (after the change)

1. Add a playlist, wait until it is shown.
2. Force-stop the app during a reload (Settings > Playlists > Reload, then Force stop in the Fire OS settings).
3. Start the app: expect the home screen with the playlist, no Add playlist screen.
4. Delete the playlist (no saved list), force-stop during the add, start: expect the add flow as before.

## Documentation needed

- CHANGELOG entry, behaviour register entry, this file.

## Test results

- Not run. No code changed yet.
