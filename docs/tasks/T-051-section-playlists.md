# T-051: Playlist selection per section

Status: **stage 1 done (logic and tests), stage 2 open (wiring and dropdown). Not compiled, not device tested.**

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

## Stage 2 (open)

- `AreaSource.kt` and `LiveSource` hold a set of indices instead of one index. Call sites: `Header.kt`, `LiveTab.kt`, `HomeScreen.kt`, `BrowseScreen.kt`, `AreaSource.filter`.
- New storage with its own preference keys (not `multi` or `active`, see CLAUDE.md section 5).
- Dropdown with checkboxes and greyed entries in `LiveTab.kt` (Live TV) and `Header.kt` (Movies, Series).
- Startpage and search stay on `multi`.

## Risks

- Changes the IPTV core (CLAUDE.md section 6). Needs the maintainer's approval for stage 2.
- Memory and start time with five large playlists: not measured.

## Test steps for the maintainer (after stage 2)

1. Five playlists, two of them "Live TV only": open Live TV, the dropdown lists only playlists that fit Live TV.
2. Movies: the Live TV-only playlists are greyed out with the reason.
3. Select 4 of 5 for Live TV: only those four channels appear.
4. Choose "All playlists" again: all eligible playlists appear.
5. Restart the app: the selection is kept.
6. Startpage and search show the same playlists as before.
