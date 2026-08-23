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

## ADR-0018 — Phase 4 media pipeline: optimized app-owned copies, non-destructive themes, platform-native compression

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Phase 4 introduces media capture (mic, camera) and library import into the composer, together with saved-timeline rendering. Relive is local-first and expected to accumulate years of moments on a personal device; keeping originals as source of truth would waste storage and expose Relive to arbitrary source formats and orientations. Camera/library flows also have to avoid touching the user's external files, and theme effects (Warm Journal / Monochrome Archive / Film Memory) must remain a rendering concern so a user changing themes later never rewrites their archive.
- **Decision:**
  1. **Optimized app-owned copies are the source of truth.** Every captured or imported blob is normalized/compressed by `MediaProcessor` into an app-owned file addressed by an opaque relative [`MediaStorageRef`](../shared/src/commonMain/kotlin/com/vaibhav/relive/domain/model/MediaAttachment.kt). Only the processed copy is persisted; no parallel "original" is kept. The user's external source file is never modified or removed.
  2. **Theme effects are non-destructive rendering only.** No sepia, monochrome, grain, or vintage color treatment is baked into the stored file. Themes apply via `ReliveTheme` at render time (see ADR-0010).
  3. **Temporary → processed → committed attachment lifecycle.** Media captured/imported is first written to a Relive-owned temp file; `MediaProcessor` produces a processed file with a fresh `MediaStorageRef`; that ref becomes a `DraftAttachment` in composer state. On successful Keep Moment, the file stays in place and the ref is written to the DB as-is (no second copy). Composer reset, per-attachment `×`, recording cancel, or processing failure all delete their draft files. A DB save failure preserves the draft files so retry does not recompress or duplicate.
  4. **Platform capabilities behind shared interfaces (ADR-0007).** New shared interfaces: [`MediaStore`](../shared/src/commonMain/kotlin/com/vaibhav/relive/platform/media/MediaStore.kt), [`MediaProcessor`](../shared/src/commonMain/kotlin/com/vaibhav/relive/platform/media/MediaProcessor.kt), [`AudioRecorder`](../shared/src/commonMain/kotlin/com/vaibhav/relive/platform/media/AudioRecorder.kt) (via `expect fun createAudioRecorder`), [`MediaPickerHandle`](../shared/src/commonMain/kotlin/com/vaibhav/relive/platform/media/MediaPicker.kt) (via `@Composable expect fun rememberMediaPickerHandle`), [`CameraCaptureSurface`](../shared/src/commonMain/kotlin/com/vaibhav/relive/platform/media/CameraCapture.kt) (`@Composable expect`), and playback composables (`RelivedImage/RelivedVideo/RelivedAudio`). Shared domain/presentation code never imports `android.*`, `AVFoundation`, `UIKit`, or file APIs.
  5. **Compression profiles.**
     - **Image** — orientation-corrected via EXIF, downscaled to ≤ 1920 px long edge (never upscaled), JPEG @ 82% quality. Android: `BitmapFactory` + `android.media.ExifInterface` + `Bitmap.compress`. iOS: `UIImage` (respects embedded orientation) + `UIImageJPEGRepresentation(0.82)`.
     - **Video** — H.264 video / AAC audio, ≤ 720p target, downscale-only. Android: **AndroidX Media3 Transformer 1.5.1** (`Presentation.createForHeight(720)`, `VIDEO_H264`, `AUDIO_AAC`). iOS: `AVAssetExportSession` with `AVAssetExportPreset1280x720`, MPEG4 output.
     - **Audio** — recorded as AAC/M4A at 64 kbps mono, 44.1 kHz. Android: `MediaRecorder` (MPEG_4 / AAC). iOS: `AVAudioRecorder` with `kAudioFormatMPEG4AAC`. Imported audio is copied through without re-encoding (already voice-appropriate on both platforms in practice).
  6. **Camera UX — single-camera Photo/Video switch.** Android: **AndroidX CameraX 1.4.2** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-video`) with a shared `PreviewView`, `ImageCapture` + `VideoCapture(Recorder)` bound on the fly as the user toggles Photo/Video. iOS: `UIImagePickerController` with `sourceType = .camera` and `mediaTypes = ["public.image", "public.movie"]`, which is the native single-camera Photo/Video experience. System intents (`ACTION_IMAGE_CAPTURE`/`ACTION_VIDEO_CAPTURE`) were rejected because they cannot present the required in-camera mode switch.
  7. **Library UX — Photo / Video / Audio.** Tapping Library opens a small sheet with three choices, then launches the appropriate modern system picker: Android `PickVisualMedia` (image-only or video-only) for photo/video, `OpenDocument(["audio/*"])` for audio (no broad storage permission); iOS `PHPickerViewController` (no permission prompt) for image/video, `UIDocumentPickerViewController` for audio.
  8. **Playback — native only.** Android uses `MediaPlayer` on a `SurfaceView` (video) and `MediaPlayer` (audio). iOS uses `AVPlayer` + `AVPlayerLayer` (video) and `AVAudioPlayer` (audio). No Media3 UI / ExoPlayer UI is added; Media3 is present only as the Transformer backend.
  9. **Live recorder waveform is real.** `AudioRecorder.state: StateFlow<RecordingState>` exposes amplitude samples polled from the platform microphone (Android `MediaRecorder.getMaxAmplitude`, iOS `AVAudioRecorder.averagePowerForChannel` in dB → normalized). Samples are kept in a rolling window (96) so recomposition cost stays bounded. **Saved-audio waveform generation is deferred**: the timeline audio surface plays without a waveform rather than fabricating one.
  10. **Add Media actions are exactly Mic / Camera / Library.** The composer never surfaces top-level Photo or Video actions; those choices happen inside Library and Camera respectively.
- **New dependencies:**
  - `androidx.camera:camera-{core,camera2,lifecycle,view,video} = 1.4.2` — required for §Decision 6 (single-camera Photo/Video); system intents cannot deliver that UX.
  - `androidx.media3:media3-{transformer,common,effect,exoplayer} = 1.5.1` — required for §Decision 5's video normalization on Android; `exoplayer` is a transitive requirement of `transformer`.
  - `androidx.activity:activity-compose = 1.13.0` (added to shared `androidMain`; already used by `androidApp`) — for `rememberLauncherForActivityResult` bindings in the picker.
  - No other new media libraries. No ExoPlayer UI, no third-party transcoder, no iOS camera library.
- **Consequences:** Adding a new stored media kind means: a new `MediaType`, a `MediaStore.extensionFor` branch, a `MediaProcessor` branch, an `AudioRecorder`/`CameraCapture`/`MediaPicker` extension, and a `Relived*` composable. Themes can change freely without rewriting stored blobs. Original library files are never mutated (`copyToTmp` on Android, `PHPickerViewController` file-representation copy + `UIDocumentPickerViewController` security-scoped copy on iOS). Retrying a failed Keep Moment reuses the same processed files — no re-encoding. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §6.2.

### Addendum — Camera polish (front/back, flash/torch, feedback)

- **Date:** 2026-08-21 · **Status:** Accepted, extends §Decision 6.
- **Rear camera is the default.** `CameraSelector.DEFAULT_BACK_CAMERA` on Android; iOS `UIImagePickerController` defaults to rear as well.
- **Front/back switching.** Android exposes an in-camera switch pill (rear ↔ front) that rebinds the same CameraX `Preview`/`ImageCapture`/`VideoCapture` use cases to the new `CameraSelector`; no camera screen re-open. Switch is hidden when `ProcessCameraProvider.hasCamera(DEFAULT_FRONT_CAMERA)` returns false, and disabled while a `VideoCapture` recording is in progress so rebinding never interrupts a capture. iOS gets the same behavior for free via `UIImagePickerController`'s native switch control.
- **Flash / torch — two states, one icon.** Both Photo and Video modes expose the same icon-only control with exactly two states: Off (outlined bolt) and On (filled bolt). No Auto. No text labels ("Flash On", "Torch On" and friends are all removed). Tap toggles Off ↔ On in both modes. Photo mode maps state to `imageCapture.flashMode ∈ {OFF, ON}`; Video mode maps state to `camera.cameraControl.enableTorch(true|false)` so the torch stays lit while recording. Default is Off. If the bound lens's `cameraInfo.hasFlashUnit()` is false (typical for the front lens), the icon is **shown muted (0.4 alpha) and inert** — taps do nothing — and any lingering On state is coerced to Off after rebind so the UI and hardware stay in sync. Screen-brightness "fake flash" is deliberately not implemented. On iOS Relive does not draw its own camera chrome — the native `UIImagePickerController` renders its own flash control (Off / On / Auto) and Relive does not override `picker.cameraFlashMode`. The scope of the two-state policy above is the Android in-camera surface where Relive is drawing the icon itself.
- **Camera switch.** Bottom-right control is a minimal two-arrow rotate glyph — no camera-body chrome, no text. In addition, **double-tap on the preview surface toggles front/back**, matching messaging-app cameras. Both entry points are disabled while a `VideoCapture` recording is in progress so a rebind never interrupts a capture, and both no-op if the device reports no front lens.
- **Feedback policy — platform-native sounds, no bundled assets.** All capture feedback comes from OS-provided sources; Relive bundles no manufacturer shutter/beep audio files, and none of these paths bypass silent-mode or regional camera restrictions (`MediaActionSound` is exactly the API the platform hands to camera apps for that purpose).
  - **Android — why not `MediaActionSound`.** The first iteration used `MediaActionSound.SHUTTER_CLICK` / `START_VIDEO_RECORDING` / `STOP_VIDEO_RECORDING`. Physical-device testing showed it is inaudible on Android 12+ and every OEM skin we tried (One UI, MIUI, ColorOS): `MediaActionSound` routes through `STREAM_SYSTEM_ENFORCED`, which those platforms gate to system-signed camera apps — `play()` returns cleanly, no audio reaches the speaker, and the media-volume slider does not affect it. Rejected.
  - **Android — implementation.** `android.media.ToneGenerator` on `AudioManager.STREAM_MUSIC` (volume 80/100). Uses the media-volume slider the user actually controls, works uniformly across OEMs, needs no bundled audio assets, and stays inside the "OS-provided" bucket. Tone codes: photo → `TONE_PROP_ACK` (90 ms), video start → `TONE_PROP_PROMPT` (140 ms), video stop → `TONE_PROP_BEEP2` (160 ms). Each `startTone(...)` call is wrapped in a try/catch since the `ToneGenerator` constructor is documented to throw `RuntimeException` when no `AudioTrack` is free; the whole feedback path degrades to haptic-only in that case rather than crashing camera.
  - **Android — photo capture.** Tone + `VibrationEffect.EFFECT_CLICK` haptic fire inside CameraX's `onImageSaved` callback (real success only, never on tap-with-failure). Photo capture does not open the mic, so no audio-contamination risk.
  - **Android — video start (mic-safe ordering).** Tap on shutter plays the start tone and haptic, then a `rememberCoroutineScope` coroutine `delay(START_TONE_MS + START_TONE_GUARD_MS)` (140 + 80 ms) **before** calling `videoCapture.output.prepareRecording(...).start(...)`. CameraX opens the `AudioSource` inside `.start(...)`, so by the time the mic is live the speaker is silent and the tone cannot bleed into the video's audio track. The perceptible delay before recording begins is ~220 ms — acceptable UX cost for a clean recording. The trailing `Video Start` event fires from CameraX's `VideoRecordEvent.Start` as before but no longer plays audio (it would land inside the recording window).
  - **Android — video stop.** `VideoRecordEvent.Finalize (no error)` fires **after** CameraX has released the microphone; only then do we play the stop tone and haptic, so the stop tone cannot enter the recorded track.
  - **iOS.** `UIImagePickerController` provides the native shutter click, recording-start tone, and recording-stop tone itself. Relive does not layer additional sounds on top; iOS's own routing already excludes those tones from the recorded video audio track.
- **Lifecycle.** On camera dispose, back, cancel, or a failed video record, `boundCamera.cameraControl.enableTorch(false)` runs before `provider.unbindAll()`, and any in-flight temp file is deleted. `ToneGenerator.release()` runs on dispose (guarded in try/catch) so the audio resource never leaks past the screen.
- **Layout.** Bottom safe-drawing bar carries Cancel | shutter | camera-switch; flash icon sits top-left. No top `×` (would hide behind cutouts). All controls use `WindowInsets.safeDrawing` — no hardcoded status/nav bar heights. (Superseded by the WhatsApp-style polish addendum below; the polish layout drops the Cancel pill entirely — system Back is the sole cancel path — and reorganizes the bottom controls.)
- **No new dependencies.** All of the above uses APIs already available via the CameraX 1.4.2 stack and platform Android SDK.

### Addendum — WhatsApp-style camera polish

- **Date:** 2026-08-21 · **Status:** Accepted, extends the addendum above.
- **Layout.** The Android camera surface now follows a WhatsApp-style vertical hierarchy: full-screen preview at the back, a small flash icon in the upper-left preview corner, and a black bottom control bar that hosts (top → bottom) zoom presets, the main control row (Gallery / Filter / Shutter / Switch), and the Photo/Video mode selector. All chrome insets from `WindowInsets.safeDrawing` — no hardcoded status/nav-bar heights, no Relive cream/brown styling inside the camera.
- **Capture button is the fixed geometric center.** The main control row is a `Box(fillMaxWidth)` with the shutter placed at `Alignment.Center`; Gallery+Filter pin to `Alignment.CenterStart` and Switch pins to `Alignment.CenterEnd`. A `Row(SpaceBetween)` would let the shutter drift whenever left/right widths differ — that path is deliberately not used. The zoom preset row above the shutter and the Photo/Video selector below are each independently centered on the same horizontal axis.
- **Gallery + Retro filter controls.** The Gallery control is a dark circular button with a stacked-photos glyph; it dismisses the camera and hands off to the composer's existing Library sheet (Photo / Video / Audio) via a new `onOpenGallery` callback on `expect fun CameraCaptureSurface`. The iOS actual ignores the callback — `UIImagePickerController` already surfaces the on-device library from its own controls. Retro filters are represented by a dark circular sparkle glyph that is currently inert; the full retro-filter processing system is reserved for a future phase and no filter dependencies are added.
- **Cancel.** The explicit Cancel pill is removed. System Back and camera dispose remain the only cancel paths and preserve the existing behavior (stop any active recording, delete any in-flight temp file, disable torch, unbind CameraX, return to composer without leaving Relive).
- **Zoom (Pixel-style dynamic presets + live ruler).** The bottom slot holds one of two visual states in the same fixed-height container so the shutter row never shifts. Normal state renders three zoom slots {Low, Mid, High} via `zoomSlotsFor(min, max)`, filtered by the *actual* CameraX `zoomState` range of the currently bound lens — 0.5× (Low) only when the lens's real `minZoomRatio ≤ 0.5`, 2× (High) only when `maxZoomRatio ≥ 2`, no upscaled or faked digital 0.5×. `activeZoomSlot(ratio, slots)` chooses the highlighted slot; that slot displays the exact current ratio via `formatZoomLabel` (e.g. `0.7×`, `1.4×`, `3.5×`) while inactive slots show their anchor label. During a two-finger pinch the slot swaps to a live ruler (`ZoomRulerRow` — log-scaled tick strip with a current-value pill above); on release the ruler lingers ~850 ms, then presets return with the intermediate ratio still displayed in the appropriate slot. Preset taps clamp to the reported CameraX range via `clampZoomRatio(...)`. Pinch uses `awaitEachGesture` (not `detectTransformGestures`) so gesture start/end is observable and the ruler can swap cleanly.
- **Legacy zoom preset helpers.** `availableZoomPresets(min, max)`, `activePresetIndex(ratio, presets)`, `ZoomPreset` remain in `CameraControls.kt` for the earlier tests and for any future non-Pixel-style surface; the Android UI now uses `ZoomSlot`/`zoomSlotsFor`/`zoomSlotLabel`.
- **Zoom lifecycle across rebinds.** Default zoom is 1× when the lens supports it, otherwise `defaultZoomRatio(...)` falls back to the reported minimum. On a Photo↔Video rebind the current ratio is preserved when the new binding still reports the same range; on a Back↔Front rebind (where the front lens typically reports a 1×-only range) the ratio is coerced to the lens default. Pinch runs in its own `pointerInput` so it composes with the double-tap detector rather than swallowing it, and it does not trigger capture.
- **Camera switch.** The dedicated switch control is a minimal loop-arrow icon in a 48 dp dark circular surface, no text, hidden when the device reports no front lens, and disabled while a video recording is in progress. Double-tap on the preview surface remains an equivalent switch shortcut (single tap is a no-op — it never triggers capture; both entry points ignore taps while recording).
- **Flash.** Position, icon set, and two-state Off/On policy are unchanged from the prior addendum. The control now sits inside the upper-left of the preview and is not part of the bottom control bar, so it cannot disturb the shutter's geometric centering.
- **Photo/Video selector.** Bottom pill shows only `Video Photo` (in that visual order); Photo is selected by default; the selected chip fills a dark rounded surface with white text, unselected chips are transparent with white text. Video Note is intentionally absent. The selector sits above `WindowInsets.safeDrawing` so it never overlaps gesture or three-button navigation.
- **Feedback policy.** Unchanged from the prior addendum — `ToneGenerator` on `STREAM_MUSIC` for photo/video-start/video-stop, ordered so the start tone completes before CameraX opens the mic. Photo captures also guard against a re-entrant tap while a capture is in flight so a double-tap never enqueues two shutter events.
- **No new dependencies.**

---

## ADR-0019 — Timeline media uses an adaptive visual collage

- **Date:** 2026-08-21 · **Status:** Accepted
- **Context:** The timeline is an editorial memory journal, not a chat feed or generic attachment list. Media is a first-class part of a Moment and should be visually prominent. The existing warm Relive identity remains authoritative: cream journal canvas, timeline rail and dots, Playfair editorial titles, Inter body text, warm brown accent, chronological timeline, no enclosing card around an entire Moment. ADR-0018 §9 deferred saved-audio waveform generation. Phase 4's carousel model (PRODUCT_SPEC §5, DESIGN_SYSTEM §14, ROADMAP Phase 4) needs to be revisited: a horizontal carousel fragments the visual hierarchy, hides attachments behind swipe discovery, and does not express the editorial density the product demands. WhatsApp is a behavioral reference for efficient multi-media presentation only — Relive must not copy WhatsApp's chat bubbles, colors, typography, or chat visual language.
- **Decision:**

  **1. Adaptive collage replaces the carousel.**
  Multiple attachments in the timeline use an inline adaptive collage instead of a horizontal carousel/pager. This supersedes the carousel model described in PRODUCT_SPEC §5, DESIGN_SYSTEM §14, ROADMAP Phase 4, ARCHITECTURE §5, and TESTING §Phase 4.

  Collage rules by attachment count:

  | Count | Layout |
  |-------|--------|
  | 1 | Large responsive media tile using most of the available Moment content width. Single images preserve their natural aspect ratio as much as practical; portrait images remain meaningfully portrait rather than being squeezed into a short landscape viewport. |
  | 2 | Two equal visual tiles side-by-side. |
  | 3 | Asymmetric composition: one dominant tile + two smaller vertically stacked tiles. |
  | 4 | 2×2 grid. |
  | 5+ | First four tiles rendered inline; the fourth tile receives a translucent `+N` overlay representing the remaining attachment count. |

  Attachment order is deterministic and follows the Moment attachment ordering contract (ADR-0013 `UNIQUE(moment_id, sort_index)`). Grid tiles may use controlled cropping to maintain collage geometry, but media must never be stretched or distorted.

  **2. All media types participate in the collage.**
  The adaptive collage is not image/video-only. All three existing media types — Image, Video, Audio — are first-class visual timeline tiles. A Moment may therefore contain mixed layouts such as Photo|Audio or Photo|Video / Audio|Photo. Audio must not be moved into a separate generic player underneath the collage.

  **3. Video presentation in the timeline.**
  Video tiles occupy the same grid system as images. Each video tile shows a representative thumbnail/frame with a subtle Play affordance. No autoplay. No persistent active video player merely because a tile is visible. Actual consumption occurs through the media viewer (§7 below).

  **4. Audio visual identity in the timeline.**
  Audio is represented as a visual media tile, not a traditional horizontal audio player, equalizer bars, voice-message bubble, generic Material audio card, or random animated bars.

  *Audio canvas:* The tile is a large black/near-black media canvas. Inside that canvas is a compact, centered waveform visualization region. The waveform must not stretch across most of the black tile — the black negative space is intentional. The waveform region occupies approximately 45–60% of the available tile width, with generous space on the left and right and comfortable vertical space around it. Visual hierarchy: large black media canvas → compact centered sound visualization → minimal playback affordance.

  *Real waveform requirement:* The waveform represents the actual audio signal, not random data. Silence → nearly flat; quiet audio → small amplitude; normal speech/music → moderate amplitude; loud audio → larger peaks; sharp events → corresponding real peaks. The waveform communicates the structure of the actual saved recording. This is an architectural/design requirement, not decoration.

  *Waveform visual form (revised 2026-08-21):* The earlier "continuous waveform/sound trace" form is superseded. The waveform is a compact row of white vertical rounded-capsule amplitude segments on the black canvas, symmetric around the horizontal midline, uniform segment width, amplitude driving height only. Segments are not decorative equalizer bars — each segment's height comes from a real bucket of the extracted amplitude envelope, so silence renders as a tiny centered dot, quiet audio as short capsules, and loud peaks as tall capsules. Approximately 9–17 visible segments depending on tile width (smaller collage tiles get fewer segments; the same rounded-capsule identity is preserved). No random amplitude, no per-segment bounce animation, no bar-width variation.

  *Waveform paused state:* Real waveform window visible, segments stationary, minimal Play affordance, subtle duration indicator. Pausing does not reset the window.

  *Waveform playing state:* The visible segments represent a bounded window into the real envelope centered on the current playback position. As playback advances, past segments exit toward the left and future segments enter from the right — the shape flows horizontally through the viewport. Segment heights remain determined by real envelope data at every frame; there is no fabricated animation. Near the head and tail of the recording the window clamps to real data so playback still finishes at the actual end of the envelope.

  *Waveform processing principle:* Saved audio is analyzed to derive a lightweight normalized/downsampled amplitude envelope: audio → decode/sample → amplitude envelope → normalize → downsample → reusable waveform representation. Do not repeatedly decode the entire audio file during Compose recomposition. Waveform information should be reusable/cached. This ADR does not introduce a database/schema change; if implementation later determines persistent waveform metadata is necessary, that requires a separate explicit architecture decision.

  **5. Full-screen media viewer.**
  Timeline = browsing memories. Media viewer = consuming media. Tapping a timeline media tile opens a dedicated dark/full-screen media viewer at the exact attachment that was tapped. If a Moment has multiple attachments, the viewer allows horizontal navigation through all attachments of that Moment. For 5+ attachments, tapping the `+N` tile opens the viewer and all hidden attachments remain accessible. No carousel in the timeline; swiping between attachments is allowed only in the dedicated viewer.

  **6. Image viewer.**
  Dark/black background. Image initially fitted appropriately, preserving aspect ratio/orientation. Pinch-to-zoom. Pan while zoomed. Double-tap zoom where practical. Android Back returns to the timeline without losing the user's scroll position. No editing controls.

  **7. Video viewer.**
  Dark viewing surface. Correct aspect ratio/orientation. Play/Pause. Playback progress/seeking. Audio. No timeline autoplay. Reuse existing media playback architecture where practical (ADR-0018 §8).

  **8. Audio viewer.**
  Full-screen audio preserves the same visual identity as the timeline audio tile: black viewing surface → larger but still intentionally bounded real waveform visualization → minimal Play/Pause → playback progress/duration. The real waveform remains synchronized with playback. It must not become a generic audio-player screen.

  **9. Performance constraints.**
  Timeline scrolling must remain smooth. Avoid: decoding original full-resolution images unnecessarily; constructing video players for every video tile; repeatedly decoding audio to regenerate waveforms; animating off-screen waveform tiles; eagerly loading hidden 5+ attachments; unnecessary recomposition caused by playback progress. Prefer: existing optimized Relive media copies (ADR-0018 §1); thumbnails/representative video frames; lazy media loading; cached/reusable waveform envelope data; playback resources created only when needed; lifecycle-aware cleanup. If practical, only one audio/video playback session should be active at a time.

  **10. Material 3 usage.**
  Material 3 should be used wherever it provides appropriate behavior/accessibility primitives: Play/Pause, navigation/back, interaction states, touch targets, semantics, standard controls. But Material 3 must not override Relive's visual identity (ADR-0003, ADR-0010). Do not introduce large icon dependencies merely for a few standard glyphs when lightweight/local vectors are sufficient.

  **11. Accessibility.**
  Minimum appropriate touch targets (DESIGN_SYSTEM §18). Meaningful content descriptions. Play/Pause state exposed semantically. Media type identifiable. `+N` exposes that more attachments are available. Interactions must not rely solely on color.

  **12. Explicit non-goals.**
  This decision does not change: timeline chronological ordering (ADR-0015), timeline rail/dots, composer, camera (ADR-0018 addenda), capture behavior, media compression policy (ADR-0018 §5), media persistence/storage semantics (ADR-0018 §1), favorite behavior, search (ADR-0006), settings, location (ADR-0008), or custom timelines.

- **Consequences:** The carousel model from PRODUCT_SPEC §5, DESIGN_SYSTEM §14, ROADMAP Phase 4, ARCHITECTURE §5, and TESTING is superseded for timeline media presentation. Those documents are updated to reference the adaptive collage. The `media.ratio.square`, `media.carousel.peek` tokens in DESIGN_SYSTEM §14 are superseded; collage geometry is defined by this ADR's layout rules. ADR-0018 §9's deferral of saved-audio waveform generation is superseded — this ADR commits to real waveform visualization as a design requirement (the specific waveform processing implementation remains a Phase 5+ task). Horizontal swiping between attachments is available only inside the full-screen media viewer, never in the timeline itself.

---

## ADR-0020 — Editorial time formatter via `expect/actual`, extending date eyebrow to `DATE • TIME`

- **Date:** 2026-08-22 · **Status:** Accepted
- **Context:** Phase 2 shipped with a date-only eyebrow (`SEPTEMBER 28, 2023`). Timeline and composer both benefit from showing time-of-day alongside the date. ADR-0014 established the `expect/actual` pattern for `EditorialDateFormatter`; the same pattern applies to time.
- **Decision:** Introduce `presentation.date.EditorialTimeFormatter` as an `expect object` with `fun format(instant: Instant): String`. Android actual uses `SimpleDateFormat("h:mm a", Locale.US)` with `TimeZone.getDefault()`; iOS actual uses `NSDateFormatter` with `dateFormat = "h:mm a"` and `localTimeZone`. The saved-moment eyebrow renders as `DATE • TIME` (e.g. `AUGUST 22, 2026 • 10:48 AM`) on a single row. Both formatters consume the same immutable `createdAt` in the device's local time zone. The `type.eyebrow` token description in `DESIGN_SYSTEM.md` is updated to reflect `DATE • TIME`.
- **Consequences:** Complements ADR-0014. No new dependencies. Consistent device-local time display across both platforms. The composer shows time in a separate label below the date using the same formatter.

---

## ADR-0021 — Inline-vs-fullscreen single-video playback rule

- **Date:** 2026-08-22 · **Status:** Accepted
- **Context:** Single-video Moments use adaptive natural sizing (ADR-0019 §1). Some videos fit within timeline bounds without being constrained; others must be scaled down. Inline playback is desirable when the video is already at comfortable viewing size, but loses value when the video was squeezed to fit constraints — the user would benefit more from immediate full-screen viewing.
- **Decision:** The adaptive sizing pass produces a `wasConstrained` flag. If the single video was **not** constrained (natural size fits within timeline max bounds): Play starts **inline** in the same adaptive bounds; body tap opens the full-screen viewer. If the video **was** constrained: both Play and body tap open the **full-screen viewer** directly; no inline player is created. Multi-media collage video tiles always navigate to the gallery/viewer. The flag is computed once per tile from `computeAdaptiveMediaPreview()` and does not change during scroll.
- **Consequences:** Small/naturally-fitting videos get quick inline playback. Large videos skip the compromised inline view and go straight to full-screen. No unnecessary player creation for constrained tiles. Complements ADR-0019 §3 and §9.

---

## ADR-0022 — Multi-media gallery as intermediate navigation surface

- **Date:** 2026-08-22 · **Status:** Accepted
- **Context:** ADR-0019 §5 specifies a full-screen viewer for media consumption. For Moments with 2+ attachments, opening the viewer directly from a collage tile would skip the opportunity to see all attachments at once. For single-attachment Moments, the gallery adds no value — the viewer alone suffices.
- **Decision:** Navigation hierarchy by attachment count: **1 attachment** — timeline tap opens the full-screen viewer directly. **2+ attachments** — timeline collage tap (including `+N`) opens a dedicated `MomentMediaGallery` first, showing all attachments for that Moment in a vertically scrollable adaptive grid on a dark surface. Tapping a gallery item opens the full-screen viewer at that index. Viewer Back returns to the gallery (scroll position preserved). Gallery Back returns to the timeline (scroll position preserved). State is tracked via `TimelineMediaNavState`, which holds optional gallery and viewer overlays.
- **Consequences:** Gallery provides attachment overview for multi-media Moments without cluttering the single-attachment path. Scroll positions are preserved at every level. Gallery is a presentation concern; it does not change persistence or domain models. Complements ADR-0019 §5.

---

## ADR-0023 — Media presentation caching and single-owner playback

- **Date:** 2026-08-22 · **Status:** Accepted
- **Context:** Media-heavy timelines with images, video thumbnails, and audio waveforms can stall the UI thread if each tile eagerly decodes media, creates players, or repeatedly extracts waveforms. ADR-0019 §9 requires smooth timeline scrolling and lifecycle-aware cleanup.
- **Decision:**
  1. **`MediaPresentationCache`** — bounded in-memory cache keyed by `MediaStorageRef` for video thumbnails (`NaturalSizePx` + bitmap), image natural dimensions, and audio waveform envelopes. All cache hits are off-main-thread; cache misses trigger background extraction. Cache is bounded and entries evict under memory pressure.
  2. **Lazy player creation** — passive audio/video tiles in the timeline do not create `MediaPlayer`/`AVPlayer` instances. Players are instantiated only when the user taps Play.
  3. **`ActivePlayback` single owner** — at most one audio or video playback session is active at any time. Starting a new playback stops the prior one. Navigation (opening gallery, viewer, or returning to timeline) stops the active playback. Compose disposal releases the player.
  4. **Scoped recomposition** — playback progress updates are scoped to the playing tile; the rest of the `LazyColumn` does not recompose for playback ticks. Stable `MomentId` keys prevent unnecessary item recomposition during scroll.
- **Consequences:** Timeline scrolling performance is protected. Waveform data is reusable without re-decoding audio. Only one set of playback resources is live at any time. Off-screen tiles release resources on disposal. Complements ADR-0019 §9 and ADR-0018 §8.

---

## ADR-0024 — Timeline Home is the navigation root

- **Date:** 2026-08-22 · **Status:** Accepted
- **Context:** Relive supports All plus custom timelines, but the former in-detail selector was the only entry point. The product needs a memory-oriented collection layer without redesigning the approved Timeline detail UI.
- **Decision:** Timeline Home is the root screen. It renders a bounded, reactive summary for All and every custom timeline, then opens the existing `TimelineScreen` scoped to the selected timeline. Timeline detail retains its selector and receives a minimal Back affordance when entered from Home. Summary previews select at most four image/video attachments ordered by Moment `createdAt` descending then attachment `sortIndex` ascending; All remains a logical query.
- **Consequences:** The app has a clear Home → detail hierarchy while Moment ownership and membership remain unchanged. The read projection avoids per-card full-Moment hydration and uses existing thumbnail/cache infrastructure. Home remains the single creation entry point for this phase; no profile, settings, menu, or bottom navigation is introduced.

---

## ADR-0025 — Rediscover is a bounded local read model and second top-level destination

- **Date:** 2026-08-23 · **Status:** Accepted
- **Context:** Timeline Home was the app-wide root under ADR-0024. Relive now needs a calm resurfacing experience without turning it into another chronological timeline, backend recommendation system, or a full archive hydration path.
- **Decision:** Timelines and Rediscover are the two top-level destinations until Search and You are implemented. Timeline Home remains the root inside Timelines. Rediscover is a dedicated SQLDelight-backed read model: On This Day returns at most 20 current-device-local prior-year matches; From Your Past returns four deterministic daily selections at least 90 days old; Places and Tags rank existing persisted usage. No new table, cloud state, analytics, AI, or recommendation backend is introduced. Media presentation reuses passive cached primitives and opens the existing viewer on demand.
- **Consequences:** This supersedes ADR-0024 only where it names Timeline Home as the app-wide navigation root. Rediscover never calls `MomentRepository.observeAll()` and batch-hydrates selected tags/attachments to avoid N+1 reads. A user timezone change may change anniversary eligibility, matching existing device-local editorial date formatting.

---

## ADR-0026 — Rediscover starts with Favorites; resurfacing sections are deferred

- **Date:** 2026-08-23 · **Status:** Superseded by ADR-0028
- **Context:** The approved Rediscover read model is useful future infrastructure, but its On This Day, From Your Past, Places, Tags, and empty-state presentations are not the current product root.
- **Decision:** Rediscover renders only the Relive app bar and a persisted Favorites system-collection card above the existing two-item bottom navigation. The existing SQLDelight Rediscover repository, queries, calendar seam, and tests remain preserved for a future presentation phase, but the app does not construct `RediscoverViewModel` or collect its flows while the sections are absent. Favorites alone observes persisted Moments to calculate its real count.
- **Consequences:** The visible-section portion of ADR-0025 is superseded. No Rediscover title, subtitle, resurfacing section, or related empty state remains in the active root. The later implementation can reuse the preserved local read model without reintroducing persistence changes.

---

## ADR-0027 — Favorites is a reactive, read-only Rediscover system collection

- **Date:** 2026-08-23 · **Status:** Superseded by ADR-0028
- **Context:** Favorites is the first active Rediscover feature and must remain a direct expression of Moment favorite state rather than another user-managed timeline.
- **Decision:** SQLDelight observes the favorite count, favorited Moment scope, and a bounded four-item visual cover directly from `moments` and `media_attachments`. The card has a heart icon plus Moment count, never a visible Favorites label. Its nested detail reuses Timeline presentation through `TimelineMode.ReadOnlySystemCollection`, which removes all write affordances and rejects mutation requests at the presentation boundary while retaining media viewing/playback.
- **Consequences:** A favorite change anywhere updates Rediscover without duplicate persistence. Normal timelines retain full mutation capability. The deferred Rediscover overview projections remain uncollected at the active root, and debug QA remains isolated to the Android debug source set.

---

## ADR-0028 — Rediscover Favorites is a bounded per-Moment shelf

- **Date:** 2026-08-23 · **Status:** Accepted
- **Context:** The original Favorites collection card hid individual memories behind a single aggregate surface. Rediscover should instead provide a compact, editorial shelf that lets a person enter the read-only Favorites timeline at a specific memory without changing favorite persistence or collection semantics.
- **Decision:** Rediscover renders a `FAVOURITES` heading, followed by a horizontally swipeable row of at most ten individual favorited-Moment cards and a text `Show all` action. Each card uses the full Favorites timeline's chronological ordering, first ordered attachment as its lead visual, and an attachment-count indicator when needed. Tapping a card opens the read-only Favorites timeline positioned at that Moment; `Show all` opens the same timeline without a selected Moment. With no favorites, Rediscover shows the approved two-line empty state and no row or action. The shelf is fed by a dedicated bounded SQLDelight projection that batch-loads its attachments, rather than hydrating the complete collection or issuing per-Moment attachment reads.
- **Consequences:** The single aggregate Favorites card and its visual-cover query are superseded. Favorite state remains the only source of truth; no memberships, mutation controls, passive players, or full-resolution image decoding are added. The Favorites detail remains read-only and Back restores the Rediscover root's saved scroll state.

---

## ADR-0029 — Compact Favorite shelf cards share a fixed visual region

- **Date:** 2026-08-23 · **Status:** Accepted
- **Context:** ADR-0028 specified attachment-led compact cards, including no empty media frame for text-only Moments. The horizontal shelf now needs stable card geometry so image/video, text-only, and audio-only Favorites swipe as one visual collection.
- **Decision:** In the compact Rediscover Favorites shelf only, every card has the same fixed card and visual-region height. Image/video cards render the first ordered attachment. Text-only and audio-only cards render the existing theme-aware card surface as an empty visual placeholder; they contain no fake media, icon, or illustration. The lower region retains Moment information and a non-interactive favorite-status heart. Normal Timeline presentation and the read-only Favorites timeline are unchanged.
- **Consequences:** This supersedes ADR-0028 only for compact text-only/audio-only shelf media treatment. It does not introduce new data, behavior, media handling, or favorite mutation paths.

---

## ADR-0030 — On This Day is a bounded, read-only calendar anniversary collection

- **Date:** 2026-08-23 · **Status:** Accepted
- **Context:** Rediscover now needs its next shipped collection without changing the established local-first archive or the existing Favorites behavior.
- **Decision:** On This Day queries a bounded, attachment-batched local projection for Moments whose current-device-local month/day matches today in a previous local calendar year. The current year is excluded; February 29 remains exact. Root cards are horizontally swipeable featured cards with stable media/text/audio dimensions and exact calendar-year labels. Tapping opens the existing read-only system-collection timeline at the selected Moment.
- **Consequences:** No membership, duplicate Moment, mutation controls, autoplay, or full-archive hydration is introduced. Places and Tags remain deferred; From Your Past is subsequently activated by ADR-0031.

---

## ADR-0031 — From Your Past is a ten-Moment deterministic daily system collection

- **Date:** 2026-08-23 · **Status:** Accepted
- **Context:** ADR-0025 described an earlier deferred From Your Past projection with a limit of four Moments. Rediscover now needs that local resurfacing collection while preserving the approved Favorites, On This Day, navigation, and read-only system-collection architecture.
- **Decision:** From Your Past renders below On This Day and returns up to ten distinct Moments from a bounded SQL-backed query. Eligible Moments are at least 90 days old, are not future timestamps, and are not currently eligible for On This Day. A local-date-derived seed makes selection and its order stable for a local day and rotates it on the next day; no recommendations are persisted. The shelf reuses the compact Favorites card geometry and media treatment but has no heart. A card opens the exact daily selection in `TimelineMode.ReadOnlySystemCollection`, positioned at that Moment and retaining shelf order.
- **Consequences:** This supersedes ADR-0025 only for From Your Past's limit, which changes from four to ten, and its deferred presentation status. No backend, membership, mutation path, autoplay, or archive hydration in Compose is introduced. Places and Tags remain deferred.

---

## Template for new decisions

```
## ADR-XXXX — <short title>
- **Date:** YYYY-MM-DD · **Status:** Proposed | Accepted | Superseded
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
