# Relive — Product Specification

This is the authoritative product definition for Relive. All development derives from this document. Do not invent behavior beyond what is specified here; where something is deferred, it is marked as such.

---

## 1. Product

**Name:** Relive

**Core idea:** *Capture moments. Relive them later.*

Relive is a private, **local-first** personal memory timeline. Users save:

- thoughts
- memories
- journal-like text
- photos
- videos
- audio
- combinations of text and media

The product must feel like a **beautiful personal life archive**, never a notes/database/CRUD application. This feeling is a hard product requirement, not a stylistic preference. Every decision serves it.

---

## 2. Navigation model

The app opens at the top of a single unified **Home surface**. Home is the app's only root: one continuous vertical scroll that runs welcome greeting → Rediscover collection row → All moments timeline. Custom timelines are not part of Home: they keep their own **Timelines** destination, where Timeline Home lists them and opens the existing scoped timeline detail unchanged.

There is always a built-in timeline:

- **All** — contains every saved moment. Logically it is the whole archive; the All moments feed rendered on Home is always **bounded** — newest-first, paged/windowed, with incremental loading as the user scrolls toward older Moments. The root surface must never observe or hydrate the complete archive on launch, so Home reads the feed through a bounded, anchored, SQL-backed paged projection, consistent with how the Rediscover collections are already read.

Users may create **custom timelines** representing chapters of their life, such as:

- College
- Japan 2026
- Family
- Internship
- Travel
- Relationship

All timelines — built-in and custom — share the **same Moment presentation and interaction model**: rail, dots, Moment cards, media treatment, and the inline composer are identical everywhere, and the information hierarchy inside a Moment never varies. A timeline may have its own visual theme. Navigation and chrome are what differ by surface: All moments is a section of the Home surface, reached by scrolling and carrying no header or Back of its own, while a custom timeline opens as a scoped detail screen with its own header, Back, and timeline actions.

Selecting a custom timeline opens the shared timeline detail experience scoped to that selection; returning goes back to the Home surface with its scroll offset preserved exactly as the user left it — focused All moments stays focused and never jumps back to the top state. The logical All timeline is not a separate destination: it renders on Home under the `All moments` heading and is reached by scrolling, never by tapping a Rediscover card.

### The Home surface

Home renders, in one scroll container with one scroll position:

1. the **welcome block** — greeting and subtitle;
2. the section heading `Relive your memories`, followed by the horizontally scrollable **Rediscover collection row** (§2A);
3. the section heading `All moments`;
4. the **inline composer**, collapsed to its rail `+` marker, at the chronological end of the feed — which, because the feed is newest-first, renders at the **head** of the feed directly beneath the `All moments` heading;
5. the **All moments feed** itself.

Home opens with `Welcome back, {name}` when a real profile display name exists, and with exactly `Welcome back` — no trailing punctuation, no placeholder — when it does not. The `Your Relive` fallback label never appears in the greeting, and no device- or account-derived name is ever substituted. The subtitle is always `Your memories are waiting for you.`

Rediscover content and the All moments timeline are parts of **one** surface. Neither is a separate page, route, destination, tab, or screen.

Home has exactly two named states, and both are purely a function of scroll offset:

- **Home top state** — the welcome block and the Rediscover row are in view.
- **Focused All moments** — welcome and Rediscover have scrolled above the viewport, the `All moments` heading is pinned, and the timeline fills the screen.

Moving between them is scrolling. It produces **no navigation event, no route change, no back-stack entry, and no screen transition**, and it uses no fade-through, container transform, or shared-axis motion — the motion is the scroll itself. The app always opens in the Home top state at scroll offset zero, with no programmatic scroll on entry. The welcome and Rediscover sections leave by scrolling and return **only** when the user manually scrolls back up; nothing else restores them — not Keep Moment, not Back, not `×`, and not returning from a custom timeline, a read-only collection, Search, or Profile.

The Relive app bar stays pinned across both states and condenses to a compact form as the surface scrolls; in focused All moments the `All moments` section heading pins directly beneath it.

The Android notification shade is an **interaction reference only**, for the single idea of one continuous, reversibly scrollable surface whose top region and list region trade dominance as you scroll. It is never a visual reference, and no shade visual language is adopted.

### Destinations

There are three top-level destinations: **Home**, **Timelines**, and **Search**. Home is the primary landing destination and the app's default. Timelines is unchanged from before this redesign — Timeline Home remains its root, listing custom timelines and opening their existing scoped detail screens. Search is unchanged. They live in a floating bottom-left navigation toolbar: it shows the active destination icon while collapsed and expands horizontally to reveal its icon actions in Home / Timelines / Search order, collapsing to the active destination icon on scroll and expanding again on reverse scroll. There is **no Rediscover destination icon**, because Rediscover is a row inside Home. Custom timeline detail, the read-only Rediscover collections, and Profile are auxiliary surfaces layered above their destination, never roots.

### Global quick capture

