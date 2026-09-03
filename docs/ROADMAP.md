# Relive — Roadmap

A phased plan from foundation to release. Work **phase by phase** ([`../AGENTS.md`](../AGENTS.md)). Phase boundaries may be refined when technically justified, but scope must **not** expand beyond [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md). Each phase ships with the tests described in [`TESTING.md`](TESTING.md) and records major decisions in [`DECISIONS.md`](DECISIONS.md).

Status legend: ☐ not started · ◐ in progress · ☑ done.

---

## Phase 0 — Foundation & design system  ☑

- Establish module layering in `shared/` (domain, data, platform, presentation, ui, di) per [`ARCHITECTURE.md`](ARCHITECTURE.md).
- Implement the tokenized design system and `ReliveTheme` (Warm Journal base) from [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md); bundle the brand serif + Inter (Fraunces + Inter as of the "Kept" typography redesign, ADR-0057).
- Replace the initial `App()` scaffold with the Relive app shell (header, canvas background) carrying the reference's warm editorial visual identity. The reference is authoritative for visual treatment only; it does not define the structure of the Home surface.
- Wire formatting + static analysis into the build.
- **Exit:** app builds on Android and iOS showing the themed shell; tokens in place; no feature logic yet.

## Phase 1 — Core memory domain & persistence  ☑

- Define domain models: `Moment`, `Timeline`, `MediaAttachment`, `Tag`, `ReliveLocation`, membership.
- Define repository interfaces and the `Clock` abstraction.
- Select and record the local database engine ([`DECISIONS.md`](DECISIONS.md) ADR-0013); implement schema: moments once, many-to-many timeline membership, attachment/tag references, All as a logical query.
- **Exit:** domain + persistence unit/persistence tests pass; CRUD at the repository level works.

## Phase 2 — Basic All moments feed  ☑

- Render the **All moments** feed: rail, dots, moment presentation (title, subtitle, content), newest-first order (ADR-0061 amends ADR-0015 for this feed), bounded/windowed/paged with incremental loading as the user scrolls toward older Moments — the root never observes or hydrates the complete archive on launch. Visual treatment matches the reference. There is no programmatic scroll on entry: the app opens at the top of the Home surface and the feed's visible position follows the user's own scrolling.
- Content expansion (`... more` / `less`).
- Favorite action (subtle).
- Date + time metadata eyebrow (`DATE • TIME`) with an optional readable saved location on the following metadata line.
- **Exit:** the All moments feed displays persisted text moments correctly under bounded paged loading; Compose UI tests for core rendering.

## Phase 3 — Inline composer (+ location data model)  ☑

- Inline composer with collapsed-by-default plus-circle marker; animated expand/collapse in place; auto date/time, title, content, tags, **Keep Moment**, reset `×` (collapses composer).
- Establish the **location data model and interfaces** (`ReliveLocation`, `LocationProvider`, `PlaceResolver`, `LocationResult`) in shared code — **before** platform GPS implementations.
- Composer location UI: show a lightweight manual place label below date/time; keep / remove / replace by editing. All fields optional; no raw coordinates in UI. Composer works fully with no location.
- Keyboard-aware inline composer (ADR-0016): IME insets keep active field visible.
- **Exit:** a text moment (optionally with manually entered location) can be composed inline and saved; new marker becomes a dot; tests for compose/save and the no-location path.

## Phase 4 — Media + platform GPS  ☑

- Media capture/storage interfaces + platform implementations (image, video, audio). Add Media flow (Mic / Camera / Library), per-attachment remove, adaptive visual collage (see [`DECISIONS.md`](DECISIONS.md) ADR-0019), correct aspect ratios; no empty placeholders.
- Single-media adaptive natural sizing; multi-media collage with border/gap tokens.
- Inline-vs-fullscreen video rule based on `wasConstrained` flag.
- Multi-media gallery → viewer navigation hierarchy. Single-attachment timeline tap opens viewer directly; 2+ opens gallery first.
- Audio waveform real-data visualization (rounded-capsule segments on black canvas).
- Camera with front/back switching, flash/torch, zoom presets/pinch, photo/video review, platform-native feedback sounds (ADR-0018 addenda).
- Audio recorder with Stop/waveform/duration/× row layout.
- Multi-select with stable draft identities, processing placeholders, bounded concurrency.
- Composer adaptive media previews with size-stable processing placeholders.
- `ActivePlayback` single-owner playback coordination.
- `MediaPresentationCache` for thumbnails, dimensions, waveform envelopes.
- Platform GPS implementations behind the Phase 3 interfaces are deferred by ADR-0038. Manual Moment location persists now; no permission, Maps, or geocoding flow is active and no background tracking is introduced.
- **Exit:** moments with multiple attachments render as an adaptive collage; manual location and media tests pass.

