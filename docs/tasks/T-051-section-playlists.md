# T-051: Playlist selection per section

Status: **stage 1 and stage 2 implemented. Not compiled (no Kotlin compiler or Android SDK here), not device tested. Performance not measured.**

## Goal

Per section (Live TV, Movies, Series) choose which shown playlists it uses, with an "All playlists" entry. A playlist that does not fit a section (content mode "Live TV only" in Movies, or "Movies & series only" in Live TV) is not selectable there. Startpage and search keep the current behaviour.

## Decisions (from the maintainer, 23 Nov 2026)

- Selection sits in the dropdown of each section.
- Startpage and search keep "shown together" (`multi`).
- A section without its own selection shows "All playlists".
- Not fitting playlists are shown greyed out with the reason (proposal, not confirmed).

## Stage 1 (done)

- `SectionSourceLogic.kt`: eligibility per content mode and section, effective selection, toggle, storage by playlist key. Pure logic, no Android imports.
- `SectionSourceLogicTest.kt`: 9 JVM tests. **Not run** (no Kotlin compiler or Gradle here).

## Stage 2 (implemented)

- `AreaSource.kt`: one selection per section, stored as playlist keys in the preference file `ipvio_section` (keys `sec_1`, `sec_2`, `sec_3`). `sources()` turns it into source indices once per change; `filter()` takes a set of sources. Empty selection = all.
- `SectionPlaylistUi.kt` (new): pill and popup with checkboxes, "All playlists" on top, greyed rows for playlists that do not fit the section (with the reason from `PlaylistMode.labels`). A playlist that is not shown yet is activated as before.
- `Header.kt`: the movies/series pill uses the new popup. `HomeScreen.kt`: the filter runs on the set and is kept in `remember`. `LiveTab.kt`: the "Playlist" row uses the popup, the "Quelle" row cycles through the sources as before (All, then each shown playlist).
- `PlaylistStore.shownUrls` gives the addresses in source order. `Dropdown.kt`: `belowAnchor` is now internal.
- Not changed: loading (`multi`, `active`), the Browse page's own "Quelle" filter, startpage and search.

## Performance

- The selection only filters playlists that are already loaded. It never loads an extra playlist.
- The source set is computed in `remember`, not for each item or each recomposition.
- Filtering is skipped when the selection covers every shown playlist.
- Membership checks use `Set<Int>` lookups.
- Not measured on a device. Memory and start time with five large playlists are still open.

## Risks

- Changes the IPTV core (CLAUDE.md section 6). Approved by the maintainer on 23 Nov 2026 with the note that performance must not suffer.
- Compile not checked. Most likely problems: small signature mismatches in the Compose helpers (`FilterPill`, `OptionRow`, `Text`).
- Browse page "Quelle" still filters on its own, not on the section selection.

## Test steps for the maintainer (after stage 2)

1. Five playlists, two of them "Live TV only": open Live TV, the dropdown lists only playlists that fit Live TV.
2. Movies: the Live TV-only playlists are greyed out with the reason.
3. Select 4 of 5 for Live TV: only those four channels appear.
4. Choose "All playlists" again: all eligible playlists appear.
5. Restart the app: the selection is kept.
6. Startpage and search show the same playlists as before.

## Stage 3: fixes after the test build of 23 Nov 2026

Reported by the maintainer on the test APK (run 45151240125, branch `test/combined-2026-11-23`):

1. A playlist with movies and series showed none (main list, counts 0 movies and 0 series in Settings > Playlists).
2. The playlist buttons at the top did not work; with "All" not every playlist was ticked; the menu could not be opened.
3. Playlists could not be edited in the settings.
4. Search layout not as expected (screenshot: squeezed tiles "Se", "Liv", hero empty, cards cut off, "Merkliste" in German).
5. The playlist edit window scrolled slowly and trailed behind.

Causes and changes:

