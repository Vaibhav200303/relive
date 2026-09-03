# Relive — Testing

Testing expectations for Relive. Any non-trivial behavior ships with tests. Tests are written in the phase that introduces the behavior ([`ROADMAP.md`](ROADMAP.md)). The architecture is designed to be testable: pure domain logic, a `Clock` abstraction, repository interfaces, and platform capabilities behind interfaces ([`ARCHITECTURE.md`](ARCHITECTURE.md)).

Test source sets already present in `shared/`:

- `commonTest` — shared multiplatform tests
- `androidHostTest` — Android host (JVM) tests
- `iosTest` — iOS tests

Run:

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
```

---

## 1. Domain unit tests

Pure-Kotlin tests in `commonTest`, no platform or framework dependencies.

- Timeline membership rules (see §5).
- The 4-day edit/forget rule (see §4).
- Content-expansion logic if any lives in the domain/presentation (more/less thresholds).
- Location model behavior: optional fields, readable representation, coordinate handling (see §6).
- Favorite toggling.

Use fakes for repositories and a controllable `Clock`.

## 2. Persistence tests

Verify the local storage layer against its contract:

- A moment is stored **once**; reads reconstruct the full moment (title, content, location, tags, attachments, favorite).
- Custom timelines **reference** moments; no duplication of moment data.
- Many-to-many timeline membership is created/removed correctly.
- Attachments and tags reference the correct moment; ordering of attachments is preserved.
- Tags are queryable, including scoped to a timeline.
- **All** is a logical query over every moment (no per-moment All membership rows).
- Forgetting a moment removes it and its references (attachments, memberships, tags links) permanently.

## 3. Search tests

Search v1 is global and local ([`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §9).

- Empty query and no-match states remain empty; the archive is never loaded into Compose for filtering.
- SQL search matches title/content case-insensitively and preserves a stable chronological ordering in presentation.
- The first result is active; Next/Previous do not pass their bounds; query changes and clear reset active state; the active result targets the correct Moment ID for scroll.
- Search is read-only: no composer, edit, Forget, membership, or favorite mutation; media viewer/playback remains available.
- Query, active result, and scroll position survive a same-session move from Search back to Home and into Search again, including when Home is left in focused All moments.

## 4. 4-day rule tests

Central, high-value behavior. Use a deterministic `Clock`.

- Editable/forgettable **within** 4 days of `createdAt`.
- **Not** editable/forgettable after 4 days.
- Boundary: at exactly the 4-day threshold (define and test the boundary precisely).
- `updatedAt` **never** extends the window: editing a moment (advancing `updatedAt`) does not reopen or extend eligibility.
- Long-press exposes Edit/Forget only while eligible; never after.
- Forget requires confirmation before removal.

## 5. Timeline membership tests

- Custom timelines persist and reappear when the database/repositories are reopened.
- Timeline creation trims surrounding whitespace and rejects blank names without losing the entered value on failure.
- Create-in-custom-timeline → moment belongs to **All** and that timeline.
- Create-in-All → optional assignment to zero or more custom timelines.
- A moment in multiple timelines exists once; appears in each referencing timeline.
- Custom timeline presentation remains chronological (oldest at top, newest above the composer). The All moments feed on the Home surface is newest-first, with the inline composer at the head of the feed directly beneath the `All moments` heading; Home always opens at the top (welcome + Rediscover) rather than at the composer/newest end, and pages further Moments as the user scrolls toward older ones.
- Switching timelines changes the observed Moment set without changing Moment identity, favorite state, tags, location, or media.
- Failed Moment + membership insertion leaves no partial Moment or membership rows and preserves the composer draft.
- Removing a moment from a custom timeline does not delete the moment or affect All.
- All is logically complete — every saved moment is a member — while the All moments feed on Home renders it through a bounded, paged window; assert membership at the repository level, never full hydration in presentation.

## 6. Location tests

