# T-078: PIN-protected profiles (plan, not started)

Status: **implemented in a small form (27 Nov 2026), on the proposed defaults** (the maintainer left the decisions open: "do what is best").

What was built: the existing parental PIN (`Parental.kt`: salted hash, attempt limit, locked profile = adult content hidden + PIN) is now also set when a profile is added (`SettingsHub.kt`, `Parental.setPinFor`). The PIN check for two entries is `ParentalLogic.confirmed` (tests). Texts in `DictT078.kt`.

Not changed: the existing lock rules, backup (the PIN is not exported, as before), picker (names stay visible).

## Goal

When a profile is created or edited, the user can set a 4-digit PIN. To enter a protected profile, the PIN must be typed in.

## What exists today (read from the code, 27 Nov 2026)

- `Profiles.kt`: profiles with id and name, stored in the preferences `META`. Data of profile N uses the suffix `_p<N>` (CLAUDE.md section 5: profile 0 keeps the original names).
- `Profiles.add`, `rename`, `remove`, `select`, `switchTo`, `ProfilePicker` (shown at start when several profiles exist).
- `Secrets.kt`: encrypts strings with an Android keystore key; falls back to plaintext on devices without a keystore (open item in CLAUDE.md section 10).

## Proposed design (to approve or change)

1. **Storage:** store only a salted hash of the PIN (PBKDF2-HMAC-SHA256, 100k iterations or the platform equivalent), never the PIN. The hash lives in a separate preference file, not in the backup JSON.
   - *Option B:* store the hash encrypted with `Secrets`. Weaker when the keystore is missing (plaintext fallback).
2. **Lock point:** the PIN is asked when the profile is opened from the profile picker (and at start if it is the active protected profile). Switching to another profile asks for the PIN of the target profile.
3. **Wrong PIN:** after 5 wrong tries the input is blocked for 30 seconds (counter kept in memory and in the preference file, so an app restart does not reset it).
4. **Forgotten PIN:** no PIN recovery by the app. The way out is to remove the profile (deleting its data) or to change the PIN from the same profile after entering the old one. *Decision needed:* is removing the profile without PIN acceptable?
5. **Scope:** the PIN protects opening the profile only. It does not hide the profile in the picker (the name stays visible). *Decision needed.*
6. **Changing or removing the PIN:** possible inside the profile settings after entering the current PIN.
7. **Backup and restore:** the PIN hash is not exported. After a restore the profile PIN has to be set again. *Decision needed.*

## Affected files (planned)

- `Profiles.kt`: PIN check and lock state; new functions `setPin`, `clearPin`, `isLocked`, `checkPin`.
- `ProfilePicker` (in `Profiles.kt`): PIN dialog before opening a locked profile.
- Profile editing screen (where `rename` is called): set, change, remove PIN.
- New logic file `ProfilePinLogic.kt` (no Android imports): PIN format check (exactly 4 digits), attempt counter and lock time. JVM tests in `ProfilePinLogicTest.kt`.
- Dictionary rows for all new texts (all languages, see CLAUDE.md section 4 and the "translate everything" rule).
- `docs/BEHAVIOUR-REGISTER.md`: new entry with the device test steps.

## Risks

- Authentication code: a mistake can lock a user out of their own profile. The reset path (remove profile) must be obvious.
- Hashing time on a weak TV box: 100k iterations may take a noticeable time. To measure on the device before fixing the count.
- This is a privacy lock for the household, not a security boundary: anyone with access to the app data can read the preference file. The text in the app must not promise more.

## Decisions needed from the maintainer

1. Hash with PBKDF2 in the preference file (option 1, recommended) or encrypted with `Secrets` (option 2)?
2. Lock on opening only (recommended), or also hide the locked profile in the picker?
3. Removing a locked profile without its PIN: allowed (recommended, with a confirmation) or not?
4. PIN after a backup restore: set again (recommended) or kept?
5. Attempt limit: 5 tries then 30 seconds (proposal), or other values?

## Test steps (after implementation)

1. Add a profile, set PIN 1234. Back to the picker, open that profile: the PIN is asked. A wrong PIN is refused; 5 wrong tries block for 30 seconds.
2. Open the profile with the right PIN: it opens and stays unlocked until the app restarts or you switch away.
3. Change the PIN in the profile settings; the old PIN no longer works.
4. Remove the PIN: the profile opens without asking.
5. Restart the app: the protected profile still asks for the PIN.
