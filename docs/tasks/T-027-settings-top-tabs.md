# T-027: Top navigation bar inside Settings

Status: **open, not planned, not implemented** (wish from the maintainer, noted on 17 Nov 2026; needs approval per CLAUDE.md section 6, visible UI change).

## Goal

Inside Settings, add a top navigation strip (like the main tabs of the app) that jumps straight to a settings section, so the user does not have to scroll back to the overview and then find the page.

## Current state (from the code, as read on 17 Nov 2026)

- `SettingsHub.kt` shows a grid of settings cards (`SettingsCard`, list `CATS`) inside a `LazyColumn`, with a back button (`SettingsBack`) on each page.
- `SettingsNav` (about line 73) holds the page indexes (`PROFILES = 10`, `HOME = 17`, `RESET_ALL = 19`, donate last) and `openPage`. Other code opens pages by number; the indexes must not change silently.
- Focus is managed with `cardFocus` and `backFocus` (about line 91-117).

## Affected files (to be confirmed in the plan)

- `app/src/main/java/com/ipvio/stream/SettingsHub.kt` (main change: top strip, jump logic, focus).
- `app/src/main/java/com/ipvio/stream/SettingsNav` object inside it (no index changes).
- `app/src/main/java/com/ipvio/stream/DictD.kt` / other dictionaries: new labels if the strip needs texts that are not there yet. The German text is the translation key (CLAUDE.md section 5): new keys need a decision by the maintainer.
- Possibly a pure `SettingsNavLogic.kt` (page order and jump target) with a JVM test in `app/src/test/java/com/ipvio/stream/`.

## Options to compare in the plan (CLAUDE.md section 14)

1. **Strip of section chips on top of the settings tab** (horizontal row, focusable with D-pad, OK jumps to the page). Simple, matches the main navigation look.
2. **Sticky section bar on each page** that jumps to other pages without going back first. More change, more focus risk.
3. **Keep the card overview and add an anchor index** only. Least change, but does not match the request.

Recommendation to check: option 1, because it is the smallest change and matches the existing top navigation.

## Acceptance criteria

1. The settings tab shows a top strip with all sections (or the groups the maintainer chooses).
2. OK on a strip item opens that page directly; Back returns to the overview as today.
3. Focus stays usable with the Fire TV remote: Down from the strip goes to the page content, Up from the first card goes to the strip.
4. No change to page indexes, SharedPreferences keys or the donate page position.
5. Phone layout: the strip scrolls horizontally, no page-wide horizontal scroll (16 px gutter).
6. New texts in every language the app supports, or an explicit decision by the maintainer (CLAUDE.md section 5 and the hint-text rule in `WORK_STATE.md`).

## Risks

- Focus after returning from a page (the existing `last` / `cardFocus` logic) can break if the strip takes the first focus.
- `SettingsNav.openPage` is used from other screens; a changed default page would surprise existing users.
- Maintainer decision needed: which sections go into the strip (all 20 pages or groups).

## Documentation needed

- `docs/SETTINGS.md` (update the navigation description).
- `CHANGELOG.md` entry when implemented.

## Test results

- Not run. No Gradle, no device in this session.

## Device test steps (for the maintainer, after the fix)

1. Open Settings: the top strip is visible, the first item has the focus (or the strip is reachable with Up).
2. Press OK on a strip item (for example Player): the page opens at once; Back returns to the overview.
3. Open each strip item once and check that Back returns to the overview and the focus is on the card of that page.
4. Use Down from the strip into the page content and Up back to the strip.
5. Reset all (the reset page) and Donate still open from their places.
6. Phone or narrow window: the strip scrolls horizontally, no page-wide horizontal scroll.
