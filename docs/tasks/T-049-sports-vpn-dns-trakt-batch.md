# T-049 – Batch of wishes: sports hub, VPN and DNS, Trakt

Status: **PLAN, waiting for approval** (CLAUDE.md sections 6 and 7). No code. Source: the maintainer's list of 22 Nov 2026 (user requests, one marked as from a Reddit user). This is several features, not small fixes, so it is split into subtasks that can be built and tested one by one.

## What exists today (read in the code, nothing run)
- Sports hub: `SportHub.kt`, `SportLeague.kt`, `SportData.kt` (ESPN public but unofficial endpoints `site.api.espn.com`, league list with paths), `SportAlerts.kt` (popup when a favourite team is about to play, kicks off or scores), `SportWatch.kt` (finds channels for a game: manual assignment, TV guide, channel name), `SportLogic.kt` (13 sport categories from keywords: `CATS`).
- Trakt: plans only (`T-030` import postponed, `T-031` live sync, status plan, no code). Needs a registered Trakt API app (client ID and secret) from the maintainer.
- VPN or DNS: nothing in the code (grep found no WireGuard or resolver code).

## Subtasks

### T-049a Sports: goal alert opens the game
- The goal popup in `SportAlerts` gets an action: OK jumps to a channel that shows the game (same matching as `SportWatch`), if there is one; otherwise it opens the match page. Only for tracked leagues or teams.
- Risk: low (UI, no storage). Needs a device test with a live game.

### T-049b Sports: find channels for a game
- A button on the match page that lists the channels `SportWatch` finds (hand-picked, guide, name) and a filter for the channel list (provider category, quality). Mostly present; the gap is a visible entry point and the filter. Needs the maintainer's view of what "filters" should be (see questions).

### T-049c Sports: many more leagues and sports
- ESPN has many more paths than the app lists (UK football tiers, Scottish, rugby, cricket, darts-like events are partly missing). Adding paths to the league list is low risk. Rows must be verified one by one against the live endpoint; the session cannot reach ESPN reliably, so each new path is **unverified** until the maintainer's device shows data.
- "Sky Sports leagues": Sky is a broadcaster, not a data source. What can be done: add the leagues Sky shows (Premier League, EFL, Scottish Premiership, F1, golf, cricket, rugby, boxing, darts) and match Sky Sports channels by name for `SportWatch`. No Sky API exists for this.
- "As much as you can find online through APIs": a second data source (for example TheSportsDB, free key with limits) is a new network dependency with its own terms and rate limits. Needs approval; recommended only after ESPN coverage is exhausted.

### T-049d Sports hub: filters, favourite sports, customise
- Filter options in the hub (by league, by day, favourites only, live only).
- Favourite sports and a customisable hub (order and hide sections, like the home page builder `HomeLayout`). New SharedPreferences keys (`ipvio_sport…`), additive.
- "Remove sport types completely" is unclear (see questions): remove the sport categories (`CATS`) from the hub and the guide filter, or only from one screen?

### T-049e VPN with WireGuard
- Needs `VpnService` (new manifest service and permission), the WireGuard tunnel library (`com.wireguard.android:tunnel`, Apache-2.0, compatible with GPL-3.0; **new dependency, needs approval**), a place to import a config (file or phone upload; the private key is a secret: stored only in encrypted preferences like `Secrets`, never logged, CLAUDE.md section 10), and a switch plus status in Settings.
- Fire TV and Android TV: the system VPN consent dialog must work with a remote; some Fire OS versions limit `VpnService`. **Unverified**, needs the device.
- Large change in network and security. Recommended to be its own pull request and release.

### T-049f Own DNS
- A DNS server of the user's choice for the app's own connections: an OkHttp `Dns` implementation with a configurable resolver (plain DNS over UDP/TCP or DNS over HTTPS). Applies to playlist, EPG, metadata, and playback requests that go through OkHttp; ExoPlayer's data source must use the same client (to be checked in `PlayerNet.kt` and `Net.kt`). Independent of the VPN, smaller, and useful alone (blocked provider hosts, privacy).
- With an active VPN the VPN's own DNS setting applies to the whole device; the two settings must not fight (decide: app DNS wins for app traffic).
- New dependency only if DoH is built with `okhttp-dnsoverhttps` (same OkHttp group, **needs approval**); plain DNS needs none.

### T-049g Trakt: mark watched VOD
- Depends on T-031 (login and sync). First step: OAuth device login and "mark watched" for movies and episodes from the app to Trakt (scrobble or history add) and the reverse on the detail page. Needs the Trakt API client ID and secret from the maintainer (secrets handling, section 10) and the official API documentation, which the earlier session could not reach (**unverified**).

## Suggested order
1. T-049a, T-049b (small, no new storage or dependency).
2. T-049c with ESPN paths only, then T-049d.
3. T-049f (DNS, plain first), then T-049e (VPN), each its own release.
4. T-049g after the maintainer registers a Trakt app.

## Decisions needed
See the questions asked in the chat (22 Nov 2026).

## Risks
- Nothing here can be built or run by the assistant; all new data sources and the VPN need device tests.
- New third-party endpoints and a VPN library change the privacy and support picture of the app; both need the maintainer's decision and a mention in `docs/SECURITY.md`.
