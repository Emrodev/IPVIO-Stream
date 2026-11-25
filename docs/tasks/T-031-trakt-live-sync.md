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
