# T-026: Remember the category cursor in Movies and Series

Status: **open, not planned, not implemented** (wish from the maintainer, noted on 17 Nov 2026; needs approval per CLAUDE.md section 6, visible behaviour).

## Goal

In Movies and Series, the category list next to the title grid should remember the last selected category, the same way the Live TV channel lists do (`LiveMemory`, `LastFocus`).

Reported behaviour: open a category in Movies or Series, search or move in the title grid, go back to the category list. The cursor does not return to the last selected category but jumps to another entry.

## Affected files (to be confirmed in the plan)

- `app/src/main/java/com/ipvio/stream/CategoriesScreen.kt`
  - `CatMemory` (`sel`, `item`, `restore`, `reset()`), object at about line 178.
  - Restore logic in `CategoriesScreen` (about line 109-111: `restoring` decides whether the first category gets focus on entry).
  - Selection write-back `SideEffect { CatMemory.sel[title] = sel }` (about line 225).
  - Opening a title from the grid sets `CatMemory.restore[title] = true` (about line 346).
- `app/src/main/java/com/ipvio/stream/MainActivity.kt` (lines 243 and 328): `CatMemory.reset()` on tab change. Check that a reset does not clear the category memory on the way back from a title.
- Reference for the working behaviour: `LiveTab.kt` (`LiveMemory`, `LastFocus.pending`, `restoreReq` at about line 180-190).

## Hypothesis (not verified)

`restoring` is set when a title is opened and read on return. While it is `true`, the category list does not request focus on entry (`LaunchedEffect(Unit)` skips `firstFocus`). The focus then lands on the default first item, not on `CatMemory.sel[title]`. This must be checked in the code path and on a device before the fix is planned.

## Acceptance criteria

1. Movies and Series: select the 5th category, open a title in the grid, go back (Back key): the cursor is on the 5th category.
2. Same after a search in the grid (the search path from the wish report).
3. Same after going to another tab and back (only if Live TV does the same; check `LiveTab` first).
4. Behaviour of the Live TV lists is unchanged.
5. The memory is in memory only (as `CatMemory` is now); no new SharedPreferences key, no storage change (CLAUDE.md section 5).
6. A unit test in `app/src/test/java/com/ipvio/stream/` is needed for any pure logic (for example the index choice on restore, moved into a `*Logic.kt` file without Android imports). If the fix is only focus wiring, the pull request says why no JVM test is possible and lists the device steps.

## Risks

- `CatMemory` is shared between Movies and Series by title key; a wrong key would restore the wrong list.
- Focus restore in Compose on Fire TV is timing-dependent (existing code uses `runCatching` and delays); a fix can cause a focus flicker or a double jump.
- The change touches the browse flow used on every start of Movies and Series; the visible effect is intended.

## Documentation needed

- `CHANGELOG.md` entry when implemented.
- `docs/FEATURES.md` if the behaviour is described there.

## Test results

- Not run. No Gradle, no device in this session.

## Device test steps (for the maintainer, after the fix)

1. Movies: pick the 5th category. Open a title in the grid, press Back: the 5th category has the focus.
2. Movies: pick a category, press Search (or move through the grid to the end), go back to the category list: the same category has the focus.
3. Series: repeat steps 1 and 2.
4. Live TV: pick a channel list, open a channel, go back: the list is unchanged (regression check).
5. Switch tabs and come back to Movies: the last category is still selected (if this was the behaviour before; note the result either way).
