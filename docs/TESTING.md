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

Search is always scoped to the current timeline ([`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §9).

- **All-search** matches across title, content, tags, and location within scope; returns match positions/count for highlight + navigation; ordering supports auto-scroll to the matching moment.
- **Tags**: only matching moments remain; suggestions come from tags present in the current timeline.
- **Places**: only matching locations remain; suggestions derive **only** from locations of moments in the current timeline (§6 Places scoping).
- Scope correctness: a query in `Japan 2026` must not return moments outside it; the same query on `All` covers every moment.

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
- All and custom timeline presentation remain chronological (oldest at top, newest above the composer).
- Switching timelines changes the observed Moment set without changing Moment identity, favorite state, tags, location, or media.
- Failed Moment + membership insertion leaves no partial Moment or membership rows and preserves the composer draft.
- Removing a moment from a custom timeline does not delete the moment or affect All.
- All always reflects every saved moment.

## 6. Location tests

Cover the required cases ([`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7):

- **Moment with no location** — composer and save work fully; moment persists with absent location.
- **Detected location** — `LocationProvider` → `PlaceResolver` yields a readable `ReliveLocation`; displayed below date/time.
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
- Composer: plus-circle marker, Add Media reveal order (attachment above, Add Media moves below), per-attachment remove, reset `×`, Keep Moment → dot.
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

---

## 11. Manual verification expectations

Behavior that requires visual or interaction verification beyond unit/UI tests. Each item should be verified on a real device or emulator.

### Inline composer expansion
- [ ] Collapsed state shows only `+` timeline marker.
- [ ] Tapping `+` smoothly expands the composer in place (no modal/sheet).
- [ ] `×` resets fields and smoothly collapses.
- [ ] Keep Moment resets fields and collapses.
- [ ] Keyboard opens without obscuring the active field (IME insets).

### Persistent debug data
- [ ] Create a moment in a debug build, kill the process, reopen — moment persists.
- [ ] Remove app from Recents, reopen — moment persists.
- [ ] No in-memory fallback silently replaces SQLDelight storage.

### Custom timelines
- [ ] All is selected by default; custom timelines appear in the selector immediately after creation.
- [ ] Blank timeline names are rejected; surrounding whitespace is trimmed.
- [ ] A custom timeline with no Moments shows the editorial empty state and the inline `+` marker.
- [ ] Creating inside a custom timeline shows the same Moment once there and once in All.
- [ ] Creating in All with no assignment keeps the Moment out of custom timelines.
- [ ] Assigning one or more custom timelines from All shows one shared Moment in every selected scope.
- [ ] Switching timelines stops active playback and never changes an unfinished draft's membership silently.
- [ ] Custom timelines and memberships survive process death, Recents removal, and a normal APK update.
- [ ] Tags, location, favorite state, images, video, audio, gallery, and viewer behavior are unchanged in custom timelines.

### Timeline Home
- [ ] Timeline Home opens first and retains its scroll position after returning from All or a custom timeline.
- [ ] All shows the persisted total Moment count; each custom card shows only its own membership count.
- [ ] Card previews contain at most four image/video attachments, ordered by latest Moment then attachment order; audio/text-only scopes show the neutral preview.
- [ ] The Home + opens the existing Create Timeline flow; the new timeline appears reactively without leaving Home.
- [ ] Detail Back returns to Timeline Home on Android and iOS; no profile, menu, or bottom navigation controls appear.

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
- [ ] Photo review shows correct orientation on first frame (no rotate-then-correct snap).
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
- [x] Long-press and accessibility actions offer Edit / Forget only before `createdAt + 4 days`; verify the exact boundary is ineligible.
- [x] Edit saves inline without changing Moment identity, creation time, favorite state, or custom-timeline memberships; an edit already open may save after expiry.
- [x] Tap outside the edit container saves; every editor control (text, tags, media, recording, playback, location, favorite) does not.
- [x] Removing existing media deletes its file only after a successful edit; failed edits leave the original moment and media intact.
- [x] Forget requires confirmation, removes the Moment from All and every custom timeline, and only then attempts attachment-file cleanup; playback stops before edit, Forget, and media deletion.
- [x] Attempting a timeline switch, composer opening, or a second edit while an edit is dirty does not silently discard it.

### Rediscover
- [ ] The active root renders only the Relive app bar, Favorites system-collection card, and the two-item bottom navigation.
- [ ] Favorites reflects persisted favorite state without showing resurfacing sections or their empty states.
- [ ] Favorites cover uses only its own image/video attachments (up to four, newest Moment first); zero or audio/text-only favorites use the editorial placeholder.
- [ ] Opening Favorites hides bottom navigation and exposes only Back, read-only timeline browsing, media viewing, and playback; Back returns to the preserved Rediscover root state.
- [ ] Timelines and Rediscover are the only visible bottom-navigation destinations; Timeline detail hides the bar and preserves Back behavior.
- Deferred, retain coverage for future reactivation: On This Day calendar eligibility, deterministic From Your Past selection, Places/Tags ranking, empty/partial archive states, and passive media behavior.

---

## Conventions

- Keep domain tests platform-free in `commonTest`.
- Inject `Clock`, repositories, and platform capabilities as interfaces; never hit real GPS, camera, or wall-clock time in tests.
- Prefer deterministic, fast tests; reserve device/simulator runs for platform-specific verification.
