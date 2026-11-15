# T-026 – Home tab, header search, guide back

Status: implemented in the working tree. Not built. Not tested on a device.

## Goal

- The Home entry is not in the sidebar and is not a start page.
- The search field at the top of Movies, Series and the other pages is gone. Search stays reachable from the sidebar.
- Starting a channel or an archive show from the TV guide, then pressing Back, opens the guide again.

## Affected files

- `MenuLogic.kt`, `SettingsLogic.kt`, `Sidebar.kt`, `Store.kt` — Home is excluded from the visible menu. Search is no longer hidden by default. One-time `menu_ver` 3 removes Search from the saved hidden list.
- `SettingsHub.kt`, `DictC.kt`, `DictNl2.kt` — start page no longer offers Home. Menu hint no longer says Home stays visible.
- `MainActivity.kt`, `HomeScreen.kt`, `SearchScreen.kt` — Home key and Back from the sidebar go to the start page (first visible page, Live TV by default).
- `Header.kt` — search pill removed.
- `PlayerScreen.kt`, `PlayerGuide.kt` — Back after a guide selection reopens the guide.
- Tests: `CoreLogicTest.kt`, `SettingsLogicTest.kt`, `PlayerLogicTest.kt`.

## Acceptance

- Sidebar has no Home. App opens on Live TV unless another start page is saved and still visible.
- No search field in the top bar. Sidebar has Search.
- From the TV guide, OK on a programme starts it. Back shows the guide again. Back through the guide, then Back, leaves the player.

## Risks

- Settings > Customize home still edits the old home rows. That page is no longer opened from the menu. The button "Direkt auf der Startseite bearbeiten" follows the Home key and opens the start page.
- Users who had hidden Search on purpose see it again once. They can hide it under Settings > Menu.
- Stored menu order can still contain id 0. It is skipped when drawing the sidebar. No key was renamed.

## Checks

Not run in this session: Gradle unit tests, debug build, device.
