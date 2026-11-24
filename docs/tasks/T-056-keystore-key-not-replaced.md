# T-056 Keystore key is never replaced while its alias exists

- **Issue:** #112 "Playlist Delete by itself" (Fire TV, also reported on 1.4.4)
- **Status:** implemented, not built, not device tested
- **Related:** T-027 (an unreadable saved list is no longer overwritten)

## Goal

Stop one way in which the saved playlists (link, user, password) become unreadable for good. The users cannot send logs, so the fix is derived from the code.

## Cause (from the code, not confirmed on a device)

`Secrets.key()` read the Keystore key and created a new one when the read returned nothing. If the alias exists but `getKey` fails for a moment (some boxes right after a restart), a new key was generated under the same alias. That replaces the old key for good. Every value encrypted with the old key (`lists_enc`) is then unreadable, even if the old key works again later. The list looks empty, as reported.

This matches that the reports come from Fire TV and not from Google TV. It is a hypothesis.

## Change

- `SecretsLogic.kt` (new, pure): `keyAction(keyReadable, aliasExists)` returns USE, GENERATE (only when the alias does not exist) or UNAVAILABLE.
- `Secrets.key()` uses it. With UNAVAILABLE it returns `null`, so `encrypt` and `decrypt` behave as before for a box without keystore (no crash, the existing plain-text fallback and the T-027 protection of the unreadable copy stay).

- Notice: when a saved list exists but cannot be decrypted, `PlaylistStore.unreadable` is set and the empty home screen shows "The saved login data could not be read. Please add the playlist again." (English row in `DictEn1.kt`, `HomeScreen.kt`). Before, the user saw an empty app without a reason.

No preference name, stored format or backup key changes.

## Not solved

- A key that is really gone (alias missing after an OS reset) still cannot be recovered. The list has to be added again (or restored from a backup).
- While the alias exists but the key is unreadable, new values are stored as plain text (the fallback for boxes without a keystore). This is the same weaker protection as on those boxes. Not changed here, needs a decision.

## Risks

Low to medium: secrets handling (CLAUDE.md section 6). The change only avoids creating a key; it adds no new storage.

## Tests

- JVM: `SecretsLogicTest` 3/3 and `DictEn1Test` 5/5 run here with the Kotlin compiler bundled in Gradle.
- Not run: Gradle build, device test (no Android SDK here).

### Manual test steps (maintainer)

1. Update over an existing install with playlists: the playlists are still there and load.
2. Fresh install, add an Xtream playlist, restart the app and the box: the playlist is still there.
3. Fire TV (the affected device): use it for a few days with a playlist, restart the box several times. Report if a playlist still vanishes.
