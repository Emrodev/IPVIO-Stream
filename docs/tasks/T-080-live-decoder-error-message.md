# T-080: Live TV stops after about 10 to 20 seconds with "This device cannot play this video format."

Status: analysed (27 Nov 2026), no code changed. Waiting for the Diagnostics log line from the device and the maintainer's approval (player core, CLAUDE.md sections 5 and 6).

## Goal

A Live TV channel plays for 10 to 20 seconds, then shows "This device cannot play this video format." The user's device (Fire TV Stick 4K Max) can decode the stream (H.264, 1920 × 1080, 20 s buffer). The message is wrong or too vague, and the channel is given up after one error.

## Reported by

User, Fire TV Stick 4K Max, Live TV, 4K channel variant.

## Cause (from code reading, not confirmed)

- `PlayerScreen.kt:743-745`: four decoder errors are treated as "decoder" errors: `DECODER_INIT_FAILED`, `DECODING_FAILED`, `DECODING_FORMAT_UNSUPPORTED`, `DECODING_FORMAT_EXCEEDS_CAPABILITIES`.
- `PlayerLogic.kt:181`: all four map to the same text, "This device cannot play this video format." (German key in the dictionary, English value in `DictB`).
- `PlayerScreen.kt:750`: a decoder error skips the reconnect loop (`!decoder`). For Live TV, `switchSource` returns false (`cur.live`), so the stream is given up after the first error.
- The 10 to 20 seconds fit a decoder failure in the middle of the stream (format change, decoder reset). Not confirmed.
- The live variant comes from `LiveBundle` (4K, Full HD, HD, SD as separate entries). Which variant was playing is not in the report.

## Affected files (planned, after approval)

- `PlayerScreen.kt` (`onPlayerError`: reconnect for live decoder errors, if the log shows a transient error)
- `PlayerLogic.kt` (`playbackErrorText`: separate text per error type, or a text that does not claim the device cannot play the format)
- `DictB.kt` and the other `Dict*.kt` files (only with a maintainer decision, section 5 UI strings)
- `app/src/test/java/com/ipvio/stream/` (JVM test for the text mapping, `Media3ContractTest` for the error codes if new ones are used)
- `docs/BEHAVIOUR-REGISTER.md`, `CHANGELOG.md`

## Information needed first

1. The line "Playback: ERROR_CODE_…" from Settings > Diagnostics > Log at the time of the error (the app writes it, `PlayerScreen.kt` `Diag.report`).
2. The channel name and the variant shown (4K, Full HD, …).
3. Whether the same channel plays in Full HD (quality switch in the player).

## Acceptance criteria

1. The error text matches the real cause, or a neutral text when the cause is unknown.
2. A transient decoder error on Live TV leads to a reconnect, as network errors do.
3. A real format limit still shows a clear message and does not loop.

## Risks

- Player core (protected, CLAUDE.md section 5). Changes need approval and a device test.
- A reconnect loop could hide a permanent failure: limit the number of attempts, as for network errors (`retries < 50` for Live TV).

## Test steps for the maintainer (after the change)

1. Play a 4K channel for at least 3 minutes, note the time of the first error.
2. Play the same channel in Full HD: expect no error after 3 minutes.
3. Check Diagnostics > Log for the "Playback: ERROR_CODE_…" line.

## Documentation needed

- CHANGELOG entry, behaviour register entry (Live TV error handling), this file.

## Test results

- Not run. No code changed yet.
