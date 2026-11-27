# T-079 Open GitHub issues: triage and next steps

Status: triage done, no code change. Built on the earlier analyses in `T-027-issue-batch.md`, `DEVICE-TEST-PLAN-2026-11-25.md`, `ISSUE-COMMENTS-2026-11-25.md` and `WORK_STATE.md`, not repeated here.

Goal: bring the open issue count to 0, oldest first. Most open issues are already fixed in code and wait for a device test, or need logs from the reporter, or need a maintainer decision (CLAUDE.md section 6). The assistant cannot build or run the app (section 3), so nothing below is reported as tested.

## Summary

| Group | Issues | What happens next |
|---|---|---|
| Fixed in code, waiting for device test | 10, 27, 75, 96, 109, 112, 125, 146, 151, 152 | Maintainer device test, then close with a reply |
| Partly fixed or likely the same as another fix | 25, 67, 105 | Device test; reporter asked for the remaining case |
| Needs logs or reporter information | 73, 84, 104, 108 | Logcat or steps from the reporter (see below) |
| Needs analysis (no earlier analysis) | 23, 28, 131 | Read the code, then decide |
| Maintainer decision (approval, CLAUDE.md 6) | 24, 68, 69, 99, 103, 129, 130, 118 | Decide before any code |

## Per issue (oldest first)

Status evidence is from the git history and the task files. "In HEAD" means the commit is in the current branch.