Cover the required cases ([`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7):

- **Moment with no location** — composer and save work fully; moment persists with absent location.
- **Saved readable location** — displayed below date/time with surrounding whitespace trimmed and only its first character capitalized; persisted data is unchanged.
- **Manually replaced location** — user replaces detected/absent location with a manual entry; persisted correctly.
- **Removed location** — user removes location; moment saves without it.
- **Permission denied** and **permanently denied** — composer continues; moment still saveable.
- **Location services disabled**, **unavailable**, **timeout/failure** — each surfaces as a handled `LocationResult`; composer continues in every case.
- **Places search scoping** — saved locations participate in timeline-scoped Places search; suggestions derive only from the current timeline.
- Raw coordinates are never surfaced in the normal timeline UI.

Use fake `LocationProvider` / `PlaceResolver` implementations to drive each outcome deterministically.

## 7. Compose UI tests (where practical)

Using Compose Multiplatform UI testing:

- Timeline renders rail, dots, and moment hierarchy; text-only moments show no media area.
- Content expansion: `... more` reveals full content; `less` collapses.
- Composer: plus-circle marker, Add Media reveal order (attachment above, Add Media moves below), per-attachment remove, reset `×`, Keep Moment → dot; after Keep the surface stays in focused All moments with the saved Moment rendered in the timeline, and the welcome/Rediscover top state is neither restored nor scrolled toward.
- Composer draft: Back collapses the inline composer and leaves the surface in focused All moments at the same scroll offset; it preserves a dirty draft only for its originating timeline; reopening restores it, while successful Keep and confirmed `×` discard clear it.
- Media collage: multiple attachments render as an adaptive visual collage (see [`DECISIONS.md`](DECISIONS.md) ADR-0019); `+N` overlay for 5+ attachments; tapping a tile opens the full-screen media viewer.
- Favorite action is present but visually subtle.
- Search app-bar transform keeps the timeline visible; highlighting and match navigation behave.
- Accessibility: icon-only controls expose content descriptions; touch targets meet the 48dp minimum ([`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) §18).

## 8. Android build verification

- `./gradlew :androidApp:assembleDebug` succeeds.
- `./gradlew :shared:testAndroidHostTest` passes.
- Formatting and static analysis pass.

## 9. iOS compatibility review

- `./gradlew :shared:iosSimulatorArm64Test` passes.
- The shared framework builds for `iosArm64` and `iosSimulatorArm64`; the `iosApp` project builds in Xcode.
- Platform implementations (location via Core Location, media) behave on iOS; permission flows use the native prompts.

## 10. Regression checks

- Before completing a task, run the relevant test tasks and confirm previously passing tests still pass.
- Guard settled rules with tests so they cannot silently regress: the 4-day window, store-once/reference membership, timeline-scoped search, and location privacy (no background tracking, no coordinate exposure).
- Review the full `git diff` to confirm no unrelated behavior changed ([`../AGENTS.md`](../AGENTS.md)).

### Behavior preferences

- Missing/invalid native values use documented defaults; stable boolean preference values round-trip. No startup-destination value is persisted — the app always opens at the top of the Home surface.
- Every setter updates observable state, and reconstructing presentation over the same repository restores the saved values.
- Startup always opens the Home surface scrolled to the top (welcome + Rediscover above All moments), and leaves any future authoritative restoration/deep-link override higher priority.
- Dirty explicit `×` requests confirmation when enabled and resets/collapses immediately when disabled; empty-draft and Back behavior do not change.
- Editable Timeline location/tag visibility follows preferences without removing data; Search and read-only collection presentation remain unchanged.
- On This Day/Favourites preference-off state removes that collection from the Home Rediscover row and collapses dependent spacing without mutating the read model or disturbing the All moments timeline below.
- Profile → Preferences → Back returns to Profile.

---

## 11. Manual verification expectations

Behavior that requires visual or interaction verification beyond unit/UI tests. Each item should be verified on a real device or emulator.

