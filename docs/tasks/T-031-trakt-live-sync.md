# T-031: live sync with Trakt through its API — plan only

**Status:** step 1 in progress. The maintainer registered the Trakt app and gave the client ID (not secret, not stored in the repository). Trakt issues **no client secret** for this app and requires **PKCE** ("Not issued. This app signs users in with PKCE"). The earlier device-code default is therefore **unverified** and must be re-checked against https://developer.trakt.tv/docs/pkce; the PKCE login on a TV needs a redirect target, which is still open. Step 1 done in code: `BuildConfig.TRAKT_CLIENT_ID` (from `secrets.properties`) and `TraktLogic.kt` with `TraktLogicTest.kt` (not run: no Gradle here). `TraktLogic` now holds the PKCE building blocks (verifier, S256 challenge, authorize URL, token body, https-only redirect check) per the Trakt PKCE guide. The token host `api.trakt.tv` is **unverified**. **Open decision:** PKCE requires an `https://` redirect URI that the app owns (Universal Links / verified App Links for mobile). The registered redirect `urn:ietf:wg:oauth:2.0:oob` is no longer allowed. A TV has no App Link target, so the sign-in needs a domain the maintainer controls (e.g. a page that shows the code for the TV to take over). Login screen, token storage and sync are not started. Read-only first (Trakt → app) remains the plan. The one-time import (T-030) stays separate and does not depend on this.

**Verification status:** the facts below come from community sources (apis.io overview, trakt-rs and trakt.py examples). The official Trakt documentation (trakt.docs.apiary.io) could not be reached from the session, so every endpoint, header, token lifetime and rate limit is **unverified** until it is checked against that documentation.

## Goal
Keep the watch history, watchlist and favorites of a user in step with their Trakt account, in both directions, without a manual export.

## Model (to verify)
- **One API application for the project.** The maintainer registers the app at Trakt once and gets a client ID and client secret. Users do not create their own keys.
- **Per-user login with OAuth.** Device code flow, suited to TV: the app shows a code, the user enters it on trakt.tv/activate on a phone. The app then holds an access token and a refresh token for that account.
- **Sync:** history, watchlist, collection, favorites, ratings and playback progress. `last_activities` tells the app what changed since the last sync, so it does not download everything each time.

