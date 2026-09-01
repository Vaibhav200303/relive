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

The home screen contains **multiple timelines**.

There is always a built-in timeline:

- **All** — contains every saved moment in chronological order.

Users may create **custom timelines** representing chapters of their life, such as:

- College
- Japan 2026
- Family
- Internship
- Travel
- Relationship

All timelines — built-in and custom — use the **same timeline UI and interaction model**. A timeline may have its own visual theme, but its information architecture and interactions must remain consistent across all timelines.

Timeline Home is the navigation root for custom timelines. Selecting a custom timeline opens the shared timeline detail experience scoped to that selection; returning goes back to Timeline Home. The logical All timeline is entered from the first Rediscover collection card.

### Rediscover navigation

Timelines, Rediscover, and Search are the three top-level destinations. They live in a floating bottom-left navigation toolbar: it shows the active destination icon while collapsed and expands horizontally to reveal all three icon actions in Timeline / Rediscover / Search order. Timeline Home remains the root within Timelines; Timeline detail is not a top-level destination.

### Global quick capture

Timeline Home, Rediscover, and Search expose one theme-aware expressive **`+ New`** floating toolbar at bottom-right, separate from the navigation toolbar. It collapses with navigation to its Add icon and expands to show a centered `+ New`; both controls remain vertically aligned with a small fixed gap. Navigation uses an accent-derived moving selected indicator while expanded. It is absent from Profile, timeline detail, media viewer, camera, recorder, and modal/detail surfaces. Tapping either visible part always opens the editable logical **All** timeline, lets the normal collapsed timeline render and settle, then expands the existing inline composer with the same restrained in-place motion as its rail `+` and focuses its first text field. It never opens a chooser or custom timeline and never creates a second composer. Timeline detail keeps its integrated rail `+` as the creation affordance instead of showing another floating action.

### Android external share capture

Android accepts a user-initiated system share of plain text/URLs, images, videos, audio, or a supported mixed batch. Relive first asks which timeline should receive one new Moment: All is first, followed by custom timelines. Selecting a custom timeline creates the normal All + custom membership; selecting All retains the normal optional assignment controls. Shared content only pre-fills the existing inline composer and is never saved without the user choosing Keep Moment. Unsupported, unreadable, or mixed batches containing unsupported files are rejected as one request; arbitrary documents are not attachments in v1. This capability is Android-only for now.

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

Rediscover currently begins as a system-collection root with All, Favorites, On This Day, and From Your Past. It is not a chronological timeline or recommendation feed.

- The root renders the Relive app bar, the editable All timeline card, a `FAVOURITES` editorial section, and the floating top-level navigation toolbar. All opens the normal editable All timeline with the same automatic cover as its card: up to nine distinct image/video previews selected deterministically from a three-hour epoch-time bucket. Selection count and curated 1–9 arrangement may change between buckets but remain stable through recomposition, scrolling, navigation, and configuration change within one bucket. Audio and text-only Moments never become collage cells. With no visual media, All uses the neutral no-cover placeholder. Users cannot choose an All cover.
- Favorites is derived reactively from each Moment's persisted favorite state. It is not a custom timeline, membership, or duplicate persistence record.
- The section shows at most ten individual favorited-Moment cards in the same chronological ordering as the full Favorites timeline. Every compact shelf card shares one fixed visual-region height: image/video cards use their first ordered visual attachment as the lead visual and quietly show an additional-attachment count; text-only and audio-only cards use the theme-aware deterministic generated cover with no fake media, icon, or illustration. This compact-shelf exception does not change normal Timeline presentation.
- Tapping a card opens the read-only Favorites timeline positioned at that Moment. `Show all` opens the complete read-only Favorites timeline. With zero favorites, the section shows `No favorite moments yet.` and `Moments you favorite will appear here.` without a row or `Show all` action.
- The Favorites detail is strictly read-only: it has Back and a centered Favorites title, but no composer, creation, edit, forget, membership, or favorite-mutation controls. Media viewing and playback remain available.
- The root uses a dedicated bounded read projection with batched attachment loading; it does not hydrate the full Favorites collection to render the shelf.
- On This Day renders directly below Favorites only when at least one eligible Moment exists. Its editorial date is the current device-local day/month and its bounded, horizontally swipeable featured cards represent only matching dates in previous local calendar years. The current year is excluded; February 29 matches only previous February 29 Moments. A card's secondary label is the exact calendar-year anniversary, e.g. `2 YEARS AGO`. It opens a read-only system collection positioned at the selected Moment. With no eligible Moments, the heading, date, placeholder, and reserved section spacing are all absent; From Your Past follows Favorites with normal section spacing.
- From Your Past renders after On This Day when that section is present, or directly after Favorites when it is absent, as a horizontally swipeable shelf of at most ten distinct Moments. It selects only Moments at least 90 days old, excludes future timestamps and active On This Day matches, and does not exclude favorites. Selection and order are deterministic for a device-local calendar day, change with the next local day, and are read from a bounded SQL-backed projection with batched attachments. Cards use the same compact dimensions and visual framing as Favorites but show no heart. Tapping opens that day's read-only From Your Past system collection positioned at the selected Moment; the detail retains the shelf order. If no Moment is eligible, the root shows `Your archive is still taking shape.` without cards.
- **Deferred capability:** Places and Tags retain their local read-model implementation for a later Rediscover presentation phase, but are not collected or rendered by the current root.

