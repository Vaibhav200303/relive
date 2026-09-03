# Relive — Architecture

This document describes Relive's implemented architecture and the boundaries still being completed. Build remaining work phase by phase per [`ROADMAP.md`](ROADMAP.md). Do not over-engineer — add structure when a phase needs it, not before.

Contributor rules that constrain this architecture are in [`../AGENTS.md`](../AGENTS.md). Product behavior is in [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md).

---

## 1. Principles

1. **Local-first.** All data lives on device. No backend, no sync, no login. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §13.
2. **Clean, decoupled layering.** Domain, data, presentation, and platform integrations are clearly separated.
3. **Shared UI where practical.** Compose Multiplatform UI lives in shared code; platform code is the thin edge.
4. **No platform APIs in shared business logic.** Media capture/storage, location, and any other platform capability are reached only through interfaces declared in shared code. Concrete implementations live in platform source sets.
5. **Open to later monetization.** The structure must accommodate RevenueCat / Stripe later (see §9) without implementing them now.
6. **Don't over-engineer.** Prefer the simplest structure that keeps the boundaries clean.

---

## 2. Current repository layout

Kotlin Multiplatform + Compose Multiplatform application:

```
Relive/
├── androidApp/            # Android application entry point and debug/release wiring
│   └── src/*/kotlin/com/vaibhav/relive/MainActivity.kt
├── iosApp/                # iOS application entry point (Xcode project, SwiftUI host)
├── shared/                # shared KMP module, domain, data, presentation, and Compose UI
│   └── src/
│       ├── commonMain/    # shared code for all targets
│       ├── androidMain/   # Android-specific implementations
│       ├── iosMain/       # iOS-specific implementations
│       ├── commonTest/
│       ├── androidHostTest/
│       └── iosTest/
├── gradle/libs.versions.toml
└── docs/
```

Implemented shared feature areas include `timeline`, `timelinehome`, `rediscover`, `search`, `composer`, `viewer`, `profile`, `settings`, and `navigation` presentation packages, plus media, backup, notifications, and Android share platform boundaries. `timelinehome` and `rediscover` are not peer destinations: together they compose the one unified **Home surface** — welcome block, Rediscover collection row, and the All moments feed in a single scroll container — which is the app's only root (ADR-0061). Those two package names predate the redesign and still carry their old spelling; folding them into a single `home` feature area is a naming refactor, not a change of structure. The source tree is intentionally still a single shared module; separate Gradle modules are not part of the current architecture.

- Package root: `com.vaibhav.relive`
- Android: `applicationId = com.vaibhav.relive`, `minSdk 24`, `compileSdk 36`, `targetSdk 36`
- iOS targets: `iosArm64`, `iosSimulatorArm64`; framework `baseName = "Shared"`, static
- Toolchain (from `libs.versions.toml`): Kotlin 2.4.10, Compose Multiplatform 1.11.1, Compose Material3 1.11.0-alpha07, AGP 9.0.1

---

## 3. Target layering

The shared module is organized into layers. Dependencies point **inward only**: presentation → domain ← data. The domain layer depends on nothing platform-specific.

```
shared/src/commonMain/kotlin/com/vaibhav/relive/
├── domain/          # pure business logic — no framework, no platform, no Compose
│   ├── model/       # Moment, Timeline, MediaAttachment, Tag, ReliveLocation, ...
│   ├── repository/  # repository interfaces (MomentRepository, TimelineRepository, ...)
│   ├── time/        # Clock abstraction, 4-day edit-window rule
│   └── usecase/     # use cases / interactors (optional; add when logic warrants)
├── data/            # implements domain repository interfaces
│   ├── local/       # local persistence (DB, DAOs, entities, mappers)
│   └── media/       # media storage coordination (paths, references)
├── platform/        # expect declarations + interfaces for platform capabilities
│   ├── location/    # LocationProvider, PlaceResolver interfaces
│   ├── media/       # MediaCapture / MediaStore interfaces
│   └── ...
├── presentation/    # ViewModels / state holders + UI state; no Android/iOS imports
│   ├── timeline/
│   ├── composer/
│   ├── search/
│   └── settings/
├── ui/              # Compose Multiplatform UI + Relive design system
│   ├── theme/       # design tokens, ReliveTheme, theme variants
│   ├── components/  # timeline rail, dot, moment card, media collage, composer, ...
│   └── screens/
└── di/              # composition root / manual dependency wiring
```

Platform implementations live in the platform source sets:

```
shared/src/androidMain/kotlin/com/vaibhav/relive/  # Android impls (media, storage, backup, sharing)
shared/src/iosMain/kotlin/com/vaibhav/relive/      # iOS impls (Core Location, media, storage)
```

### Layer rules

