# Relive

**Capture moments. Relive them later.**

Relive is a private, **local-first** personal memory timeline for Android and iOS. You save thoughts, memories, journal-style text, photos, videos, audio, or any combination — and revisit them later as a beautiful chronological archive of your life: every moment in your All moments feed, plus custom timelines like *College* or *Japan 2026*.

Relive is designed to feel like a personal life archive, not a notes app or database.

---

## Tech stack

- **Kotlin Multiplatform** — shared business logic across platforms
- **Compose Multiplatform** — shared UI where practical
- **Android + iOS** targets
- **Material 3** as the interaction/accessibility/component foundation, with a custom Relive design system on top
- **Local-first persistence** — no backend, no login, no cloud sync

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full picture.

---

## Current development status

**Active implementation / polish phase.** The repository contains a working Kotlin Multiplatform + Compose Multiplatform app with persistent Moments and custom timelines; the unified Home surface (the app's single root, where a welcome greeting, the Rediscover collection row — `Favourites`, `On This Day`, `From Your Past`, `All Photos` — and the chronological All moments feed sit on one continuous vertical scroll, so reaching focused All moments is a scroll position rather than a separate screen); inline capture and editing (the composer expands in place from the All moments timeline rail — never a modal or bottom sheet — and the user stays in focused All moments after Keep Moment); media capture/playback, favorites, global Search, appearance and timeline wallpapers, Profile settings, Android external-share capture, and local archive insights. The All moments feed is newest-first and paged/windowed — the root surface never hydrates the complete archive on launch. Android also contains the Google Drive backup/restore integration seam and implementation; account/OAuth configuration and physical-device verification remain release setup work.

The current product is local-first and uses persistent SQLDelight/SQLite storage in both debug and release builds. GPS detection, monetization, Export, and production release configuration remain deferred or incomplete as tracked in [`docs/ROADMAP.md`](docs/ROADMAP.md) and [`docs/RELEASE.md`](docs/RELEASE.md).

Progress is tracked in [`docs/ROADMAP.md`](docs/ROADMAP.md).

---

## Build & run

Requires a recent JDK, Android SDK, and (for iOS) Xcode on macOS.

- **Android app:**
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
- **iOS app:** open the `iosApp/` directory in Xcode and run it there.

### Tests

- **Android host tests:**
  ```bash
  ./gradlew :shared:testAndroidHostTest
  ```
- **iOS simulator tests:**
  ```bash
  ./gradlew :shared:iosSimulatorArm64Test
  ```

See [`docs/TESTING.md`](docs/TESTING.md) for the full testing strategy.

---

## Authoritative documentation

Read these before contributing. They are the source of truth:

- [`AGENTS.md`](AGENTS.md) — operating rules for contributors and AI agents
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) — what Relive does
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — how it is structured
- [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) — tokens and visual language
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — architectural decision record
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — phased plan
- [`docs/TESTING.md`](docs/TESTING.md) — testing expectations
- [`docs/RELEASE.md`](docs/RELEASE.md) — release requirements (planned)

The approved UI reference lives in `docs/ui-reference/` and is authoritative for Relive's warm editorial visual language — type, color, spacing, card and timeline-rail treatment. It is **not** authoritative for navigation structure: the unified Home surface defined in [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) supersedes any separate Timeline Home / Rediscover / All-timeline-page structure shown there.
