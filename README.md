# Chat-Later

A two-way English ↔ Spanish voice translator for Android. Speak into the phone, it translates and speaks the result back out loud. Login with a free trial, then a paid subscription via Stripe.

## Why no ongoing API cost

Everything runs **on-device** — no per-use billing to a third-party translation API:
- **Speech-to-text**: Android's built-in `SpeechRecognizer` (free, on-device where supported)
- **Translation**: Google ML Kit Translate (free, downloads a small language model once, then works offline)
- **Text-to-speech**: Android's built-in `TextToSpeech` (free, on-device)

Firebase (Auth + Firestore) tracks logins and trial/subscription status — free at this app's scale.

## Tech stack

- Kotlin + Jetpack Compose
- Firebase Authentication (email/password login)
- Firebase Firestore (per-user trial start date + paid status)
- ML Kit Translate (on-device EN↔ES)
- Stripe Payment Link for subscription payment (manual link for now, no in-app billing SDK)

## Current status

Built so far:
- [x] Login / sign-up screen (Firebase Auth)
- [x] 7-day free trial tracking per user (`users/{uid}` in Firestore: `trialStartMillis`, `isPaid`)
- [x] Two-way conversation translator screen — "🎤 Speak English" / "🎤 Hablar Español" buttons, each doing mic → translate → speak-it-back
- [x] Trial-expired screen with a "Subscribe Now" button (opens Stripe Payment Link in browser)

Still to do:
- [ ] Swap the placeholder Stripe Payment Link (`STRIPE_PAYMENT_LINK` in `HomeScreen.kt`) for the real one
- [ ] A Firebase Cloud Function to receive Stripe's webhook and set `isPaid = true` on successful payment
- [ ] Firestore security rules (currently open/default — needs locking down so users can only read/write their own doc)
- [ ] GitHub Actions workflow to build a release APK automatically
- [ ] Test on a real device (currently tested via Android Studio emulator/USB debug run)

## Distribution

No Google Play Store — the built APK will be distributed as a direct download (e.g. a GitHub Release) that users sideload after paying.

## Project setup (for reference)

- Package name / Firebase Android app ID: `com.titanbusinesspros.chatlater`
- Firebase project: `chat-later-7f73a`
- Min SDK: 26 (Android 8.0)
- `google-services.json` lives in `app/` (already committed — needed for Firebase to work)
