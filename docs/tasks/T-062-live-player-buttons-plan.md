# T-062 Live TV player: fewer buttons, rewind on catch-up channels, own button layout

- **Issues:** #131 (no way to rewind in Live TV); maintainer request of 24 Nov 2026
- **Status:** approved and implemented on 24 Nov 2026 (all three parts), not built, not device tested. The plan below is kept as written; the section "Decisions and what was built" says what changed against it.

## Goal

1. The Live TV player shows only the important buttons; the rest moves into the player menu and the settings.
2. On catch-up channels (archive) there are rewind and fast-forward buttons.
3. The maintainer can choose and order the buttons of the Live TV player, with a separate layout for catch-up channels.

## What exists today (read from `PlayerScreen.kt`, lines about 1399 to 1500)

- The live button row already scrolls sideways (`horizontalScroll`).
- Left opens the channel list and Right opens the TV guide when the controls are hidden (`PlayerScreen.kt` about line 1094). Up and Down change the channel. The hint under the buttons says "Up and Down change the channel".
- Live buttons in order: Channel list, TV guide, Back to live, 30 s back (only with the local buffer `LiveTimeshift`), Record, From the beginning and Archive (only with `Catchup.has`), Favorites, Audio & subtitles, Aspect ratio, Second picture, Quality (several streams), Score overlay (only with the mini score), Settings, Open externally.
- Catch-up: `Catchup.urlFor(item, entry)` builds the archive address for a show from its start and end (`CatchupLogic.url`). `playArchive` plays it as a non-live item with an `ArchiveRef`, and then the normal movie controls (step back/forward, seek bar, Left/Right) work.

## Part 1: fewer buttons

- Remove the buttons "Channel list" and "TV guide". The hint line says: "Left opens the channel list, Right opens the TV guide" next to "Up and Down change the channel". (The keys already work.)
- Move "Aspect ratio", "Open externally" and "Second picture" into the player's own menu ("Settings" panel, `Panel.MENU`). The app settings page gets a matching entry where it makes sense.
- Result in the default layout: Back to live, (rewind and forward on catch-up channels), Record, From the beginning, Archive, Favorites, Audio & subtitles, Quality (if offered), Score overlay (if shown), Settings.

## Part 2: rewind and forward on catch-up channels

Idea: on a live catch-up channel, "rewind N" plays the archive from (now minus N) as a synthetic show: `EpgEntry(start = now - N, end = now + a margin or the end of the current show)`. The player then is in archive mode, where the existing seek bar and step buttons work. "Forward" is available in archive mode; near the live edge it switches back to live (`backToLive`).

- Buttons: "Back 30 s" / "Back 5 min" (steps to be chosen), "Forward" (only in archive mode).
- Channels without archive but with the local buffer (`LiveTimeshift.active`): the existing `seekBy` is used for the same buttons.
- New pure logic `CatchupSeekLogic` (JVM testable): target time, clamp to the archive days of the channel, decide archive/buffer/none, and the switch back to live near the live edge.

Open points to verify (unverified):
- Many Xtream servers take the archive start with minute precision. Then the app starts at the full minute before the target and seeks inside the stream by the remainder.
- Providers differ: some only serve whole shows, some cut at the show end. A fallback is to start the show that contains the target time and seek inside it.
- The archive stream is a delayed copy of the live stream (a few seconds to minutes late). The label must not promise exact timing.

## Part 3: own button layout

- A registry of buttons by id (enum, pure Kotlin), each with: default position, "always visible" flag (Settings cannot be hidden), availability (needs archive, needs buffer, needs several qualities, needs the mini score).
- Two layouts: "Live TV" and "Catch-up channels". Each is an ordered list of ids plus a hidden set. Defaults as in part 1.
- Settings page "Live TV player buttons": show/hide, move left/right (same pattern as moving categories), reset to default, a separate switch for the catch-up layout.
- Storage: a new preference file `ipvio_playerbtns` per profile (additive, new name; no existing key changes). A stored string is validated on read (unknown ids dropped, new buttons appended, Settings always present), so a broken value falls back to the default. Add the name to the backup list (`Backup.kt` `NAMES`) only after the maintainer agrees (protected list).
- Buttons that do not fit just scroll to the right (already so).

## Stages (each one can ship alone)

1. Part 1 (layout cleanup and hint line). Small.
2. Part 2 (catch-up rewind/forward). Medium; needs a device with a catch-up provider.
3. Part 3 (customisation). Medium; stage 1 and 2 first so the registry knows all buttons.

## Decisions needed from the maintainer

1. Rewind steps on catch-up: 30 s and 5 min? Or the seek step from Settings ("Jump left/right")?
2. Should Left/Right (with the controls hidden) stay "channel list / TV guide" on catch-up channels, or seek there? Proposal: unchanged, so the keys never change meaning per channel.
3. Is the catch-up layout a separate list (proposal) or one list with per-button "also on catch-up" flags?
4. May `ipvio_playerbtns` be a new stored file and be added to the backup (protected area)?
5. German UI text is the translation key: new texts need dictionary rows (English first).

