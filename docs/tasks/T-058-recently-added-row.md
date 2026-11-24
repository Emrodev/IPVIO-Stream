# T-058 Home row "Recently added" matches the "Recently added" page

- **Issue:** #125 "Films Start View - recently added carrousel"
- **Status:** implemented, not built, not device tested

## Goal

The row "Recently added" on the Movies and Series start view shows the same titles, in the same order, as the first entries of the "Recently added" page.

## Cause (from the code)

`HomeScreen.kt` built the row as `rowMovies.takeLast(40).reversed()`, the last 40 entries of the list. The page (`BrowseScreen.kt`, `RecentSortLogic.newestFirst`) sorts by the provider's added date (#22, T-027). The two agree only if the provider lists its titles in ascending order of the added date. For other providers the row and the page differ. This is derived from the code, not reproduced.

## Change

- `RecentSortLogic.newestN(items, n, added)`: the `n` newest items, same order as `newestFirst`. One pass with a heap, so a list of tens of thousands of titles is not sorted in full on the main thread. Without any date it returns the last `n` entries, last first, exactly as before.
- `HomeScreen.kt`: `newMovies` and `newSeries` use it.

Playlists without dates (M3U, older caches) show the same row as before. No stored format changes.

## Risks

Low. Only the choice of the 40 titles in two rows changes, and only for playlists that have added dates.

## Tests

- JVM: `RecentSortLogicTest` 6/6 (3 old, 3 new: equal to `newestFirst(...).take(n)` for several `n`, equal dates keep the playlist order, no dates and `n = 0`). Run here with the Kotlin compiler bundled in Gradle.
- Not run: Gradle build, device test.

### Manual test steps (maintainer)

1. Xtream playlist with dates: open Movies. The first cards of the row "Recently added" are the same as the first titles on the "Recently added" page.
2. Same for Series.
3. M3U playlist (no dates): the row looks as before.
