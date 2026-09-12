# Chat-Later

A two-way voice translator for Android. Pick any two of **59 languages** (the full on-device ML Kit Translate catalog), speak into the phone, and it translates and speaks the result back out loud. Log in with email/password or Google, get a free 7-day trial, then a paid subscription via Stripe.

## Why no ongoing API cost

Everything runs **on-device** — no per-use billing to a third-party translation API:
- **Speech-to-text**: Android's built-in `SpeechRecognizer`
- **Translation**: Google ML Kit Translate (downloads a small language model once per pair, then works offline)
- **Text-to-speech**: Android's built-in `TextToSpeech`

Firebase (Auth + Firestore) tracks logins and trial/subscription status — free at this app's scale.

## Tech stack

- Kotlin + Jetpack Compose
- Firebase Authentication — email/password and Google Sign-In (via Credential Manager)
- Firebase Firestore — per-user trial start date + paid status (`users/{uid}`), locked down with security rules (see `firestore.rules`)
- ML Kit Translate — all 59 on-device supported languages, picked via a searchable dialog
- Stripe Payment Link for subscription payment (manual link for now, no in-app billing SDK)

## Current status — v1.13 (versionCode 14)

Published and live at https://github.com/TitanBusinessPros/Chat-Later/releases/latest

Built so far:
- [x] Login / sign-up screen — email/password and Google Sign-In
- [x] 7-day free trial tracking per user, enforced via Firestore + security rules
- [x] Two-way conversation translator — searchable picker for any 2 of 59 languages, mic → on-device translate → speak-it-back
- [x] Trial-expired screen with "Subscribe Now" (opens Stripe Payment Link)
- [x] App branding: custom launcher icon leads Login/Translator/Trial-Expired screens; Titan Business Pros logo + contact/legal footer on all three
- [x] `terms.html` / `privacy.html` published on this repo's GitHub Pages site, linked from the in-app footer
- [x] Signed release build (own keystore, not debug-signed) — see `SIGNING.md`
- [x] In-app "check for updates" (reads `version.json` on the Pages site) on both the sign-in and translator screens
- [x] On-device crash reporter — shows the last crash's details on next launch if one occurred
- [x] Fixed a real crash (`IllegalStateException: Translator has been closed`) that could occur if the screen changed mid-translation — each request now owns its own Translator, closed exactly once
- [x] Fixed a crash on launch on real devices (`IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported`) — the header app icon was loading `R.mipmap.ic_launcher`, which resolves to an `<adaptive-icon>` XML that Compose's `painterResource()` cannot render; it now loads the plain PNG `R.mipmap.ic_launcher_foreground` instead
- [x] Fixed the translator not speaking the translation out loud — `speak()` ignored whether the TTS engine had finished initializing and never checked whether the target language's voice was installed, so it failed silently; it now checks both and shows an on-screen message if no voice is available for that language on the device

Still to do:
- [ ] Swap the placeholder Stripe Payment Link (`STRIPE_PAYMENT_LINK` in `HomeScreen.kt`) for the real one
- [ ] A Firebase Cloud Function to receive Stripe's webhook and set `isPaid = true` on successful payment
- [ ] GitHub Actions workflow to build/release automatically (currently done manually via local `gradlew` + `gh release`)
- [ ] Real device or emulator visual/functional testing — not yet done. No phone has been connected in this dev environment, and the one configured emulator (`Pixel_10_Pro_XL`) can't boot because Windows Hypervisor Platform / hardware acceleration was never successfully enabled on this machine. Everything shipped so far is verified by source audit, compilation, `lintRelease`, unit tests, and `apksigner` — not by actually running the app.

## Distribution

No Google Play Store — the built APK is distributed as a GitHub Release that users sideload. The download page (`index.html`, served via GitHub Pages) and the in-app updater both point at `releases/latest/download/Chat-Later.apk`, so that link always resolves to the newest release without needing to be edited each time.

## Project setup (for reference)

- Package name / Firebase Android app ID: `com.titanbusinesspros.chatlater`
- Firebase project: `chat-later-7f73a`
- Min SDK: 26 (Android 8.0), target/compile SDK: 37
- `google-services.json` lives in `app/` (committed — needed for Firebase to work)
- **Release signing key**: `keystore/` (gitignored, never committed) — see `SIGNING.md` for why it must be backed up separately and what happens if it's lost. A full backup exists at `C:\Users\adona\Downloads\Interpertor\V-1\`.