All three destinations — the Home surface in **both** its top state and focused All moments, Timeline Home, and Search — expose one theme-aware expressive **`+ New`** floating toolbar at bottom-right, separate from the navigation toolbar. Wherever it is tapped it always lands on Home in focused All moments with the existing inline composer expanded; it never creates a Moment in a custom timeline. It has two presentations — collapsed to its Add icon, and expanded to show a centered `+ New` — and both controls remain vertically aligned with a small fixed gap. Navigation uses an accent-derived moving selected indicator while expanded. `+ New` presentation is defined **per Home state, not per raw scroll direction**: it does not collapse to a bare Add icon merely because the user scrolled down into focused All moments. It is **hidden while the inline composer is expanded** — Keep Moment is then the primary action — and it is absent from Profile, custom timeline detail, read-only Rediscover collections, media viewer, camera, recorder, and modal/detail surfaces.

Tapping either visible part brings Home into focused All moments and expands the **existing** inline composer in place, from the timeline rail, at the head of the All moments feed, performing only the minimum scroll needed to seat the composer and its Keep Moment button in view. It never navigates, never opens a chooser or a custom timeline, never creates a second composer, and never opens a modal, dialog, bottom sheet, or separate composer screen. Per ADR-0059 it requests no field focus and does not open the IME; the person taps a field when ready. The composer's own in-place expansion is the only animated element in the flow.

On Home the floating `+ New` and the rail `+` are the same creation affordance in two positions, and both remain visible in focused All moments. Custom timeline detail keeps its integrated rail `+` as its only creation affordance instead of showing a floating action.

### Back on Home

Back precedence on the Home surface, in order: (1) an open contextual selection bar, (2) an expanded inline composer, (3) the platform default. Exiting selection or collapsing the composer keeps the user in focused All moments at the same scroll offset and never restores the Home top state.

### Android external share capture

Android accepts a user-initiated system share of plain text/URLs, images, videos, audio, or a supported mixed batch. Relive first asks which timeline should receive one new Moment: All is first, followed by custom timelines. Selecting a custom timeline creates the normal All + custom membership; selecting All retains the normal optional assignment controls and brings Home into focused All moments rather than opening a separate All screen. Shared content only pre-fills the existing inline composer and is never saved without the user choosing Keep Moment. Unsupported, unreadable, or mixed batches containing unsupported files are rejected as one request; arbitrary documents are not attachments in v1. This capability is Android-only for now.

### Moment / timeline relationship

- A moment is **stored only once**.
- Timelines **reference** moments; they never duplicate them.
- A moment may belong to:
  - **All** — automatically, always
  - **zero or more** custom timelines

Membership rules:

- If a moment is **created inside a custom timeline**, it automatically belongs to **All** and to **that current timeline**.
- If a moment is **created from All**, the user may optionally assign it to one or more custom timelines.

`All` membership is logical and automatic — it is not stored as an explicit per-moment membership row. See [`ARCHITECTURE.md`](ARCHITECTURE.md) for persistence design.

## 2A. Rediscover

Rediscover is a horizontally scrollable **collection row inside the Home surface**, rendered under the `Relive your memories` heading and above the `All moments` heading. It is not a root, a destination, a screen, a chronological timeline, or a recommendation feed.

- The row holds four collection cards, in this fixed order: **Favourites**, **On This Day**, **From Your Past**, **All Photos**. It has no app bar, no editable All timeline card, and no vertically stacked editorial sections of its own; the app bar and the floating toolbars belong to the Home surface (§2).
- **All Photos** is a bounded, read-only system collection of Moments that have at least one image or video attachment, read through the same bounded projection pattern as Favourites and From Your Past. It introduces no new table, no membership, and no duplicate persistence. It is **never** an entry point into the editable All moments feed, which is always present on Home beneath the `All moments` heading. Its card cover is generated automatically: up to nine distinct image/video previews selected deterministically from a three-hour epoch-time bucket. Selection count and curated 1–9 arrangement may change between buckets but remain stable through recomposition, scrolling, navigation, and configuration change within one bucket. Audio and text-only Moments never become collage cells. With no visual media, the card uses the neutral no-cover placeholder. Users cannot choose an All Photos cover.
- **Favourites** is derived reactively from each Moment's persisted favorite state. It is not a custom timeline, membership, or duplicate persistence record.
- Tapping the Favourites card opens the complete read-only Favourites timeline; the card is its single entry point. Individual favorited-Moment cards live inside that collection, not on Home, and positioning at an individual Moment applies only inside it. Every compact card there shares one fixed visual-region height: image/video cards use their first ordered visual attachment as the lead visual and quietly show an additional-attachment count; text-only and audio-only cards use the theme-aware deterministic generated cover with no fake media, icon, or illustration. This compact-card exception does not change normal Timeline presentation. With zero favorites, the collection shows `No favorite moments yet.` and `Moments you favorite will appear here.`
- The Favourites detail is strictly read-only: it has Back and a centered Favourites title, but no composer, creation, edit, forget, membership, or favorite-mutation controls. Media viewing and playback remain available.
- The Home surface uses bounded read projections with batched attachment loading throughout — for the Rediscover row and for the All moments feed alike. It never hydrates the full Favourites collection, the full All Photos collection, or the complete archive to render.
- **On This Day** is the second card in the row. Its editorial date is the current device-local day/month, and the bounded collection it opens represents only matching dates in previous local calendar years. The current year is excluded; February 29 matches only previous February 29 Moments. A Moment's secondary label is the exact calendar-year anniversary, e.g. `2 YEARS AGO`. It opens a read-only system collection positioned at the selected Moment. When no Moment is eligible the card is omitted and the row closes up without leaving a gap or reserved spacing.
- **From Your Past** is the third card in the row. Its bounded, deterministic selection of at most ten distinct Moments defines the contents of the read-only collection the card opens, not a shelf on Home. It selects only Moments at least 90 days old, excludes future timestamps and active On This Day matches, and does not exclude favorites. Selection and order are deterministic for a device-local calendar day, change with the next local day, and are read from a bounded SQL-backed projection with batched attachments. Cards inside the collection use the same compact dimensions and visual framing as Favourites but show no heart, and the detail retains that order. If no Moment is eligible, the From Your Past card is omitted from the row; Home never substitutes sample memories.
- **Deferred capability:** Places and Tags retain their local read-model implementation for a later Rediscover presentation phase, but are not collected or rendered by the current Rediscover row.

