# Relive — Motion & Shape Implementation Plan

A staged plan to bring Material 3 motion (transitions, easing/duration) and the M3
shape system (corner scale, shape morph) into Relive. The goal is an app that feels
**alive, tactile, and delightful** — where navigating, opening a memory, favoriting,
or recording each rewards the user with motion that is smooth, purposeful, and
consistent — without betraying Relive's warm, nostalgic, editorial character.

Every step below is written as a **self-contained prompt** you can paste into an agent
(or follow yourself). Steps are ordered so each builds on the previous. Do the
**Foundation (Phase 0)** first — everything else depends on it.

---

## Progress status

Mark each step `[x]` when its acceptance criteria pass and the build compiles. Any new
chat should read this table first to know where to continue. (See "How to continue in a
new chat" at the bottom.)

| Done | Step | Summary |
|---|---|---|
| [ ] | 0.1 | Complete/correct motion tokens (durations + 6 easings, fix emphasized) |
| [x] | 0.2 | Reduced-motion capability (accessibility gate) |
| [x] | 0.3 | 10-step shape scale + shape-library availability decision |
| [ ] | 0.4 | Skeleton loader primitive |
| [ ] | 1.1 | Top-level fade-through (bottom nav) |
| [ ] | 2.1 | Forward/backward — profile settings tree |
| [ ] | 2.2 | Forward/backward — theme + home→profile (shared helper) |
| [ ] | 3.1 | Container transform — photo → MediaViewer (flagship hero) |
| [ ] | 3.2 | Container transform — MomentCard → MomentMediaGallery |
| [ ] | 3.3 | Container transform — New-Moment FAB → composer |
| [ ] | 3.4 | Container transform — Rediscover card → collection screen |
| [x] | 4.1 | Lateral — verify media pager (no fade/parallax) |
| [ ] | 5.1 | Enter/exit — sheets, dialogs, snackbars, menus |
| [ ] | 5.2 | Enter/exit — scroll-driven app bar & floating controls |
| [ ] | 6.1 | Skeleton loaders wired into loading states |
| [ ] | 7.1 | Shape morph — favorite toggle |
| [ ] | 7.2 | Shape morph — recording/progress |
| [ ] | 7.3 | Shape morph — floating toolbar / bottom-nav selection |
| [ ] | 8.1 | Optical roundness audit + fix (nested cards) |
| [ ] | 8.2 | Decorative shape — avatar mask & photo crop (sparing) |
| [ ] | 9.1 | Fix overlapping crossfades |
| [ ] | 9.2 | Consistency & reduced-motion QA + doc update |

---

## Design principles that govern every step

Derived from the M3 motion & shape guidance, filtered through Relive's aesthetic:

1. **Tokens only.** No hardcoded durations, easings, radii, or shapes in components.
   Everything routes through the theme (`ReliveMotion`, `ReliveDimensions`,
   `ReliveShapes`). This matches the existing strict-token rule in
   [`docs/DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).
2. **One pattern per navigation type.** Same transition for the same kind of move,
   app-wide. Hierarchy = forward/backward. Peers = lateral. Top-level = fade-through.
   Expand-to-detail hero = container transform. Components in context = enter/exit.
3. **No jump cuts.** Instant screen swaps are disorienting; every navigation gets a
   transition. (Exception: pure-efficiency menus.)
4. **Clean fades.** Fade content fully out before fading new content in. Never leave
   two partially-transparent layers overlapping. Existing crossfades that run in
   parallel are a defect to fix.
5. **Respect reduced motion.** When the OS reduced-motion setting is on, replace
   slides/scales/morphs with subtle fades and disable decorative effects. This is an
   accessibility requirement, not a nicety — build the gate first.
6. **Simple style for common transitions.** No bouncy springs on screen navigation.
   Springs are reserved for shape morph on interaction/progress (delight accents).
7. **Restraint on shape tension.** Relive is warm and rounded (photo-print, cream).
   Use shape morph for **interaction and progress feedback**; use decorative shapes
   only for avatar masking and photo cropping. Do not scatter abstract/square shapes.
8. **Stable layouts.** Content must not pop in or shift. Use skeleton loaders.
9. **Optical roundness.** Nested rounded objects use different radii:
   `inner = outer − padding`.

### Motion token reference (M3 legacy easing/duration system)

Durations (ms): `short1=50 short2=100 short3=150 short4=200 medium1=250 medium2=300`
`medium3=350 medium4=400 long1=450 long2=500 long3=550 long4=600 extraLong1=700 …`.

Easing sets:

| Token | Cubic / path |
|---|---|
| `emphasized` | 2-part path (Compose `PathEasing`), **not** a single cubic |
| `emphasizedDecelerate` | `cubic(0.05, 0.7, 0.1, 1.0)` |
| `emphasizedAccelerate` | `cubic(0.3, 0.0, 0.8, 0.15)` |
| `standard` | `cubic(0.2, 0.0, 0.0, 1.0)` |
| `standardDecelerate` | `cubic(0.0, 0.0, 0.0, 1.0)` |
| `standardAccelerate` | `cubic(0.3, 0.0, 1.0, 1.0)` |

Suggested pairings: begin+end on screen → Emphasized 500ms · enter → Emphasized
decelerate 400ms · exit permanently → Emphasized accelerate 200ms. Small/utility →
Standard set. Container transform (card→fullscreen) → Emphasized ~500ms. FAB→sheet →
Emphasized 400ms. Enter longer than exit, always.

### Corner radius scale (M3 10-step)

`None 0 · XS 4 · Small 8 · Medium 12 · Large 16 · LargeIncreased 20 · XL 28 ·
XLIncreased 32 · XXL 48 · Full`.

---

## Phase 0 — Foundation (do this first)

### Step 0.1 — Complete and correct the motion tokens

**Prompt:**
> In `shared/src/commonMain/kotlin/com/vaibhav/relive/ui/theme/ReliveMotion.kt`,
> replace the current motion tokens with the full M3 system.
>
> - `ReliveDurations`: expose the full M3 duration scale as named tokens
>   (`short1..short4`, `medium1..medium4`, `long1..long4`, `extraLong1`), values 50,
>   100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 700 ms. Keep the existing
>   `fastMillis`/`standardMillis`/`slowMillis` names as aliases mapping onto the new
>   scale (fast→short4 200? — no: keep current behaviour by aliasing fast=short3 150,
>   standard=medium2 300, slow=long2 500) so existing call sites compile; mark aliases
>   `@Deprecated("use the M3 duration scale")`.
> - `ReliveEasings`: add all six easing tokens. `standard`, `standardDecelerate`,
>   `standardAccelerate`, `emphasizedDecelerate`, `emphasizedAccelerate` as
>   `CubicBezierEasing` with the exact control points from the plan's easing table.
>   For `emphasized`, build the true 2-part M3 curve using `PathEasing` from
>   `androidx.compose.animation.core` (path: M0,0 C0.05,0 0.133333,0.06 0.166666,0.4
>   C0.208333,0.82 0.25,1 1,1). **Note:** the current `emphasized = cubic(0.2,0,0,1)`
>   is actually the *standard* curve mislabeled — do not carry that mistake forward.
> - Keep `ReliveMotion` immutable and exposed via `ReliveTheme.motion` exactly as now.
>
> Do not touch component code yet. Confirm the module compiles.

**Acceptance:** all six easings present with correct math; full duration scale; old
names still resolve; project builds.

---

### Step 0.2 — Reduced-motion capability (accessibility gate)

**Prompt:**
> Add a cross-platform way to read the OS "reduce motion" / animator-scale setting.
>
> - In `commonMain`, declare `expect fun prefersReducedMotion(): Boolean` (or a
>   composable `@Composable expect fun rememberReducedMotion(): Boolean`) in a new
>   `ui/theme/ReduceMotion.kt`.
> - Android actual: read `Settings.Global.ANIMATOR_DURATION_SCALE` (0f means animations
>   off) and/or `AccessibilityManager` reduce-motion where available; return true when
>   the user has reduced/disabled animation. iOS actual: return false for now (or wire
>   `UIAccessibility.isReduceMotionEnabled` if the iOS target is active).
> - Expose it through the theme as `ReliveTheme.reduceMotion` (a `Boolean`
>   CompositionLocal) so any composable can branch without re-querying the platform.
> - Add a small helper `ReliveMotion.spec(reduceMotion, full, reduced)` returning the
>   appropriate `FiniteAnimationSpec` — when reduced, always a short fade
>   (`tween(short3, easing = standard)` with no slide/scale).
>
> Every transition built in later phases MUST route through this helper so reduced
> motion degrades to a plain fade automatically.

**Acceptance:** toggling the OS setting flips `ReliveTheme.reduceMotion`; helper
returns a fade-only spec when reduced.

---

### Step 0.3 — Shape tokens: complete the 10-step scale + shape library gate

**Prompt:**
> Extend the shape system to the full M3 scale and prepare shape-morph availability.
>
> - In `ReliveDimensions.kt`, expand `ReliveRadii` to the full M3 10-step scale with
>   correct names: `none=0, xs=4, small=8, medium=12, large=16, largeIncreased=20,
>   xl=28, xlIncreased=32, xxl=48, full=999` (full = fully rounded). Keep the existing
>   `sm/md/lg/dialog/pill` names as `@Deprecated` aliases mapping onto the new tokens
>   (`sm→small`, `md→medium`, `lg→largeIncreased`, `dialog→xl`, `pill→full`) so nothing
>   breaks. **Flag:** the old `lg=20` is M3 *large-increased*, not M3 *large (16)* —
>   audit call sites in a later step to confirm intended roundness.
> - Create `ui/theme/ReliveShapes.kt` with a `ReliveShapes` holder exposing
>   `RoundedCornerShape`s built from the radii tokens (e.g. `card`, `dialog`, `sheet`,
>   `chip`, `button`, `pill`), and asymmetric shapes where needed (e.g. bottom-sheet =
>   top corners only). Expose via `ReliveTheme.shapes`.
> - Verify whether `androidx.compose.material3.MaterialShapes` and shape `Morph`
>   resolve in `commonMain` for the pinned Compose Multiplatform version. If yes,
>   document it. If not, add `androidx.graphics.shapes` (`RoundedPolygon`, `Morph`) to
>   the shared graphics deps or wrap behind `expect/actual`. Record the outcome in
>   `docs/DECISIONS.md`. Shape morph steps (Phase 7) depend on this.
>
> Do not restyle components yet.

**Acceptance:** full radius scale available; `ReliveTheme.shapes` exists; a written
decision on whether `MaterialShapes`/`Morph` is usable in common code.

---

### Step 0.4 — Skeleton loader primitive

**Prompt:**
> Create a reusable skeleton-loader composable in
> `ui/components/ReliveSkeleton.kt`.
>
> - A `ReliveSkeletonBox(modifier, shape)` that fills its bounds with a token-colored
>   placeholder and a subtle pulsing shimmer (indeterminate). Use a low-amplitude
>   alpha or gradient sweep animated top-left → bottom-right, tuned to Relive's warm
>   palette (no harsh grey; use a soft sepia/cream tint from theme colors).
> - Respect `ReliveTheme.reduceMotion`: when reduced, show a static placeholder (no
>   pulse).
> - Provide skeleton layouts that mirror real content silhouettes:
>   `TimelineHomeSkeleton`, `TimelineDetailSkeleton`, `RediscoverSkeleton` — matching
>   the card/list geometry so nothing shifts when real content fades in.
> - When content loads, cross-fade real content in over the skeleton with a short
>   `medium1` fade (clean fade: skeleton out, content in).
>
> Do not wire into screens yet (Phase 6 does that); just build the primitives with a
> `@Preview`.

**Acceptance:** skeleton primitive + three silhouette variants render in preview;
pulse animates; reduced-motion shows static.

---

## Phase 1 — Top-level transition (bottom navigation)

### Step 1.1 — Fade-through between Timelines / Rediscover / Search

**Prompt:**
> Wrap the top-level destination swap in `shared/src/commonMain/kotlin/com/vaibhav/relive/App.kt`
> (the `when (topLevel)` block that currently renders `TimelineHomeScreen` /
> `RediscoverScreen` / `SearchScreen` instantly, around lines 423–487) in an
> `AnimatedContent` implementing the **M3 top-level / fade-through** pattern.
>
> - Motion: outgoing content **fades out first** (`emphasizedAccelerate`, `short4`
>   ≈200ms) with a slight scale-down to 92%; then incoming content **fades in**
>   (`emphasizedDecelerate`, `medium2` ≈300ms) scaling up from 92% to 100%. Use
>   `AnimatedContent`'s sequential timing so the fades do NOT overlap (clean fade).
> - No horizontal slide — top-level destinations are unrelated; a slide would imply
>   swipeable peers and conflict with carousels/list swipes. Do **not** enable
>   swipe-to-switch between top-level tabs.
> - Route the spec through `ReliveMotion.spec(reduceMotion, …)` so reduced motion
>   becomes a plain fade.
> - Key the `AnimatedContent` on `topLevel` only. Keep list state (`homeListState`,
>   etc.) hoisted as it already is so scroll positions survive.
>
> Verify the bottom bar itself stays put (it should remain outside the animated
> content).

**Acceptance:** switching tabs fades cleanly with no slide, no overlap; scroll
positions preserved; reduced motion = simple fade.

---

## Phase 2 — Forward/backward (hierarchical navigation)

### Step 2.1 — Profile settings tree

**Prompt:**
> Apply the **M3 forward/backward** pattern to the Profile settings hierarchy in
> `App.kt` — the `when (profileNavigation.destination)` block (Profile → Preferences,
> MediaStorage, BackupRestore, Location, Notifications, Privacy, Help, About, Licenses,
> around lines 260–308), which currently swaps instantly.
>
> - Wrap in `AnimatedContent` keyed on `profileNavigation.destination`.
> - Forward (deeper): new screen slides in from the right + fades in, using
>   `emphasizedDecelerate` at `medium4` (400ms); old screen slides left a short
>   distance + fades out with `emphasizedAccelerate` at `short4` (200ms). Use a small
>   slide offset (e.g. 1/5 width) like Android's default, not a full-width slide.
> - Backward (shallower): reverse direction. Detect direction by comparing the previous
>   vs new destination depth (Profile is root; children are depth 1; About→Licenses is
>   depth 2). Store the previous destination to pick slide direction.
> - **Do not** use container transform here — this is a deeper hierarchy and container
>   transform would feel excessive.
> - Route through the reduced-motion helper (fade only when reduced).

**Acceptance:** drilling into a settings page slides forward; back slides backward;
depth-based direction correct; reduced motion = fade.

### Step 2.2 — Timeline detail → Timeline theme, and Home → Profile

**Prompt:**
> Apply the same forward/backward pattern (from Step 2.1, ideally via a shared
> `reliveForwardBackward()` transition-spec helper you extract into
> `ui/theme/ReliveTransitions.kt`) to:
> - `TimelinesDestination.TimelineDetail` ↔ `TimelinesDestination.TimelineTheme`
>   (App.kt ~line 350).
> - Opening Profile from Timeline Home (`profileNavigation.openProfile()`), and
>   returning.
>
> Extract the forward/backward enter/exit builders into `ReliveTransitions.kt` so all
> hierarchical navigations share one implementation and stay consistent.

**Acceptance:** theme screen and profile open/close use the same forward/backward
motion as the settings tree; one shared helper drives all of them.

---

## Phase 3 — Container transform (hero expand-to-detail)

Reserve container transform for **hero moments and shallow expand/collapse**. Build
the highest-impact one first.

### Step 3.1 — Photo thumbnail → full-screen MediaViewer (hero)

**Prompt:**
> Implement an M3 **container transform** from a tapped photo/media thumbnail into the
> full-screen `MediaViewer`. Today the viewer appears instantly
> (`shared/.../ui/screens/TimelineScreen.kt` around line 589, `if (viewer != null)
> MediaViewer(...)`).
>
> - Introduce a `SharedTransitionLayout` high enough in the tree to enclose both the
>   thumbnail (in `MomentCard` / `TimelineMediaSection` / `MomentMediaGallery`) and the
>   `MediaViewer`. Give the media a stable `sharedBounds`/`sharedElement` key derived
>   from the attachment id.
> - On open: the thumbnail's container (image + rounded corners) grows/morphs into the
>   full-screen viewer surface. Corner radius animates from the card's radius to 0 (or
>   viewer radius). Use **Emphasized** easing at **`long2` (500ms)** — this is a large
>   hero transition (the guidance's canonical card→fullscreen example).
> - The photo is the persistent hero element; surrounding chrome (viewer controls,
>   wallpaper backdrop) fades in with `emphasizedDecelerate`.
> - On close: reverse — viewer collapses back into the originating thumbnail. Wire to
>   the existing `onClose`/back handling so it collapses rather than cutting.
> - Keep the existing `HorizontalPager` behavior inside the viewer intact.
> - Route through reduced-motion: when reduced, skip the morph and just fade the viewer
>   in/out.
>
> This is the flagship "wow" moment — get it buttery. Verify open AND close both
> animate, and that back-gesture collapse works.

**Acceptance:** tapping a photo visibly expands that photo into full screen and
collapses back to the same thumbnail; 500ms emphasized; reduced motion = fade.

### Step 3.2 — MomentCard → MomentMediaGallery

**Prompt:**
> Using the same `SharedTransitionLayout` and shared-element approach from Step 3.1,
> add a container transform from a `MomentCard` into `MomentMediaGallery`
> (TimelineScreen.kt ~line 573). Share the moment's cover/first-media element as the
> persistent hero. Emphasized easing, `long1`–`long2` (450–500ms). Reduced motion =
> fade.

**Acceptance:** opening a moment's gallery grows from its card and collapses back.

### Step 3.3 — New-Moment FAB → composer

**Prompt:**
> Apply a container transform from the New-Moment control
> (`ui/components/navigation/GlobalNewMomentButton.kt` / the floating toolbar's create
> action) into the `MomentComposer` surface (TimelineScreen.kt ~line 1063 / 1135).
>
> - The FAB is the persistent container: its shape and (optionally) its plus icon
>   morph into the composer sheet. This is the guidance's explicit "FAB with persistent
>   container and icon" case.
> - Emphasized easing at `medium4` (400ms) — the FAB→sheet canonical duration.
> - On dismiss, collapse the composer back into the FAB.
> - Coordinate with the existing composer `AnimatedContent` (there is already an
>   `AnimatedContent` around the composer at TimelineScreen.kt ~905/1135 — reconcile so
>   there is one coherent transition, not two competing ones).
> - Reduced motion = fade + no shape morph.

**Acceptance:** tapping create grows the composer out of the FAB and collapses back;
one coherent transition (no double-animation).

### Step 3.4 — Rediscover collection card → collection screen

**Prompt:**
> Apply container transform from a Rediscover collection card (Favorites / On This Day
> / From Your Past cards) into its full collection screen. These open via
> `rediscoverDestination` changes in `App.kt` (~lines 455–463) that route into a
> `TimelineScreen` in read-only mode. Share the collection's cover image as the hero.
> Emphasized, `long1` (450ms). Reduced motion = fade.

**Acceptance:** each Rediscover collection expands from its card and collapses back.

---

## Phase 4 — Lateral (peer browsing)

### Step 4.1 — Verify media pager is a clean lateral transition

**Prompt:**
> Audit the `HorizontalPager` in `ui/components/viewer/MediaViewer.kt` (and any pager
> in Rediscover carousels). Confirm swiping between a Moment's attachments uses a
> **lateral** transition: content slides horizontally in unison with **no fade and no
> parallax** (fade would weaken the swipe affordance and mimic forward/backward). Use
> the Standard easing set for the settle animation. Ensure zoomed-image state still
> disables pager scroll as it does today. Fix any fade currently applied during page
> change. No hierarchy navigation should use lateral.

**Acceptance:** attachment swiping is a pure horizontal slide, no fade/parallax; zoom
lock intact.

---

## Phase 5 — Enter / exit (components in context)

### Step 5.1 — Composer overlays, dialogs, snackbars, menus

**Prompt:**
> Standardize enter/exit motion for in-context components using `AnimatedVisibility`
> with M3-correct specs (extract shared builders into `ReliveTransitions.kt`):
>
> - **Within screen bounds** (dialogs, menus, snackbars, tooltips): expand/collapse
>   along x or y — **no scale, no z-axis** (M3 avoids elevation-implying motion).
>   Enter `emphasizedDecelerate` `medium4` (400ms); exit `emphasizedAccelerate`
>   `short4` (200ms). A menu expands from the edge nearest its anchor; a bottom
>   snackbar expands upward.
> - **Beyond screen bounds** (composer sheet, pickers in `ComposerOverlays.kt`): slide
>   on from the bottom (sheets enter from bottom — sensible spatial model), slide off
>   on dismiss. Enter decelerate 400ms, exit accelerate 200ms. **Do not fade the sheet
>   as it slides** (fading a sliding sheet creates messy crossfade frames — guidance
>   explicitly warns against this).
> - Apply to `AttachmentPreviews.kt` appearance and any confirm dialogs.
> - Reduced motion: dialogs/menus become a short fade; sheets a short fade.
>
> Note: this pattern is for components in context, NOT for screen-to-screen navigation.

**Acceptance:** sheets slide (no fade), dialogs expand along an axis (no scale/z),
exits are faster than enters, reduced motion degrades to fades.

### Step 5.2 — Scroll-driven app bar & floating controls

**Prompt:**
> Make the top app bar (`ReliveWordmarkAppBar`) and the floating bottom controls
> (`ReliveFloatingBottomControls` / `ReliveBottomBar.kt`) slide off-screen on scroll
> down and back on scroll up, using the enter/exit "beyond screen bounds" motion. The
> app already tracks `navigationToolbarExpanded` in `App.kt` and passes
> `onNavigationToolbarExpand/Collapse` into each screen — drive the slide from that
> state instead of instantly toggling.
>
> - Bar slides up/off the top; bottom controls slide down/off the bottom, emphasizing
>   their shape as they leave (expand/collapse along y).
> - Enter decelerate `medium2`, exit accelerate `short4`.
> - Reduced motion: fade instead of slide.

**Acceptance:** scrolling hides/reveals bars with a slide tied to existing expand
state; reduced motion = fade.

---

## Phase 6 — Skeleton loaders (stable layouts)

### Step 6.1 — Wire skeletons into loading states

**Prompt:**
> Replace empty/loading placeholders with the skeleton primitives from Step 0.4.
>
> - `ui/screens/TimelineHomeScreen.kt`: `TimelineHomeLoading()` is currently an empty
>   `Box` (~line 536). Replace it with `TimelineHomeSkeleton` mirroring the timeline
>   card layout. Cross-fade real content in (`medium1`) when `TimelineHomeContent.Loaded`
>   arrives.
> - Do the same for Rediscover, Search, and MediaStorage loading states (audit each
>   `Loading` branch found in those screens).
> - Ensure the skeleton silhouette matches final geometry so nothing shifts on load
>   (no pop-in).
> - Reduced motion: static placeholders (no pulse), still cross-fade content in.

**Acceptance:** every loading state shows a matching skeleton; content fades in without
layout shift; reduced motion static.

---

## Phase 7 — Shape morph (interaction & progress delight)

Depends on Step 0.3's availability decision. Shape morph defaults to the expressive
(spring) motion scheme — that spring is acceptable here because these are
interaction/progress accents, not screen navigation.

### Step 7.1 — Favorite toggle morph

**Prompt:**
> Make favoriting a Moment morph its indicator shape (not just a color/scale change) to
> communicate the interaction state, per the M3 "morph for interaction states"
> guidance. In `ui/components/rediscover/FavoriteMomentCard.kt` (and wherever the
> favorite toggle lives), morph between two `MaterialShapes` (or `RoundedPolygon`s) —
> e.g. a soft rounded shape → a fuller "bloom"/cookie shape on favorite — using the
> shape library `Morph`. Pair with a gentle haptic (there is already
> `ui/feedback/ReliveHaptics.kt`). Keep it subtle and warm, not cartoonish. Reduced
> motion: cross-fade the two states, no morph.

**Acceptance:** favoriting visibly morphs the indicator + haptic; reduced motion =
fade.

### Step 7.2 — Recording / progress morph

**Prompt:**
> Use shape morph to show **action in progress** in the audio recorder
> (`ui/components/composer/LiveRecorderCard.kt`, `WaveformView.kt`): morph the record
> button/indicator shape while recording (the guidance's "loading indicator uses shape
> morph to show progress" analogue). Optionally drive subtle shape response from audio
> amplitude. Reduced motion: no morph — use a simple static/blinking indicator.

**Acceptance:** recording state is communicated by a morphing shape; reduced motion
falls back.

### Step 7.3 — Floating toolbar / bottom-nav selection morph

**Prompt:**
> In `ui/components/navigation/ReliveBottomBar.kt` and the floating toolbar, morph the
> selection indicator shape as the user changes top-level destination or presses the
> create control (the M3 "standard button group uses shape morph to show interaction"
> case). The New-Moment control's compact→expanded change already animates width; add a
> shape morph so the container feels tactile on press. Keep motion quick and subtle.
> Reduced motion: no morph.

**Acceptance:** selecting a destination / pressing create morphs the indicator/
container; reduced motion static.

---

## Phase 8 — Decorative shape & roundness correctness

### Step 8.1 — Optical roundness audit + fix

**Prompt:**
> Audit every place where rounded media is nested inside a rounded, padded card and
> apply optical roundness (`inner = outer − padding`):
> - `ui/components/timeline/MomentCard.kt` collage tiles, `TimelineMediaSection.kt`,
>   composer previews (`AttachmentPreviews.kt`), timeline-home cards, Rediscover cards.
> - Where an inner media tile has its **own** `RoundedCornerShape` inside a padded
>   parent card, set the inner radius to `outerRadius − padding` rather than reusing the
>   outer radius. Add radii tokens if needed. Where media shares the card's single clip
>   (e.g. `OnThisDayMomentCard.kt` clips the whole card at one radius), leave it — no
>   nesting, no change.
> - Also resolve the `lg=20` naming issue from Step 0.3: confirm each call site wanting
>   "large" actually wants 20 (large-increased) vs 16 (M3 large), and remap to the
>   correct token.

**Acceptance:** nested cards use proportional inner/outer radii; no double-clip at the
same radius; radius token usage matches intended roundness.

### Step 8.2 — Avatar masking & photo crop (decorative, sparing)

**Prompt:**
> Use the shape library for two decorative moments only (guidance: decorative uses are
> the most flexible; use sparingly):
> - **Profile avatar** (`ReliveProfileDimensions.avatarSize`, `ProfileComponents.kt` /
>   `ProfileScreen.kt`): optionally mask the avatar with a soft `MaterialShapes` shape
>   (e.g. a gentle scalloped/cookie) instead of a plain circle — only if it reads warm,
>   not gimmicky. Provide a toggle/default that keeps a circle if in doubt.
> - **Photo-print / cover crop**: apply a tasteful `MaterialShapes` crop to hero cover
>   imagery where it enhances the nostalgic photo-print feel. Do NOT apply unconventional
>   shapes to text-heavy containers.
> Keep both restrained and consistent with the warm editorial aesthetic. If a shape does
> not clearly add delight, don't add it.

**Acceptance:** avatar and/or cover use a tasteful shape treatment; text containers
untouched; nothing feels cluttered.

---

## Phase 9 — Fix existing motion defects & consistency pass

### Step 9.1 — Fix overlapping crossfades

**Prompt:**
> The incoming-share route transition in `App.kt` (~line 219) and the moment-selection
> app-bar transition in `TimelineScreen.kt` (~line 905) currently run `fadeIn + slideIn
> togetherWith fadeOut + slideOut` — fades overlapping in parallel, which the M3
> guidance calls a defect (messy partially-transparent frames). Rework them so outgoing
> fades out first, then incoming fades in (sequential), or hide the crossfade during the
> fastest part with a short duration. Reuse the shared transition helpers from
> `ReliveTransitions.kt`. Also migrate their hardcoded `standardMillis`/`fastMillis`
> onto the correct M3 enter/exit tokens (decelerate-in / accelerate-out).

**Acceptance:** no two content layers are simultaneously semi-transparent during these
transitions; durations/easings come from tokens.

### Step 9.2 — Consistency & reduced-motion QA

**Prompt:**
> Final pass:
> - Grep for any remaining hardcoded `tween(...)`, raw millisecond literals, or
>   `RoundedCornerShape(<dp literal>)` in `ui/` and route them through tokens.
> - Verify every navigation type uses exactly one pattern app-wide (hierarchy =
>   forward/backward, peers = lateral, top-level = fade-through, hero expand = container
>   transform, in-context = enter/exit). Fix any mismatch.
> - Enable OS reduced motion and walk every flow: confirm all transitions degrade to
>   subtle fades and all shape morphs/parallax are disabled.
> - Confirm no jump cuts remain (every screen change animates).
> - Update `docs/DESIGN_SYSTEM.md` sections 13 (durations) & 14 (easing) and add a shape
>   scale + morph section documenting the final tokens.

**Acceptance:** no stray literals; one pattern per nav type; reduced motion clean
throughout; design system doc updated.

---

## Suggested order & grouping

1. **Phase 0** (all four steps) — nothing else works without it.
2. **Phase 1** + **Phase 2** — cheap, high perceived-quality lift across the whole app.
3. **Phase 3.1** — flagship hero (photo→viewer). Ship and feel it before the rest.
4. **Phase 6** (skeletons) — removes the "empty box" jank.
5. **Phase 3.2–3.4**, **Phase 5** — remaining transforms + component motion.
6. **Phase 4**, **Phase 9.1** — audits/fixes.
7. **Phase 7**, **Phase 8** — shape morph & decorative delight (the "addictive to play
   with" layer).
8. **Phase 9.2** — final QA.

## How to continue in a new chat

This effort spans many chats. To resume in a fresh session, paste the prompt below and
fill in the one blank. Update the **Progress status** table (`[x]`) after each step lands,
and commit per step (e.g. `feat(motion): Phase 1.1 top-level fade-through`) so `git log`
also reveals what's done.

```
Relive app — continuing a staged Motion & Shape implementation.

The full plan lives at docs/MOTION_AND_SHAPE_PLAN.md. Read it first, plus
docs/DESIGN_SYSTEM.md (strict token rules) and ui/theme/ReliveMotion.kt +
ReliveDimensions.kt for current token state.

I want to work on: <PHASE / STEP — e.g. "Phase 3, Step 3.1: photo→MediaViewer container transform">

Before writing code:
1. Read the plan doc and locate that step's prompt, files, specs, and acceptance criteria.
2. Check the Progress status table and git log to see what earlier phases already landed,
   so you don't redo or conflict with completed work.
3. Verify the Foundation (Phase 0) tokens/helpers that step depends on already exist. If a
   dependency is missing, tell me which and stop — don't silently skip it.

Then implement ONLY that step, following its exact durations/easings/tokens, routing every
animation through the reduced-motion helper, and using tokens only (no hardcoded ms/dp/shapes).
When done: report against that step's acceptance criteria, confirm the build compiles, tick the
step in the Progress status table, and suggest a commit message.
```

---

## Definition of done (whole effort)

- Every navigation and component transition uses a tokenized M3 pattern; no jump cuts,
  no overlapping crossfades.
- Reduced-motion users get clean fades everywhere; no motion sickness triggers.
- Loading states show matching skeletons; no layout shift.
- Shape morph rewards interaction (favorite, record, select) and progress.
- Decorative shape is used sparingly and warmly (avatar, covers); optical roundness is
  correct on nested cards.
- The app feels tactile and delightful to navigate while staying true to Relive's warm,
  nostalgic, editorial character.
