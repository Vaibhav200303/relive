# Relive — Architecture Decision Record

This is the running log of **major** architectural and product-structure decisions. Record a decision here when it settles something future work must respect. Once recorded, a decision is **settled**: do not silently change it (see [`../AGENTS.md`](../AGENTS.md)). To change a settled decision, add a new entry that supersedes the old one and explains why.

Format for each entry:

- **ID / date / status** (Accepted | Superseded | Proposed)
- **Context** — the situation forcing a choice
- **Decision** — what was chosen
- **Consequences** — what this enables/constrains

---

## ADR-0001 — Local-first, no backend

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Relive is a private personal memory archive. The product explicitly excludes backend, cloud sync, login, and social features for the current product.
- **Decision:** All data is stored on-device. No server, network data layer, authentication, or sync is built now.
- **Consequences:** Persistence is a local database plus on-device media files. No networking stack or auth. Any future sync would be a separate, explicitly approved decision. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §1, §5.

---

## ADR-0002 — Kotlin Multiplatform + Compose Multiplatform, shared UI

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Target Android and iOS with a small team and a distinctive, consistent UI.
- **Decision:** Use Kotlin Multiplatform for shared logic and Compose Multiplatform for shared UI. Platform apps (`androidApp`, `iosApp`) are thin entry points. Toolchain is pinned in `gradle/libs.versions.toml` (Kotlin 2.4.10, Compose MP 1.11.1, Material3 1.11.0-alpha07, AGP 9.0.1).
- **Consequences:** UI and business logic live in `shared/`. Platform code is minimized. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §2–§3.

---

## ADR-0003 — Material 3 foundation, custom Relive design system on top

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** The app must be accessible and behave correctly, but must **not** look like a default Material app. It must match the approved warm-editorial reference.
- **Decision:** Build on Material 3 for interaction, accessibility, and component behavior; apply a strict tokenized Relive design system for all visual styling. The approved UI reference in `docs/ui-reference/` is authoritative for timeline UI.
- **Consequences:** No raw styling in components — everything comes from tokens. When tokens conflict with the reference, work stops and the conflict is reported. See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

---

## ADR-0004 — Store each moment once; timelines reference moments

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** A moment can appear in multiple timelines; duplicating it would corrupt edits and waste storage.
- **Decision:** Persist each moment exactly once. Custom timelines reference moments via a many-to-many membership table. Attachments and tags reference their moment. The built-in **All** timeline is a logical query over all moments, not per-moment membership rows.
- **Consequences:** Editing/forgetting a moment affects all timelines consistently. Membership is cheap to change. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §2, §13 and [`ARCHITECTURE.md`](ARCHITECTURE.md) §5.

---

## ADR-0005 — 4-day edit/forget window keyed on immutable `createdAt`

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Memories should feel permanent shortly after capture, with a brief correction window, and must not be endlessly editable.
- **Decision:** A moment may be edited or forgotten only within 4 days of its **immutable `createdAt`**. `updatedAt` **never** extends the window. The rule is centralized in the domain layer using a `Clock` abstraction and governs long-press actions, inline editing, and forgetting. Forgetting requires confirmation.
- **Consequences:** Deterministic, testable rule. UI must hide Edit/Forget after the window closes. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §8 and [`TESTING.md`](TESTING.md).

---

## ADR-0006 — Timeline-scoped search

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Users navigate by timeline (chapter of life); search should respect the current context.
- **Decision:** Search always operates within the currently selected timeline, with All / Tags / Places filters scoped to that timeline. On the built-in All timeline, scope is every moment. All-search behaves like WhatsApp chat search (highlight, match count, up/down navigation, auto-scroll).
- **Consequences:** Search queries carry the active timeline scope. Places/Tags suggestions derive only from the current timeline's moments. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §9.

---

## ADR-0007 — Platform capabilities behind shared interfaces

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Location, media capture/storage, and similar capabilities differ per platform, but shared logic must stay platform-agnostic.
- **Decision:** Declare capability interfaces in shared code (e.g. `LocationProvider`, `PlaceResolver`, media capture/storage). Implement them in `androidMain` (Android APIs) and `iosMain` (Core Location, AVFoundation/UIKit). Shared domain/presentation code depends only on the interfaces and never imports platform APIs.
- **Consequences:** Business logic is testable with fakes; implementations can change without touching shared logic. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §6.

---

