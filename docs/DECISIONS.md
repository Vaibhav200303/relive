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

## Template for new decisions

```
## ADR-XXXX — <short title>
- **Date:** YYYY-MM-DD · **Status:** Proposed | Accepted | Superseded
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
