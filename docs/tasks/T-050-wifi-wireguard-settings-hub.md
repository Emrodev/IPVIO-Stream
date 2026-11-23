# T-050 – WireGuard file from the phone, file-picker crash, settings hub tiles

Status: **implemented, not built with Gradle, not device tested.** Source: the maintainer's requests of 22 Nov 2026 (option a chosen for the phone import: 4-digit code as before).

## Goal
1. **T-050a** WireGuard `.conf` can be sent from the phone over the Wi-Fi (many people have the file on the phone, not on the Fire TV stick).
2. **T-050b** Settings hub: smaller tiles, more of them visible at once. Titles and descriptions are unchanged (they are translation keys, `CLAUDE.md` section 5).
3. **Crash fix** (part of the VPN request): pressing "WireGuard-Datei auswählen" closed the app on devices without a document picker. Cause is **unverified**; the fix catches the exception the same way `ListImportUi` already does.

## Affected files
- `AppVpn.kt`: `configVersion` (counts stored/removed configurations, so the page refreshes after a phone import).
- `VpnSettings.kt`: picker wrapped in try/catch with a note; "Import im WLAN starten" with code, QR code and stop button (same pattern as `ListImportUi.kt`); callback from the phone saves via `AppVpn.save`.
- `PhoneImport.kt`: new callback `onVpn`, new feature flag `Features.vpn`, new section on the phone page (file input, `sendFileJs`), new route `POST /vpn` (code check, size limit `VpnLogic.MAX_BYTES`, `VpnLogic.check` before the callback, so the phone sees an error at once).
- `SettingsHub.kt`: 4 columns instead of 3, tile height 96 → 84 dp, description up to 2 lines.
- `app/src/test/.../PhoneImportTest.kt`: `vpnSectionOnlyWhenTheTvOffersIt` (page JS and section only when the feature is set).
- `CHANGELOG.md`: three entries under Unreleased.

## Acceptance criteria
- On a device with no file picker, the picker button shows the note and the app stays open.
- "Import im WLAN starten" shows address, QR code and code; the phone opens the page, sends a valid `.conf`, and the settings page shows the server name without restarting.
- An invalid file (no `[Interface]` or `[Peer]`, too large) is refused on the phone with the "file could not be used" message; nothing is stored.
- Wrong code: 403; five wrong codes: lock (existing behaviour).
- Settings hub: all 21 tiles fit in fewer rows; no text is cut in the tile titles.

## Risks
- **The private key is sent over plain HTTP in the local network**, protected only by the 4-digit code (open item in `CLAUDE.md` section 10). Maintainer chose option a (accepted for now). Option b (confirmation on the TV before the file is stored, shorter code lifetime) is not implemented.
- The phone page strings are new: they use the German key plus the English text. Other languages fall back to English until the dictionaries get entries (open).
- The settings tile change is visual only; a device check of text fit on a small TV is needed.
- Nothing was built or run here (no Gradle, no device). Syntax was checked by reading only.

## Test steps (maintainer)
1. Build the debug app in Android Studio; run `PhoneImportTest` and `VpnLogicTest` (JVM).
2. On a Fire TV stick without a file picker (if available): Settings > General > VPN > "WireGuard-Datei auswählen". Expected: note appears, app stays open.
3. Settings > General > VPN > "Import im WLAN starten". Open the address and the code on the phone, choose "WireGuard configuration (VPN)", enter the code, upload a `.conf`. Expected: phone says "Sent"; on the TV the server name appears; "VPN verbinden" is shown.
4. Upload a text file that is not a WireGuard config. Expected: phone shows "The file could not be used."; TV unchanged.
5. Settings hub: count the visible tiles on a 1080p screen; check that every description fits in two lines.

## Documentation
`CHANGELOG.md` (done). `docs/SETTINGS.md` and `docs/PHONE.md`: **not updated yet** (open).

## Test results
- JVM tests: **not run** (no Gradle in this environment).
- Device tests: **not run**.

## Open
- Phone import: option b (TV confirmation before storing the key) if the maintainer wants it.
- Translations of the new phone and VPN strings.
- Crash cause on a real device (unverified).
