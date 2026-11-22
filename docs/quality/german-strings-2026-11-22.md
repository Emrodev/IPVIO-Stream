# German text outside the dictionaries – 2026-11-22

Status: report, no code changed. Decision needed from the maintainer (CLAUDE.md sections 4 and 5).

## Method

- Tool: `.claude/scripts/english-check.sh` (word list of distinctive German words, case-insensitive, whole words).
- Scope: all `*.kt` under `app/src/main`, except `Dict*.kt`, scanned on the whole tree (base is the empty tree).
- Classes: KEY (German as the first argument of `t(...)` or `tr(...)`, the translation key, allowed), UI (German in a UI call such as `Text`, buttons, toggles), MSG (German in an error, status or return text), CODE (other German words; includes user-visible text the heuristic does not recognise, and some false positives), COMMENT (German in a code comment).
- The classes UI, MSG and CODE are heuristics. Most CODE hits checked by sampling are user-visible German text. A line-by-line review is not done.
- This report contains no German text, by CLAUDE.md section 4. To see the lines, run the script in the repository (see "Reproduce").

## Totals (whole tree, 116 files)

| Class   | Lines | Meaning                                               |
|---------|------:|-------------------------------------------------------|
| KEY     |   261 | translation key, allowed                              |
| UI      |   100 | German label in a UI call, not via a dictionary       |
| MSG     |    96 | German error or status text, not via a dictionary     |
| CODE    |   435 | other German words; mostly user-visible text (sampled)|
| COMMENT |    18 | German comment, must be English                       |
| TEXT    |    57 | German in docs or CHANGELOG (see note)                |

Total outside KEY: 706 lines.

## Largest files (all classes)

| File | Total | KEY | UI | MSG | CODE | COMMENT |
|------|------:|----:|---:|----:|-----:|--------:|
| SettingsHub.kt | 168 | 78 | 39 | 5 | 43 | 3 |
| PlayerScreen.kt | 44 | 4 | 0 | 2 | 38 | 0 |
| DriveUi.kt | 38 | 5 | 5 | 13 | 15 | 0 |
| SettingsExtra.kt | 32 | 1 | 1 | 0 | 30 | 0 |
| PlaylistStore.kt | 29 | 0 | 0 | 13 | 16 | 0 |
| AddPlaylistScreen.kt | 28 | 9 | 5 | 7 | 4 | 3 |
| LiveTab.kt | 28 | 3 | 6 | 0 | 19 | 0 |
| HomeScreen.kt | 27 | 9 | 4 | 0 | 14 | 0 |
| WhatsNew.kt | 27 | 1 | 0 | 0 | 26 | 0 |
| Boomer.kt | 26 | 10 | 7 | 0 | 9 | 0 |
| DetailScreen.kt | 19 | 7 | 1 | 0 | 11 | 0 |
| SportTab.kt | 19 | 8 | 3 | 0 | 8 | 0 |

The remaining 104 files have fewer than 19 lines each.

## Observations

1. **The dictionary system is the existing path.** `t("German", "English")` and `tr(li, "German", "English")` already carry most UI text (261 lines). The hard-coded German in the table is outside that path, so it does not follow the language setting.
2. **`WhatsNew.kt` (26 CODE lines)** holds German text that is shown to users. Its texts are release information; check how this file is used before changing it.
3. **Hint texts.** WORK_STATE says hint texts are English on purpose (maintainer request), but German hint texts exist in `AddPlaylistScreen.kt`, `DriveUi.kt` and `FitCalibration.kt`. Check whether these are old or whether the rule changed.
4. **`TEXT` (57 lines)** is spread over about 20 files: task files (for example T-015 with 9 lines, T-025 with 5), `CHANGELOG.md` (7), `docs/tasks/WORK_STATE-history.md` (7, archive), `docs/RELEASE.md` and `docs/SECURITY.md` (1 each). These are errors under section 4. Task files from before the rule are the bulk.
5. **False positives.** Example: the ISO code for an undetermined audio language in `MediaProbeLogic.kt` matches the word list. Expect a few per file.

## Options for the maintainer

- **A. Move the German UI and message texts to dictionary rows** (`t(de, en)` or `tr(li, de, en)`), file by file, starting with the smallest files. Visible change for users in English and other languages; no change for German users. Needs approval (section 5 and 6). Recommended, in stages of one or two files per pull request, each with a device test.
- **B. Keep the hard-coded German and record it as an exception** in `docs/REFACTORING-PROGRESS.md`. Smallest work now, but the section 4 rule is then not met.
- **C. Translate the archive and the old CHANGELOG/task lines (TEXT).** Documentation only; no user effect. Needs a decision on history rewriting first.

Recommendation: C now (no user effect, cleans the English-only rule), then A starting with `WhatsNew.kt` and `MediaProbeLogic.kt` (check false positives first), after the maintainer decides the staging.

## Reproduce

```
.claude/scripts/english-check.sh "$(git hash-object -t tree /dev/null)"          # summary and first lines
FULL_OUT=/path/out.tsv .claude/scripts/english-check.sh "$(git hash-object -t tree /dev/null)"   # all lines, class and file
```

## Limits

- Word list only. German without a listed word is not found.
- Counts are heuristic in UI, MSG and CODE. Treat them as a size estimate, not as a list of errors.
- No test was run; this is an analysis, not a build or device result.
