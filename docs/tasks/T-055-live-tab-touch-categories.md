# T-055 Live TV: select categories by tap on phones

- **Issue:** #152 "Live TV: categories cannot be selected by touch on phones"
- **Status:** implemented, not built, not device tested

## Goal

On a phone (touch, no remote), tapping a category in the Live TV tab or the "Search channels" row shows the channel list / search. Remote behaviour stays unchanged.

## Cause

`CatRow` sets the category (`hot`, `sel`) in `onFocus`. A tap in touch mode does not move focus, so only `onClick` runs. The click called `enterChannels()`, which focuses the first channel row. In touch mode that focus never happens, so nothing changes.

## Affected files

- `app/src/main/java/com/ipvio/stream/LiveTab.kt`: click handler of the normal category row and of the "Search channels" row.
- `CHANGELOG.md`

## Change

- Normal category row: `onClick` sets `hot = n` before `enterChannels()`. With a remote, `hot` is already `n` (set on focus), so this is a no-op there.
- "Search channels" row: `onClick` sets `hot` and `sel` to `"SEARCH"` before focusing the search field. Same as the focus path.

The patch proposed in the issue uses an input-mode check (`LocalInputModeManager`). That class does not exist in this code base, so it was not used.

## Acceptance criteria

- Phone, touch only: tapping "All channels" or any category shows its channels.
- Phone, touch only: tapping "Search channels" opens the search panel.
- Remote (Android TV / Fire TV): Right/OK on a category still enters the channel list as before.

## Risks

- Low. Two click handlers in one file, no storage, no player code.
- Not covered: long press on touch (channel menu, category menu) and the info panel that follows focus. These are separate problems and stay open in #152.

## Tests

- Unit test: not applicable. The change is in Compose UI handlers and does not contain pure logic.
- Syntax check: not run (no Kotlin compiler in this environment).
- Build: not run (no Gradle / Android SDK here).

### Manual test steps (maintainer)

1. Phone, touch only: open Live TV, tap "All channels". Expected: channel list shows.
2. Phone, touch only: tap a provider category. Expected: its channels show.
3. Phone, touch only: tap "Search channels". Expected: search panel opens.
4. Android TV with remote: move to a category with Down, press OK. Expected: channel list opens with focus on the first channel, as before.
5. Android TV with remote: Right on a category. Expected: same as step 4.

## Documentation

- `CHANGELOG.md` (Unreleased): one line.
- Release notes: not needed until a release is prepared.
