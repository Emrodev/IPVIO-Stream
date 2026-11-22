# T-054 – Categories show every title of the provider, playlist switch in the categories screen

IDs T-051 to T-053 are used on the branch `claude/exciting-cori-6wbewl` (not yet in `dev`), so this task takes T-054.

## Goal
1. A provider category must list as many titles as the provider (and TiviMate) shows. Report: "All films 2026" has 200 films at the provider, the app showed 100. Series and channels were also reported missing.
2. The categories screen of Movies and Series gets a playlist switch like Live TV.

## Analysis (code read, no device data)
- Movies: `HomeScreen.buildHome` reduced the whole movie list to one entry per title + year + language (`PlaylistLogic.firstPerKey(movies0) { mk|lang }`) and **then** grouped it into provider categories. A film that the provider puts into two categories (typical: a genre category and "All films 2026") stayed only in the first category found. The second category lost it. This is the cause of 100 instead of 200. The reduction is older than the repository history (initial commit). It was meant for the same film in HD and 4K in "All movies".
- Series: `M3uParser.parseLines` merges entries with the same cleaned name into one series (variants per episode, so the detail page can offer languages and sources). The merged series got the category of the first entry only, so it was missing in every other category the provider lists it in.
- Live channels: no reduction found in the code (`liveEntry` skips only entries without a name or numeric id, the parser skips separator names such as `***`, `---`, `###`). Not explained by this task. See "Open".
- Filters that remove titles on purpose and are not part of this task: language selection and the memory question for very large lists (`LangFilter`, `LoadLogic.systemPreselect`), "Ignore" categories, parental control, "hide broken titles", content mode of a playlist (movies and series only / live only).

## Changes
- `CategoryLogic.kt` (new, pure Kotlin): `uniqueMovies` (the former reduction, now only for "All movies", genres, home rows, Top lists) and `byGroup` (provider categories from the full list; a series is also listed in `Item.moreGroups`).
- `HomeScreen.kt`: `movieP` built from the full movie list. `providerGroups` uses `CategoryLogic.byGroup`. With "no adult titles on the home pages" the home rows by category come from the full list as well (private categories and adult titles left out as before).
- `Models.kt`: `Item.moreGroups` (default empty). Parser: a series that appears again in another category records it (at most 16). `Item.group` stays the first category, so ids, resume and My list are unchanged.
- `PlaylistCacheIO.kt`: cache version 7 stores `moreGroups` after the added date. Versions 3 to 6 are still read (no further categories until the next load). This is a cache, not user data. The app writes version 7 at the next load; an older app version reading a version 7 file rejects it and loads the playlist again.
- `Header.kt`: `AreaPlaylistPill` made `internal`, now with `modifier` and `onSwitched`; picking the current entry does nothing. The Movies/Series header behaves as before.
- `CategoriesScreen.kt`: "Playlist" pill (only with several playlists, Movies and Series). On a change the screen closes and asks the movie or series page to open the categories again (`AppNav.pendingVodCats`).
- `HomeScreen.kt`: the "open categories" effect waits until the titles of the current playlist are computed (`cached != null`), otherwise it could open the previous playlist's categories.
- Not changed: "All movies" still shows one entry per title and language. The count in Settings > Playlists counts every parsed entry.

## Affected files
`CategoryLogic.kt` (new), `Models.kt`, `PlaylistCacheIO.kt`, `HomeScreen.kt`, `Header.kt`, `CategoriesScreen.kt`, `CategoryLogicTest.kt` (new).

## Risks
- Provider categories now also show the same film twice when the provider lists it twice in one category (HD and 4K entries). This is what TiviMate shows. "All movies" and the genres still show it once.
- Category lists are longer; the preview and the category grid take the larger lists (no limit was added or removed).
- Series: a series appears in several categories, but is one title (same id). Marking it as watched or adding it to My list applies to all places.
- "Ignore" (hide a category) and parental control still use the first category of a merged series: a series whose first category is hidden is hidden in its other categories too.
- Cache version 7 (see above). Protected area check: no SharedPreferences name, backup key or file name changed.
- After the update the stored list (version 6) has no further categories for series until the next load or refresh.
- Playlist switch: the first switch to a playlist not loaded before shows the loading screen; the categories open again when it is done. If the switch fails, the categories do not reopen (the pending flag is cleared when you choose a tab from the menu).

## Open
- Live channels reported missing: not explained. Needed from the maintainer: the count shown in Settings > Playlists (Live TV, Movies, Series) for the affected playlist, and the count in TiviMate for the same playlist. If Live TV is lower than TiviMate with the content mode "all" and no language hidden, the provider response must be inspected (Settings > Diagnostics, or a copy of `get_live_streams`).
- Series whose names differ only by year or language tag ("Dark (2017)", "Dark (2020)") still merge into one title (the cleaned name drops brackets). Splitting them would change ids and needs a decision.
- A film that the provider lists in `category_ids` with several categories: only the first value is read (`XtreamApi.flat`). Not changed.

## Test results
- Ran in a scratch Gradle project outside the repository (JDK 21, Kotlin 2.2.10, JUnit 4.13.2) with the Android-free source files: `CategoryLogicTest` (new, 11 tests) and 431 other JVM tests: 442 passed, 0 failed. 14 existing test files and 7 source files that need Android classes were left out of that run (`PhoneImportTest`, `CoreLogicTest`, `Round17Test`, `Round20Test`, `Round25Test` and others).
- Not run: the Gradle build of the app. `HomeScreen.kt`, `Header.kt`, `CategoriesScreen.kt` not compiled. No device test.

## Device test steps (maintainer)
1. Open the playlist whose category "All films 2026" shows 100 titles. Update the playlist (Settings > Playlists > Reload). Open Movies > Categories: the category should list the same number as TiviMate (200).
2. Compare the number next to three other categories with TiviMate.
3. Movies > "All movies": a film that is in two categories appears once. Open it from a category: the detail page opens, "Source" offers the versions.
4. Series: a series that the provider lists in two categories is now in both (after the Reload). Open it from the second one: the episodes load.
5. With two playlists: Movies > Categories shows a "Playlist" pill. Choose the other playlist: the screen closes and the categories of the other playlist open. Switch back (instant if shown before). Try the same under Series. With one playlist the pill is not there.
6. With "Show several playlists" on: the pill lists "All playlists" and each playlist; a change reopens the categories filtered to that playlist.
7. Back from the categories, the menu and "straight to categories" (Settings > General) still work.
8. Start the app over the previous version without reloading: the stored list loads (no crash, no empty list).

## Status
Implemented on `claude/youthful-bohr-7ejf3n`, open for build and device test.