- **Future Places** groups readable saved location labels only; raw coordinates, maps, and geocoding are not introduced.
- **Future Tags** reuse the existing canonical tag system and rank tags by Moment usage.
- No Moment content leaves the device, and no analytics, cloud, AI, or recommendation backend is used.
- Empty sections remain quiet and editorial; Relive never substitutes sample memories.

---

## 3. Timeline UI

**The approved reference design is authoritative:** `docs/ui-reference/timeline-reference.png` and `docs/ui-reference/timeline-reference.html`.

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

When a user manually moves toward older Moments in any timeline detail, a bottom-centered return-to-newest arrow appears. It remains available until the newest end is reached. Selecting it visibly and rapidly scrolls to the terminal timeline item; touching the scrolling timeline cancels the motion at the current Moment without activating content beneath that touch. The control is absent on empty and non-scrollable timelines.

### 3.1 Date navigation

Every editable All or custom Timeline has a Calendar action for navigation, never filtering. The selected date uses the current device-local calendar day: an exact date opens that day's first Moment in the existing chronological order; a missing date opens the next available Moment, or the closest previous Moment when no later Moment exists. The empty global Search capsule has the same Calendar action, which leaves Search and opens All at the resolved Moment. Entering a non-blank search query replaces that action with the result counter and previous/next controls.

Timeline Home has a separate capsule for filtering the already-observed custom timeline summaries by timeline name. It uses live, case-insensitive partial matching and preserves the repository's newest-first order. The logical All timeline and Moment fields never participate. This capsule has a search glyph and the `Search timelines...` field only: it has no Back, Calendar, result counter, or previous/next controls. A non-blank query with no matches shows `No timelines found.`

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

Creation happens **inline inside the current timeline**. The composer sits at the chronological end of the timeline (after the newest moment). Its rail terminates at the center of the final plus marker; no rail continues below it.

### 6.1 Composer collapse/expand behavior

The composer is **collapsed by default**. In the collapsed state:

- Only the **`+` timeline marker** (plus-circle) is visible.
- Tapping `+` **expands the existing composer inline** at the same timeline position.
- No modal, no bottom sheet, no separate screen — the composer opens in place.
- Expansion and collapse are **smoothly animated** (`AnimatedContent` with expand/shrink vertical transitions).
- The `×` reset button resets all fields and **collapses** the composer.
- A successful **Keep Moment** resets all fields and **collapses** the composer.
- System/visible Back preserves an unfinished draft for its current timeline and leaves/collapses without a discard dialog. Reopening that same timeline restores it during the app session; `×` remains the explicit discard-confirmation path.
- Keyboard behavior keeps the active composer usable above the IME (see ADR-0016).
- Entry from global `+ New` waits for All's content projection and one collapsed composed frame, then runs this same expansion transition and requests first-field focus after the expanding composer enters composition. It uses no arbitrary delay; subsequent media actions do not re-request focus.

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

