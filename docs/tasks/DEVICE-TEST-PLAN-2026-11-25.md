# Device test plan, 25 Nov 2026 (tasks T-055 to T-066)

One list for the maintainer. Details and edge cases are in the task file of each item. Nothing here was built or run on a device. What did run: 518 JVM tests (pure logic, with the Kotlin compiler bundled in Gradle; 15 test files need Android classes and are left to CI) and a syntax check of the changed Android files.

Order: first build in Android Studio. If the build fails, the likely places are `PlayerScreen.kt` (`applyTunneling`, the rebuilt live button row), `CuelessMkvExtractor.kt` (Media3 overrides) and `Secrets.kt`.

## Must test first (player and data)

| Task | Issue | Device | What to check |
|---|---|---|---|
| T-060 | #10, #25 | any | MKV without Cues: skip forward and resume do not restart at 0:00. MKV with Cues, MP4 and HLS seek as before. Phone: tap and drag on the timeline. |
| T-062 | #131 | Android TV, phone | Live TV button row: no channel list / TV guide buttons; Left and Right open them. Settings > Live TV > Player buttons: hide, show, reorder, restart, page reset. |
| T-062 | #131 | catch-up provider | Rewind button on a catch-up channel plays the show from the right place; "Back to live" returns. No rewind buttons on a channel without archive and without buffer. With the buffer on: back and forward. |
| T-056 | #112 | Fire TV | Update over an existing install: playlists still there. Restart the box several times over some days. If the saved list cannot be read, the empty home screen says so. |
| T-065 | #151 | Xtream with seasons as series | "Combine seasons" off: as before. On: one series with all seasons; episodes play; resume works. Off again: old series and history are back. |

## Then test

| Task | Issue | What to check |
|---|---|---|
| T-055 | #152, #67 | Phone, touch only: tap "All channels", a category and "Search channels" in Live TV. Remote: OK and Right on a category as before. |
| T-058 | #125 | Movies and Series start view: "Recently added" row and page show the same first titles. M3U list: as before. |
| T-059 | #27 | Movies > Categories > an automatic genre: Genre pill shows that genre. Provider categories: pill shows "All". |
| T-061 | search | Search with the filter "Persons": Down reaches the people. |
| T-063 | #96 | Settings > Live TV > "Genre categories in Live TV" off: provider categories follow "All channels"; on: genres are back. |
| T-064 | #145 | M3U list with `tvg-group` or `#EXTGRP`: categories have their names. Normal list: unchanged; favorites and hidden categories still apply. |
| T-066 | #146 | Settings > Player > "Tunneled playback (Live TV)": on a channel that stutters; black picture or sound out of sync means switch it off. Second picture: tunneling is off while it is shown. Movies: unchanged. |
| T-057 | #130 | Settings > Sports Hub > Delay offers up to 120 s; an old value is kept. |

## Finally

- Issues to close after a positive test: #67, #152 (T-055), #109 (already in 1.4.9), #125, #130, #96, #146, #10, #25.
- Stay open: #27 (switching category through the filter), #112 (until the Fire TV is stable), #151 and #145 (need the reporters' answers), #84 (needs information), #108, #104, #73 (need logs), #129 (Play Protect, not a code problem).
- Comment drafts: `docs/tasks/ISSUE-COMMENTS-2026-11-25.md`. Nothing is posted except the comments on #131 and #118 of 24 Nov.
- Release: version bump and release notes (CLAUDE.md section 18) are not done; they are part of preparing the release.