## ADR-0008 — Location is moment-scoped and local-only

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Location adds value to a moment but must not become tracking or a privacy liability.
- **Decision:** Location is captured only at moment-creation time, on demand, via `LocationProvider` → `PlaceResolver`. No background tracking, no separate location history, no third-party location analytics. Permission is requested only when needed via the platform flow; denial never blocks saving. All failure states (denied, permanently denied, services disabled, unavailable, timeout) let the composer continue. `ReliveLocation` fields are all optional; raw coordinates are never shown in the normal timeline UI. Location data stays local with the moment. The location data model and interfaces are established **before** platform implementations.
- **Consequences:** Strong privacy posture; robust composer behavior. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7 and [`ARCHITECTURE.md`](ARCHITECTURE.md) §6.1.

---

## ADR-0009 — Monetization deferred but seam kept open

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** RevenueCat (Pro), RevenueCat Funnels, and Stripe (web) are planned later but must not be built now.
- **Decision:** Represent Pro entitlement behind an interface in shared code, initially backed by a local stub. No RevenueCat/Stripe dependencies are added until the monetization phase. Gating logic lives in domain/presentation so a real source can be swapped in.
- **Consequences:** The app ships free-tier now and can add real entitlements later without restructuring. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §9, [`ROADMAP.md`](ROADMAP.md) Phase 9, [`RELEASE.md`](RELEASE.md).

---

## ADR-0010 — Relive design tokens exposed via `ReliveTheme`, layered on Material 3

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Phase 0 must establish a reusable visual foundation that (a) keeps Material 3 for accessibility/component semantics, (b) forces every visual value to come from a named token, and (c) leaves room for future per-timeline themes (Warm Journal, Monochrome Archive, Film Memory) without rewriting components.
- **Decision:** Introduce a `ReliveThemeTokens` bundle (colors, typography, dimensions, motion) selected by `ReliveThemeId`. `ReliveTheme` composable resolves the active bundle, publishes it through a `staticCompositionLocalOf`, and derives a minimal Material 3 `ColorScheme` + `Typography` from it so `MaterialTheme.*` reads stay valid. Components read design values exclusively via `ReliveTheme.colors / typography / dimensions / motion`; no raw colors, sizes, or durations in UI code. Warm Journal is the only concrete token set in Phase 0; `MonochromeArchive` and `FilmMemory` are declared ids that currently resolve to Warm Journal until their palettes are transcribed.
- **Consequences:** Adding a new theme is a token-set addition plus a `when` branch — no component changes. Non-color tokens (spacing, radii, icons, stroke, timeline, media, opacity, motion) are shared, preventing per-screen drift. Material 3 semantics/accessibility are preserved. See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) and [`ARCHITECTURE.md`](ARCHITECTURE.md) §7–§8.

---

## ADR-0011 — Playfair Display + Inter bundled locally under the Compose Multiplatform resources system

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** `DESIGN_SYSTEM.md` §8 requires Playfair Display (serif) and Inter (sans) to be the actual families used at runtime, with no network font loading. Phase 0 must ship the completed design-system foundation, so temporary platform-family fallbacks are not acceptable.
- **Decision:** Both families are bundled as static TTF binaries inside the design-system layer under `shared/src/commonMain/composeResources/font/`, exposed to Compose only through the internal `rememberReliveSerifFamily()` / `rememberReliveSansFamily()` helpers. `ReliveTheme` composes the typography scale from those families and publishes it via `ReliveTheme.typography`. Only the weights/styles actually referenced by the token mappings are bundled: Playfair Display Regular + Italic, and Inter Regular + Italic + Medium + SemiBold. Both fonts are licensed under SIL Open Font License 1.1; the full license texts are checked into the repository at `shared/licenses/fonts/`. Neither the resource filenames nor the generated `Res` class are exposed outside the `com.vaibhav.relive.ui.theme` package — the Compose resources module is generated with `publicResClass = false`.
- **Consequences:** Visual fidelity to the approved UI reference is achieved without any runtime network dependency. Adding a new typography weight is a design-system-layer change: bundle the TTF, add one `Font(...)` entry in `ReliveFonts.kt`. Redistribution obligations are satisfied by shipping the OFL text alongside the binaries. If a future theme (Monochrome Archive, Film Memory) needs different families, they are declared the same way and injected via `ReliveTheme` — components remain unchanged.

---