- **domain** — pure Kotlin. No Compose, no Android/iOS, no persistence framework types. Holds the moment model, timeline membership logic, and the 4-day rule.
- **data** — implements domain repository interfaces using the local database and media storage. Maps between persistence entities and domain models.
- **platform** — declares interfaces (and `expect` where appropriate) for capabilities that must be implemented per platform. Shared code depends on these interfaces, never on `android.*` or Core Location directly.
- **presentation** — ViewModels/state holders expose UI state and consume domain use cases/repositories. No platform imports.
- **ui** — Compose Multiplatform components and the Relive design system. Renders presentation state.

---

## 4. Domain model (conceptual)

The following domain model is implemented and persisted through the repository interfaces:

- **Moment**
  - `id`
  - `createdAt` — **immutable** creation instant; source of truth for the 4-day rule
  - `updatedAt` — last edit; **never** used to compute the edit window
  - `title`
  - `content`
  - `location: ReliveLocation?`
  - `tags: List<Tag>`
  - `isFavorite: Boolean`
  - `attachments: List<MediaAttachment>`
- **Timeline**
  - `id`, `name`, `appearance`, where custom-timeline appearance is persisted with that timeline
  - built-in **All** is represented logically, not as a stored custom timeline
- **MomentTimelineMembership** — many-to-many link between moments and custom timelines
- **MediaAttachment** — `id`, `momentId`, `type` (image | video | audio), storage reference, ordering index
- **Tag** — queryable; associated with moments (many-to-many)
- **ReliveLocation** — all fields optional: `latitude?`, `longitude?`, `placeName?`, `locality?`, `region?`, `country?`

### The 4-day edit/forget rule

- Centralize the rule in the domain layer (e.g. `EditWindow` using a `Clock`).
- `isEditable(moment, now) = now < moment.createdAt + 4 days`.
- Uses `createdAt` only. The rule governs whether Edit/Forget appear on long-press, whether inline editing is permitted, and whether a moment may be forgotten.
- A `Clock` abstraction makes this deterministically testable. See [`TESTING.md`](TESTING.md).

---

## 5. Persistence design