- **Future Places** groups readable saved location labels only; raw coordinates, maps, and geocoding are not introduced.
- **Future Tags** reuse the existing canonical tag system and rank tags by Moment usage.
- No Moment content leaves the device, and no analytics, cloud, AI, or recommendation backend is used.
- Empty collections remain quiet and editorial; Relive never substitutes sample memories.

---

## 3. Timeline UI

**The approved reference design is authoritative for Relive's warm editorial visual identity — typography, color, rail, card, and media treatment — only:** `docs/ui-reference/timeline-reference.png` and `docs/ui-reference/timeline-reference.html`. It is not authoritative for the Home surface's composition, ordering, or scroll model, which are defined in §2.

Visual direction:

- warm editorial / nostalgic aesthetic
- generous whitespace
- elegant **serif** typography for titles
- clean **sans-serif** typography for body and metadata
- subtle brown/sepia accents
- cream background
- thin vertical timeline rail
- small circular timeline dots for existing moments
- a plus-circle integrated into the timeline for creating a new moment
- subtle borders
- minimal shadows
- no excessive cards
- premium media presentation
- highly polished spacing and typography
- collection-card media/generated covers meet their lower information surfaces directly, with no fade, overlap, or fake shadow

The interface must **encourage continuous scrolling**. It must **not** feel like a database or a list of records.

When a user manually moves toward older Moments, a bottom-centered return-to-newest arrow appears. On Home it is scoped to **focused All moments only**: because that feed is newest-first, movement toward older Moments is downward scroll, and the control returns to the newest end at the head of the feed. It is hidden whenever the Rediscover row is visible, so it never competes with the upward scroll that restores the Home top state, and it never restores that state itself. In custom timeline detail it behaves as before. It remains available until the newest end is reached. Selecting it visibly and rapidly scrolls to that newest end; touching the scrolling timeline cancels the motion at the current Moment without activating content beneath that touch. The control is absent on empty and non-scrollable timelines.

### 3.1 Date navigation

All moments and every custom Timeline have a Calendar action for navigation, never filtering; on Home it stays reachable in focused All moments. The selected date uses the current device-local calendar day: an exact date moves to that day's first Moment in the feed's existing order; a missing date moves to the next available Moment, or the closest previous Moment when no later Moment exists. On Home this is explicit Calendar navigation only, never an entry position: it resolves a Moment and the bounded feed re-anchors its window there rather than paging through history, and the app always opens at the top of Home with the All moments feed never auto-positioned on launch. The empty global Search capsule has the same Calendar action, which leaves Search and returns to Home in focused All moments at the resolved Moment. Entering a non-blank search query replaces that action with the result counter and previous/next controls.

Timeline Home, the root of the Timelines destination, has a separate capsule for filtering the already-observed custom timeline summaries by timeline name. It uses live, case-insensitive partial matching and preserves the repository's newest-first order. The logical All timeline and Moment fields never participate. This capsule has a search glyph and the `Search timelines...` field only: it has no Back, Calendar, result counter, or previous/next controls. A non-blank query with no matches shows `No timelines found.` It belongs to the Timelines destination, not to Home.

Concrete visual tokens (colors, typography, dimensions) are defined in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) and must match the reference.

---

## 4. Moment structure

A moment may contain:

1. **immutable creation timestamp** (`createdAt`)
2. optional **location**
3. **title**
4. **content**
5. zero or more **media attachments**
6. zero or more **tags**
7. **favorite** state
8. membership in zero or more **custom timelines**

Rules:

- **Text-only moments are fully supported.**
- **Do not show empty media placeholders.** A moment with no media shows no media area at all.

### 4.1 Timeline metadata format

Saved moments display metadata on the eyebrow row as:

