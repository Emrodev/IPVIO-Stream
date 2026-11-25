# T-067 – Missing series, partial Xtream lists and EPG matching

## Goal

Maintainer report (25 Nov 2026): users see far fewer channels, movies and series than their provider delivers, and the
TV guide misses programmes. Priority 1. Find where entries get lost between the provider and the screen, fix what is
certain, and make the rest visible on the device.

## Analysis (what was checked)

Path: Xtream API or `get.php` → M3U lines → `M3uParser` (`Models.kt`) → language filter / content mode (while parsing)
→ parental control, ignored groups, broken titles (`PlaylistStore.shownOf`) → screens.

Confirmed on the JVM with a test list (7 provider series → 3 shown):

1. **Series of the same name were merged into one.** The parser keys series by the cleaned name, and `clean()` removes
   brackets, language prefixes and quality tags. So "The Office (US)" and "(UK)", "Battlestar Galactica (1978)" and
   "(2004)", and, on Xtream/Stalker logins, "Dark" in "Netflix DE" and "Dark" in "Netflix EN" became one series. The others
   existed only as hidden variants, and the series was missing from every category but the first. Xtream lists many series
   once per language category, so this cut the series count a lot.
2. **`get.php` with only part of the movies or series.** Since T-051 stage 3 the app switches to the API only when the list
   has **no** movie or series at all. A list with a few movie categories was accepted as complete.

Not a loss, but checked and made visible: separator lines (`### DE ###`), the language filter (one setting for all
playlists, `ipvio_lang`), the content mode, parental control, ignored groups and broken titles. Which one hides how many
entries could not be told on the device without a logcat.

EPG: XMLTV programmes were matched by exact tvg-id only. Channels without tvg-id got nothing from XMLTV, and a tvg-id that
differs only in case of country ending or variant ("DasErste.de" against "daserste.de@HD") stayed empty.

## Changes

- `SeriesKeyLogic.kt` (new, pure): when two entries of the same name are two series. **Only placeholders** (one entry per
  provider series, Xtream `.xtser` / Stalker) are split: in different categories, or with a different year or country in
  brackets ("(2004)", "(UK)"). Lists with episode lines (plain M3U) merge exactly as before (review 25 Nov 2026: some
  providers write the season's year into the episode name, which would have split one series).
- `Models.kt` (`M3uParser.parseLines`): uses it. **The first series of a name keeps its old key**, so its ID (`s_<name>`),
  resume points and lists stay; its title and year stay too, so its stored TMDB data is used as before. The others get
  `s_<name>~<year/country>` and/or `~g<hash of category>`; only they get the year in the year field or the country in the title
  ("The Office (UK)"). New `ParseReport` (entries, divider lines,
  hidden by language, hidden by content mode, series entries).
- `MatchLogic.searchTitle`: drops a trailing "(US)/(UK)/(AU)/(NZ)/(CA)", so the TMDB search still finds "The Office".
- `LoadLogic.vodGroups` (pure) + `XtreamApi.vodCategoryNames` / `seriesCategoryNames`: `PlaylistStore.vodViaApi` compares the
  movie and series categories of a fresh `get.php` list with the provider's. When most are missing (same rule as the live
  check, `missingCategories`: fewer than half, at least 5 categories), the whole list is loaded through the API. If the API
  fails, the `get.php` list is kept (unchanged).
- `LoadReportLogic.kt` (new, pure) + `PlaylistStore.reportLoad`: after every download one line in Settings > Diagnostics >
  Log: entries of the list, live / movies / series, and how many were left out by divider lines, the language filter (with the
  codes), the content mode, and parental control / ignored categories / broken titles together. Host only, no address.
- `EpgMatchLogic.kt` (new, pure): normalized tvg-id (case, country ending, `@variant`) and normalized channel name (no language
  tag, no quality words, no accents) against the XMLTV `<channel>` IDs and display names. A name used by several guide channels
  is only taken when the guide ID's country fits the channel language; otherwise no programme (better none than a wrong one).
