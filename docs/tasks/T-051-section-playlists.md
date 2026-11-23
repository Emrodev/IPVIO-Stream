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
