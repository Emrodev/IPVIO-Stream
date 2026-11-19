# T-026: README rewrite

**Status:** done in the working branch, open for review. Not built (documentation only).

## Goal

Rewrite `README.md` so the most important facts come first (what the app is, the Downloader code, requirements, the legal notice), then the key features, then a short "Good to know" section with remote-control tips that are not obvious at first. Keep the existing install, API key, build, licence and support content.

## Affected files

- `README.md`: rewritten (English, as required by CLAUDE.md section 4).
- `CHANGELOG.md`: entry under "Unreleased".
- `docs/tasks/T-026-readme-rewrite.md`: this file.

## Acceptance criteria

- The first screen shows what the app is, the Downloader code `4200542`, the minimum Android version (API 24 from `app/build.gradle.kts`), and the notice that the app provides no content.
- Every remote-control tip matches the code. Checked against: `PlayerScreen.kt` (Left opens the channel list, Up/Down zap, Right opens the guide, number keys, 0 = last channel), `Keys.kt` (`LONG_OK_MS` = 350 ms, menu key and long OK), `MenuLogic.kt` (search and EPG hidden by default, search in the header), `HomeScreen.kt` and `Sidebar.kt` (My list = bookmark, Favourites = heart), `HomeParts.kt` (favourites menu with move actions).
- Screenshots are the existing files in `docs/screenshots/`. They will be replaced by the maintainer.

## Risks

- Public text: wrong claims mislead users. Items that are not verified in the code were removed (store names for the Downloader app, theme and background counts, "no tracking", "Reset all" as a label).
- Live sync (Google Drive, version 1.4.3) is not device tested. The README mentions only the optional Google Drive backup.

## Documentation needed

- `docs/FAQ.md` still says "Stalker: series are not supported yet". The code loads Stalker series when they are opened (`StalkerApi.kt`). Not changed in this task; the FAQ needs its own check.

## Test steps for the maintainer

1. Open `README.md` on GitHub: the first screen shows the Downloader code, requirements and the notice without scrolling far.
2. Check the links in the "Documentation" table and the anchors (the README has no table of contents).
3. On a device, check the tips in the "Good to know" section: Left in a live stream opens the channel list; Up/Down zap; long OK on a favourite opens the move menu; the sidebar and menu settings are where the README says.
4. Replace the screenshots in `docs/screenshots/` if you want new ones; the file names can stay.

## Test results

- Documentation only. No Gradle build and no unit test were run. Markdown was checked by reading, not by a renderer.
