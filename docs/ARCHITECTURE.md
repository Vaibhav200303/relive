# Relive — Architecture

This document proposes a production-quality structure for Relive. It is a target architecture: not every module below exists yet. Build it out phase by phase per [`ROADMAP.md`](ROADMAP.md). Do not over-engineer — add structure when a phase needs it, not before.

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

Generated Kotlin Multiplatform + Compose Multiplatform starter:

```
Relive/
├── androidApp/            # Android application entry point
│   └── src/main/kotlin/com/vaibhav/relive/MainActivity.kt
├── iosApp/                # iOS application entry point (Xcode project, SwiftUI host)
├── shared/                # shared KMP module
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
shared/src/androidMain/kotlin/com/vaibhav/relive/  # Android impls (location, media, storage)
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

Names are indicative; finalize during Phase 1.

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

Local-first, on-device. The concrete engine (e.g. a KMP-compatible local database) is selected in Phase 1 and recorded in [`DECISIONS.md`](DECISIONS.md). Regardless of engine, the schema must satisfy [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §13:

- **moments** — one row per moment, stored once.
- **custom timelines** — reference moments; never duplicate moment data.
- **moment ↔ timeline** — a many-to-many membership table for custom-timeline references.
- **attachments** — reference their moment (`momentId`), ordered for collage/viewer display (see [`DECISIONS.md`](DECISIONS.md) ADR-0019).
- **tags** — stored so they can be queried efficiently, including scoped to a timeline.
- **All** — logically automatic: a query over all moments, **not** an explicit membership row per moment.

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

Define a **platform-independent location abstraction** in shared code, conceptually:

```
Composer → LocationProvider → current coordinates → PlaceResolver → human-readable location
```

- **`LocationProvider`** — obtains the current device coordinates on demand (moment-scoped, one-shot; **never** continuous/background). Declared in shared code.
  - Android implementation uses Android location APIs.
  - iOS implementation uses Core Location.
- **`PlaceResolver`** — reverse-geocodes coordinates into a readable `ReliveLocation` (place name, locality, region, country). Behind its own interface so the resolution implementation can change later.

The moment **domain model must not couple to any specific GPS SDK.** Shared domain/presentation code must not import Android location or Core Location types.

Location acquisition must model and surface these outcomes so the composer can continue normally in every case (see [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7.2):

- permission denied
- permission permanently denied
- location services disabled
- location unavailable
- timeout / failure

A sealed result type (e.g. `LocationResult { Available, PermissionDenied, PermissionPermanentlyDenied, ServicesDisabled, Unavailable, Timeout }`) is the recommended shape. Permission requests use the platform's normal permission flow and happen only when needed.

### 6.2 Media capture and storage

Define interfaces for capturing (camera, microphone, gallery picker) and persisting media, implemented per platform. Shared logic references these interfaces only. Do not leak `android.*` or `AVFoundation`/`UIKit` types into shared business logic.

### 6.3 Other platform capabilities

Any future platform capability follows the same pattern: interface in shared code, implementation in the platform source set, decision recorded in [`DECISIONS.md`](DECISIONS.md).

---

## 7. Presentation & UI

- **Presentation** holds ViewModels/state holders (using the Compose/AndroidX lifecycle-viewmodel already available) that expose immutable UI state and receive user intents. No platform imports.
- **UI** is Compose Multiplatform, built on **Material 3** for interaction/accessibility/component foundations, with the **Relive design system** layered on top (tokens and components in `ui/theme` and `ui/components`). See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).
- The app must **not** look like a default Material application. Material 3 provides behavior and accessibility; Relive tokens provide the look.
- Timeline UI must match the authoritative reference in `docs/ui-reference/`. If tokens and the reference conflict, **stop and report** (see [`../AGENTS.md`](../AGENTS.md)).

---

## 8. Themes

Global app appearance and timeline appearance are separate models and persistence paths. Global System/Light/Dark mode and the app palette are stored by an `AppearanceRepository` backed by Android SharedPreferences or iOS UserDefaults. Each persisted custom timeline owns a non-null `TimelineAppearance` in SQLDelight, containing a `TimelineWallpaper` and a Moment-theme identity. `TimelineViewModel` exposes the active timeline's appearance without writing to app preferences. The shared timeline UI resolves a saved wallpaper identity to its approved Compose resource and renders it behind timeline content; it does not alter global tokens or Moment foreground styling. Later redesign stages may render Moment treatment, but they must not retheme Settings, Profile, navigation, or the global Material theme. Themes never alter navigation, timeline structure, moment hierarchy, composer interaction, media data, or search. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §11 and [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

---

## 9. Monetization seams (later)

RevenueCat (Pro entitlement), RevenueCat Funnels, and Stripe (web subscription) are planned for later phases and **must not be implemented now**. To keep the seam open:

- Represent Pro state behind an **entitlement interface** in shared code (e.g. `EntitlementProvider` returning free/pro). Early phases can back it with a local stub.
- Keep gating decisions (what Pro unlocks) in the domain/presentation layers so a real entitlement source can be swapped in later.
- Do not add RevenueCat/Stripe dependencies until the monetization phase.

See [`ROADMAP.md`](ROADMAP.md) Phase 9 and [`RELEASE.md`](RELEASE.md).

---

## 10. Dependency management

- All versions are declared in `gradle/libs.versions.toml`. Do not add dependencies ad hoc; add them there and only when a task requires them.
- No backend/network stack, analytics, or AI/ML dependencies (see non-goals in [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §15).

---

## 11. Media presentation performance

Media-heavy timelines require careful performance architecture. The following constraints are enforced:

- **No expensive media decode on the UI thread.** Video thumbnails and image aspect/dimensions are resolved off the main thread.
- **Video thumbnail cache.** Representative frames are generated once and cached via `MediaPresentationCache`; the cache is bounded.
- **Image aspect/dimensions cached.** Natural size is probed once per `MediaStorageRef` and reused for adaptive sizing calculations.
- **Audio waveform extraction cached.** Amplitude envelopes are extracted once and reused for all timeline/viewer renders. No repeated audio decoding during Compose recomposition.
- **Player creation is lazy.** Passive audio/video tiles in the timeline do **not** create media players. Players are instantiated only when the user initiates playback.
- **Only one active playback at a time.** `ActivePlayback` coordinates ownership — starting a new audio or video stops any prior one.
- **Off-screen cleanup.** Playback and animation resources for tiles scrolled off-screen are cleaned up.
- **Stable timeline keys.** `LazyColumn` items use stable `MomentId` keys to avoid unnecessary recomposition.
- **No whole-timeline recomposition for playback ticks.** Playback progress updates are scoped to the playing tile, not the entire timeline list.

These decisions support ADR-0019 §9 (performance constraints).

---

## 12. Persistence: debug and release behavior

Both debug and release builds use **persistent SQLDelight/SQLite storage** (ADR-0013). There is no in-memory debug replacement or sample repository that silently substitutes for real user persistence. User Moments in debug builds survive process death, removal from Recents, and normal APK updates — the same as release builds.

---

## 13. Testing seams

The layering above is designed for testability: pure domain logic, a `Clock` for the 4-day rule, repository interfaces for fakes, and platform capabilities behind interfaces for substitution in tests. See [`TESTING.md`](TESTING.md).

## 14. Rediscover read model

Rediscover is a read-model feature. `RediscoverRepository` exposes the active reactive Favorites and On This Day system collections plus bounded deferred local projections for From Your Past, Places, and Tags. Favorites reads its count, favorited Moment scope, and up-to-four visual cover attachments in SQL; On This Day reads only a bounded prior-year local-calendar preview and batch-loads its attachments. Neither has a custom-timeline row, duplicate membership, or table. SQLDelight reads core Moment rows, then batch-loads their tags and attachments to avoid query-per-card behavior. Current-device-local calendar conversion is isolated behind a presentation `expect/actual` seam, while SQLite applies the same local-calendar rule for bounded eligibility queries. Rediscover has no persistence tables, backend, or recommendation state.

## 15. Behavior preferences

App behavior settings use a domain-owned `BehaviorPreferencesRepository` with observable `BehaviorPreferences` state. Android implements it with a dedicated `SharedPreferences` file; iOS uses `NSUserDefaults`. Stable string identifiers are persisted and decoded through shared code, with safe defaults for missing or invalid values. The repository is process-scoped in `ReliveAppContainer`; presentation observes it through `BehaviorPreferencesViewModel`.

Behavior preferences remain outside SQLDelight because they configure app presentation/navigation rather than archive data. Startup reads the persisted destination once when the app root is created, while discard, editable-Timeline metadata visibility, and Rediscover section visibility consume reactive state. Location/tag hiding never removes fields from `MomentPresentation`, so Search, edit/composer, persistence, and read-only system collections retain the underlying data. Rediscover continues collecting its bounded read projections when a section is hidden; only root rendering changes.
