# T-057 Score alerts: longer delay options

- **Issue:** #130 "Turn off score notification"
- **Status:** implemented, not built, not device tested

## Goal

The reporter watches a stream that runs behind the live score, so the alert spoils the goal. They want to turn alerts off, for specific teams or games.

## Finding

Most of it exists already (Settings > Sports Hub, section Alerts):

- The switch "Alerts for games of your teams" turns all alerts off (`SportAlerts.enabled`).
- "Delay" holds back an alert: Instant, 30 s (default), 60 s.

Not available: turning alerts off per team or per game. That would be a new feature and a new stored setting (`ipvio_sportalerts`, protected), so it is not part of this task.

## Change

`SportAlerts.DELAYS` is extended from `0, 30, 60` to `0, 30, 60, 90, 120`. A stored value stays valid (the check is "value is in the list"), no key or format changes. A stream that is more than a minute behind can now be matched.

## Risks

Low. Additive options only. Same preference key `delay`. Default stays 30 s.

## Tests

- Unit test: not applicable. `SportAlerts` holds Compose state and Android preferences; the change is a constant.
- Build: not run. Device: not tested.

### Manual test steps (maintainer)

1. Settings > Sports Hub > Delay: the list shows Instant, 30 s, 60 s, 90 s, 120 s.
2. Pick 120 s, restart the app: 120 s is still selected.
3. Update from an older version with 60 s selected: 60 s is still selected.
4. With a favourite team playing, the alert appears about 120 s after the score.

## Reply for #130 (draft, not posted)

Alerts can be switched off in Settings > Sports Hub ("Alerts for games of your teams"). "Delay" holds an alert back; the next version offers up to 120 s. Per-team switches are not available yet.
