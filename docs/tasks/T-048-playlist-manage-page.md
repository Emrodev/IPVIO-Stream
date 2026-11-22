# T-048 – Manage page per playlist, apply step, refresh plan per playlist

Status: **PLAN, waiting for approval** (CLAUDE.md sections 6 and 7: visible change for existing users, new stored keys). No code changed yet.

## Request (maintainer, 22 Nov 2026, German, summarised)
1. Every playlist has its own reload button.
2. The many buttons on the playlist rows move behind one menu button ("Manage" or similar).
3. Playlist settings (for example "Live TV only") are confirmed at the end, so they apply only then.
4. Every playlist gets its own refresh option (daily and so on), because one list may never need updates, or only weekly.

## Current state (read in `SettingsHub.kt` `PlaylistPage`, `General.kt`, `Updater.kt`, `SettingsLogic.kt`)
- Each row carries up to 7 buttons: Not shown additionally, Make main list, Reload, content mode (cycles on OK and applies at once), Rename, Backup servers, Delete.
- A Reload button per row exists since 1.4.6 (`SmallPill("Reload")`). On a non-main list it makes that list the main list first.
- Content mode applies immediately and re-parses the stored list.
- Refresh plan is global: `General.refreshMode` (0 every start, 1 every N hours, 2 daily, 3 weekly, 4 by hand), interval, hour, minute, weekday, and one `last_refresh`. Used by `MainActivity` (cold start), `UpdateWorker` (hourly job) and the settings page. Only the main list is refreshed by the plan; other lists are refreshed only when made main or reloaded.
- `refreshCacheOnly(c, url)` exists (cache refresh without loading into memory): usable for non-main lists.

## Proposed design
**A. Row: two buttons.** Each row shows name, host, counts, and only **Reload** and **Manage**. "Manage" opens a page (sub-page of the playlist settings, Back returns) for that playlist.

**B. Manage page, with Apply.** Contents: Make main list / Stop showing additionally, Rename, Content mode (Live TV + VOD / Live TV only / VOD only), Backup servers (Xtream only), Refresh plan (D), Delete (confirm). Edits are kept as a draft. Two buttons at the bottom: **Apply** (writes everything, then runs the one reload step needed, for example re-parse when the content mode changed) and **Cancel**. Back with a changed draft asks "Discard changes?". Delete and Make main list stay immediate actions with their own confirmation, because they are not settings of the list.

**C. Reload.** Stays on the row. Behaviour unchanged (main list reloads where it is; another list becomes main first). Open point: reload a non-main list in the background without switching (`refreshCacheOnly`)? Recommended, but it changes today's behaviour of the 1.4.6 button.

**D. Refresh plan per playlist.** New keys, existing keys untouched:
- Per playlist (key = existing `key(url)` hash): mode, interval, hour, minute, weekday, last refresh, stored in a new SharedPreferences file `ipvio_plrefresh` (new file, nothing renamed). Default "Use the general setting" = today's global plan, so after the update nothing changes for anyone.
- Options per list: Use general setting / At every start / Every few hours / Daily / Weekly / **Never (by hand only)**.
- `SettingsLogic.refreshDue` and `nextDue` already take a plan and a last time: no change. New pure helper chooses the plan for a URL.
- `MainActivity` start: refresh the main list by its own plan (as now, with its own last time). `UpdateWorker` (hourly): for each list whose own plan is due, and which is not "by hand": the main list as now; other lists via `refreshCacheOnly` (needs approval, see questions). The hourly job stays scheduled when any list has a plan in 1..3.
- The global page "Automatically update" stays and becomes "General setting (default for all playlists)".
- Backup (`Backup.kt` `NAMES`): the new file would be added to the list so it is saved and restored. Protected area; included in the plan, not changed before approval.

## Affected files (planned)
`SettingsHub.kt` (row, Manage page, draft/apply), new `PlaylistManageLogic.kt` (draft, what has to be applied: pure), new `PlaylistRefreshLogic.kt` (plan per list, pure) plus store functions, `Updater.kt`, `MainActivity.kt` (own plan for the main list), `PlaylistStore.kt` (per list last refresh, refresh other lists), `Backup.kt` (NAMES), `DictK.kt` or a new dictionary block (English and other languages for the new texts), `CHANGELOG.md`, `docs/`. Tests: `PlaylistManageLogicTest`, `PlaylistRefreshLogicTest`.

## Split into subtasks
1. T-048a Manage page with Apply (UI only, no new storage).
2. T-048b Refresh plan per playlist (storage, logic, worker, backup).
3. T-048c Reload of non-main lists in the background (only if approved).

## Risks
- Visible change on the main settings page for every user (buttons move). Mitigation: same actions, same texts.
- New stored keys: additive, no migration; missing keys mean "general setting".
- Worker refreshing several lists: memory and data use. Mitigation: `refreshCacheOnly` only, one list at a time, not while playing (as now).
- Not buildable or testable here: Gradle build and device test by the maintainer; only pure logic gets JVM tests.

## Decisions needed
1. Order: build 048a first, then 048b (recommended), or everything at once?
2. Background refresh of non-main lists according to their own plan (cache only)?
3. Reload on a non-main list: keep "make main first" or refresh in the background?
