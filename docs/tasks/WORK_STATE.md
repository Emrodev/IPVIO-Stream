# Work state

Last update: 2026-11-11. Read `CLAUDE.md` first, then this file.

## Current task

**Integration branch `integration/2026-11-11`**: open PRs (#7, #8, #9, #13, #14, #19 Stalker part, #30-#34, #36, #38, #39) merged locally into one branch, plus fixes for reported bugs: Live TV focus (Right/Left keep the spot), #22 Recently added order, #26 language filter icon, #25 tap-to-seek, Stalker loading texts translated. Device test on an NVIDIA SHIELD in progress (results so far: #20 movie playback and #28 resume pass, #22 newest first confirmed). Open: #10 (MKV without Cues, needs a player-core plan), #27 (not reproduced yet), #18 (search; device test pending). Unit tests: all pass except two `PhoneImportTest`/`PhoneTransferTest` socket tests that also fail on `origin/main`.

## Earlier task

**T-009 Xtream backup servers** (see `docs/tasks/T-009-xtream-backup-servers.md`): merged into `main` through #37 (#3 rebased with the review fixes from #35, without the commit of #2; #3 and #35 closed). CI passed. Device test PENDING before the next release.

None active. **P2 batch 8** (final comment sweep) is done in the working copy; the commit by the maintainer is pending. The restructuring plan U-0 is recorded in `docs/REFACTORING-PROGRESS.md`.
**U-1** (move the Media3-dependent code out of `PlayerLogic.kt`) is done on branch `refactor/U-1`, based on `pre-U-1`. The pull request (#30) is open; the CI check "Build APK" (unit tests and debug build) passed on commit c98e16b. The device test is still open. The maintainer confirms the merge.

## Completed steps

- P2 batch 8 (comment sweep) is on `main` (commit 8d01000 and the work-state commit 581099c). The earlier note "commit pending" is obsolete.
- U-1: `collectTracks`, `TrackOpt`, `trackLabel` and `httpStatusOf` moved verbatim into the new `PlayerTracks.kt` (Media3 side).
- U-1: `PlaybackErrorCodes` (eight numbers, `internal`) and `IptvRetryRules` (pure) added to `PlayerLogic.kt`; `epgSane` moved from `Epg.kt` into `PlayerLogic.kt`.
- U-1: `IptvLoadErrorPolicy` companion delegates to `IptvRetryRules`; the class name stays.
- U-1: `CoreLogicTest` uses `PlaybackErrorCodes` (import removed); `PlayerBufferTest` uses `IptvRetryRules`.
- U-1: new `Media3ContractTest.kt` compares the eight numbers with Media3 `PlaybackException`.
- U-1: `PlayerLogic.kt` has no `androidx`/`android` import left.

## Changed files

Main: `PlayerLogic.kt`, `PlayerTracks.kt` (new), `PlayerNet.kt`, `Epg.kt`.
Tests: `CoreLogicTest.kt`, `PlayerBufferTest.kt`, `Media3ContractTest.kt` (new).
Docs: `docs/REFACTORING-PROGRESS.md`, `docs/tasks/WORK_STATE.md`, `CHANGELOG.md` (Unreleased entry).

## Test results

- Literal fingerprint of `app/src/main`: 4206 distinct string literals, identical before and after.
- Run in the cloud (Kotlin 2.0.21 compiler, JUnit 4.13.2): `PlayerLogicTest` and `PlayerBufferTest`, 20 tests, OK.
- Not run in the cloud: Gradle (`assembleDebug`, `testDebugUnitTest`), `CoreLogicTest` (its dependencies do not translate without Android), `Media3ContractTest` (needs the Media3 jars). The CI workflow "Build APK" on the pull request is the check for these.

## Open problems

- `PlaybackErrorCodes` values come from the Media3 documentation; `Media3ContractTest` verifies them in CI.
- Device test for U-1 is still open (see `docs/REFACTORING-PROGRESS.md`).
- T-004 (quality baseline values), T-006 (backup NAMES, `Backup.kt:17`) and the CHANGELOG entries for 1.2 and 1.3 remain open.
- T-002: device test on TV or Fire TV.

## Next step

U-2 (inventory of German identifiers, list only) after U-1 is merged.