`DATE • TIME`

Example: `AUGUST 22, 2026 • 10:48 AM`

- Both date and time are derived from the same immutable `createdAt`.
- Both are rendered in the device's local time zone.
- Date and time appear on the **same eyebrow row**, separated by a centered dot (`•`).
- Location, when present, is optional secondary metadata on the same row.

### 4.2 Tags

- UI displays lowercase hashtags: `#travel`.
- The `#` prefix is **presentation-only** — it is supplied by the UI, **not** stored as part of the tag identity or label.
- The composer tag input visually includes a permanent `#` prefix; the user types only the tag text.
- Leading `#` characters typed manually by the user are stripped before processing.
- New tag labels are **normalized to lowercase** before persistence.
- Existing persisted uppercase labels may remain in the database but always **render lowercase** in the UI.
- Tag identity uses a canonical form (lowercased, whitespace-collapsed, trimmed) for deduplication; the first persisted display label wins (ADR-0013).

### 4.3 Content expansion

- Long content initially shows only a few lines.
- At the end, show a WhatsApp-style `... more` control.
- Tapping `more` expands the complete content **inline**.
- Expanded content may provide a `less` control to collapse it again.

---

## 5. Media

Supported media types:

- image
- video
- audio

Rules:

- A moment may have **multiple attachments**.
- Multiple attachments use an **adaptive visual collage** inline (see [`DECISIONS.md`](DECISIONS.md) ADR-0019 for layout rules by count). All media types — image, video, audio — participate as visual tiles.
- For 5+ attachments, only the first four render inline; the fourth tile shows a translucent `+N` overlay.
- Media should feel **integrated into the timeline**, not enclosed in heavy cards.

### 5.1 Single-media presentation

- Adaptive natural/aspect-preserving sizing: the container shrink-wraps around the media's actual aspect ratio.
- Max width and max height are **constraints only**, never forced dimensions.
- Smaller media is **not** stretched to fill — it remains at its natural size.
- Large media scales down proportionally when either max bound is exceeded.
- The image/video/audio container follows the actual displayed media shape.
- A visible border uses the same semantic color as timeline dots (`color.accent` / `#6F4E37`).
- Single-media outer border thickness matches multi-media outer border thickness.

### 5.2 Multi-media collage

Adaptive collage layout by attachment count:

| Count | Layout |
|-------|--------|
| 1 | Adaptive single tile (§5.1) |
| 2 | Two equal tiles side-by-side |
| 3 | One dominant tile + two vertically stacked tiles |
| 4 | 2×2 grid |
| 5+ | First four tiles + `+N` overlay on fourth tile |

- Mixed photo/video/audio supported in the same collage.
- Attachment order is preserved (follows `sort_index`).
- Each media tile has a visible internal boundary/divider.
- Outer collage border exists.
- Internal gaps and outer multi-media border use the same approved weight (`4dp`), so adjacent tiles yield one separator rather than two overlapping strokes.
- Border color matches timeline-dot color (`color.accent`).

### 5.3 Timeline video playback

**Single-video inline behavior depends on whether the video was constrained:**

- If the video fits within timeline bounds **without** being constrained:
  - Play button starts **inline playback** in the exact same adaptive bounds.
  - Pause works inline.
  - Tapping elsewhere on the video opens the **full-screen viewer**.
- If the video had to be **constrained** by timeline max bounds:
  - Play button opens the **full-screen viewer** directly.
  - Body tap opens the **full-screen viewer** directly.
  - No inline player starts.

Multi-media collage video tiles always navigate to the gallery/viewer rather than inline playback.

**Playback lifecycle:**

- Only one active media playback owner at a time (`ActivePlayback`).
- Navigating away stops previous playback.
- No background playback.

### 5.4 Timeline audio visual identity

Audio is represented as a visual media tile, not a traditional audio player or voice-message bubble.

- **Canvas:** black/near-black tile with generous negative space.
- **Waveform:** compact centered row of white vertical rounded-capsule amplitude segments, symmetric around the horizontal midline.
- **Real data:** waveform represents actual audio signal amplitude — silence renders as tiny/nearly-flat segments, loud sections as taller segments.
- **Segment count:** approximately 9–17 visible segments depending on tile width.
- **Paused state:** waveform window visible, segments stationary, Play affordance, subtle duration indicator.
- **Playing state:** the waveform window moves horizontally with playback — past segments exit left, future segments enter right. Segment heights remain determined by real envelope data at every frame.
- **No:** random equalizer bars, thin continuous line, per-segment bounce animation, bar-width variation.
- Playback lifecycle is coordinated with other media through `ActivePlayback`.

See ADR-0019 §4 for the full specification.

### 5.5 Multi-media gallery and full-screen viewer

Navigation hierarchy depends on attachment count:

**One attachment:**
Timeline → Full-screen viewer

**Two or more attachments:**
Timeline collage → Moment media gallery → tap item → Full-screen viewer

**Gallery:**
- Shows all attachments for the Moment in original order.
- Mixed image/video/audio.
- `+N` from timeline opens the gallery (not the viewer directly).
- Back returns to timeline with scroll position preserved.

