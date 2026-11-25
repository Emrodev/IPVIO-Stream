# T-066 Optional tunneled playback for Live TV

- **Issue:** #146 "Feature Request: Optional Tunneling Playback for Smoother Live TV" (and #99, part 2: a TiviMate-like mode for channels where the picture stutters while the sound plays normally)
- **Status:** implemented as an option (off by default), not built, not device tested
- **Protected area:** player. Small, additive change; no stored key changes. Done on the maintainer's go-ahead of 25 Nov 2026; a review is needed.

## Change

- Settings > Player > "Source & quality" > "Tunneled playback (Live TV)", default off. Stored as key `tunneling` in the existing file `ipvio_playback` (additive). The player page reset switches it off.
- `TunnelingLogic.wanted(enabled, live, secondPictureOn)` (pure): tunneling is only asked for in Live TV, and not while a second picture is shown (a device usually has one tunneled decoder).
- `PlayerScreen.kt`: `applyTunneling()` sets `setTunnelingEnabled(...)` on the `DefaultTrackSelector.Parameters` (same cast as the existing audio channel option). It runs with the track settings of every title and again when the second picture is opened or closed, or the setting changes.
- Media3 falls back to normal playback by itself when the device has no tunneled decoder for the stream.

## Limits and risks (device behaviour, not tested here)

- Known with tunneling on some devices: black picture, audio/video out of sync, or no effect. That is why it is off by default and the description says what to do.
- The audio delay setting and the app's own audio processing may have no effect while tunneling is active (Media3 does not run audio processors in tunneling mode). Not checked on a device.
- The API (`DefaultTrackSelector.Parameters.Builder.setTunnelingEnabled`) was not compiled here (no Media3 library available). The same cast pattern is used for another option in the same function; if the build fails, this is the line.
- Whether it helps against the reported stutter depends on the channel (codec, resolution) and the device. Nothing was measured.

## Tests

- JVM: `TunnelingLogicTest` 2/2 and `DictEn1Test` 5/5, run with the Kotlin compiler bundled in Gradle. Syntax check of the changed Android files: no syntax errors. Not built, not device tested.

### Manual test steps (maintainer)

1. Settings > Player > Source & quality: the switch "Tunneled playback (Live TV)" is off.
2. Switch it on, open a Live channel that stutters (picture slow, sound fine): note whether it plays better. Try a few channels, also HD and 4K.
3. If the picture stays black or the sound is out of sync: switch it off and reopen the channel; it must play as before.
4. Open a second picture: playback stays correct. Close it: tunneling returns.
5. Movies and episodes: no change with the option on or off.
6. Settings > Player > "Reset this page": the option is off again.