Local-first, on-device. The implemented engine is SQLDelight/SQLite (ADR-0013), and its schema satisfies [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §13:

- **moments** — one row per moment, stored once.
- **custom timelines** — reference moments; never duplicate moment data.
- **moment ↔ timeline** — a many-to-many membership table for custom-timeline references.
- **attachments** — reference their moment (`momentId`), ordered for collage/viewer display (see [`DECISIONS.md`](DECISIONS.md) ADR-0019).
- **tags** — stored so they can be queried efficiently, including scoped to a timeline.
- **All** — logically automatic: a `createdAt`-ordered query over the moments table, **not** an explicit membership row per moment. Because All moments is the feed of the root Home surface, the data layer exposes it as a **bounded, anchored, paged** read — a newest-first `limit` + keyset query anchored on `createdAt`/`id` — that loads one window and appends older windows as the user scrolls. `MomentRepository.observeAll()` returns the complete archive and must not back the Home feed: the root may neither observe nor hydrate the whole archive on launch, nor obtain newest-first order by reversing a full collection in presentation (ADR-0061). `observeAll()` remains appropriate for surfaces entered on demand. Each loaded page batch-loads its tags and attachments under the shared read-model rule in §14. Calendar date navigation (see [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §3.1) re-anchors this keyset window at the resolved `createdAt`/`id` rather than paging forward through history.

Data-layer repositories implement the domain repository interfaces and hide the storage engine from the rest of the app.

### Media storage

- Media binaries are stored on the device file system; the database holds **references** (paths/identifiers), not blobs, unless a later decision says otherwise.
- Writing/reading media files is a platform capability behind an interface (§6), so shared code never touches platform file APIs directly.
- Android external shares are normalized at the platform edge through `IncomingShareGateway`: provider URIs are copied into Relive-owned temporary files before shared UI sees a request. The gateway owns those files until the composer claims the payload; it deletes them on cancellation or validation failure. Common presentation receives only text metadata and `RawMedia`, never Android `Intent`, `Uri`, or `ContentResolver` types.
- Archive-insights reads only persisted attachment references, then asks `MediaStore` to inspect each Relive-managed file on a background dispatcher. It never walks arbitrary device storage, hydrates the complete Moment archive, or mutates media discovered to be missing.

---

## 6. Platform capability boundaries

Any capability that differs by platform is declared as an interface in shared code and implemented per platform. Shared domain/presentation code depends only on the interface.

### 6.1 Location

Product requirements: [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7.

The shared model and composer currently support **optional manual readable location labels**. The platform-independent GPS abstraction is defined as a future seam, but GPS acquisition and reverse geocoding are not active in the current app:

```
Composer → LocationProvider → current coordinates → PlaceResolver → human-readable location
```

- **`LocationProvider`** — future on-demand, moment-scoped coordinate capability. No Android or iOS GPS implementation is currently active.
- **`PlaceResolver`** — reverse-geocodes coordinates into a readable `ReliveLocation` (place name, locality, region, country). Behind its own interface so the resolution implementation can change later.

The moment **domain model must not couple to any specific GPS SDK.** Shared domain/presentation code must not import Android location or Core Location types.

Location acquisition must model and surface these outcomes so the composer can continue normally in every case (see [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7.2):

- permission denied
- permission permanently denied
- location services disabled
- location unavailable
- timeout / failure

A sealed result type (e.g. `LocationResult { Available, PermissionDenied, PermissionPermanentlyDenied, ServicesDisabled, Unavailable, Timeout }`) is the recommended future shape. Permission requests and reverse geocoding are future work; the current manual-label flow requests no location permission.

### 6.2 Media capture and storage

Define interfaces for capturing (camera, microphone, gallery picker) and persisting media, implemented per platform. Shared logic references these interfaces only. Do not leak `android.*` or `AVFoundation`/`UIKit` types into shared business logic.

### 6.3 Other platform capabilities

Any future platform capability follows the same pattern: interface in shared code, implementation in the platform source set, decision recorded in [`DECISIONS.md`](DECISIONS.md).

---

## 7. Presentation & UI

- **Presentation** holds ViewModels/state holders (using the Compose/AndroidX lifecycle-viewmodel already available) that expose immutable UI state and receive user intents. No platform imports.
- **UI** is Compose Multiplatform, built on **Material 3** for interaction/accessibility/component foundations, with the **Relive design system** layered on top (tokens and components in `ui/theme` and `ui/components`). See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).
- The app must **not** look like a default Material application. Material 3 provides behavior and accessibility; Relive tokens provide the look.
- **Home is the landing root.** The app has three top-level destinations — Home, Timelines and Search — and Home is the primary landing destination: the unified **Home surface**, one vertically scrollable container holding the welcome block, the `Relive your memories` heading and its horizontally scrollable Rediscover collection row, the `All moments` heading, the inline composer at the head of the feed, and the All moments feed itself. Home's two states — the top state and focused All moments — are scroll offsets on that one container, not screens: moving between them produces no navigation event, route change, back-stack entry, or screen transition, so no navigation motion pattern applies to it. Rediscover is a row inside Home, not a destination, and its cards are never the way into All moments; the floating navigation toolbar's destination set is Home / Timelines / Search, where Timelines retains Timeline Home and the custom-timeline detail screens unchanged. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) and ADR-0061 in [`DECISIONS.md`](DECISIONS.md).
- The reference in `docs/ui-reference/` is authoritative for Relive's warm editorial visual identity — rail, dot, moment card, type, spacing, and color. It is **not** authoritative for navigation, surface composition, or scroll behavior, which follow the unified Home surface described in [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md). If tokens and the reference conflict on visual identity, **stop and report** (see [`../AGENTS.md`](../AGENTS.md)).

---

## 8. Themes

Global app appearance and timeline appearance are separate models and persistence paths. Global System/Light/Dark mode and the app palette, plus the logical All timeline's `TimelineAppearance`, are stored by an `AppearanceRepository` backed by Android SharedPreferences or iOS UserDefaults. Each persisted custom timeline owns a non-null `TimelineAppearance` in SQLDelight, containing a `TimelineWallpaper` and a Moment-theme identity. `TimelineViewModel` exposes the active custom timeline's appearance; the shared timeline UI reads All's native-local appearance directly. The shared timeline UI resolves a saved wallpaper identity to its approved Compose resource and renders it behind timeline content; on the Home surface it stays behind the All moments feed and never rethemes the welcome block or the Rediscover row, and it does not alter global tokens or Moment foreground styling. Later redesign stages may render Moment treatment, but they must not retheme Settings, Profile, navigation, or the global Material theme. Themes never alter navigation, timeline structure, moment hierarchy, composer interaction, media data, or search. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §11 and [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

---

## 9. Monetization seams (later)

RevenueCat (Pro entitlement), RevenueCat Funnels, and Stripe (web subscription) are planned for later phases and **must not be implemented now**. To keep the seam open:

- Represent Pro state behind an **entitlement interface** in shared code (e.g. `EntitlementProvider` returning free/pro). Early phases can back it with a local stub.
- Keep gating decisions (what Pro unlocks) in the domain/presentation layers so a real entitlement source can be swapped in later.
- Do not add RevenueCat/Stripe dependencies until the monetization phase.

See [`ROADMAP.md`](ROADMAP.md) Phase 10 and [`RELEASE.md`](RELEASE.md).

---

## 10. Dependency management

- All versions are declared in `gradle/libs.versions.toml`. Do not add dependencies ad hoc; add them there and only when a task requires them.
- No backend/network stack, analytics, or AI/ML dependencies (see non-goals in [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §15).

---

## 11. Media presentation performance

Media-heavy timelines — and the root Home surface, which scrolls them in one continuous container — require careful performance architecture. The following constraints are enforced:

- **No expensive media decode on the UI thread.** Video thumbnails and image aspect/dimensions are resolved off the main thread.
- **Video thumbnail cache.** Representative frames are generated once and cached via `MediaPresentationCache`; the cache is bounded.
- **Image aspect/dimensions cached.** Natural size is probed once per `MediaStorageRef` and reused for adaptive sizing calculations.
- **Audio waveform extraction cached.** Amplitude envelopes are extracted once and reused for all timeline/viewer renders. No repeated audio decoding during Compose recomposition.
- **Player creation is lazy.** Passive audio/video tiles in the timeline do **not** create media players. Players are instantiated only when the user initiates playback.
- **Only one active playback at a time.** `ActivePlayback` coordinates ownership — starting a new audio or video stops any prior one.
- **Off-screen cleanup.** Playback and animation resources for tiles scrolled off-screen are cleaned up.
- **Stable Home surface keys.** The single Home `LazyColumn` uses stable keys for every item type it holds — the welcome block, the section headings, the Rediscover row, the inline composer, and `MomentId` for timeline items — to avoid unnecessary recomposition. Custom timeline detail lists use the same `MomentId` keys.
- **Bounded All moments feed.** Home loads All moments in newest-first pages and appends older pages as the user scrolls toward older Moments; it never observes or hydrates the complete Moment archive on launch, and never reaches newest-first order by reversing a full collection in presentation. The bound is a data-layer contract (§5), not a rendering optimization.
- **No whole-timeline recomposition for playback ticks.** Playback progress updates are scoped to the playing tile, not the entire timeline list.

These decisions support ADR-0019 §9 (performance constraints).

---

## 12. Persistence: debug and release behavior

Both debug and release builds use **persistent SQLDelight/SQLite storage** (ADR-0013). There is no in-memory debug replacement or sample repository that silently substitutes for real user persistence. User Moments in debug builds survive process death, removal from Recents, and normal APK updates — the same as release builds.

---

## 13. Testing seams

The layering above is designed for testability: pure domain logic, a `Clock` for the 4-day rule, repository interfaces for fakes, and platform capabilities behind interfaces for substitution in tests. See [`TESTING.md`](TESTING.md).

## 14. Rediscover read model

Rediscover is a read-model feature rendered as a horizontally scrollable collection row inside the unified Home surface, not a separate top-level destination or root of its own. Its cards open their own read-only collection views; they are never the entry point into All moments, which is the continuous feed directly beneath the row on the same surface. `RediscoverRepository` exposes the active reactive Favorites and On This Day system collections plus bounded deferred local projections for From Your Past and All Photos — the four collections in the Home Rediscover row — and the deferred Places and Tags projections, which remain implemented but unrendered. Favorites reads its count, favorited Moment scope, and up-to-four visual cover attachments in SQL; On This Day reads only a bounded prior-year local-calendar preview and batch-loads its attachments. All Photos is a bounded read-only projection of the Moments carrying at least one image or video attachment, read through the same pattern. None of them has a custom-timeline row, duplicate membership, or table. SQLDelight reads core Moment rows, then batch-loads their tags and attachments to avoid query-per-card behavior; this batch-hydration rule is a shared read-model contract rather than a Rediscover-only one, and each loaded page of Home's All moments feed (§5) hydrates its tags and attachments the same way. Current-device-local calendar conversion is isolated behind a presentation `expect/actual` seam, while SQLite applies the same local-calendar rule for bounded eligibility queries. Rediscover has no persistence tables, backend, or recommendation state.

## 15. Behavior preferences

App behavior settings use a domain-owned `BehaviorPreferencesRepository` with observable `BehaviorPreferences` state. Android implements it with a dedicated `SharedPreferences` file; iOS uses `NSUserDefaults`. Stable string identifiers are persisted and decoded through shared code, with safe defaults for missing or invalid values. The repository is process-scoped in `ReliveAppContainer`; presentation observes it through `BehaviorPreferencesViewModel`.

Behavior preferences remain outside SQLDelight because they configure app presentation rather than archive data. There is no persisted start-destination preference: with a single root, startup always opens the Home surface at its top, at scroll offset zero, with no programmatic scroll on entry (ADR-0061), and state restoration and deep-link destinations keep their existing higher priority. The retired start-destination key is neither read nor written. Discard, editable-Timeline metadata visibility, and Rediscover card visibility consume reactive state. Location/tag hiding never removes fields from `MomentPresentation`, so Search, edit/composer, persistence, and read-only system collections retain the underlying data. Rediscover continues collecting its bounded read projections when a card is hidden; only what the Home surface's Rediscover row renders changes.