**Full-screen viewer:**
- Dark/black surface.
- Image, video, and audio supported.
- Opens at exact selected index.
- Mixed-media horizontal navigation allowed in viewer.

**Image viewer:**
- Image initially fitted to available viewport, preserving aspect ratio and orientation.
- Pinch-to-zoom.
- Pan while zoomed.
- Double-tap toggles fit ↔ zoom.
- At fit: horizontal swipe pages between attachments.
- While zoomed: image owns pan; pager is disabled.
- Correct orientation.
- Back returns to gallery (if 2+) or timeline (if 1) with scroll position preserved.

**Video viewer:**
- Play/Pause.
- Progress/seek.
- Sound.
- Correct orientation and aspect ratio.
- Playback released when leaving page/viewer.

**Audio viewer:**
- Same real waveform visual identity as timeline tile.
- Full-screen black surface.
- Waveform remains bounded/centered (not stretched full-width).
- Play/Pause + progress/duration.

---

## 6. Creating a moment

Creation happens **inline inside the current timeline**. The composer sits at the chronological end of the timeline, after the newest moment.

On the Home surface the All moments feed is newest-first, so that chronological end renders at the **head** of the feed, directly beneath the `All moments` heading: the composer's rail terminates at the center of its plus marker and **no rail continues above it**. The composer is therefore always inside the loaded newest window, and `+ New` reaches it with a short scroll rather than by paging through history. In custom timeline detail the composer keeps its existing position at the end of the timeline, where its rail terminates at the center of the final plus marker and no rail continues below it.

### 6.1 Composer collapse/expand behavior

The composer is **collapsed by default**. In the collapsed state:

- Only the **`+` timeline marker** (plus-circle) is visible.
- Tapping `+` **expands the existing composer inline** at the same timeline position.
- No modal, no bottom sheet, no separate screen — the composer opens in place.
- Expansion and collapse are **smoothly animated** (`AnimatedContent` with expand/shrink vertical transitions).
- The `×` reset button resets all fields and **collapses** the composer.
- A successful **Keep Moment** resets all fields and **collapses** the composer to its rail `+` marker. The surface does not move with it: saved from Home, the user remains in focused All moments at the same scroll offset, and the welcome and Rediscover sections are never automatically restored (see §6.7).
- System/visible Back collapses the composer and preserves its unfinished draft without a discard dialog. On Home it only collapses the composer — the user stays in focused All moments at the same scroll offset and the Home top state is not restored; in custom timeline detail it collapses the composer and then leaves. Reopening that same composer restores the draft during the app session; `×` remains the explicit discard-confirmation path. Back precedence on Home is given in §2.
- Keyboard behavior keeps the active composer usable above the IME (see ADR-0016).
- Entry from global `+ New` brings Home into focused All moments and runs this same in-place expansion transition on the existing rail composer, scrolling only the minimum needed to seat the composer and its Keep Moment button. There is no navigation, no waiting on a fresh content projection, no settled-frame handoff, and no arbitrary delay. Per ADR-0059 the composer requests no field focus and opens no IME — the person taps a field when ready — and subsequent media actions do not request focus either.

### 6.2 Composer fields

The expanded composer contains:

- automatically generated **date/time** (from `createdAt`, device-local timezone)
- optional **detected/selected location** (see §7)
- **title**
- **content**
- **tags** (see §4.2 for tag behavior)
- **media attachments**
- **Add Media** control
- primary action: **Keep Moment**, rendered as a prominent, centered, theme-aware Material 3 primary button
- reset/cancel **`×`** at the top-right

### 6.3 Add Media flow

Tapping **Add Media** reveals three evenly distributed actions inside one rounded Relive surface:

- **Voice** (audio recording)
- **Camera** (photo or video)
- **Media** (the existing photo, video, or audio library choice)

Before Add Media, Voice, Camera, Media, or a nested photo/video/audio picker or recorder experience opens, Relive clears composer focus and dismisses the software keyboard. Returning preserves the complete draft and does not automatically reopen the keyboard.

After media is added:

- the attachment appears **above** Add Media
- each attachment has its own remove **`×`**
- **Add Media moves below** existing attachments
- the user may continue adding more media

The entire composer can be reset with the top-right **`×`**.

### 6.4 Composer media preview

- Media previews use **adaptive natural/aspect-preserving sizing** (same rules as timeline §5.1).
- Max composer width/height are constraints only — smaller media remains smaller.
- A thin boundary hugs actual preview bounds using the Relive semantic border color.
- No oversized empty outer card wrapping the preview.

**Video composer preview:**
- Poster/thumbnail shown while ready/paused.
- Processing placeholder uses the expected future adaptive dimensions (probed from source metadata).
- Spinner centered in actual preview bounds.
- Processing → Ready transition should not cause a size jump.
- Ready state shows poster + Play, not a black box.
- Inline playback remains in same adaptive bounds.
- No distortion.

### 6.5 Multi-select and processing placeholders