- (2) Since 1.4.2 a playlist could not be switched on as "shown additionally", so only the main list was loaded. The menu listed the other playlists unticked, and a click replaced the main list. The popup also requested no focus, so the remote could not reach it. **Decision (maintainer, 23 Nov 2026): ticking a playlist loads it in addition; "All playlists" loads every playlist that fits the section.** `SectionSourceLogic.checked` and `click` (pure logic, tested), `PlaylistStore.showAsync` (shows and loads in `switchScope`, retries up to 30 s while a load runs; memory guard of `loadExtras` unchanged), `SectionPlaylistUi.kt` (focus on open, Box around the header pill, loading line and `extraError` in the menu). The playlists stay loaded when they are unticked later; "Shown additionally: Off" in the edit window unloads one.
- (1) Probable cause (not confirmed on the device): Xtream `get.php` with live channels only. `missesCategories` only compares live categories, so such a list was accepted. New `PlaylistStore.vodViaApi`: when the list has no `/movie/` or `/series/` address (`LoadLogic.hasVod`) and the provider lists movie or series categories (`XtreamApi.hasVodCategories`), the list is loaded through the API and the login is marked for the API. If the API fails, the `get.php` list is kept. Needs "Reload" once. If the playlist is not Xtream (plain M3U or Stalker), this change does not help; then the diagnostic log is needed.
- (3) No logic change found in the settings page on this branch. The edit window requested no focus; it now focuses the name field when it opens. If editing still fails, more detail is needed (what happens on OK).
- (5) The window used the app-wide soft scrolling (`SmoothSpec`); it now uses `InstantSpec`, as the add screen does.
- (4) The search column was a fixed 600 dp (half of a 960 dp TV screen); now 36 % of the screen width (330 to 600 dp). Hero 300 dp to 190 dp, title sizes smaller, tiles with less padding. New row `Merkliste¦Watchlist` in `DictEn4.kt`. The sketch arrived later the same day; the search screen was then rebuilt from it (see `T-051-search-handoff.md`, third round).

Checks run here: the pure-logic files were compiled with the Kotlin 2.0.21 compiler bundled with Gradle (all 111 source files without Android imports), and `SectionSourceLogicTest`, `LoadLogicTest`, `DictEn4Test` were run with JUnit 4: 34/34 passed. Not run: the Gradle build, Android files (`SectionPlaylistUi.kt`, `PlaylistEditDialog.kt`, `PlaylistStore.kt`, `XtreamApi.kt`, `SearchScreen.kt` were not compiled), device tests.

### Test steps for the maintainer (stage 3)

1. Settings > Playlists: "Reload" on the playlist without movies. Afterwards the line under it shows movies and series (if it is an Xtream login).
2. Movies: open the "Playlist" menu with OK. The first row has the focus. Tick a second playlist: it loads (line "Loading …"), then its movies appear.
3. Choose "All playlists": every playlist that fits Movies is ticked after loading.
4. Untick one: it disappears from Movies, not from Live TV.
5. Live TV: same menu in the "Playlist" row.
6. Fire TV Stick with large playlists: when memory is short, a notice appears in the menu, the app does not crash.
7. Settings > Playlists > Edit: the name field has the focus; scroll down with the D-pad: the window follows at once. "Shown additionally" can be set to On.
8. Search: compare with the maintainer's sketch (typing, empty field, empty category). The tiles show full names, the result cards are fully visible, the button reads "Watchlist" in English. The TV guide search and the Live TV channel search keep their old keyboard.

## Stage 4: second test build (23 Nov 2026)

Maintainer report on the APK of run 45170319820:

- Movies and series still missing on `iptv.watchhd.to` (not the main list; counts Live TV 1138, movies 0, series 0, all filters off).
- The playlist menu does not open in Movies/Series and in Live TV; "Edit" in Settings > Playlists does not open the window ("Reload" works).
- Search: the keyboard should sit at the bottom; the row heading with "Show all" is hidden under the category tiles.
- TMDB: "Secure connection failed" (from the key check in `Config.kt`, any `SSLException`).

Changes:
- Search: keyboard at the bottom of the panel; `SearchRow` and `HitRow` bring the whole row (heading included) into view when a card gets the focus.
- Diagnostics: the download path writes to `Diag.note` (Settings > Diagnostics > Log): M3U address (not Xtream), Xtream list with or without movies/series, API used, API failed. Host name only, no credentials.
- Settings > Playlists: type (Xtream, M3U, Stalker) and content mode on each row; "Show all content" when the mode is not "all" (a "Live TV only" mode removes movies and series while loading, which gives exactly "movies 0, series 0").

Not solved, cause unknown: the menu and the edit window. Read again: no exception path found (`PlaylistMode`, `PlaylistRefresh` decoding, backup types), no window focus handling, no loading gate. The other header dropdowns use the same pill and `OptionPopup`. Needed from the device: Settings > Diagnostics, "Last crash" and "Log" right after trying to open the menu and the window.

TMDB: no network code changed on this branch. Possible causes to check on the device: the device date and time (the emulator clock), own DNS or the VPN in Settings > General.