### Inline composer expansion
- [ ] Collapsed state shows only `+` timeline marker.
- [ ] Tapping `+` smoothly expands the composer in place (no modal/sheet).
- [ ] `+ New` moves the Home surface into focused All moments (welcome + Rediscover scroll offscreen) and expands the existing inline composer from the timeline rail in one continuous motion — no navigation, no modal, dialog, or sheet, no second composer, no archive-wide scroll, and no upward content jump. It scrolls only enough to seat the composer and its Keep Moment button.
- [ ] `+ New` requests no field focus and opens no IME ([`DECISIONS.md`](DECISIONS.md) ADR-0059); the composer settles collapsed-to-expanded in place and the person taps a field when ready.
- [ ] Android share of text, URL, image, video, audio, and a supported mixed batch opens Relive's timeline picker; All appears first, custom timelines retain order; selecting All settles Home into focused All moments (custom timelines open their own detail), then the existing inline composer expands with ordered processing placeholders.
- [ ] Canceling the share picker removes temporary files and returns to the source app. Unsupported, unreadable, oversized-text, empty, and over-50-item shares show no partial draft; a claimed share is saved only after Keep Moment.
- [ ] `×` resets fields and smoothly collapses.
- [ ] Keep Moment resets fields, collapses the inline composer to its rail `+` marker, keeps the surface in focused All moments at the same scroll offset with no app-initiated scroll of any kind, and renders the saved Moment in the timeline beside the collapsed composer; the welcome/Rediscover top state is never automatically restored.
- [ ] Keep Moment reads as the primary Material 3 action and has enabled, disabled, and pressed feedback.
- [ ] Keyboard opens without obscuring the active field (IME insets).
- [ ] Timeline rail reaches the plus center but never renders above it; date/time, dots, and plus share the rail axis at normal and enlarged font scales.
- [ ] Within focused All moments, manually scrolling toward older Moments reveals the bottom-centered return-to-newest arrow; it is hidden whenever the Rediscover row is visible, so it never competes with the upward scroll that restores the top state. It stays visible until the newest end at the head of the feed, works in custom timeline and read-only collection details, and is absent when no scroll toward older Moments is possible.
- [ ] Selecting the arrow returns to the newest end of the feed and never restores the welcome/Rediscover top state; because the feed is windowed it is not required to animate through the whole archive. The first touch during that motion stops at the current position and does not activate the touched Moment content; Snackbar feedback remains above the arrow.

### Persistent debug data
- [ ] Create a moment in a debug build, kill the process, reopen — moment persists.
- [ ] Remove app from Recents, reopen — moment persists.
- [ ] No in-memory fallback silently replaces SQLDelight storage.

### Custom timelines
- [ ] Custom timelines appear newest-created-first wherever the custom-timeline list is surfaced; timestamp ties use deterministic ordering.
- [ ] Each custom timeline card shows its scoped Moment count and persisted creation date; card previews contain at most four image/video attachments, ordered by latest Moment then attachment order, and audio/text-only scopes show the neutral preview.
- [ ] Blank timeline names are rejected; surrounding whitespace is trimmed.
- [ ] Entering a custom timeline with no Moments smoothly expands its existing inline composer every time; after its first Moment is saved, normal entry remains collapsed.
- [ ] Creating inside a custom timeline shows the same Moment once there and once in All.
- [ ] Creating in All with no assignment keeps the Moment out of custom timelines.
- [ ] Assigning one or more custom timelines from All shows one shared Moment in every selected scope.
- [ ] Creating a timeline persists it and immediately opens that exact empty timeline.
- [ ] Custom timelines and memberships survive process death, Recents removal, and a normal APK update.
- [ ] Tags, location, favorite state, images, video, audio, gallery, and viewer behavior are unchanged in custom timelines.