- Device picker supports **multi-select** where platform APIs allow.
- Multiple photos, videos, and/or audio can be selected together.
- Mixed photo/video selection where supported.
- Selection order is preserved.
- Each selected attachment gets a **stable draft identity** (`draftId`).
- Processing placeholders appear **immediately** on selection.
- Large media processing is asynchronous and off the UI thread.
- Each tile shows a centered circular processing indicator.
- Completed media replaces its own placeholder **in place**.
- Failures remain visible with retry/remove behavior.
- Processing concurrency is bounded.
- No fake determinate percentage if real progress is unavailable.

### 6.6 Audio recording

Active recording row layout: **Stop | flexible waveform | duration | ×/remove**

- All controls are in a normal `Row` layout — no absolute positioning or overlays.
- Duration and `×` must **never overlap**.
- `×` retains proper touch target (`48dp` minimum).
- Live waveform uses real amplitude data from the platform microphone.

### 6.7 Save behavior

Pressing **Keep Moment** saves the moment. After save:

- the composer resets and collapses to its rail `+` marker
- the saved moment renders immediately in the timeline adjacent to the collapsed composer, taking a normal timeline dot and adopting the standard timeline presentation
- the surface does not move: there is **no app-initiated scroll of any kind** — not to the top, not to the newest item, not to the composer. Saved from Home, the user remains in focused All moments at the same scroll offset, and the welcome and Rediscover sections are never restored.

---

## 7. Location

Relive currently supports **optional manual location entry** when creating or editing a Moment. The lightweight location-pin field sits directly below the automatically generated date/time and accepts an unstructured readable label such as `Jalandhar`, `NIT Jalandhar`, `Home`, or `Central Park`. Empty location is valid. The value is persisted with the Moment, survives media round-trips, participates in the existing edit flow, and resets with the rest of the composer after a successful save or explicit reset. Saved Moment cards render a readable location directly below `DATE • TIME`; display trims surrounding whitespace and capitalizes only the first character without mutating persisted data.

GPS/location detection, Maps, geocoding, and location permission requests are **future work**. This release does not attempt device location. When detection is later activated it remains moment-scoped and on-demand: never continuous/background tracking and never collection while merely browsing.

### 7.1 Location representation

Prefer a **readable place representation** for the UI, e.g. `NIT Jalandhar, Punjab` — **not** raw coordinates.

Internally, the location model is designed to **optionally** preserve:

- latitude
- longitude
- human-readable place name
- locality / city
- region / state
- country

Rules:

- **Do not require every field to be present.**
- **Do not expose raw coordinates** in the normal timeline UI.

### 7.2 Future GPS failure handling

When GPS detection is implemented later, location acquisition must handle all of the following, and in every case the composer continues normally and the moment can still be created and saved:

- permission denied
- permission permanently denied
- location services disabled
- location unavailable
- timeout / failure

The location abstraction and platform boundary are defined in [`ARCHITECTURE.md`](ARCHITECTURE.md).

### 7.3 Location privacy

- Relive is local-first. Detected location data remains **local with the moment** unless a future, explicitly approved feature requires otherwise.
- **No background location tracking.**
- **No location history separate from saved moments.**
- **No third-party location analytics.**

---

## 7A. Camera behavior (Android)

Camera is accessed from the composer's Add Media → Camera action. The camera captures both photo and video through a single in-camera surface with a Photo/Video mode selector.

The Android Relive UI is locked to portrait at the application Activity configuration, independent of the device auto-rotate setting. This is a UI constraint, not a replacement for capture orientation handling: CameraX still writes capture rotation metadata, photo review/persistence still normalizes EXIF orientation, and video playback still respects recorded metadata. Relive's equivalent iOS product requirement is portrait-only UI through the Xcode/Info.plist supported-interface-orientation configuration; that separate platform setting must be implemented and verified on macOS rather than inferred from the Android configuration.

### Layout

- Full-screen preview with flash icon (upper-left), zoom controls, main control row (Gallery / Shutter / Switch), and Photo/Video selector.
- Shutter button is the fixed geometric center of the main control row.
- All controls use `WindowInsets.safeDrawing` — no hardcoded status/nav bar heights.

### Controls

- **Front/rear switching:** in-camera switch control (loop-arrow icon) + double-tap on preview surface. Both are disabled while a video recording is in progress. Hidden when device reports no front lens.
- **Flash/torch:** icon-only two-state Off/On toggle (outlined bolt / filled bolt). No Auto state, no text labels. Photo mode maps to `flashMode`; Video mode maps to `enableTorch`. Default is Off. Muted and inert when bound lens has no flash unit.
- **Zoom:** Pixel-style dynamic presets (0.5×/1×/2×) filtered by actual CameraX zoom range + pinch-to-zoom live ruler. No faked digital 0.5×.

### Capture feedback

