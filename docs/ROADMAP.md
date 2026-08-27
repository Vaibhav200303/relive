# Relive — Roadmap

A phased plan from foundation to release. Work **phase by phase** ([`../AGENTS.md`](../AGENTS.md)). Phase boundaries may be refined when technically justified, but scope must **not** expand beyond [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md). Each phase ships with the tests described in [`TESTING.md`](TESTING.md) and records major decisions in [`DECISIONS.md`](DECISIONS.md).

Status legend: ☐ not started · ◐ in progress · ☑ done.

---

## Phase 0 — Foundation & design system  ☑

- Establish module layering in `shared/` (domain, data, platform, presentation, ui, di) per [`ARCHITECTURE.md`](ARCHITECTURE.md).
- Implement the tokenized design system and `ReliveTheme` (Warm Journal base) from [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md); bundle Playfair Display + Inter.
- Replace the starter `App()` scaffold with the Relive app shell (header, canvas background) matching the reference.
- Wire formatting + static analysis into the build.
- **Exit:** app builds on Android and iOS showing the themed shell; tokens in place; no feature logic yet.

## Phase 1 — Core memory domain & persistence  ☑

- Define domain models: `Moment`, `Timeline`, `MediaAttachment`, `Tag`, `ReliveLocation`, membership.
- Define repository interfaces and the `Clock` abstraction.
- Select and record the local database engine ([`DECISIONS.md`](DECISIONS.md) ADR-0013); implement schema: moments once, many-to-many timeline membership, attachment/tag references, All as a logical query.
- **Exit:** domain + persistence unit/persistence tests pass; CRUD at the repository level works.

## Phase 2 — Basic All timeline  ☑

- Render the **All** timeline: rail, dots, moment presentation (title, subtitle, content), chronological order (oldest-top, newest-bottom per ADR-0015), continuous scroll — matching the reference.
- Content expansion (`... more` / `less`).
- Favorite action (subtle).
- Date + time metadata eyebrow (`DATE • TIME`) with an optional readable saved location on the following metadata line.
- **Exit:** All timeline displays persisted text moments correctly; Compose UI tests for core rendering.

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

## Phase 5 — Custom timelines + Timeline Home  ☑

- Timeline Home is the app root: it lists the All card plus visual custom-timeline cards, opens the existing scoped timeline detail, and exposes Create Timeline from the top app bar.
- Create custom timelines; home screen lists All + custom timelines.
- Filter the already-observed custom Timeline Home cards by live, case-insensitive partial name matching while preserving newest-first order; All and Moment content remain outside this local filter.
- Membership rules: create-in-custom → All + that timeline; create-in-All → optional assignment to custom timelines.
- Opening an observed empty custom timeline expands its existing inline composer; once it has a Moment, normal entry remains collapsed.
- **Exit:** moments appear in the correct timelines without duplication; membership tests pass.

## Phase 6 — Edit / forget rules  ☑

- Long-press in All opens a swift contextual app bar: Edit / Forget remain within 4 days (keyed on `createdAt`), while add-to-custom-timeline organization remains available afterward.
- Inline editing (add/remove media while editing); tap-outside save that does not trigger on control interactions.
- Forget with confirmation → permanent removal.
- **Exit:** 4-day rule enforced everywhere; edit/forget and window-boundary tests pass.

## Phase 7 — Rediscover  ◐

- Add the Timelines / Rediscover top-level navigation shell; do not introduce Search or You placeholders.
- Render the Rediscover root as a `FAVOURITES` section with a bounded, horizontally swipeable row of individual favorited-Moment cards and a `Show all` action above the existing two-item navigation. The empty state remains visible when no favorites exist.
- Render the bounded local On This Day shelf directly below Favorites when at least one eligible previous-year local-date match exists, with exact calendar-year labels and selected-Moment read-only navigation; omit the complete section and its spacing when empty. Render From Your Past below On This Day when present, or directly after Favorites with normal spacing when absent: deterministic daily local selection of up to ten Moments at least 90 days old, excluding current On This Day matches and future timestamps, with the same compact card family as Favorites and read-only selected-Moment navigation. Preserve Places and Tags as deferred capability.
- **Exit:** The bounded Favorites, On This Day, and From Your Past shelves render reactively from persisted data, each opens its read-only system collection at a selected Moment, and deferred Rediscover projections remain tested and isolated from the active root.

## Phase 8 — Search  ◐

- Add Search as a top-level destination with a dedicated, autofocus search screen.
- Search globally and locally across Moment title/content using case-insensitive SQL `LIKE`; results retain All Timeline ordering and presentation.
- Provide match count, up/down active-match navigation, scroll-to-Moment, and read-only results. Filters, categories, Places, Tags, ranking, history, and AI search remain out of scope for v1.
- **Exit:** Search v1 is SQL-backed, globally scoped, read-only, state-preserving across top-level tabs, and covered by focused search/navigation tests.

## Phase 9 — Themes & settings  ◐

- System/Light/Dark appearance plus Original, Evergreen, Lilac Dusk, Crimson Keepsake, Blue Hour, and Rosewood token sets; global app default with optional per-custom-timeline palette overrides; themes change only presentation.
- Settings screen: Profile, Themes, Upgrade to Pro, Export (entries present; detailed behavior deferred where unspecified).
- Profile Media & Storage: read-only local archive storage/category insights; management actions remain deferred.
- Profile Preferences: native-local observable behavior settings for startup destination, explicit composer-discard confirmation, editable-Timeline location/tag presentation, and Rediscover On This Day/Favorites section visibility. Fixed 12-hour time and passive media playback remain unchanged; 24-hour time and media autoplay controls are deferred.
- **Exit:** switching themes changes only presentation; navigation/structure/interaction unchanged; theme tests pass.

### Approved Phase override — Profile foundation

- Profile is implemented as an auxiliary destination from Timeline Home, never a bottom-navigation destination. Its joining date is a singleton local `profile_metadata.created_at` written only for fresh databases; migrated databases retain an absent date and hide the Since line.

### Approved Phase override — Android external share capture

- Android system shares of supported Moment media/text may enter the existing composer through a timeline-selection surface. This is an additive Phase 4/5 capability: it adds no schema, dependency, duplicate composer, automatic persistence, or iOS share extension.

## Phase 10 — RevenueCat / Pro  ☐

- Implement Pro entitlement behind the existing entitlement interface using RevenueCat; wire "Upgrade to Pro."
- (Later/optional) RevenueCat Funnels + Stripe for web subscription conversion — see [`RELEASE.md`](RELEASE.md).
- **Exit:** Pro state drives gated features; entitlement is swappable; monetization dependencies added only now.

## Phase 11 — Production polish + Shipaton release  ☐

- Performance, accessibility, empty/edge states, final visual polish against the reference.
- Release readiness per [`RELEASE.md`](RELEASE.md): Android release, iOS build/signing, screenshots/demo video, privacy checks, Shipaton submission.
- **Exit:** release candidate builds for both platforms; release checklist complete.

---

## Guardrails across all phases

- No unrequested dependencies or scope. No backend, sync, login, social, AI, analytics.
- Keep platform APIs out of shared business logic.
- Tests for important behavior; formatting + static analysis before completion.
- Never commit/merge/push without explicit approval.
