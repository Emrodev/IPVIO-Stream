Last update: 2026-11-22 (T-036, workflow files). Read `CLAUDE.md` first, then this file.

The former log is kept unchanged in `docs/tasks/WORK_STATE-history.md`. Do not add new entries there.

## Current task

T-036 (done, committed `dd4c17a`): this file, the archive, `/start`, `/new-task`. Task file `docs/tasks/T-036-workflow-state-and-commands.md`.

T-037 (done, commit `5cdea91`): `/protected-check` and the release-notes skill, each with a check script in `.claude/scripts/`. Task file `docs/tasks/T-037-protected-check-release-notes.md`.

T-038 (done, in the branch commit after `5cdea91`): `/english-check`, report only, with `.claude/scripts/english-check.sh`. Task file `docs/tasks/T-038-english-check.md`. T-039 (done, in the branch commit after `7778f0f`): inventory of German text outside `Dict*.kt`, report `docs/quality/german-strings-2026-11-22.md`. About 700 lines outside the translation keys, by word-list estimate. T-040 (done, in the branch commit after `fff53a8`): option C of T-039 — 21 docs and the archive translated to the English dictionary labels. Open: option A, started as T-041 (measurement only so far: 184 unique German texts without a dictionary row, see the correction in the German-strings report). And 7 CHANGELOG lines in released sections (section 18). No app code changed.

## Release state (from git log, 2026-11-22)

- Latest release: **1.4.7** (`versionCode` 13), merged into `main` (PR #144, merge `ef3d17e`) and `dev` (PR #143, merge `09fcc2a`). Release notes: `docs/release-notes/v1.4.7.md`.
- Releases since the old log: 1.4.5 (category scrolling, Sublists page removed, T-034 performance), 1.4.6 (settings tabs, Reload button per playlist, refresh translation, Live TV addresses without `/live/`), 1.4.7 (Live TV playback over agreed http, instant playlist switch from memory, English diagnostics).
- The CHANGELOG marks these changes as "Not device tested yet". Device results are not recorded in the repository.

## Open device tests and decisions

The full list with test steps is in `docs/tasks/WORK_STATE-history.md`, section "Open problems". Its status is from 21 Nov 2026 and is not re-verified. Main items:

- Playlist mode switch, demo mode, favorites reorder (T-014/T-018), settings reset: device tests were pending in the old log.
- T-024 (R8 over the whole app): not built, not device tested when last recorded. Rollback is `enable = false`.
- T-015 (update check), T-014 (first-start update card), T-026 (playlist cache), T-028 (private categories), T-033 (stream info): device tests pending.
- T-006 and T-009 (backup): device checks pending.
- Maintainer decisions open: PR #40 (conflicts with `main`), T-010 and T-012 (Stalker, against on-demand loading), `ipvio_plmode` in the backup, T-004 (quality baseline: ktlint, detekt, Lint on the maintainer's machine), removal of the sublist load filter in the IPTV core (needs its own approval).

## Task IDs

- Highest ID in `docs/tasks/`: T-040. **Next free ID: T-041** (use `/new-task`, which checks this).
- IDs used by more than one file: T-009, T-014, T-026, T-027, T-032. The files are not renamed here, because the old log and other task files refer to them by name. Renaming needs a decision.

## Conventions

- One branch and one PR per change, into `dev`. Merge into `main` only through release PRs (`CLAUDE.md` section 16).
- Protected areas (`CLAUDE.md` section 5) and approval list (section 6) apply before any change.
- The assistant cannot run Gradle or test on a device. Every change ends with test steps for the maintainer.
