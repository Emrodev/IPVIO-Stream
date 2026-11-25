# T-068 – Playlist switch without loading screen, faster parsing

## Goal

Maintainer report (25 Nov 2026): the app should be fast; switching a playlist should happen at once without the screen
changing (today Live TV turns into the loading screen).

## Analysis

- `PlaylistStore.switchTo` emptied the list before reading the stored copy of the new playlist (to keep only one list in
  memory on weak Fire TV sticks). An empty list makes `HomeScreen` show the skeleton or the loading overlay instead of Live
  TV / Movies / Series until the read is done. Switching back to a list still in memory (`switchFromMemory`) was already instant.
- Parser profile on the JVM (synthetic list: 30,000 channels, 150,000 movies, 25,000 series, 32 MB): `M3uParser.clean()`
  was the largest part; it runs four regular expressions on every movie and series title.
- Start: the stored list is already read ahead on a background thread (T-047); reading 205,000 items took 180 to 400 ms on the
  JVM. No safe change found there without device measurements. Live TV already caches its groupings (`LiveGenre.split`).

## Changes

- `SwitchLogic.kt` (new, pure): may both lists be in memory for a moment (stored copy × 5 + 64 MB reserve below the free heap)?
- `PlaylistStore.switchKeepingList`: when that holds and the stored copy fits the language selection, the current list stays on
  screen, the new playlist is marked active at once, its stored copy is read in the background and then replaces the list in one
  step. The read stops when memory gets tight (existing guard); then the old switch runs (empty first, loading screen). One
  diagnostics line with the read time.
- `Models.kt` (`M3uParser.clean`): every regular expression runs only when a cheap necessary condition holds (a `|` or `:`, a
  bracket, a quality or tag word, two spaces in a row). `LangTagLogic.stripDashTag`: no dash, no regular expression. Same result.

## Not changed

Storage format, start-up order, the screens.

## Acceptance criteria

1. Switching between two playlists with stored copies keeps Live TV (or Movies / Series) on screen; the list changes in place.
2. On a device with little memory the switch still works (then with the loading screen, as before).
3. Parsing gives the same titles as before.

## Risks

- Both lists in memory for the length of the read; guarded by the estimate and the memory check, not measured on a Fire TV.
- While the new copy is read, the old channels are still shown under the new playlist name (a moment).

## Documentation

`CHANGELOG.md` (Unreleased), `docs/tasks/WORK_STATE.md`.

## Test results

- JVM: parsing the synthetic list 909 ms → 623 ms (warm runs, JVM on the build machine; a Fire TV is slower, the ratio is not
  measured there). Old and new parser output compared on 50 tricky names (tags, brackets, dashes, tabs, accents): identical.
- `SwitchLogicTest` 3/3; all 75 JVM-runnable test classes 491/491 (with T-067).
- **Not run:** Gradle build, device test, time measurement on a device.

## Status

Implemented on branch `claude/modest-johnson-415uuv`, open for build and device test.

## Test steps for the maintainer

1. Two playlists, both loaded once. Live TV: switch the playlist in the header. Live TV stays visible and changes to the new
   channels; no loading screen.
2. Settings > Diagnostics > Log: "Playlist switch: stored list N items read in X ms, the screen stayed". Please send X from the
   Fire TV.
3. Same on a Fire TV Stick with a large list: the switch works (with the loading screen if memory is short), no crash.
4. "Opened …: N ms" lines in the same log (T-056) show the screen opening times; please send them for Live TV, Movies, Series.
