# Relive — Release

Release requirements tracker. **None of these are configured yet.** This document records what will be needed so it is not forgotten; items are checked off only when actually done. Do not treat any item as complete until it is verified in the repository.

Status legend: ☐ not started · ◐ in progress · ☑ done.

---

## 1. Android release  ☐

- ☐ Release signing config (keystore, key alias) kept out of version control; injected securely.
- ☐ Application id confirmed (`com.vaibhav.relive`), version code/name strategy defined.
- ☐ `minSdk 24` / `targetSdk 36` / `compileSdk 36` reviewed against store requirements at submission time.
- ☐ ProGuard/R8 rules validated if minification is enabled (currently `isMinifyEnabled = false`).
- ☐ Release build produced and smoke-tested: `./gradlew :androidApp:assembleRelease` (and/or `bundleRelease`).
- ☐ Google Play listing: title, description, category, content rating, data-safety form.

## 2. iOS build & signing  ☐

- ☐ Apple Developer account / team configured.
- ☐ Bundle identifier, provisioning profiles, and signing certificates set up.
- ☐ Shared framework builds for device (`iosArm64`) and archives cleanly in Xcode.
- ☐ App Store Connect record: app info, privacy nutrition labels, screenshots.
- ☐ TestFlight build validated.

## 3. RevenueCat (Pro entitlement)  ☐

- ☐ RevenueCat project + API keys (Android/iOS) provisioned.
- ☐ Products/entitlements configured in App Store Connect and Google Play; mapped in RevenueCat.
- ☐ RevenueCat SDK integrated behind the existing entitlement interface (Phase 9, [`ROADMAP.md`](ROADMAP.md)).
- ☐ Purchase, restore, and entitlement-gating flows tested on both platforms.

## 4. RevenueCat Funnels  ☐

- ☐ Funnels configured for conversion tracking (later; optional).
- ☐ Events/attribution defined without adding disallowed third-party analytics to the core app.

## 5. Stripe (web subscription conversion)  ☐

- ☐ Stripe account + products/prices configured (later; optional).
- ☐ Web subscription flow defined and linked with RevenueCat Funnels where applicable.
- ☐ Entitlement reconciliation between Stripe (web) and app stores clarified.

## 6. Shipaton submission  ☐

- ☐ Submission requirements reviewed and checklist derived.
- ☐ Required artifacts (builds, listing, media) assembled.
- ☐ Submission completed by the deadline.

## 7. Screenshots & demo video  ☐

- ☐ Device screenshots for Android and iOS store listings (all required sizes).
- ☐ Screenshots reflect the approved Warm Journal look and real timeline content.
- ☐ Demo video showing capture → relive flow.

## 8. Privacy checks  ☐

- ☐ Confirm local-first posture holds: no backend, no cloud sync, no login, no third-party analytics, no location history separate from moments, no background location tracking.
- ☐ Location usage strings / permission descriptions written (Android manifest strings, iOS `Info.plist` usage descriptions) — moment-scoped, requested only when needed.
- ☐ Store data-safety / privacy labels accurately reflect on-device-only data and optional location.
- ☐ Export feature (Settings) privacy reviewed once its behavior is defined.
- ☐ Media and location data confirmed to remain local with the moment.

---

## Notes

- Monetization items (3–5) depend on Phase 9 and are added only when that phase begins; no RevenueCat/Stripe dependencies before then ([`ARCHITECTURE.md`](ARCHITECTURE.md) §9).
- Keep secrets (keystores, API keys, signing certs) out of the repository.
- Update this file as items are configured; record any structural decisions in [`DECISIONS.md`](DECISIONS.md).
