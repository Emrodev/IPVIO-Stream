# T-028 Sound levelling: quiet IPTV channels and unclear speech

## Goal

User report: live channels are much quieter than YouTube or Netflix on the same TV, raising the TV volume does not help enough, and speech is hard to follow. Fix it in the app, on by default, with settings to adjust or switch it off.

## Findings

- Measured on a real channel (Xtream live stream, AAC stereo 48 kHz, 133 kbit/s) with `ffmpeg -af ebur128`: integrated loudness **−22.0 LUFS**, peak −5.1 dBFS. That is the broadcast norm (EBU R128, −23 LUFS). YouTube and Netflix play at about −14 to −16 LUFS, so live TV is 7–9 dB quieter.
- On the test device (Xiaomi TV Box S 2nd gen, Android TV 11) the media volume was already at maximum (15/15) and the remote's volume keys go to the TV over HDMI-CEC. The app's audio track ran at 0 dB. Nothing in the app or the device reduced the level; the source itself is quiet.
- The app had no gain, normalization or dialogue option (only audio language and the FFmpeg software decoder, `SoftAudio`).

## Change

- `AudioLevelLogic.kt` (new, pure Kotlin, no Android): presets, compressor curve, and `AudioLevelEngine`, which processes interleaved float PCM:
  1. speech lift: +3 to +9 dB presence peak at 2.5 kHz on the centre (mid of stereo, centre channel of 5.1/7.1); side and bass stay as they are,
  2. channel-linked compressor with a soft knee (light / medium / strong),
  3. automatic gain towards −16 LUFS, measured with a K-weighted meter (BS.1770 style); rises at most 2 dB/s, never above the chosen maximum, never makes audio quieter, holds during silence (gate at −50 LUFS),
  4. peak limiter at −1 dBFS (no clipping).
  Mode Off leaves the samples bit-identical.
- `AudioLevel.kt` (new): per-profile settings in `ipvio_audio` (`mode`, `boost`, `level`, `speech`, all indexes) like the audio delay in `PlayerSync`, and `AudioLevelProcessor`, a Media3 `BaseAudioProcessor` for 16-bit and float PCM. Settings apply immediately, also during playback. On seek the automatic gain keeps its level.
- `SoftAudio.kt`: `factory()` returns a `DefaultRenderersFactory` subclass whose `buildAudioSink` is Media3 1.11.1's default plus the processor. Both the platform decoder and the FFmpeg renderer use this sink. `PlayerScreen.kt` and `DualView.kt` are unchanged.
- Settings > Sound & subtitles: new row "Ton angleichen" with Aus / Standard / Klare Sprache / Nachtmodus / Eigene Einstellung. "Eigene Einstellung" shows Maximale Anhebung (Aus, +3 … +12 dB), Dynamik angleichen (Aus/Leicht/Mittel/Stark), Dialoge hervorheben (Aus, +3/+6/+9 dB) and Zurücksetzen.
- `DictAudio.kt` (new): the texts in all 10 languages, loaded additively like `DictW.kt`, so `Lang.kt` is not touched.
- `MainActivity.kt`: `AudioLevel.init(ctx)` next to `PlayerSync.init(ctx)`.

Default is **Off** (changed by the maintainer from the original Standard): the user switches it on, so existing sound does not change until then. Presets: Standard = up to +9 dB, medium levelling, speech +3 dB; Klare Sprache = speech +6 dB; Nachtmodus = up to +12 dB, strong levelling, speech +4 dB.

## Not changed

Encoded passthrough (AC3/E-AC3/DTS bitstream to a receiver) never reaches Media3 audio processors, so it is untouched. `PlayerScreen.kt`, playlist sources, EPG, existing SharedPreferences keys, backup `NAMES`. No new dependency.

## Validation

- `AudioLevelLogicTest` (12 tests, JVM): presets and clamping, compressor curve, Off is bit-exact, a −26 dBFS tone is raised by ≥ 6 dB and stays under −12 dBFS, full-scale input never exceeds −1 dBFS, speech lift +6 dB on mid / 0 dB on side / 0 dB on 100 Hz, silence is not raised, 5.1 lifts only the centre, flush keeps the gain, reset clears it, translations complete. **Ran: 12 passed.**
- Full unit test suite: 376 tests, 2 failures (`LoadLogicTest.vorauswahlNachGeraetesprache`, `SubSearchLogicTest.subdlHitsDropKeyAndOtherEpisodes`). Both fail the same way on unmodified `main` (abc5a21), so they are existing and not caused by this change (possibly locale-dependent, unverified).
- Real stream audio (30 s capture of the channel above, processed by the engine in a JVM harness, measured with ffmpeg ebur128, settled part 10–30 s): input −22.4 LUFS → Standard −16.0 LUFS (peak −1.2 dBFS), Klare Sprache −16.0 LUFS, Nachtmodus −17.6 LUFS with loudness range 2.7 → 1.8 LU.
- Device: debug/release build installed on a Xiaomi TV Box S 2nd gen (Android TV 11); see the PR for the result.

## Test steps for the maintainer

1. Play a live channel, then YouTube on the same TV: live TV should now be about as loud. After a channel change the level settles within a few seconds.
2. Settings > Sound & subtitles > Ton angleichen: switch between modes during playback (picture-in-picture or come back to the player); the change is audible at once. "Aus" sounds exactly like 1.4.2.
3. Eigene Einstellung: each control changes the sound; Zurücksetzen returns to Standard.
4. With a receiver and passthrough (Dolby/DTS) the sound is unchanged.
5. Profiles: a second profile keeps its own setting.

## Risks

- A small CPU cost per audio buffer (biquads and gain per sample in Kotlin). Not measured on slow devices.
- Users who liked the old (quieter, unprocessed) sound need to choose "Aus". The default could be discussed; it is on because the problem affects most live channels.
- `ipvio_audio` is not in the backup `NAMES` yet (protected list); could be added in a follow-up.

## Status

Done, waiting for review.