### Home surface
- [ ] The Home surface opens first, scrolled to its top at offset zero: the welcome block, the `Relive your memories` heading with the Rediscover row, then the `All moments` heading, the collapsed inline composer, and the All moments feed.
- [ ] Home is one scroll container with one scroll position — the Rediscover row and the All moments feed scroll together — and moving between the Home top state and focused All moments is scrolling only: no navigation event, route change, back-stack entry, or screen transition.
- [ ] The Relive app bar stays pinned across both states and condenses as the surface scrolls; in focused All moments the `All moments` heading pins directly beneath it.
- [ ] Home retains its exact scroll offset after returning from a custom timeline, a read-only collection, Search, or Profile — if it was left in focused All moments it returns there, never to the welcome/Rediscover top state, which only manual upward scroll restores.
- [ ] The Create Timeline entry point still persists a new timeline, lists it first, and opens it immediately; `+ New` on Home never opens that flow — it expands the inline composer (see Inline composer expansion above).
- [ ] Detail Back returns to the Home surface at its preserved scroll offset (including focused All moments) on Android and iOS; no profile, menu, or bottom navigation controls appear in detail.

### Adaptive single media
- [ ] Small image renders at natural size, not stretched.
- [ ] Large image scales down proportionally, not distorted.
- [ ] Portrait photo/video remains meaningfully portrait.
- [ ] Audio tile uses black canvas with centered bounded waveform.

### Collage borders and dividers
- [ ] Multi-media collage outer border is visible and uses accent color.
- [ ] Internal gaps between tiles are visible and match outer border width.
- [ ] No doubled shared-edge strokes between adjacent tiles.
- [ ] Single-media border thickness matches multi-media border thickness.

### Inline-vs-fullscreen video rule
- [ ] Unconstrained single video: Play starts inline, body tap opens full-screen.
- [ ] Constrained single video: Play opens full-screen, body tap opens full-screen.
- [ ] Multi-media collage video: always navigates to gallery/viewer.

### Gallery → viewer hierarchy
- [ ] Single attachment: timeline tap opens viewer directly.
- [ ] 2+ attachments: timeline tap opens gallery; gallery tap opens viewer.
- [ ] `+N` tile opens gallery (not viewer at index 3).
- [ ] Viewer Back returns to gallery; gallery Back returns to timeline.
- [ ] Timeline scroll position preserved after returning from gallery/viewer.

### Audio waveform
- [ ] Timeline audio tile shows real waveform capsule segments on black canvas.
- [ ] Silent audio renders tiny/nearly-flat segments.
- [ ] Playing state shows waveform moving horizontally with playback.
- [ ] Paused state freezes waveform in place.
- [ ] Full-screen audio viewer preserves same waveform identity.

### Playback ownership
- [ ] Starting audio stops any playing video (and vice versa).
- [ ] Navigating to gallery/viewer stops timeline playback.
- [ ] Navigating away from viewer stops viewer playback.
- [ ] No background audio/video continues after leaving a screen.

### Multi-select processing placeholders
- [ ] Multiple files selected: each gets immediate placeholder tile.
- [ ] Spinner centered in actual preview bounds.
- [ ] Completed media replaces placeholder in place without reorder.
- [ ] Failed tile shows retry/remove.
- [ ] Processing → Ready does not cause a size jump (video).

### Camera orientation and feedback
- [ ] With Android system auto-rotate both on and off, rotating the device leaves every Relive surface in portrait.
- [ ] Photo review shows correct orientation on first frame (no rotate-then-correct snap).
- [ ] Portrait-locked camera captures rear/front photos and videos that review, persist, and replay with correct orientation metadata.
- [ ] Shutter sound plays on real capture success only.
- [ ] Video start tone completes before mic opens (no bleed into recording).
- [ ] Video stop tone plays after mic releases.
- [ ] Front/back switch works; disabled while recording video.
- [ ] Flash/torch toggles; muted on front camera.

### Recorder duration/× layout
- [ ] Active recording row: Stop, waveform, duration, × — all visible.
- [ ] Duration and × never overlap on any screen width.
- [ ] × has proper touch target (48dp).
- [ ] Waveform shows real live amplitude.