- the plus marker becomes a normal timeline dot
- the new moment immediately adopts the standard timeline presentation
- the composer resets and collapses

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

Long-pressing a Moment in the editable **All** timeline selects it and smoothly replaces the normal header with a contextual Material 3 app bar. The bar has a Back action that exits selection, plus:

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

Search v1 is a dedicated top-level destination. It searches the complete local archive, not a selected timeline.

- The autofocus `Search memories...` field performs a debounced, case-insensitive SQL search across Moment title and content.
- Empty queries show `Find anything you've saved.` and never render the whole archive. No results show `No moments found.`
- Matches retain the same chronological ordering and full timeline rail/card/media presentation as All Timeline, but are strictly read-only. Media viewing/playback remains available.
- The first match is active. The `N / total` counter and up/down controls move through matches without wrapping and scroll the active Moment into view.
- Search state (query, result selection, and scroll position) persists while switching top-level destinations during the app session.

Filters, chips, categories, Tags/Places tabs, suggestions, search history, relevance ranking, AI search, and timeline-name results are not part of Search v1. Text highlighting is deferred unless it can be added without restructuring MomentCard.

---

## 10. Favorites

- Every moment has a subtle **favorite/heart** action.
- Favorite state must **not** visually dominate the moment.
- Rediscover's bounded Favorites shelf and full read-only Favorites timeline are derived from this same state, so favoriting or unfavoriting elsewhere updates both immediately.

---

## 11. Themes

All timelines share the **same layout and behavior**. Themes only change **presentation**.

Selectable palettes:

- **Original** (Warm Journal)
- **Evergreen**
- **Lilac Dusk**
- **Crimson Keepsake**
- **Blue Hour**
- **Rosewood**

The global appearance mode is **System**, **Light**, or **Dark**. System follows the live platform appearance. The selected palette is the app default. The editable All timeline and each custom timeline own independent `TimelineAppearance` values; All's appearance is stored in native local preferences because All is logical, while custom timeline appearances are archive data. Profile, Search, Rediscover, and read-only system collections use the app default. A timeline's mode always remains global.

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

Profile is an auxiliary destination opened from Timeline Home's profile affordance; it is not a bottom-navigation destination. It shows a neutral identity placeholder, installation joining date when known, informational Moment/custom-Timeline/place counts, an inline Appearance card, and the approved remaining Profile IA: Preferences; Media & storage; Backup; Location; Rediscover notifications; Privacy & security; Help & feedback; About Relive. Appearance provides persistent System/Light/Dark and app-default palette controls. Media & Storage is a read-only archive-insights screen showing Relive-managed attachment storage and category counts; it provides no optimization, deletion, cleanup, or device-wide storage controls. Other Profile-row functionality remains deferred unless a real destination exists.

### 12.1 Behavior preferences

Preferences is a small behavior-only screen; it does not duplicate Appearance, Backup, Media & Storage, Privacy & security, account, per-timeline themes, or archive actions. Its persisted controls are:

- **Start Relive on** — Timelines by default, or Rediscover. This applies only to a fresh app-root startup; an authoritative restoration or deep-link destination remains higher priority. Search is not a startup option.
- **Confirm before discarding** — on by default. It controls only a dirty inline composer's explicit `×` reset. When off, `×` immediately resets and collapses; system/visible Back continues preserving the originating timeline's session draft.
- **Show locations** and **Show tags** — on by default. These are presentation-only controls for normal editable All/custom Timeline Moment cards. They do not alter stored Moment data, composer/edit fields, Search matching or results, or read-only Rediscover collection details.
- **On This Day** and **Favorites** — on by default. Turning one off omits that complete section from the Rediscover root without changing eligibility, favorite state, Moment data, or read-only collection behavior. Remaining section spacing collapses naturally.

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