| # | Title (English, as on GitHub) | Status | Next step |
|---|---|---|---|
| 10 | Seeking in MKV files without Cues restarts at 0 | Done: `204f5a1` (in HEAD, release 1.5.1). Not device tested. | Device test: skip, number jump and Resume on an MKV without Cues. Then reply and close. Player core: no further change without approval. |
| 23 | Language not shown when browsing movies; description language differs from audio | Not analysed | Analysis: which field gives the language in the browse view and where the description comes from. |
| 24 | Show stream metadata before playback | Partly: `3000438` (T-033, technical info on the detail page). Poster badges and probing not done. Earlier decision "#24 not now" (`7e4527e`). | Maintainer decision. Probing with a new tool needs approval (section 6). |
| 25 | Multiple touch and interaction issues on mobile | Tap-to-seek done: `204f5a1`. Other touch problems not reproduced. | Ask the reporter which controls fail; device test on a phone. |
| 27 | Movies filter: selected category still shows "All", filters combine | Done: `e441fff` (Genre pill). Stays open until the filter switch is tested. | Device test (DEVICE-TEST-PLAN lists it as "stay open"). |
| 28 | Playback starts at the beginning after exit | Earlier: "no change needed, to be confirmed on a device" (WORK_STATE-history). | Ask for steps; device test of resume after leaving playback. |
| 67 | Cannot click Live TV categories (tablet) | Probably the same as #152 (`c91a808`). Tablet not checked. | Device test on a tablet. Close with #152 if it works. |
| 68 | Build and sign release APKs in CI, with provenance | Not done. Changes `build.yml` and `release.yml`. Batch plan in `4e87c17`. | Maintainer decision (sections 6 and 12). |
| 69 | HTTPS silently falls back to HTTP on certificate mismatch | Consent before an HTTP fallback is in place per T-026 (WORK_STATE-history). Device test pending. | Maintainer security review; device test of the consent dialog. |
| 73 | Playlist setup fails: "content too large, choose fewer languages" | Analysed: memory estimate for a large Xtream list; persists on 1.4.9 (WORK_STATE). | Logs and device memory data from the reporter. |
| 75 | STB model for Stalker portals | Done: `c5c093a` (in HEAD). | Device test against a Stalker portal that needs a non-MAG250 model. Then close. |
| 84 | Series cannot be played | Strong8K episodes of Xtream series do not show; next step `XtreamApi.walkEpisodes` (WORK_STATE). | Read `walkEpisodes` and `episodeList`; ask the reporter for the provider and a log. |
| 96 | "FR" subcategories merged | Done: `1c4f354`, `c3da2b8`, `140b853` (1.5.1). | Device test, then close. |
| 99 | Arabic language support (RTL) and live stream mode | Arabic: plan in `506cc22`, large dictionary work. Live mode: tunneling `5a8ee54` (off by default). | Maintainer decision on Arabic. |
| 103 | Secondary EPG source per channel | Plan in `506cc22`. EPG core: approval needed (section 6). | Maintainer decision. |
| 104 | Series episode: "Connection lost … retrying" while the provider app plays it | Analysed, no change (T-027). No logs. | Logcat from the moment of the error. Player core: no change without approval. |
| 105 | Show new chapters of a followed series under "Continue watching" | Notification for new episodes: `a1f951a` (in HEAD). "Continue watching" part not confirmed. | Ask the reporter what "chapter" means; device test of the notification. |
| 108 | Regression: movies and episodes show "Connection lost … retrying" since the latest update | Analysed (T-027): candidates are the HTTPS fallback change (#69), the R8 build (T-024) and the retry rules in `PlayerScreen.onPlayerError` / `IptvLoadErrorPolicy`. Not confirmed. The buffer change `2c7799d` (26 Nov) is after the report (18 Nov), so it cannot be the cause of this report. | Logcat and the release the reporter had installed. Player core: approval needed for any change. |
| 109 | Home counts show 0 after playback | Done in code: `fc824dc` (Live TV) and `494f00d` (movies and series). Both in HEAD. | Device check, then close. |
| 112 | Playlist disappears by itself | Done: `2bb5611` (keystore never replaces an existing key), `e77e57d` (notice). In 1.5.1. | Keep open until the Fire TV is stable (DEVICE-TEST-PLAN). |
| 118 | Question (manual refresh) and feature idea (search movies by actor) | Question: a per-playlist reload button exists since 1.4.5 (WORK_STATE). Actor search: not planned. | Reply on GitHub (maintainer approval to post). |
| 125 | Films "recently added" carousel does not match the section | Done: `ba99c3b` (in HEAD). | Device test, then close. |
| 129 | Google Play Protect blocks the official 1.4.4 APK | Not a code problem: Play Protect classification of the signed APK. | Maintainer: check the Play Protect appeal. Release guard (1.5.2) keeps the signing certificate fixed. |
| 130 | Turn off score notifications | Delay options up to 120 s: `a087a73`. A per-team or per-game off switch is not built. | Maintainer decision: add the off switch or close with the delay option. |
| 131 | Left/Right skip on a remote without FF/REW | Not analysed. Left opens the channel list in Live TV (T-062). | Analysis of the Left key in `PlayerScreen.kt`. Behaviour change in movies needs a register entry (section 7a). |
| 146 | Optional tunneled playback for Live TV | Done: `5a8ee54` (off by default, 1.5.1). | Device test, then close. |
| 151 | Summarise a series and its episodes together | Done: `a834606` (option "Combine seasons", off by default). | Reply to the reporter about the option; device test; then close. |
| 152 | Live TV categories cannot be selected by touch on phones | Done: `c91a808` (1.5.1). Reporter's patch not applied (reporter could not build it). | Device test on a phone, then close. |

## Notes

- Nothing was closed, commented or pushed to GitHub in this task (CLAUDE.md: outward-facing actions need approval).
- Issue #152 comes with a patch from the reporter. It is not applied: `c91a808` already covers the case, and the patch was not built.
- Changes in the player core (sections 5 and 6) need maintainer approval before code is written: #104, #108, #10 follow-ups, #131.

## Test steps (maintainer)

1. Device test of the "fixed in code" group (table above): install the current `dev` build, run the steps of each issue, and note the result per issue.
2. Send the Diagnostics log lines for #108 and #104 (Settings > Diagnostics > Log) during an error, plus the release the reporter had.
3. Decide the group "Maintainer decision" (24, 68, 69, 99, 103, 129, 130, 118).

## Checks

- Documentation only. Nothing built, nothing run, no tests changed.
- `CHANGELOG.md`: no entry (no user-visible change).
