# T-012 EPG matching

- **Goal:** more channels get guide data; Stalker portals get a guide.
- **Cause:** `Xmltv` matched shows only by exact tvg-id; `<display-name>` was ignored. Stalker lists have no XMLTV address (`epgFor` adds one only for Xtream).
- **Change:**
  - `EpgNameLogic.kt` (pure, tested): name key without country tags, HD/FHD/4K/fps tags, punctuation and spaces.
  - `Epg.kt`: `Xmltv.parse` reads the guide's channel list; a guide channel also feeds the name key when a playlist channel with that name has no tvg-id or one this guide does not know. New `Xmltv.forItem(item)` (tvg-id first, then name); callers `Epg.now`, `EpgFull`, `GuideSearch`, `RemoteHub`, `SportTab`, `SportHub` switched to it.
  - Stalker: `StalkerApi.write` stores "hash of play address → channel id" in `stk_ch_KEY.map`; `Epg.now` asks the portal (`get_short_epg`) for Stalker live channels. Lists loaded with an older version get the table at the next refresh.
- **Risks:** name matching can attach the wrong guide when two different channels have the same normalized name (e.g. regional variants). Portal guide = one request per visible channel (cached 2 min, like Xtream).
- **Checks run:** `EpgNameLogicTest` (4), `StalkerEpgTest` (1), all pure tests green locally; CI build.
- **Device test:** M3U/Xtream with channels without tvg-id → guide line under the channel; Settings → EPG shows "Geladen: N Sender" (should rise); Stalker: "Neu laden" once, then Live TV shows the running show.
- **Status:** on branch `fix/round28-major`, device test open.
