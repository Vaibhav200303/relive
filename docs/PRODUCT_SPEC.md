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

Timeline Home is the navigation root. Selecting All or a custom timeline opens the shared timeline detail experience scoped to that selection; returning goes back to Timeline Home.

### Rediscover navigation

Rediscover is a second top-level destination alongside Timelines. Until Search and You are implemented, bottom navigation contains exactly **Timelines** and **Rediscover**. Timeline Home remains the root within Timelines; Timeline detail is not a top-level destination.

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

Rediscover currently begins as a system-collection root with Favorites, On This Day, and From Your Past. It is not a chronological timeline or recommendation feed.

- The root renders the Relive app bar, a `FAVOURITES` editorial section, and the two-item bottom navigation.
- Favorites is derived reactively from each Moment's persisted favorite state. It is not a custom timeline, membership, or duplicate persistence record.
- The section shows at most ten individual favorited-Moment cards in the same chronological ordering as the full Favorites timeline. Every compact shelf card shares one fixed visual-region height: image/video cards use their first ordered attachment as the lead visual and quietly show an additional-attachment count; text-only and audio-only cards use the theme-aware empty visual surface with no fake media, icon, or illustration. This compact-shelf exception does not change normal Timeline presentation.
- Tapping a card opens the read-only Favorites timeline positioned at that Moment. `Show all` opens the complete read-only Favorites timeline. With zero favorites, the section shows `No favorite moments yet.` and `Moments you favorite will appear here.` without a row or `Show all` action.
- The Favorites detail is strictly read-only: it has Back and a centered Favorites title, but no composer, creation, edit, forget, membership, or favorite-mutation controls. Media viewing and playback remain available.
- The root uses a dedicated bounded read projection with batched attachment loading; it does not hydrate the full Favorites collection to render the shelf.
- On This Day renders directly below Favorites: its editorial date is the current device-local day/month and its bounded, horizontally swipeable featured cards represent only matching dates in previous local calendar years. The current year is excluded; February 29 matches only previous February 29 Moments. A card's secondary label is the exact calendar-year anniversary, e.g. `2 YEARS AGO`. It opens a read-only system collection positioned at the selected Moment. An empty anniversary set shows `Nothing from this date—yet.` without a card.
- From Your Past renders below On This Day as a horizontally swipeable shelf of at most ten distinct Moments. It selects only Moments at least 90 days old, excludes future timestamps and active On This Day matches, and does not exclude favorites. Selection and order are deterministic for a device-local calendar day, change with the next local day, and are read from a bounded SQL-backed projection with batched attachments. Cards use the same compact dimensions and visual framing as Favorites but show no heart. Tapping opens that day's read-only From Your Past system collection positioned at the selected Moment; the detail retains the shelf order. If no Moment is eligible, the root shows `Your archive is still taking shape.` without cards.
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

The interface must **encourage continuous scrolling**. It must **not** feel like a database or a list of records.

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

Creation happens **inline inside the current timeline**. The composer sits at the chronological end of the timeline (after the newest moment).

### 6.1 Composer collapse/expand behavior

The composer is **collapsed by default**. In the collapsed state:

- Only the **`+` timeline marker** (plus-circle) is visible.
- Tapping `+` **expands the existing composer inline** at the same timeline position.
- No modal, no bottom sheet, no separate screen — the composer opens in place.
- Expansion and collapse are **smoothly animated** (`AnimatedContent` with expand/shrink vertical transitions).
- The `×` reset button resets all fields and **collapses** the composer.
- A successful **Keep Moment** resets all fields and **collapses** the composer.
- Keyboard behavior keeps the active composer usable above the IME (see ADR-0016).

### 6.2 Composer fields

The expanded composer contains:

- automatically generated **date/time** (from `createdAt`, device-local timezone)
- optional **detected/selected location** (see §7)
- **title**
- **content**
- **tags** (see §4.2 for tag behavior)
- **media attachments**
- **Add Media** control
- primary action: **Keep Moment**
- reset/cancel **`×`** at the top-right

### 6.3 Add Media flow

