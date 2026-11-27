# T-077: user feedback batch 2 (27 Nov 2026)

Status: part done (27 Nov), part planned. Items 8, 10 and 12 done on 27 Nov 2026 after the maintainer's answers (see below). Not built with Gradle, not device tested. Branch `claude/confident-ritchie-byuqg1`.

## Done in this batch

| Item | Files | What |
|---|---|---|
| Search placeholder still showed "Search movies, series or channels" | `DictB.kt`, `DictAr1.kt`, `DictNl1.kt`, `DictPl1.kt`, `DictPt1.kt`, `DictZh.kt` | The search screen uses the key "Filme, Serien oder Sender suchen…"; the English (and other) values are now "Search..." etc. The earlier change had hit a different key. |
| "Movie" word on every search cover | `Components.kt` (`PosterCard`, new switch `kindBadge`), `SearchScreen.kt` | Result covers in the search no longer show the kind word; the heading above the row already names it. Other cards keep the badge. |
| Live TV: Up on the first category did not wrap | `LiveTab.kt` | The "TV-Guide", "Multi view", "Playlist", "Settings", "Hidden", "New list" and "Search channels" rows did not report focus, so the wrap did not know the row. Every row now reports its index. |
| Search: Up from the number row did not reach the recent searches | `SearchScreen.kt` (`SketchKeyboard`, `onUp`) | Up on the number row focuses the first recent search. |

Register: B-013 (new), B-003 (extra marker). `.claude/scripts/behaviour-check.sh`: 23 markers, 0 missing.

## Planned, not done (decisions needed where marked)

### 8. Genre rows in Live TV can be moved (DONE 27 Nov, approved)
- Today: "Sport", "Movies", "Documentaries", "Kids", "News", "Music" are fixed; only "hide" works. The reason is in `LiveTab.kt`: `plainCat` excludes `LiveGenre.isGenreKey(n)`, so the move keys are not attached.
- Existing switch: Settings > Live TV > "Genre categories in Live TV" (T-063) turns all of them off. It is on by default. Maybe the maintainer did not see it.
- Plan: allow the move for genre rows (they take part in `CatOrder.LIVE`, same as provider categories). Visible change; storage: the order list already exists (`ipvio_catorder`-type key, check before writing).
- **Question:** approve the move for genre rows? Should the switch stay as it is?

### 9. Moving the last channel category: it sticks at the bottom; the cursor disappears after a wrap
- "Move Up / Move Down" on the last category: it does not move back down. Likely in `CatOrder.move` at the list end and in the floating handler (`floatingCat`). To check on the device with the steps.
- Wrap from the last category with Down: the list scrolls, the focus moves to the first row, but the cursor is not visible. Candidate: the row is focused but scrolled out of the visible area, or the focus highlight is drawn with `hot` on another row. Needs logs or a reproduction.

### 10. Playlist picker: Left and Right close it (DONE 27 Nov, the top-bar Playlist list on Home, Movies and Series)
- Which screen: "where the playlists are listed" (maintainer to confirm: Settings > Playlists, or the playlist menu). Plan: `onPreviewKeyEvent` for Left and Right → close, like Back.
- **Question:** which screen exactly?

### 11. Playlist reload: show a percentage
- The loading overlay shows a spinner only. Xtream downloads report progress (see `T-008-xtream-progress.md`). Plan: show "xx %" when the total size is known, otherwise the item count. Check the source of the progress value first.

### 12. Settings: the page stops moving; Down at the end goes to the Back arrow (DONE 27 Nov, Back arrow chosen)
- Wanted (maintainer): the page stops moving at the second-to-last focusable control; only the cursor moves to the end. Wrap at the end of a settings page goes to the back arrow next to the page title (maintainer offered the choices: back arrow, or the first control of the page; recommendation: back arrow, because it is always visible).
- Cause: the page uses the centring spec from T-074 (`FocusCenterLogic`), so the page moves on every step. Settings and the Settings hub need their own spec, with the old edge-only behaviour, or no move at all.
- **Question:** back arrow (recommended) or first control of the page?

### 13. Search and Sports Hub open slowly the first time
- Search takes about 2 s on the first open; Sports Hub shows no data at first. Plan: measure first (Diag log), then build the search index and the Sports Hub data earlier, in the background at start, and show what is ready at once instead of waiting.

## Test steps (for the maintainer, once built)
1. Search: type a few letters, press Up on "1" from the keyboard: the first recent search is focused.
2. Search, a category with hits: covers show no "Movie"/"Series" word.
3. Live TV: on the first category Up goes to the last one; on the last category Down goes to the first one. Test on "TV-Guide", "Multi view", "Playlist" rows too.
4. Search placeholder: one line "Search..." in every language.

## Done on 27 Nov 2026 (second part)

- **8, genre rows move (B-015):** `LiveTab.kt`. Genre rows take part in the category order. The default order does not change.
  Every move saves the whole own order (`liveMoveKeys`); a shorter list would have dropped hidden categories and genre positions.
- **10, Left/Right closes the top-bar Playlist list (B-016):** `Dropdown.kt` (`OptionPopup`, new switch `closeOnSide`, off by default),
  `SectionPlaylistUi.kt`, `Header.kt`.
- **12, settings page (B-014):** `SettingsHub.kt` sets `FocusScrollMode.edgeOnly` while the page is open; `MainActivity.kt` uses the
  edge rule there (old rule with a 90 px margin, `FocusCenterLogic.edgeDistance`, 2 new JVM tests). Down at the last control goes to the Back arrow.
  The end-of-page hint scroll was removed.
- Still open from this list: 9 (last category stuck when moving; wrap cursor), 11 (percentage while reloading), 13 (search and Sports Hub open slowly).
- Checks: `FocusCenterLogicTest` 6/6 (run here). Not built with Gradle, not device tested. `behaviour-check.sh` marker check: see the commit message.