- **Platform-native sounds, no bundled audio assets.** Android uses `ToneGenerator` on `STREAM_MUSIC` (not `MediaActionSound`, which is inaudible on most Android 12+ OEM skins).
- **Photo:** tone + haptic fire on `onImageSaved` (real success only).
- **Video start:** start tone plays and completes (with guard delay) **before** CameraX opens the microphone, preventing audio bleed into the recording.
- **Video stop:** stop tone fires **after** CameraX releases the microphone.
- **iOS:** `UIImagePickerController` provides its own native feedback; Relive does not layer additional sounds.

### Photo and video review

- **PhotoReview** before Confirm: photo must already display with correct orientation on first frame — no visible rotate-then-correct snap.
- **VideoReview** before Confirm: Play/Pause supported; video trim/mute/editor behavior available.
- **Retake / Confirm** actions.
- Expensive processing stays off the UI thread; review appears quickly without waiting for full post-processing.

Platform-specific camera behavior should not be described as identical between Android and iOS where it is not.

---

## 8. Editing and forgetting

A moment may be **edited** or **forgotten** only during the **first 4 days after its original creation time**.

- The rule uses the immutable `createdAt`.
- **Never extend the edit window based on `updatedAt`.**

### Within the first 4 days

Long-pressing a Moment in **All moments** on Home selects it and smoothly animates a contextual Material 3 app bar in over Home's app bar, in either Home state, without leaving the surface or restoring the Home top state; All moments needs no persistent header of its own. Long-pressing in a custom timeline replaces that timeline's header the same way. Back exits selection before any other Back behavior (§2), and the bar itself has a Back action that exits selection, plus:

- **Edit**
- **Add to timeline** — opens a single-choice add-only picker for custom timelines. Already assigned timelines remain visible but disabled. Choosing an unassigned timeline immediately creates only that membership; it does not duplicate the Moment.
- **Forget**

### After 4 days

- no editing
- no forgetting
- Edit and Forget are absent from contextual actions.
- **Add to timeline** remains available when at least one custom timeline exists because membership is archive organization, not content editing.
- When no custom timeline exists, long-press exposes no action after the edit window closes.

### Editing behavior

- Editing happens **inline inside the timeline**.
- Existing media attachments receive **remove controls** while editing.
- The user can **add additional media** while editing.
- Saving inline edits may occur when the user **taps outside the active editor**, but interactions **within editor controls must not accidentally trigger a save**.

### Forgetting behavior

- **Forget requires confirmation** before permanent removal.

---

## 9. Search

Search v1 is the one separate destination alongside the Home surface. It searches the complete local archive, not a selected timeline.

- The autofocus `Search memories...` field performs a debounced, case-insensitive SQL search across Moment title and content.
- Empty queries show `Find anything you've saved.` and never render the whole archive. No results show `No moments found.`
- Matches keep their own chronological ordering and the full timeline rail/card/media presentation, but are strictly read-only. Media viewing/playback remains available. Search results are not the All moments feed, so Home's newest-first, bounded/paged feed rules do not govern them.
- The first match is active. The `N / total` counter and up/down controls move through matches without wrapping and scroll the active Moment into view.
- Search state (query, result selection, and scroll position) persists while the user leaves Search for Home and returns during the app session. Home's own scroll state — top state or focused All moments — is likewise preserved across those returns, except when the Search Calendar action resolves a Moment, which returns to Home in focused All moments positioned at that Moment (§3.1).

Filters, chips, categories, Tags/Places tabs, suggestions, search history, relevance ranking, AI search, and timeline-name results are not part of Search v1. Text highlighting is deferred unless it can be added without restructuring MomentCard.

---

## 10. Favorites

- Every moment has a subtle **favorite/heart** action.
- Favorite state must **not** visually dominate the moment.
- The Rediscover row's Favourites card and the full read-only Favourites timeline are derived from this same state, so favoriting or unfavoriting elsewhere updates both immediately.

---

## 11. Themes

All timelines share the **same Moment presentation and interaction model**. Themes only change **presentation**.

Selectable palettes:

- **Original** (Warm Journal)
- **Evergreen**
- **Lilac Dusk**
- **Crimson Keepsake**
- **Blue Hour**
- **Rosewood**

The global appearance mode is **System**, **Light**, or **Dark**. System follows the live platform appearance. The selected palette is the app default. All moments and each custom timeline own independent `TimelineAppearance` values; All's appearance is stored in native local preferences because All is logical, while custom timeline appearances are archive data. On Home, All's `TimelineAppearance` governs the **All moments band only**: the welcome block, the `Relive your memories` heading, and the Rediscover row always render on the plain canvas. Profile, Search, the Rediscover row, and read-only system collections use the app default. A timeline's mode always remains global.

Original/Warm Cream and Evergreen/Sage Green are available in Free. All other palettes and wallpapers are Relive Pro appearance options. A former Pro subscriber continues to see an already-selected premium appearance, but cannot select a new premium appearance without Pro.

Themes **may** affect:

- color palette
- typography
- borders
- surfaces
- timeline styling
- media treatment
- subtle texture

Themes **must NOT** change:

- navigation model
- timeline structure
- moment information hierarchy
- composer interaction model
- search behavior

---

## 12. Settings

