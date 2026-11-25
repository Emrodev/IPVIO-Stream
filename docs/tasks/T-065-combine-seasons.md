# T-065 Combine seasons that a provider lists as separate series

- **Issue:** #151 "Summarise series and their episodes together" (each season is shown as its own series; the maintainer wrote in the issue that this should be fixed in the next patch)
- **Status:** implemented as an option (off by default), not built, not device tested
- **Protected areas touched:** IPTV core (Xtream series loading) and the series id. Done on the maintainer's go-ahead of 25 Nov 2026; a review is needed.

## What is known

The reporter sent two screenshots (not readable here), no provider type. The symptom fits an Xtream provider that lists every season as its own series ("Show S01", "Show S02", "Show Staffel 3"). The M3U parser already puts all seasons of one name into one series; for Xtream the series name of each provider series becomes the key, including the season text. This is a hypothesis.

## Change

New option Settings > Playlists > Series > "Combine seasons" (default off).

- `SeasonMergeLogic` (pure): splits "Show S02", "Show - Staffel 3", "Show (Saison 5)", "Show.S06" ... into title and season. Only a marker at the end after a separator, with something in front of it. `remap` moves the episodes of such a provider series to the season of the marker when the provider numbers them from 1 again; episodes that already carry their own season numbers stay as they are.
- `LangTagLogic.MERGE_SEASONS` is a third pseudo code in the "hidden" set (like the content mode). So the option reaches the parser, and a change invalidates the stored parse cache; `PlaylistMode.withMode` adds it. The page switch calls the same "read the stored list again" step as the language filter.
- `M3uParser.parseLines`: with the option on, an entry with the Xtream placeholder ending (`.xtser`) is split; title and season come from the marker, so all seasons of the show become one series with one placeholder per season.
- `XtreamLogic.lazyParts` (and `lazyUrls` on top of it) returns all placeholders of such a series; before, only series with exactly one episode counted. A cheap check of the ending keeps the many series cards fast.
- `LazySeries.load`: loads every placeholder and merges the episodes (existing `MultiLogic.mergeEpisodes`), with the season fix from above.
- `AppSettings.mergeSeasons` (new key `merge_seasons` in the existing settings file).

## Effect on user data (why it is an option)

The series id is built from the cleaned title (`s_<title>`). With the option on, a combined series has the id `s_show` instead of `s_show s01`. Watch history, resume positions and "My list" of those series are stored under the old ids and are not shown for the combined series. They are not deleted: switching the option off shows them again. Nothing else changes with the option off.

## Not done / limits

- M3U playlists (already combined by name), Stalker (has its own seasons) and series whose provider already lists one entry for the whole show: not changed.
- A series with a marker that is part of the real title (for example a show called "Foo S2") would be combined with "Foo S1" if both exist. The option is off by default for this reason.
- If a provider numbers the episodes of each provider series from 1 and also has real season 1 only, the remap puts them into the season of the marker. If the provider uses its own season numbers for the episodes (for example all 2), they are kept.
- Counts on cards before opening a series show the number of placeholders (one per season), as for any series that is only loaded on opening.

## Tests

- JVM: `SeasonMergeLogicTest` and `SeasonMergeParseTest` 8/8, `XtreamLogicTest`, `DictEn1Test` 5/5 and `M3uGroupLogicTest` 6/6; 26 tests in all, run with the Kotlin compiler bundled in Gradle. The parser tests cover: option off keeps the ids as before; option on joins the seasons and finds both placeholders; real files and titles without a marker stay untouched.
- Syntax check of the changed Android files: no syntax errors. Not built, not device tested.

### Manual test steps (maintainer)

1. Xtream playlist whose provider lists seasons as separate series: Series shows "Show S01", "Show S02" ... as before.
2. Settings > Playlists > Series > "Combine seasons": on. The playlist is read again; Series shows one "Show" with the seasons.
3. Open "Show": the seasons and episodes of all provider series are there; episodes of season 2 are in season 2 and play.
4. Play an episode, leave, come back: resume works for the combined series.
5. Switch the option off again: the separate series are back with their old history and "My list" entries.
6. A series without a season marker, and a normal M3U playlist: unchanged with the option on and off.
7. Startup time and scrolling in Series with a large playlist: no change.