### Phase 6 — Edit / forget (physical-device checklist complete)
- [ ] In focused All moments, long-press and accessibility actions smoothly enter the contextual app bar over Home's own app bar; Back exits selection first and leaves the surface in focused All moments at the same scroll offset, without popping or scrolling toward the welcome/Rediscover top state.
- [ ] Edit / Forget appear only before `createdAt + 4 days`; verify the exact boundary is ineligible while Add to timeline remains available when custom timelines exist.
- [ ] Add to timeline lists current assignments as disabled, adds one selected unassigned custom timeline without duplicating the Moment, and retains the picker for retry after failure.
- [x] Edit saves inline without changing Moment identity, creation time, favorite state, or custom-timeline memberships; an edit already open may save after expiry.
- [x] Tap outside the edit container saves; every editor control (text, tags, media, recording, playback, location, favorite) does not.
- [x] Removing existing media deletes its file only after a successful edit; failed edits leave the original moment and media intact.
- [x] Forget requires confirmation, removes the Moment from All and every custom timeline, and only then attempts attachment-file cleanup; playback stops before edit, Forget, and media deletion.
- [x] Attempting a timeline switch, composer opening, or a second edit while an edit is dirty does not silently discard it.

### Home — Rediscover row
- [ ] The Rediscover row is horizontally scrollable, sits between the `Relive your memories` and `All moments` headings on the same vertically scrolling surface, and its horizontal position is independent of Home's vertical offset.
- [ ] The Rediscover row order is Favourites, On This Day, From Your Past, All Photos. Cards open their read-only collections; no card opens the editable All moments feed, which is reached only by scrolling Home down into focused All moments or via `+ New`.
- [ ] `All Photos` is a bounded, read-only collection of Moments with at least one image or video attachment, read through the same bounded projection as Favourites and From Your Past, and introduces no new table, membership, or duplicate persistence.
- [ ] Favourites reflects persisted favorite state in the same chronological ordering as the full Favourites collection; its bounded preview batch-loads attachments and does not hydrate the complete collection. The All moments feed on the same surface is likewise bounded, windowed, and paged, and the root never hydrates the complete archive on launch.
- [ ] Media, text-only, and audio cards use their appropriate compact presentation; media uses the first ordered attachment with a quiet additional-count indicator and audio never autoplays.
- [ ] Zero favourites shows the approved two-line empty state with no preview or `Show all`.
- [ ] Opening a Rediscover collection opens its read-only detail — from a Moment-level preview at that Moment, and from `Show all` without a selected Moment. The detail hides the floating Home controls and exposes only Back, read-only timeline browsing, media viewing, and playback; Back returns to the Home surface at its preserved vertical scroll offset with the Rediscover row's horizontal position intact.
- [ ] On This Day matches only exact previous local calendar years, excludes the current year, preserves February 29 behavior, uses exact calendar-year labels, and loads only a bounded attachment-batched preview.
- [ ] An empty On This Day remains compact; media, text-only, and audio-only entries retain featured-card geometry; tapping routes to the selected read-only collection.
- [ ] The floating navigation toolbar collapses to the active destination icon on downward scroll and expands in Home / Timelines / Search order on upward scroll; its accent-derived selected pill moves smoothly between icons without an intermediate artifact, and the scroll-driven collapse never interferes with scrolling welcome + Rediscover back into view. `+ New` presentation follows the Home state rather than raw scroll direction, so it does not collapse to a bare Add icon merely because the surface scrolled into focused All moments, and it is hidden while the inline composer is expanded. Custom timeline and read-only collection details hide the floating controls and preserve Back to Home; focused All moments is not a detail screen and keeps the controls available.
- [ ] Floating navigation and `+ New` share height, bottom alignment, warm-stone surface, safe insets, and an 8dp gap; narrow screens retain three equal navigation touch targets (Home, Timelines and Search), and the IME does not cover either control.
- Deferred, retain coverage for future reactivation: deterministic From Your Past selection, Places/Tags ranking, empty/partial archive states, and passive media behavior.

### Appearance and themes
- [ ] System follows live OS appearance; explicit Light and Dark ignore subsequent OS changes.
- [ ] Original and all five nostalgic palettes restore after relaunch and expose distinct light/dark tokens with accessible text/action contrast.
- [ ] Profile controls retain 48dp targets, selected/radio semantics, readable two-line labels, and horizontal scrolling at narrow widths and large text sizes.
- [ ] Theme transitions preserve navigation, list position, composer drafts, active selection, and media state.
- [ ] A custom timeline palette affects that timeline's card wherever it is listed and its detail only; “Use app theme” clears the override; the Home surface, All moments, and system collections remain global.
- [ ] Android and iOS status-bar content follows the resolved mode; stored media pixels and Timeline layout remain unchanged.