## ADR-0012 — Moment minimum validity: at least one of title, content, or media

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** PRODUCT_SPEC §4 lists title, content, media, tags, and location as moment fields and confirms text-only moments are supported (§4). It does not explicitly define whether a moment with an empty title AND empty content AND zero attachments is valid. Such a moment would render as a bare timeline dot with nothing to relive, which contradicts the product's "beautiful personal life archive" feeling.
- **Decision:** A moment is semantically valid only if at least one of the following is non-empty: title (non-blank), content (non-blank), or attachments (≥ 1). This rule is enforced in the domain layer by `MomentValidation.validate` and returns a structured result (`Ok` / `Invalid(reasons)`). Structural invariants (unique attachment ids/sortIndex, no duplicate tags in canonical form, `updatedAt >= createdAt`, coordinates present as a pair) are enforced in the constructors of `Moment`, `MediaAttachment`, and `ReliveLocation`. User-entered title/content is preserved verbatim; the only trimming performed is on tag input, and the original label is retained for display.
- **Consequences:** The composer / presentation layer can rely on `MomentValidation` to decide whether a "Keep Moment" tap is allowed. Media-only and text-only moments both remain valid. If PRODUCT_SPEC is later refined to permit truly empty moments (or to add further required fields), this ADR is superseded by a new one.

---

## ADR-0013 — SQLDelight + SQLite as the local persistence engine

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Phase 1B needs a production-quality local database for Android and iOS with a shared schema and query definitions in `commonMain`. Requirements: normalized relational schema (moments once, timelines reference moments, many-to-many memberships, ordered attachments, tags queryable per-timeline), foreign-key enforcement, atomic aggregate writes, no platform APIs leaking into `domain`, no cloud/backend, no runtime code generation. The engine must run on Android (`AndroidSqliteDriver`), iOS (`NativeSqliteDriver` via Kotlin/Native SQLite), and on the JVM host (`JdbcSqliteDriver`) for `androidHostTest`.
- **Decision:** Adopt **SQLDelight 2.1.0** with SQLite as the storage engine. The schema and queries live as `.sq` files in `shared/src/commonMain/sqldelight/com/vaibhav/relive/data/local/db/`; SQLDelight generates typed `ReliveDatabase` + `*Queries` classes at build time under package `com.vaibhav.relive.data.local.db`. Driver construction is platform-specific behind an `expect class DatabaseDriverFactory` (Android actual takes a `Context`, iOS actual is parameterless) and every driver flows through `ReliveDatabaseFactory.create(driver)`, which unconditionally runs `PRAGMA foreign_keys = ON` before returning the typed database. Repositories (`SqlDelightMomentRepository`, `SqlDelightTimelineRepository`, `SqlDelightTagRepository`) implement domain-owned interfaces in `com.vaibhav.relive.domain.repository`; `domain` code depends on no SQLDelight-generated types. `kotlinx.coroutines` is added to expose `Flow`-based observers via SQLDelight's `coroutines-extensions`.
- **Schema decisions:**
  - **Six tables** — `moments`, `custom_timelines`, `moment_timeline_memberships`, `tags`, `moment_tags`, `media_attachments`. `Timeline.All` is never stored — it is served by `MomentRepository.listAll()`; the API rejects `All` as a membership target.
  - **Foreign keys** — enabled at connection level. `moment_timeline_memberships`, `moment_tags`, and `media_attachments` cascade on moment delete. `custom_timelines` cascade on membership only; deleting a custom timeline never deletes moments. `moment_tags → tags(canonical)` uses `ON DELETE RESTRICT` so a referenced tag row cannot be dropped; unused tag rows are cleaned up explicitly by `TagRepository.pruneUnused()`.
  - **Composite primary keys** — `moment_timeline_memberships (moment_id, timeline_id)` and `moment_tags (moment_id, tag_canonical)` enforce membership/tag-link uniqueness. `media_attachments` has a UNIQUE(moment_id, sort_index) constraint so attachment ordering is deterministic and never collides.
  - **Tags** — canonical form (lowercased, whitespace-collapsed, trimmed) defines tag identity and is the primary key. The **first persisted display label wins**: insertion uses `INSERT ... ON CONFLICT(canonical) DO NOTHING`, so once a canonical row exists, attaching a later equivalent tag (differing only in case or whitespace, e.g. `Travel` → `travel` → `TRAVEL`) reuses the existing row without mutating its display label. This keeps historical UI presentation stable regardless of what a later moment typed. Editing a tag's display label is out of scope for the persistence layer and would require an explicit rename operation.
  - **Time** — persisted as `INTEGER` epoch milliseconds via the domain's `Instant(epochMilliseconds: Long)` value class. No timezone or calendar semantics live in persistence.
  - **Identity** — string PKs matching Phase 1A domain id types (`MomentId`, `TimelineId`, `MediaAttachmentId`). No database-generated numeric ids.
  - **Location** — coordinates and human-readable parts sit in the moments row itself (six nullable columns). Coordinate presence is validated by `ReliveLocation` on the way back out; the row-decoder returns `null` only when both coordinates and every readable field are absent.
  - **Indexes** — `moments(created_at)`, `moments(updated_at)`, `custom_timelines(created_at)`, per-side indexes on both membership tables and `moment_tags`, `media_attachments(moment_id)`. Sufficient for the timeline-scoped queries described in `PRODUCT_SPEC.md` §9; FTS is deliberately deferred until Phase 7's search UI can justify the added schema surface.
