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

- Create-in-custom-timeline → moment belongs to **All** and that timeline.
- Create-in-All → optional assignment to zero or more custom timelines.
- A moment in multiple timelines exists once; appears in each referencing timeline.
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
- Media carousel: multiple attachments swipe with subtle page indicators.
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

## Conventions

- Keep domain tests platform-free in `commonTest`.
- Inject `Clock`, repositories, and platform capabilities as interfaces; never hit real GPS, camera, or wall-clock time in tests.
- Prefer deterministic, fast tests; reserve device/simulator runs for platform-specific verification.