## Phase 5 — Custom timelines + Home surface  ☑

- The Home surface is the app root: the app always launches at the top of it (welcome, subtitle, `Relive your memories` and the Rediscover collection row, then `All moments` and its feed on one continuous scroll). All moments renders inline beneath its heading — there is no All card and no separate All detail screen. Visual custom-timeline cards still open the existing scoped timeline detail, and Create Timeline remains available from the top app bar.
- Create custom timelines; All moments is Home's own inline feed, never an entry in the custom-timeline card list.
- Filter the already-observed custom-timeline cards by live, case-insensitive partial name matching while preserving newest-first order; the inline All moments feed and Moment content are not part of this local filter.
- Membership rules: create-in-custom → All + that timeline; create-in-All → optional assignment to custom timelines.
- For custom timelines, opening an observed empty timeline expands its existing inline composer; once it has a Moment, normal entry remains collapsed. On the Home surface the All moments composer is entered through `+ New`, which enters focused All moments and expands the existing inline composer in place from the timeline rail — never a chooser, modal, dialog, bottom sheet, or separate composer screen — scrolling only enough to seat the composer and its Keep Moment button.
- **Exit:** moments appear in the correct timelines without duplication; membership tests pass.

## Phase 6 — Edit / forget rules  ☑

- Long-press in All moments opens a swift contextual app bar that animates in over Home's app bar in either Home state and needs no persistent header of its own: Edit / Forget remain within 4 days (keyed on `createdAt`), while add-to-custom-timeline organization remains available afterward. Back precedence on Home runs open contextual selection bar, then expanded inline composer, then the platform default.
- Inline editing (add/remove media while editing); tap-outside save that does not trigger on control interactions.
- Forget with confirmation → permanent removal.
- **Exit:** 4-day rule enforced everywhere; edit/forget and window-boundary tests pass.

## Phase 7 — Rediscover  ◐

- Add Rediscover as an in-Home section: a horizontally scrollable collection row under the `Relive your memories` heading on the Home surface. Rediscover is not a top-level destination and adds no navigation shell; do not introduce You placeholders.
- Render `Favourites` as the first card in the Home Rediscover row, opening the bounded read-only Favourites collection of individual favorited-Moment cards. The row sits inline on the Home surface with no top-level destination bar beneath it; the floating navigation toolbar's destination set is Home / Timelines / Search. The empty state remains visible when no favorites exist.
- Present `On This Day` and `From Your Past` as bounded cards following Favourites in that same horizontally scrollable row, each opening its read-only collection: On This Day covers eligible previous-year local-date matches with exact calendar-year labels and selected-Moment read-only navigation, and is omitted when no match exists; From Your Past is a deterministic daily local selection of up to ten Moments at least 90 days old, excluding current On This Day matches and future timestamps, with the same compact card family as Favourites and read-only selected-Moment navigation. Add `All Photos` as the fourth card: a bounded, read-only system collection of Moments with at least one image or video attachment, read through the same bounded projection pattern as Favourites and From Your Past — it adds no table, membership, or duplicate persistence, and it is not an entry point to the editable All moments feed, which already renders on Home. Preserve Places and Tags as deferred capability.
- **Exit:** The bounded Favourites, On This Day, From Your Past, and All Photos cards in the Home Rediscover row render reactively from persisted data, each opens its read-only system collection at a selected Moment, and deferred Rediscover projections remain tested and isolated from the row rendered on Home; every Home section — Rediscover row and All moments feed alike — stays bounded so the root never hydrates the complete archive.

Current implementation note: Favourites, On This Day, and From Your Past are implemented as bounded reactive cards in the Home Rediscover row with read-only collection navigation; All Photos is pending. Places and Tags remain deferred, and the phase stays in progress for final polish and verification.

### Approved Phase override — Unified Home surface

- Rediscover's collection row and the All moments timeline are one continuous, vertically scrollable Home surface, which is the app's single root (ADR-0061). This replaces this phase's `Timelines / Rediscover` top-level shell: Rediscover is no longer a destination, the navigation toolbar's destination set becomes Home / Timelines / Search — Timelines retaining Timeline Home and its custom-timeline detail screens unchanged — and the `Start Relive on` preference is removed because Home is always the landing destination.
- Home opens at the top. Its two states — the welcome/Rediscover top state and focused All moments — are scroll positions on one surface, never separate screens.
- The All moments feed on Home is newest-first, bounded, windowed and paged; the root never hydrates the complete archive on launch.
- This override adds no schema, dependency, backend, or duplicate composer.

## Phase 8 — Search  ◐