- **Consequences:** All persistence is on-device SQLite, no runtime SQL is hand-written outside SQLDelight, and every mapper is loud on corrupt values (`PersistenceMappingException`). Adding a table or query is a `.sq`-file edit; changing the schema requires a SQLDelight migration alongside `verifyMigrations = true`. Coroutines becomes a shared-module dependency, which is required for Phase 2+ anyway. Media binaries are still filesystem-owned — only opaque `MediaStorageRef` strings live in the database. Adopting SQLDelight is consistent with ADR-0002 (KMP + Compose MP) and preserves ADR-0004 (moments stored once; timelines reference moments; `All` is logical). See [`ARCHITECTURE.md`](ARCHITECTURE.md) §5.

---

## ADR-0014 — Platform date formatting via `expect/actual`, no new date dependency

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Phase 2 must show each moment's `createdAt` as an editorial eyebrow (e.g. `SEPTEMBER 28, 2023`) in the user's device time zone. The domain's `Instant` is a raw epoch-millisecond value class (`domain/time/ReliveTime.kt`); it deliberately carries no calendar or timezone semantics. Options considered: (a) add `kotlinx-datetime` to the shared module, (b) hand-roll Gregorian conversion in `commonMain`, (c) declare an `expect object EditorialDateFormatter` and implement it per platform. `kotlinx-datetime` is explicitly deferred for this phase, and hand-rolled Gregorian arithmetic is disallowed by the Phase 2 brief.
- **Decision:** Introduce `presentation.date.EditorialDateFormatter` as an `expect object` in `commonMain` with `fun format(instant: Instant): String`. The Android actual uses `java.text.SimpleDateFormat("MMMM d, yyyy", Locale.US)` bound to `TimeZone.getDefault()`, uppercased. The iOS actual uses `NSDateFormatter` with `en_US_POSIX`, `dateFormat = "MMMM d, yyyy"`, `timeZone = NSTimeZone.localTimeZone`, then `uppercaseStringWithLocale(en_US_POSIX)`. Both consume the domain `Instant` unchanged; no `kotlinx-datetime` dependency is added, and no Gregorian math lives in shared code. The formatter belongs to the presentation layer (it exists solely to build the string a UI reads), not to the domain, so the moment model stays platform-agnostic per ADR-0007.
- **Consequences:** Editorial dates are produced by well-tested platform calendar libraries and always render in the device's local time zone. Adding another formatted date variant is a one-line addition per platform actual. If a later phase legitimately needs cross-platform calendar arithmetic (e.g. Phase 6's 4-day rule already uses raw milliseconds, but future features like grouping by day/month may not), that phase records its own ADR — potentially introducing `kotlinx-datetime` at that point — rather than reversing this decision retroactively.

---

