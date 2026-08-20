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

### 4.1 Content expansion

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
- Images and videos use a **horizontally swipeable carousel**, similar to an Instagram post.
- Show **subtle page indicators**.
- Media should feel **integrated into the timeline**, not enclosed in heavy cards.

---

## 6. Creating a moment

Creation happens **inline inside the current timeline**. The timeline marker for the new entry becomes a **plus-circle**.

The composer contains:

- automatically generated **date/time**
- optional **detected/selected location** (see §7)
- **title**
- **content**
- **tags**
- **media attachments**
- **Add Media** control
- primary action: **Keep Moment**
- reset/cancel **`×`** at the top-right

### Add Media flow

Tapping **Add Media** reveals options:

- microphone (audio)
- image
- video

After media is added:

- the attachment appears **above** Add Media
- each attachment has its own remove **`×`**
- **Add Media moves below** existing attachments
- the user may continue adding more media

The entire composer can be reset with the top-right **`×`**.

Pressing **Keep Moment** saves the moment. After save:

- the plus marker becomes a normal timeline dot
- the new moment immediately adopts the standard timeline presentation

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

Settings currently contains:

- **Profile**
- **Themes**
- **Upgrade to Pro**
- **Export**

Detailed functionality will be defined later. **Do not invent behavior beyond what is currently specified.**

---

## 13. Storage

The application is **local-first**. Persistence is designed so that:

- moments are stored **once**
- custom timelines **reference** moments
- attachments **reference** moments
- tags can be **queried efficiently**
- timeline membership is **many-to-many** where needed
- **All** is logically automatic rather than duplicating every membership row unnecessarily

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
