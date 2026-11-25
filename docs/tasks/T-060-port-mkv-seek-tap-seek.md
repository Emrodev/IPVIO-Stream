# T-060 Port of MKV seeking without Cues (#10) and tap-to-seek (#25)

- **Issues:** #10 "Seeking in MKV files without Cues restarts playback from 0", #25 "Multiple touch and interaction issues" (timeline tap)
- **Status:** ported, not built, not device tested
- **Source:** closed pull request #40 (branch `integration/2026-11-11` in the fork of `browanoncoder`). The pull request was never merged, although issue comments say "fixed". Only these two parts were taken over; the rest of #40 (Live TV, search, Stalker, translations, test scripts) was not.
- **Protected area:** touches the player (`PlayerScreen.kt`). Done on the maintainer's go-ahead of 24 Nov 2026; a review of the player change is still needed.

## What was taken over

- `MkvSeekLogic.kt` (pure): finds a Matroska Cluster in a byte buffer, estimates and refines the byte position for a target time.
- `CuelessMkvExtractor.kt`: wraps Media3's Matroska extractor only when it reports an unseekable file with known duration and size. It publishes an estimated seek map, finds the next Cluster, reads its timecode and corrects the position (at most 10 extra range requests), then hands over to the normal extractor. If anything does not fit it gives up and behaves as before (start from 0:00). Files with Cues and all other formats are not touched.
- `TapSeekLogic.kt` (pure): touch position on the timeline to target time; stays one second before the end.
- `PlayerScreen.kt`: `DefaultMediaSourceFactory(ctx, CuelessMkvExtractor.Factory())`; the seek bar gets `onTouchSeek`.
- `PlayerControls.kt`: `PSeekBar` handles tap and horizontal drag (`onTouchSeek`, default does nothing, so other callers are unchanged).
- Tests: `MkvSeekLogicTest`, `TapSeekLogicTest`.

Not taken over: the `ChannelUp` / `ChannelDown` keys from the same patch (a different change).

## Review done here

- Read the whole code of the author before taking it over. No network access, no storage, no new dependency. Media3 version is unchanged since the first commit (1.11.1), the version the author built against.
- The extractor reads at most 12 MiB of the file around a seek position (192 windows of 64 KiB), then gives up.

## Risks

- Medium: player core. A wrong guess means a seek lands at a different place or restarts at 0:00, as before. It cannot make a working file worse, because it only acts when Media3 reports the file as unseekable.
- A file with a non-default timecode scale is detected (timecode beyond the duration) and falls back to the old behaviour.
- Overrides of `ExtractorsFactory` depend on the Media3 version; not compiled here.

## Tests

- JVM: `MkvSeekLogicTest` and `TapSeekLogicTest` 10/10 run here with the Kotlin compiler bundled in Gradle.
- Syntax check of the changed Android files: no syntax errors. Type check and build: not run (no Android SDK). Device: not tested.
- The author reports a test on an NVIDIA SHIELD and on the Android TV emulator with generated files (20 MB and 2.7 GB); this was not repeated.

### Manual test steps (maintainer)

1. Play an MKV without Cues (for example remuxed with `mkvmerge --no-cues`) and skip forward with the remote: playback continues near the new position, not at 0:00.
2. Resume a started MKV without Cues: it continues where it stopped.
3. An MKV with Cues, an MP4 and an HLS stream: seeking works as before.
4. Phone or tablet: tap on the timeline, and drag along it. Playback jumps to that position. Live TV: the timeline does nothing.
5. Remote: Left/Right on the timeline still jump by the set step.