## ADR-0015 — Chronological timeline order applied at the presentation layer

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** ADR-0013's SQLDelight schema exposes `MomentRepository.observeAll()` as newest-first (`ORDER BY created_at DESC`), which is the canonical persistence contract. Phase 3 needs the All timeline to read like a WhatsApp chat: oldest moment at the top, newest saved moment at the bottom, with the inline composer pinned as the final timeline item immediately after the newest moment, and the initial viewport landed at the latest/bottom end so a newly kept moment appears just above the composer without a long animated scroll through history.
- **Decision:** Keep the repository ordering contract untouched. `AllTimelineViewModel` reverses the emitted list once (`moments.asReversed()`) before publishing it as `AllTimelineUiState.Loaded`, so presentation renders oldest → newest top-to-bottom. `AllTimelineScreen` renders the moments inside a `LazyColumn`, with the composer as the terminal `item("composer")` after the moment `items(...)`. A `rememberLazyListState()` tracks the item count: on the first non-empty snapshot the list `scrollToItem(moments.size)` (no animation) lands the user at the composer; on subsequent growth it `animateScrollToItem(moments.size)` so a freshly kept moment slides into view directly above the composer. SQLDelight ordering is never changed for UI reasons.
- **Consequences:** The persistence contract stays newest-first (cheap `LIMIT`/pagination for later phases still works), and any other consumer of `observeAll()` continues to receive that order. Presentation owns the visual chronology; changing the top/bottom orientation later is a single VM change, not a schema migration. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §7 and [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §5.

---

## ADR-0016 — Keyboard-aware inline composer (no detached floating composer)

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** The composer is the final item in the timeline `LazyColumn`, not a separately positioned surface. When a composer input receives focus the IME appears and could either (a) obscure the active field, (b) force the app to hoist a detached floating composer above the keyboard, or (c) rely on hardcoded keyboard-height constants that break across devices and platforms. Options (b) and (c) both fragment the layout and diverge from the "timeline reads as one document" feel.
- **Decision:** The composer remains a normal timeline item. `AllTimelineScreen` applies `Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))` at the timeline container, so the visible list is inset by whatever the IME actually reports. Compose's default focus-based auto-scroll then brings the focused field above the keyboard using the reported inset — no hardcoded heights, no manual `WindowInsets.ime.getBottom(...)` arithmetic, and no measurement of keyboard chrome anywhere in the codebase. Scrolling occurs only as needed to reveal the focused input; closing the keyboard collapses the IME inset back to zero and the timeline layout returns to its normal resting state.
- **Consequences:** One layout handles keyboard-open and keyboard-closed states; there is no second composer surface to keep in sync. Behavior is correct on any device the platform IME insets are correct on (Android IME animation callbacks, iOS keyboard notifications). If a future phase needs a truly overlayed composer (e.g. an expanded modal editor), it will be an additive surface, not a replacement of the inline one. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §7.

---

## ADR-0017 — `SystemClock` and `UuidGenerator` as presentation-layer platform seams

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** The composer needs a wall-clock reading (to stamp `createdAt`/`updatedAt` on a kept moment and to render the "now" eyebrow while composing) and a fresh identifier (to build `MomentId` for the persisted row). Both are inherently platform-provided. Options considered: (a) call `java.util.UUID`/`NSUUID` and `System.currentTimeMillis()`/`NSDate` directly from shared code — violates ADR-0007 by importing platform APIs into `commonMain`; (b) add `kotlinx-datetime` and a UUID multiplatform library to `shared` just for this — pulls two dependencies for one clock read and one string; (c) declare thin `expect object` seams that resolve to the platform primitive per target.
- **Decision:** Introduce two `expect object` seams in the presentation layer: `presentation.time.SystemClock : Clock` and `presentation.id.UuidGenerator : IdGenerator`, actualised in `androidMain` (JDK `System.currentTimeMillis()` / `java.util.UUID.randomUUID().toString()`) and `iosMain` (`NSDate.timeIntervalSince1970` / `NSUUID().UUIDString`). Both are exposed via the shared `ReliveAppContainer` (`clock`, `idGenerator`) and threaded down through `App` → `AllTimelineScreen` → `MomentComposerViewModel`. Tests substitute deterministic fakes at the container/VM boundary. The domain contracts (`domain.time.Clock`, `domain.id.IdGenerator`) stay platform-free and dependency-free.
- **Consequences:** Shared UI and shared tests never touch a platform API; deterministic composer tests do not need to mock time or generate UUIDs from real sources. No new shared-module dependency is added. Adding a third target (JVM, wasm, desktop) is a two-file `actual` addition, not a shared-code change. If a future phase legitimately needs richer calendar arithmetic or UUID semantics, it may introduce `kotlinx-datetime` / a UUID library then, superseding this ADR — but the composer itself will continue to depend only on the `Clock` / `IdGenerator` interfaces. Complements ADR-0007 and ADR-0014.

---

## Template for new decisions

```
## ADR-XXXX — <short title>
- **Date:** YYYY-MM-DD · **Status:** Proposed | Accepted | Superseded
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
