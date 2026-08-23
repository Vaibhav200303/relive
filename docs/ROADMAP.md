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
- Date + time metadata eyebrow (`DATE • TIME`).
- **Exit:** All timeline displays persisted text moments correctly; Compose UI tests for core rendering.

## Phase 3 — Inline composer (+ location data model)  ☑

- Inline composer with collapsed-by-default plus-circle marker; animated expand/collapse in place; auto date/time, title, content, tags, **Keep Moment**, reset `×` (collapses composer).
- Establish the **location data model and interfaces** (`ReliveLocation`, `LocationProvider`, `PlaceResolver`, `LocationResult`) in shared code — **before** platform GPS implementations.
- Composer location UI: show resolved place below date/time; keep / remove / replace with manual entry. All fields optional; no raw coordinates in UI. Composer works fully with no location.
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
- Platform GPS implementations behind the Phase 3 interfaces: Android location APIs; iOS Core Location; reverse geocoding via `PlaceResolver`. Handle permission denied / permanently denied / services disabled / unavailable / timeout — composer continues in every case. Permission requested only when needed; no background tracking.
- **Exit:** moments with multiple attachments render as an adaptive collage; detected location works and degrades gracefully; location + media tests pass.

## Phase 5 — Custom timelines + Timeline Home  ☑

- Timeline Home is the app root: it lists the All card plus visual custom-timeline cards, opens the existing scoped timeline detail, and exposes Create Timeline from the top app bar.
- Create custom timelines; home screen lists All + custom timelines.
- Membership rules: create-in-custom → All + that timeline; create-in-All → optional assignment to custom timelines.
- **Exit:** moments appear in the correct timelines without duplication; membership tests pass.

## Phase 6 — Edit / forget rules  ☑

- Long-press within 4 days shows Edit / Forget; hidden after 4 days (keyed on `createdAt`).
- Inline editing (add/remove media while editing); tap-outside save that does not trigger on control interactions.
- Forget with confirmation → permanent removal.
- **Exit:** 4-day rule enforced everywhere; edit/forget and window-boundary tests pass.

## Phase 7 — Rediscover  ◐

- Add the Timelines / Rediscover top-level navigation shell; do not introduce Search or You placeholders.
- Render the Rediscover root as a `FAVOURITES` section with a bounded, horizontally swipeable row of individual favorited-Moment cards and a `Show all` action above the existing two-item navigation. The empty state remains visible when no favorites exist.
- Render the bounded local On This Day shelf directly below Favorites: previous-year local-date matches only, exact calendar-year labels, compact empty state, and selected-Moment read-only navigation. Render From Your Past below On This Day: deterministic daily local selection of up to ten Moments at least 90 days old, excluding current On This Day matches and future timestamps, with the same compact card family as Favorites and read-only selected-Moment navigation. Preserve Places and Tags as deferred capability.
- **Exit:** The bounded Favorites, On This Day, and From Your Past shelves render reactively from persisted data, each opens its read-only system collection at a selected Moment, and deferred Rediscover projections remain tested and isolated from the active root.

## Phase 8 — Search  ☐

- Search icon transforms the app bar; timeline stays visible; All / Tags / Places filters scoped to the current timeline.
- All-search: highlight, match count, up/down navigation, auto-scroll (WhatsApp-style).
- Tags: filter + suggestions (current timeline). Places: filter + suggestions derived only from the current timeline's moment locations.
- **Exit:** timeline-scoped search across title/content/tags/location; Places scoping and search tests pass.

## Phase 9 — Themes & settings  ☐

- Additional themes (Monochrome Archive, Film Memory) as token sets; per-timeline theme selection; themes change only presentation.
- Settings screen: Profile, Themes, Upgrade to Pro, Export (entries present; detailed behavior deferred where unspecified).
- **Exit:** switching themes changes only presentation; navigation/structure/interaction unchanged; theme tests pass.

### Approved Phase override — Profile foundation

- Profile is implemented as an auxiliary destination from Timeline Home, never a bottom-navigation destination. Its joining date is a singleton local `profile_metadata.created_at` written only for fresh databases; migrated databases retain an absent date and hide the Since line.

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