- `Epg.kt` (`Xmltv`): reads the `<channel>` elements, matches at the first `<programme>`, keeps a redirect map (tvg-id → guide
  ID) and a map for channels without tvg-id. New `forItem` / `hasItem`; one diagnostics line with the number of matched channels.
  A list without any tvg-id no longer stops the XMLTV load.
- Callers of `Xmltv.forChannel(x.tvgId)` / `Xmltv.has(x.tvgId)` now call `forItem(x)` / `hasItem(x)`: `Epg.kt`, `EpgFull.kt`,
  `GuideSearch.kt`, `RemoteHub.kt`, `SearchScreen.kt`, `SportHub.kt`, `SportTab.kt`.

## Not changed

Storage format (cache version stays 6, no new files or preference keys), backup, the language filter itself, IDs of movies,
live channels and of the first series of each name, the player.

## Acceptance criteria

1. A playlist with "Dark" in two categories shows "Dark" in both categories.
2. "The Office (US)" and "The Office (UK)" are two series; the TMDB data of each is found.
3. Resume and My list of a series watched before the update still work (first series of the name keeps its ID).
4. An Xtream login whose `get.php` lacks most movie or series categories loads its movies and series through the API.
5. Settings > Diagnostics > Log shows the load report after a reload.
6. A channel without tvg-id, or with a tvg-id that differs only in case/country ending, shows its XMLTV programme.

## Risks

- **Visible change:** more series appear; series that were one with variants are now separate in their categories (Xtream and
  Stalker logins, and same-name series with different year/country). Lists with "Dark" in a DE and an EN category show it twice.
- A same-name series watched through its second variant before the update has its progress under the first ID; the separate
  second series starts without progress.
- Name matching in the guide could give a wrong programme where two channels share a name and the language is not tagged; such
  names are skipped, but a guide with one wrongly named channel would still match.
- `get.php` category names that differ from the API names would switch such a login to the API without need (complete list,
  series episodes then load on opening).
- Name matching costs one pass over the channels per guide file (background thread, not measured on a device).

## Documentation

`CHANGELOG.md` (Unreleased), `docs/TESTING.md` (test list), `docs/tasks/WORK_STATE.md`.

## Test results

- Update check (review 25 Nov 2026): the same mixed list (Xtream placeholders in three categories, plain M3U episodes with
  season years, live, movies) parsed by 1.4.9 (`main` f6270c3) and by this version. Every 1.4.9 ID is still there, no existing
  item changes its title, year or category, plain M3U gives the same result, and each version reads the other's stored list
  (update and rollback). XMLTV display names are read without `nextText()` (cannot throw on unusual guides); a failed or
  cancelled playlist switch sets the previous playlist active again.

- Run here (Kotlin 2.0.21 compiler, JUnit 4.13.2, the 115 main files without Android imports): new `SeriesSplitTest` (9),
  `EpgMatchLogicTest` (5), 2 new tests in `LoadLogicTest`; all 75 JVM-runnable test classes: **491 of 491 passed**.
- **Not run:** Gradle build. `PlaylistStore.kt`, `Epg.kt`, `XtreamApi.kt` and the changed screens use Android and were not
  compiled here (CI compiles them). No device test.

## Status

Implemented on branch `claude/modest-johnson-415uuv`, open for build and device test. Touches the IPTV core and the EPG
(CLAUDE.md section 5); started on the maintainer's request of 25 Nov 2026 ("Los, löse meine größten Probleme").

## Test steps for the maintainer

1. Install over the current version. Settings > Playlists > "Reload" on an Xtream playlist.
2. Settings > Diagnostics > Log: a line "<host>: list has N entries → … left out: …". Compare live / movies / series with the
   provider's numbers. If something is still missing, the line says where (language filter, content mode, parental control …).
3. Series: open two categories that both contain the same series name: it is in both. Open a series watched before: resume works.
4. A login with few movies before: the log says "misses most movie or series categories … loading through the API"; the movies
   are there afterwards.
5. TV guide with a custom EPG address (Settings > TV guide) and a list without tvg-ids: programmes appear; the log line
   "Guide: … matched" shows how many.