Profile is an auxiliary destination opened from the Home surface's profile affordance; it is not a bottom-navigation destination. It shows the profile identity — a real display name when one is set, otherwise a neutral identity placeholder — and this is the only source for Home's `Welcome back, {name}` greeting; with no real display name Home shows exactly `Welcome back` (§2). It also shows installation joining date when known, informational Moment/custom-Timeline/place counts, an inline Appearance card, and the approved remaining Profile IA: Preferences; Media & storage; Backup; Location; Rediscover notifications; Privacy & security; Help & feedback; About Relive. Appearance provides persistent System/Light/Dark and app-default palette controls. Media & Storage is a read-only archive-insights screen showing Relive-managed attachment storage and category counts; it provides no optimization, deletion, cleanup, or device-wide storage controls. Other Profile-row functionality remains deferred unless a real destination exists.

### 12.1 Behavior preferences

Preferences is a small behavior-only screen; it does not duplicate Appearance, Backup, Media & Storage, Privacy & security, account, per-timeline themes, or archive actions.

Startup is fixed rather than preferred: with a single root there is nothing to choose between, so Relive always starts at the top of the Home surface, in the Home top state. There is no start-destination preference; an authoritative restoration or deep-link destination remains higher priority.

Preferences' persisted controls are:

- **Confirm before discarding** — on by default. It controls only a dirty inline composer's explicit `×` reset. When off, `×` immediately resets and collapses; system/visible Back continues preserving the originating timeline's session draft.
- **Show locations** and **Show tags** — on by default. These are presentation-only controls for normal All moments and custom Timeline Moment cards. They do not alter stored Moment data, composer/edit fields, Search matching or results, or read-only Rediscover collection details.
- **On This Day** and **Favourites** — on by default. Turning one off omits that collection card from the Rediscover row on Home without changing eligibility, favorite state, Moment data, or read-only collection behavior. The row closes up naturally and Home's welcome → Rediscover row → All moments ordering is unaffected.

Behavior preferences take effect reactively where safe and persist outside the archive database through native local preferences. The current fixed 12-hour editorial time format remains settled by ADR-0020. Video autoplay remains prohibited by the passive, lazy, single-owner playback architecture, and audio has no approved automatic-play context; those three controls are not shown.

Future Settings entries remain Themes, Upgrade to Pro, and Export. Detailed functionality is defined only when separately specified.

---

## 13. Storage

The application is **local-first**. Persistence is designed so that:

- moments are stored **once**
- custom timelines **reference** moments
- attachments **reference** moments
- tags can be **queried efficiently**
- timeline membership is **many-to-many** where needed
- **All** is logically automatic rather than duplicating every membership row unnecessarily

### 13.1 Persistence in debug and release builds

Both debug and release builds use **persistent SQLDelight/SQLite storage**. User Moments survive:

- process death
- removing from Recents
- normal APK update

Only explicit app data clear, uninstall, or delete removes persisted user data. There is **no** in-memory debug replacement — debug builds use the same persistent storage as release builds.

Do **not** add: backend, cloud sync, login, social features, AI, embeddings, recommendations — unless explicitly requested later.

Persistence design detail lives in [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 14. Monetization

Relive launches with exactly two product tiers: **Free** and **Relive Pro**. Monthly, annual, and lifetime are billing choices for the single `relive_pro` entitlement, not feature tiers. `relive_pro_monthly` and `relive_pro_annual` are subscriptions; `relive_pro_lifetime` is a non-consumable one-time purchase. Any annual trial is configured in the relevant store, never in application logic.

Free includes unlimited Moments; text, photos, video, and audio; Search; Favorites; On This Day; From Your Past; App Lock and privacy controls; manual backup; every restore operation; and permanent access to existing content. Free users may create three custom timelines. Original/Warm Cream and Evergreen/Sage Green appearance are free.

Relive Pro adds scheduled automatic backup and its cadence/network controls, unlimited custom timelines, and all premium palettes and wallpapers. When Pro expires, existing timelines, Moments, and premium appearance selections remain visible and editable. A person cannot create a further custom timeline while above the free limit, and cannot select a new premium appearance until Pro is active. Manual backup and restore are never gated.

Purchases and restores are provided by RevenueCat behind a shared entitlement interface. The only entitlement identifier is `relive_pro`; offerings are configured in RevenueCat and map the three store product identifiers above. Missing platform public API keys leave the app fully functional on Free and show purchasing as unavailable rather than crashing. Public keys and product IDs are build/configuration values so they can change without changing gates or UI logic. RevenueCat Funnels and Stripe web purchases remain deferred.

---

## 15. Explicit non-goals for the current product

Unless explicitly requested later, Relive does **not** include:

- backend / server
- cloud sync
- login / accounts
- social features
- AI / embeddings / recommendations
- background location tracking
- location history separate from moments
- third-party analytics

Engineering constraints and layering are defined in [`ARCHITECTURE.md`](ARCHITECTURE.md); visual tokens in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md); contributor rules in [`../AGENTS.md`](../AGENTS.md).
