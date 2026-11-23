# T-052: Setting for Up / Down on the remote in Live TV

Status: implemented in the branch (23 Nov 2026), not built, not device tested. Default: current behaviour.
Source: user wish. "All other players: Up goes to the next channel on live TV. Here you have to press Down. Please add a setting in the Live TV guide tab to choose: Up = next channel or previous channel."

## Goal

A setting that chooses what Up and Down do on the remote in Live TV: "Up = next channel" or "Up = previous channel". Default: the current behaviour.

## Current behaviour

- `PlayerScreen.kt:1120`: Up = `zap(-1)` (previous channel). `PlayerScreen.kt:1121`: Down = `zap(1)` (next channel). Only when `cur.live`.
- `PlayerGuide.kt:344-345`: Up and Down move the focus in the channel guide. Wrap-around logic in `GuideWrapLogic`.
- `PlayerLive.kt:253` and `:269`: Up and Down in the channel list.

## Plan

1. Add a boolean pref, for example `live_up_next`, in the existing live settings (`LiveSettings` in `LiveTimeshift.kt`, pattern of `save`/`load` with `ipvio_live` or the existing live prefs). **New key, no rename of existing keys** (section 5).
2. Show a toggle in the Live TV guide tab of the settings (section "Live TV guide", English label from the dictionary, no German text).
3. In `PlayerScreen.kt:1120-1121`, choose the direction from the pref: default `zap(-1)` for Up, `zap(1)` for Down (unchanged). With the setting on: swap.
4. Guide navigation (`PlayerGuide.kt:344-345`) is not changed. Only the channel zap keys.
5. Pure logic (`LiveKeyLogic.kt`, no Android imports): `direction(key: Key, upIsNext: Boolean): Int`. JUnit test `LiveKeyLogicTest.kt` in `app/src/test/java/com/ipvio/stream/`.

## Open decision (maintainer)

- Which value is the default? Proposal: **keep the current behaviour (Up = previous)**, so no existing user sees a change.
- Setting name and place: "Live TV guide" tab, as requested by the user.

## Acceptance criteria

- Default: Up = previous, Down = next, as before.
- With the setting on: Up = next, Down = previous. Also after an app restart.
- The setting does not change Up/Down in the guide list.
- No stored key is renamed.

## Risks

- `PlayerScreen.kt` is protected. Change limited to two lines plus pref read.
- Existing backup JSON keys (`Backup.kt`, list `NAMES`): a new pref is not in the backup unless added. Decide with the maintainer.

## Test steps (maintainer, device or Fire TV)

1. Default setting: in live playback press Up, note the channel number (goes to the previous). Press Down (goes to the next).
2. Turn the new setting on in the Live TV guide tab. Press Up: next channel. Press Down: previous channel.
3. Close the app, start it again: setting still on.
4. In the channel guide (the list on the right), Up and Down must still move the focus.
5. Turn the setting off again: back to the default.

## Documentation

- `CHANGELOG.md` and `docs/FEATURES.md` (new setting). User text in `docs/release-notes/` when released.

## Test results

- Not started. Plan only.
