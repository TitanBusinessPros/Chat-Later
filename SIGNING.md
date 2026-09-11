# App signing key

Chat-Later's release build is signed with a private key at `keystore/chatlater-release.jks`,
with its passwords in `keystore/keystore.properties`. **Both files are gitignored on purpose —
they contain secrets and must never be committed.**

## Why this matters

Every release build must be signed with the *same* key, forever. If this keystore is lost:

- Users can never install a future "update" over their current app — Android treats a
  differently-signed APK as a different app entirely, so it would refuse to overwrite the old one.
- If Chat-Later is ever published to the Google Play Store, this is the same key Play requires
  for every future update — losing it there is unrecoverable without Play's account-recovery process.

## Back it up now

Copy the entire `keystore/` folder somewhere outside this project and outside Git — a password
manager's file storage, an encrypted USB drive, or a secure cloud folder. If this computer is lost
or wiped and there's no backup, this app can never be updated again under its current identity.

## Regenerating (only if you accept losing update continuity)

This is a last resort — everyone with an older version of the app cannot upgrade to a build signed
with a new key. If ever necessary, generate a new keystore with `keytool -genkeypair` and update
`keystore/keystore.properties` to match.