## Decisions the maintainer must make
1. **Client secret in an open-source app.** It is visible to anyone who reads the APK or the source. Need to check whether Trakt allows the device flow without it, or accept the exposure for a non-commercial app. Recommendation: check first; do not commit the secret to the repository (CLAUDE.md section 10). Store it in GitHub secrets for the build if needed.
2. **Token storage.** Access and refresh tokens per profile, encrypted with `Secrets`. Known weakness: `Secrets` falls back to plain text on devices without a keystore (CLAUDE.md section 10, open item). Recommendation: refuse to store the tokens when no keystore is available, instead of falling back.
3. **Direction of sync.** Both ways, or Trakt → app only at first (lower risk, no writes to a user's account). Recommendation: start with read-only (Trakt → app), then add writes after a test.
4. **Conflicts.** Local watch progress and Trakt history can disagree. Recommendation: a timestamp rule (the newer change wins) and never lowering progress.
5. **When to sync.** At start and when the app is in front, with a rate limit. No background service for the first version.
6. **Settings and privacy.** A visible switch "Trakt-Konto verbinden" and "Trennen" (delete the tokens). The tokens never go into logs, backups or error reports.

## Affected code (to read before implementation)
- Watch progress and "watched" state (`EpProgress`, `WatchStore`, `Playback`).
- Backup keys (`Backup.kt`, list `NAMES`): tokens must be excluded or handled by a decision.
- Network: a new OkHttp client with the API headers; the existing `TlsFallback` must not apply to Trakt (it is not a provider host).
- Settings page and the dictionary (new German keys need translation rows).

## Risks
- Account security: a leaked secret or token allows writes to the user's Trakt account.
- Trakt terms of use and rate limits: to check in the official documentation.
- Data loss: a wrong merge could mark titles unwatched. Mitigation: read-only first, dry run, never lower progress.
- Offline use: the app must work when Trakt is unreachable.

## Steps (after approval)
1. Verify the official Trakt documentation (endpoints, headers, token lifetime, rate limits, terms) — needs trakt.docs.apiary.io to be allowed in the session network, or the maintainer checks it.
2. Register the app at Trakt (maintainer).
3. Device-flow login and token storage, with JVM tests for the token state logic.
4. Read-only sync of history with `last_activities`, JVM tests for the merge rule.
5. Settings screen and texts.
6. Device test (login on Fire TV via phone code, sync, disconnect).
7. Write-back only after a separate decision.

## Login plan: PKCE without an own domain (planned, no code yet)

**Decision (maintainer, 2026):** no own domain. Plan "phone sign-in" (Weg 2), corrected: Android App Links also need a domain the app owns (`assetlinks.json` on it), so the phone is not opened by the callback directly.

**Flow**
1. TV creates the PKCE verifier (`TraktLogic.newVerifier`), keeps it in memory for this sign-in only, and shows a QR code of the authorize URL (`TraktLogic.authorizeUrl`, `state` per sign-in).
2. Phone scans the QR code and signs in at Trakt in the phone browser (`https://auth.trakt.tv/...`).
3. Trakt redirects to a static `https://` page on GitHub Pages (free, no own domain; URL to be chosen, e.g. the maintainer's `github.io` account). The page only shows the one-time `code` and says "Go back to the TV". It stores nothing and sends nothing to third parties.
4. The phone page sends the code to the TV over the existing local phone-remote page (same Wi-Fi, existing 4-digit code check). This needs a new route; the phone remote's routes are a protected area (CLAUDE.md section 5), so it needs approval.
5. The TV posts `code` + verifier to the token endpoint (`TraktLogic.tokenBody`, no client secret), stores the tokens via `Secrets` only, and refuses to store them when no keystore is available.

**Fallback without the phone route:** the user types the code with the TV remote. Trakt codes are long, so this is slow; use only as a backup.

**Needs approval before implementation (CLAUDE.md section 6)**
- A new GitHub Pages site (public, under the maintainer's account; new deployment).
- A new phone-remote route (protected area, phone-to-TV transfer).
- Token storage through `Secrets` (authentication and storage).

**Open checks (unverified until read from the Trakt docs):** the redirect URI accepted for a GitHub Pages URL, the token host `api.trakt.tv`, and the token response fields and lifetime.

**Risks:** the code is short-lived and one-time, but anyone who sees the phone screen can take it. The phone route is plain HTTP on the local network (open item in SECURITY.md), so the code crosses the LAN unencrypted. Keep the code's lifetime short and discard it after use.

**Tests:** JVM tests for the verifier, challenge, URL and body builders (done in `TraktLogicTest`). On device: full sign-in with a real Trakt account, check that the TV stores tokens only with a keystore, and that the code cannot be reused.

## Phone route: detailed plan (planned, no code, needs approval)

**Simpler than before:** the TV already shows a phone page (`PhoneImport.kt`, QR code, 4-digit code, lock after 5 wrong codes, runs only while its screen is open). Use that page for Trakt, so the TV needs only one QR code.

**Steps**
1. TV, on the Trakt screen: create the verifier (`TraktLogic.newVerifier`) and a `state`, keep both in memory for this sign-in only, and show the phone page.
2. Phone page: a "Sign in to Trakt" button opens `TraktLogic.authorizeUrl(...)` in the phone browser (the URL includes the challenge, never the verifier).
3. Phone browser: user signs in at Trakt; Trakt redirects to `TraktLogic.REDIRECT_URI` (GitHub Pages helper page), which shows the code.
4. User copies the code into a field on the phone page and sends it with the 4-digit code. New route, e.g. `POST /trakt` (name to be chosen; the route list is a protected area).
5. TV checks `state`, exchanges code + verifier at the token endpoint (`TraktLogic.tokenBody`), stores the tokens only through `Secrets`, and clears the verifier. Refuse to store tokens when no keystore is available.

**Why the plain-HTTP LAN is acceptable here (to confirm in review):** the code alone is useless. The exchange needs the verifier, which never leaves the TV. An attacker who sees the code on the LAN cannot turn it into tokens. The 4-digit code still guards the route against other devices on the Wi-Fi.

**Feature flag:** add the Trakt part behind a setting (off by default) so existing users see no change (CLAUDE.md section 6: visible effect).

**Changes needed (each needs approval):** new phone route and page text (protected area, translations per `docs/TESTING.md`), new Trakt screen on the TV, token storage in `Secrets`, new setting.

**Tests:** JVM for `state` check, code length and charset validation, token body; device test for the whole flow with a real Trakt account, wrong code (lock after 5), keystore missing (no token stored), sign-out removes tokens.

## Status (step 3 built, not device tested)
- `TraktSignIn.kt`: holds the one pending sign-in in memory (verifier, state); starting again replaces it; finishing always consumes it.
- `PhoneImport.kt`: `GET /trakt` (asks for the 4-digit code, then links to Trakt) and `POST /trakt` (checks the code, parses `code|state`, runs the exchange). Plain HTTP on the LAN as before; the code alone cannot be exchanged without the verifier.
- `RemoteHub.kt`: wires the two callbacks.
- Helper page on GitHub Pages now shows `code|state` as one copyable text.
- Open: no entry point on the TV yet (the phone page is reached by typing `http://<TV-IP>:<port>/trakt`); English-only texts on the phone page; `api.trakt.tv` and the response fields unverified; the Secrets side effect (`plainFallback`) when the keystore is missing; not compiled here.

## Test results (maintainer, Android Studio)
- `assembleDebug`: BUILD SUCCESSFUL.
- `TraktLogicTest`: 13 of 13 passed.
- Not yet done: phone route on a device (`/trakt`), real Trakt sign-in with an account, token storage check.

## Steps 4 and 5: read-only sync (25 Nov 2026, branch `claude/modest-johnson-415uuv`)

Decided by the maintainer on 25 Nov 2026 ("4 and 5 must work"): history and watchlist from Trakt into the app, read only. Writing back to Trakt (plan step 7) is not part of it.

- `TraktSyncLogic.kt` (pure, JVM tests `TraktSyncLogicTest`): API paths, headers, JSON refresh body, parsing of `/sync/watched/{movies,shows}` and `/sync/watchlist/{movies,shows}`, pagination stop, and the plan: titles matched with the playlist by name and year (`ListImportLogic.match`); watched marks only for episodes **without any progress** in the app (an episode in the middle stays as it is); new titles only while the progress store (`EpProgress`, 400 titles) has room, most recently watched first, so no title of the user is pushed out.
- `TraktSync.kt`: reads the four lists (paged, `limit=1000`, at most 20 pages each), renews the access token when it is expired or refused once (JSON body, `grant_type=refresh_token`, no client secret), stores "Trakt · Gesehen" and "Trakt · Merkliste" through `ImportedLists`, adds the marks through `EpProgress.addWatched`. Runs after a successful sign-in, at app start at most every 6 hours (8 s after the playlist is there, `MainActivity`) and with "Sync now". "Trakt trennen" revokes the token at Trakt (best effort), deletes it and the two categories; check marks stay. Diagnostics line with numbers only.
- `TraktAuth.exchange`: JSON body instead of a form, User-Agent header.
- `ListImportUi.kt`: when connected, a panel with status, last sync time, "Jetzt synchronisieren" and "Trakt trennen" instead of the connect button.
- `DictT031.kt`: German keys with English rows (other languages show English).
- `EpProgress.count`, `capacity`, `addWatched` (`Store.kt`).

**API source:** the official documentation hosts (developer.trakt.tv, trakt.docs.apiary.io) could not be resolved from the session. Paths, headers, the JSON token body and the response fields were taken from two open-source clients: `trakt.tv` 8.3.0 (npm: `methods.json`, `trakt.js`) and `trakt.py` 4.4.0 (PyPI: `interfaces/sync`, `mapper/sync.py`, `core/request.py`). Both agree. Still to confirm on a device with a real account.

**Release blocker:** `release.yml` must pass the repository secret `TRAKT_CLIENT_ID` to the release build (workflow change, needs the maintainer's approval).

**Test results:** `TraktSyncLogicTest` 11/11 and `TraktLogicTest` 13/13 on the JVM in the session (Kotlin 2.0.21, JUnit 4.13.2, org.json 20250517). Not run: Gradle build of the Android files (CI), device test with a Trakt account.