Tapping **Add Media** reveals three options:

- **Mic** (audio recording)
- **Camera** (photo or video)
- **Library** (photo, video, or audio from device)

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

Relive supports **optional GPS-based location detection** when creating a moment. Location is **moment-scoped**, not continuous tracking.

When the inline composer opens:

- Relive **may** attempt to detect the user's current location.
- Location permission is requested **only when needed**, using the platform's normal permission flow.
- Denying location permission must **never** prevent creating or saving a moment.
- Relive must **not** continuously track location in the background.
- Relive must **not** collect location when the user is merely browsing the timeline.

The composer displays the resolved location **below** the automatically generated date/time. The user must be able to:

- keep the detected location
- remove the location
- replace it with a manually selected/entered location

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

### 7.2 Failure handling

Location acquisition must handle all of the following, and in every case the composer continues normally and the moment can still be created and saved:

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

Long-pressing a moment shows:

- **Edit**
- **Forget**

### After 4 days

- no editing
- no forgetting
- long-press must **not** expose either action

### Editing behavior

- Editing happens **inline inside the timeline**.
- Existing media attachments receive **remove controls** while editing.
- The user can **add additional media** while editing.
- Saving inline edits may occur when the user **taps outside the active editor**, but interactions **within editor controls must not accidentally trigger a save**.

### Forgetting behavior

- **Forget requires confirmation** before permanent removal.

---

## 9. Search

Search **always operates within the currently selected timeline**.

- Pressing the search icon transforms the app bar into a search interface.
- The timeline **remains visible**.
- Below the search field, show filters:
  - **All**
  - **Tags**
  - **Places**

The meaning of these filters is **scoped to the current timeline**.

Example — if the current timeline is `Japan 2026`:

- **All** searches title/content/tags/location **only inside** Japan 2026
- **Tags** searches/filters tags present **inside** Japan 2026
- **Places** searches/filters locations present **inside** Japan 2026

If the current timeline is `All`, the search scope is **every saved moment**.

### 9.1 All search

Search across:

- title
- content
- tags
- location

Behavior resembles WhatsApp chat search:

- matching text is **highlighted**
- the current **match count** is displayed
- **up/down arrows** navigate matches
- the timeline **automatically scrolls** to the relevant matching moment

### 9.2 Tags

- Only matching moments remain visible.
- Provide matching **tag suggestions** while typing.
- Filtered results retain the **same timeline UI**.

### 9.3 Places

- Only matching locations remain visible.
- Provide matching **saved-location suggestions** while typing.
- Places suggestions are derived **only** from locations represented by moments in the **current timeline**.
- Selecting/searching a place filters to matching moments while preserving the normal timeline presentation.
- Filtered results retain the **same timeline UI**.

---

## 10. Favorites

- Every moment has a subtle **favorite/heart** action.
- Favorite state must **not** visually dominate the moment.
- Rediscover's bounded Favorites shelf and full read-only Favorites timeline are derived from this same state, so favoriting or unfavoriting elsewhere updates both immediately.

---

## 11. Themes

All timelines share the **same layout and behavior**. Themes only change **presentation**.

Initial conceptual themes:

- **Warm Journal**
- **Monochrome Archive**
- **Film Memory**

Theme may vary **per timeline**.

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

Profile is an auxiliary destination opened from Timeline Home's profile affordance; it is not a bottom-navigation destination. It shows a neutral identity placeholder, installation joining date when known, informational Moment/custom-Timeline/place counts, and the approved Profile IA: Appearance & themes; Media & storage; Backup; Location; Rediscover notifications; Privacy & security; Help & feedback; About Relive. Profile-row functionality remains deferred unless a real destination exists.

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

## 14. Monetization (planned, not implemented now)

- **RevenueCat** will be integrated later for Pro entitlement.
- **RevenueCat Funnels + Stripe** may be used later for web subscription conversion.

The architecture should **allow** these additions later but must **not** implement them now. See [`ROADMAP.md`](ROADMAP.md) and [`RELEASE.md`](RELEASE.md).

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