- Add Search as the third destination in the floating navigation toolbar (Home / Timelines / Search), with a dedicated, autofocus search screen; it reinstates no Rediscover destination.
- Search globally and locally across Moment title/content using case-insensitive SQL `LIKE`; results keep their own chronological ordering and the full timeline rail/card/media presentation; they are not the All moments feed, so Home's newest-first, bounded/paged rules do not govern them.
- Provide match count, up/down active-match navigation, scroll-to-Moment, and read-only results. Filters, categories, Places, Tags, ranking, history, and AI search remain out of scope for v1.
- **Exit:** Search v1 is SQL-backed, globally scoped, read-only, state-preserving across a Search round trip — returning restores the Home surface's scroll offset, including focused All moments, except where the Search Calendar action resolves a Moment — and covered by focused search/navigation tests.

Current implementation note: the dedicated autofocus screen, debounced SQL-backed matching, active-result counter/navigation, read-only timeline presentation, and preservation of the Home surface's scroll state across a Search round trip are implemented. The phase remains in progress for final verification.

## Phase 9 — Themes & settings  ◐

- System/Light/Dark appearance plus Original, Evergreen, Lilac Dusk, Crimson Keepsake, Blue Hour, and Rosewood token sets. Custom timelines persist a dedicated `TimelineAppearance` independently of global app appearance; themes change only presentation.
- Settings screen: Profile, Themes, Upgrade to Pro, Export (entries present; detailed behavior deferred where unspecified).
- Profile Media & Storage: read-only local archive storage/category insights; management actions remain deferred.
- Profile Preferences: native-local observable behavior settings for explicit composer-discard confirmation, editable-Timeline location/tag presentation, and visibility of the On This Day and Favourites cards within the Home Rediscover row. There is no startup-destination preference: Home is the single root, so the app always launches at the top of the Home surface. Fixed 12-hour time and passive media playback remain unchanged; 24-hour time and media autoplay controls are deferred.
- Timeline-owned appearance: editable All and custom timelines persist independent wallpaper selections and render the approved bundled wallpaper artwork without changing global appearance.
- **Exit:** switching themes changes only presentation; navigation/structure/interaction unchanged; theme tests pass.

Current implementation note: global appearance, Profile, Preferences, Media & Storage, Backup & Restore, Rediscover reminders, Privacy & Security/App Lock, Help, About/Licenses, custom-timeline themes, and All-timeline themes are present. Upgrade to Pro and Export remain entries/deferred behavior.

### Approved Phase override — Profile foundation

- Profile is implemented as an auxiliary destination opened from the Home surface, never a destination in the floating navigation toolbar; returning from Profile leaves Home's scroll offset untouched, including focused All moments. Its joining date is a singleton local `profile_metadata.created_at` written only for fresh databases; migrated databases retain an absent date and hide the Since line.

### Approved Phase override — Android external share capture

- Android system shares of supported Moment media/text may enter the existing composer through a timeline-selection surface. This is an additive Phase 4/5 capability: it adds no schema, dependency, duplicate composer, automatic persistence, or iOS share extension.

## Phase 10 — RevenueCat / Pro  ◐

- Implement the `relive_pro` entitlement behind a swappable shared interface using RevenueCat; wire upgrade and store restore flows.
- Offer monthly (`relive_pro_monthly`), annual (`relive_pro_annual`), and non-consumable lifetime (`relive_pro_lifetime`) billing choices. Configure annual trials store-side only.
- Gate scheduled automatic backup, creation beyond three custom timelines, and premium appearance; retain manual backup, every restore operation, existing archive access, Original/Warm Cream, and Evergreen/Sage Green in Free.
- Keep platform public API keys and product IDs configuration-driven. A missing key must degrade to Free without a crash.
- (Later/optional) RevenueCat Funnels + Stripe for web subscription conversion — see [`RELEASE.md`](RELEASE.md).
- **Exit:** Pro state drives the approved gates; entitlement is swappable; purchase/restore state is graceful when unavailable; monetization dependencies are isolated to this phase.

## Phase 11 — Production polish + Shipaton release  ☐

- Performance — including verified bounded/windowed/paged All moments loading so the Home surface never hydrates the complete archive on launch — accessibility, empty/edge states, and final visual polish. The reference governs warm editorial visual identity only; it is not authoritative for the structure of the Home surface, and the Android notification shade is an interaction reference for one continuous, reversibly scrollable surface, never a visual one.
- Release readiness per [`RELEASE.md`](RELEASE.md): Android release, iOS build/signing, screenshots/demo video, privacy checks, Shipaton submission.
- **Exit:** release candidate builds for both platforms; release checklist complete.

---

## Guardrails across all phases

- No unrequested dependencies or scope. No backend, sync, login, social, AI, analytics.
- Keep platform APIs out of shared business logic.
- Tests for important behavior; formatting + static analysis before completion.
- Never commit/merge/push without explicit approval.