## Risks

- Player core: control row and key handling. Changes stay in the button row and a new logic file; playback itself is not touched in stages 1 and 3.
- Stage 2 starts archive playback from a computed time. Wrong guesses mean "Not available in the archive" (existing flash) and live continues.
- Nothing can be tested here: no Gradle, no device, no catch-up provider. JVM tests only for the new pure logic.

## Tests (planned)

- JVM: `PlayerButtonsLogicTest` (defaults, validation of stored values, new buttons appended, Settings never hidden, availability filter), `CatchupSeekLogicTest` (target time, clamp to archive days, live-edge switch).
- Manual: on a catch-up channel rewind and forward; without archive the buttons are missing; a custom layout survives a restart; a broken stored value falls back.

## Decisions and what was built (24 Nov 2026)

Decisions of the maintainer:

1. Rewind and fast-forward use the seek step from Settings ("Jump left/right", `AppSettings.seekStep`).
2. Left and Right stay "channel list" and "TV guide"; seeking is done with the buttons.
3. One layout. The seek buttons and "Settings" are fixed; everything else is chosen by the user. The seek buttons stay in the row so the user can seek while watching.
4. Storage: decided here. New file `ipvio_playerbtns` (additive), added to the backup list (`Backup.kt`, `NAMES`); the Live TV page reset clears it.

Built:

- Part 1: the buttons "Channel list" and "TV guide" are gone. The hint line says "Left: channel list · Right: TV guide · Up and Down change the channel". Aspect ratio, second picture and "Open externally" are not shown by default; they were already in the player menu "Settings".
- Part 2: new fixed buttons "<step> seconds back" (and "<step> seconds forward" while the local buffer is active).
  - With the buffer (`LiveTimeshift.active`): `seekBy` as before, now with the seek step.
  - Catch-up channel without buffer: `rewindLive()` takes the show that is on air at (now minus step) from the full schedule (`EpgFull`), plays it with the existing archive link (`playArchive`) from its start and moves to the offset (`startMs`). The offset avoids the minute precision of the archive start. If no show is known or the archive cannot play it, the existing message "Not available in the archive" appears.
  - In archive mode the normal movie controls (back/forward by the step, previous/next show, "Back to live") are used, as before.
  - The old fixed "30 s back" button is replaced by these.
- Part 3: Settings > Live TV > "Player buttons (Live TV)": a switch per button and a position per shown button. The page reset (Settings > Live TV) restores the default. `PlayerButtonsLogic` (pure) and `PlayerButtonPrefs` (stored list; a broken value falls back to the default).
- New files: `PlayerButtonsLogic.kt`, `CatchupSeekLogic.kt`, `PlayerButtonPrefs.kt`; tests `PlayerButtonsLogicTest`, `CatchupSeekLogicTest`.

Changes against the plan: one layout instead of two; no separate catch-up list. The default order of the shown buttons is: Back to live, Record, From the beginning, Archive, Favorites, Audio & subtitles, Quality, Score overlay (each only where it applies).

Unverified (needs a device with a catch-up provider):

- That the archive playback starts at `startMs` for an archive item (the same field is the resume position of movies).
- That the archive of the running show can be played this early (`CatchupLogic.canPlay` needs a start at least 60 s ago); a show that started less than a minute ago gives "Not available in the archive".
- That the archive stream is complete up to "now". Many servers lag by seconds to minutes.
- Two focus requesters on one button (for example Archive as the first button) are chained; this is believed to work in Compose, not tried.

Tests run here (JVM, Kotlin compiler bundled in Gradle): `PlayerButtonsLogicTest` and `CatchupSeekLogicTest` 11/11, `DictEn1Test` 5/5. Syntax check of the changed Android files: no syntax errors. Not built, not device tested.

### Manual test steps (maintainer)

1. Live TV player, any channel: the button row has no "Channel list" or "TV guide". Left (controls hidden) opens the channel list, Right opens the TV guide. The hint line says so.
2. Settings > Live TV > Player buttons: hide "Favorites" and move "Audio & subtitles" to position 1. Open the player: the row follows. Restart the app: the choice stays. "Reset this page" restores the default.
3. Add "Aspect ratio", "Second picture" and "Open externally" back: they appear in the row. Without them, all three are in the player menu "Settings".
4. Catch-up channel (archive): a button "10 seconds back" is the first button (the step from Settings). Press it: the show of that time plays from the right place, the normal movie controls appear, "Back to live" returns.
5. Catch-up channel, step 60 s or more and the target in the previous show: it plays the previous show at the right offset.
6. Channel with the local buffer on: "back" and "forward" move inside the buffer by the step.
7. Channel without archive and without buffer: no rewind buttons.
8. Remote without media keys: reach and press every button in the row with the D-pad; Down closes the controls as before.