### Media & Storage archive insights
- [ ] Profile → Media & Storage → Back returns to the preserved Profile screen; Profile Back returns to the Home surface at its preserved scroll offset, including focused All moments when that is where Profile was opened from.
- [ ] Empty and text-only archives show `0 B` without a chart while retaining the actual Moment count.
- [ ] Photo, video, audio, and defensive Other values match only Relive-managed attachment references; missing files do not crash or contribute bytes.
- [ ] The hero, breakdown, and archive rows remain readable at large font scales and expose category/value semantics across every palette and appearance mode.

### Behavior preferences

- [ ] Profile → Preferences opens the calm open-page layout; Back returns to the retained Profile destination.
- [ ] Fresh startup, force-stop, and relaunch all open the Home surface scrolled to the top (welcome + Rediscover above All moments), with no destination flash; Preferences offers no start-destination choice.
- [ ] Dirty composer `×` confirms when enabled and immediately resets/collapses when disabled; empty composer `×` remains immediate and Back still preserves the draft.
- [ ] Show locations/tags updates the All moments feed and custom timeline cards without deleting data or hiding composer/edit fields; Search and read-only collection detail still show their normal metadata.
- [ ] On This Day/Favourites switches remove their cards from the Home Rediscover row with no empty gap and without shifting the `All moments` heading or the timeline below it; re-enabling restores current repository data immediately.
- [ ] Every row remains readable and operable at large font scale with TalkBack/VoiceOver, and switches announce state.
- [ ] Toggle haptics fire once from direct interaction and never during restoration/recomposition.
- [ ] All six palettes render Preferences correctly in Light, Dark, and System modes with no hardcoded-color artifacts.

---

## 12. Coverage to add — unified Home surface

Behavior introduced by the unified Home surface ([`DECISIONS.md`](DECISIONS.md) ADR-0061). These are the expectations tests must assert when the surface is built, in the phase that introduces it ([`ROADMAP.md`](ROADMAP.md)) — they are not coverage that exists today.

### Greeting

- A real profile display name renders `Welcome back, {name}`.
- No real profile display name renders exactly `Welcome back` — no trailing punctuation, no placeholder.
- The `Your Relive` fallback label never leaks into the greeting in either case.
- The subtitle is always `Your memories are waiting for you.`

### Initial position and scroll container

- The app opens at the top of Home, in the top state, at scroll offset zero, with no programmatic scroll on entry.
- The Rediscover collection row and the All moments timeline live in **one** scroll container with one scroll position: assert a single scrollable, not two.
- Focused All moments is entered both ways — by scrolling the surface down, and by tapping `+ New`.
- Keep Moment and Back never auto-restore the welcome/Rediscover top state and never change the scroll offset.
- Manual upward scroll is the only thing that restores the top state, including after returning from a custom timeline, a read-only collection, Search, or Profile.

### Inline composer

- `+ New` expands the existing composer in place from the timeline rail: no modal, dialog, bottom sheet, or second composer instance is created.
- `+ New` requests no field focus and opens no IME (ADR-0059).

### Bounded, paged loading

- The root never observes or hydrates the complete archive on launch.
- Paging loads incrementally toward older Moments as the user scrolls.

### Header indices and scroll anchors

- The welcome block, the collection row, and the section headings occupy leading items in the same list as the Moments, so every index computation — scroll targets, selected-Moment lookup, first-visible-item position — accounts for the header offset.
- These index computations stay correct as header items are added or removed.

---

## Conventions

- Keep domain tests platform-free in `commonTest`.
- Inject `Clock`, repositories, and platform capabilities as interfaces; never hit real GPS, camera, or wall-clock time in tests.
- Prefer deterministic, fast tests; reserve device/simulator runs for platform-specific verification.
