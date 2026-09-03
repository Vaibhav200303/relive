# Relive — Design System

A **strict, tokenized** design system. Every visual value used in the app comes from a named token. Do **not** invent random per-screen styling, and do not hardcode raw colors, sizes, or durations in components.

This document defines the planned **token categories** and seeds them with concrete values derived from the approved UI reference. Values marked *(planned)* are placeholders to be confirmed against the reference as the corresponding UI is built.

---

## 0. Authority and conflict rule

The approved UI reference is authoritative for the **All moments timeline band** — its rail, dots, Moment cards, metrics, and tokens — and for custom timeline UI:

- `docs/ui-reference/timeline-reference.png`
- `docs/ui-reference/timeline-reference.html`

It is **not** authoritative for the composition of the Home surface, whose approved order is the welcome block, the `Relive your memories` heading, the Rediscover collection row, the `All moments` heading, then the timeline band (see [`DECISIONS.md`](DECISIONS.md) ADR-0061).

> **When written design tokens in this document conflict with the approved UI reference, STOP and report the conflict.** Do not improvise a resolution, do not silently pick one side. Surface it for a human decision, then record the resolution in [`DECISIONS.md`](DECISIONS.md).

The base color, typography, and dimension tokens below are transcribed from the reference stylesheet. They are the starting truth; the reference image remains the tie-breaker for timeline-band visuals and tokens. The image is not evidence about Home's composition, navigation, scroll behavior, or entry points, all of which are governed by the approved unified Home surface decision in [`DECISIONS.md`](DECISIONS.md).

**Reference exception — fixed bottom Edit/Share controls:** The fixed bottom Edit and Share controls present in the reference HTML/screenshot are explicitly excluded from the approved Relive timeline design. They must not be implemented unless separately specified later. Tokens that existed only to support that bottom action bar (its background fade and filled-button color) are intentionally omitted from this document.

---

## 1. Design language

- Warm editorial / nostalgic aesthetic.
- Generous whitespace; visually continuous scrolling; not a database or list of records. Continuity is a visual quality only — the All moments feed on Home is newest-first, bounded, paged, and windowed, and the root surface never observes or hydrates the complete archive on launch.
- Serif titles, sans-serif body/metadata.
- Subtle brown/sepia accents on a cream background.
- Thin timeline rail, small circular dots, integrated plus-circle for composing.
- Home is one surface, not a stack: the All moments band is a scroll state of Home, never a pushed detail destination — nothing is pushed or popped to reach it, Back inside it (outside the composer) follows the platform's root behavior, and the welcome block and Rediscover row return only on a manual scroll back to the top. "Timeline detail" refers to custom timeline screens only.
- Focused All moments may add the approved bottom-centered return-to-newest control after manual movement toward older Moments; it never fires on entry and is hidden whenever the Rediscover row is visible. It is a token-styled Material 3 Expressive small FAB, not a persistent bottom action bar.
- Home always launches at the top of the welcome/Rediscover state at scroll offset zero; the All moments band is never auto-scrolled to the newest Moment or to the composer on entry.
- Subtle borders, minimal shadows, no excessive cards, premium media presentation.

Material 3 provides component behavior and accessibility; these tokens provide the Relive look. The app must not look like a default Material app.

---

## 2. Token categories

The design system defines tokens in the following categories. Each is enumerated below.

1. Background colors
2. Text colors
3. Accent colors
4. Surfaces
5. Borders
6. Typography
7. Spacing
8. Radii
9. Icon sizing
10. Stroke widths
11. Timeline dimensions
12. Media aspect ratios
13. Animation durations
14. Easing
15. Opacity
16. Accessibility / touch-target requirements

Token **names** below (e.g. `color.bg.canvas`) are the intended shape of the system. Finalize exact naming when the theme layer is implemented in Phase 0; keep names stable thereafter.

---

## 3. Background colors

Base theme (**Warm Journal**), from the reference:

| Token              | Value     | Usage                          |
| ------------------ | --------- | ------------------------------ |
| `color.bg.canvas`  | `#F6F4F0` | app background (cream)          |
| `color.bg.header`  | `#F6F4F0` @ `opacity.veryHigh` (+ blur) | pinned app bar and pinned section-heading background |

`color.bg.header` remains valid under the unified Home surface. Home keeps the Relive app bar pinned across both of its scroll states, condensing to a compact form as the surface scrolls, and in focused All moments the `All moments` section heading pins directly beneath it; the token backs both.

---

## 4. Text colors

| Token                   | Value     | Usage                                   |
| ----------------------- | --------- | --------------------------------------- |
| `color.text.primary`    | `#3C3633` | titles, body                            |
| `color.text.secondary`  | `#3C3633` @ ~70% | italic subtitles, metadata       |
| `color.text.muted`      | `#3C3633` @ ~40–60% | placeholders, page dots         |
| `color.text.onAccent`   | `#FFFFFF` | reserved: text/icons on any future filled control (none approved yet) |

Opacity variants are drawn from the opacity scale (§15), not arbitrary values.

---

## 5. Accent colors

| Token                   | Value     | Usage                                    |
| ----------------------- | --------- | ---------------------------------------- |
| `color.accent`          | `#6F4E37` | serif wordmark, date labels, timeline dot |
| `color.accent.muted`    | `#6F4E37` @ ~70% | uppercase date/eyebrow text        |

---

## 6. Surfaces

| Token             | Value     | Usage                                 |
| ----------------- | --------- | ------------------------------------- |
| `color.surface.card` | `#EFECE5` | media container / tag chip / composer media-action surface |
| `color.surface.card.translucent` | `#EFECE5` @ ~50% | quiet secondary surface treatment |
| `color.surface.floating` | `#E1D8CB` | floating navigation and quick-capture controls |
| `color.surface.overlay` | `#F6F4F0` | dialogs, menus, and modal sheets |
| `color.action.destructive` | `#98111E` (light) / `#FF8A95` (dark) | destructive actions and delete affordances |

Surfaces are used sparingly — the product avoids excessive cards.

---

## 7. Borders

| Token                | Value     | Usage                              |
| -------------------- | --------- | ---------------------------------- |
| `color.border`       | `#D5CDBF` | timeline rail, chip and media borders |
| `color.border.muted` | `#D5CDBF` @ ~50% | subtle media/card borders    |
| `border.width.hairline` | `1px`  | default border/stroke width        |

---

## 8. Typography

Two families, both bundled locally in the app via the Compose Multiplatform resources system (no network font loading, ever):

- **Serif** — `Fraunces`, SIL Open Font License 1.1. Bundled cuts: Medium (500), SemiBold (600) — both roman. These are static instances taken from the Fraunces variable font at a fixed 72pt optical size (`opsz=72`), moderate softness (`SOFT=40`), and no wonk (`WONK=0`), for a warm, intimate-journal character. The serif is never set italic.
- **Sans** — `Inter`, SIL Open Font License 1.1. Bundled weights/styles: Regular, Italic, Medium, SemiBold.

Only the weights and styles actually referenced by the token mappings below are bundled. License files are stored alongside the font binaries in the design-system layer. Callers reference typography exclusively through `ReliveTheme.typography.*` — the underlying font resource names are an implementation detail of the design-system layer.

The scale is one modular editorial system — the "Kept" direction (§8.4) — not a bag of per-screen sizes. Every role sets size, line height, tracking, and weight; the serif carries the large brand/emotional roles and the sans carries every text and label role. Weight is used intentionally rather than defaulting to bold: serif brand roles are Medium, the moment title is serif SemiBold (the strongest text element), reading roles are sans Regular, metadata/controls are sans Medium, and only the primary call to action is sans SemiBold.

| Token                   | Family | Size / line / style                       | Usage                         |
| ----------------------- | ------ | ----------------------------------------- | ----------------------------- |
| `type.display`          | serif  | 34 / 40, Medium, tight tracking            | hero / empty-state heading     |
| `type.wordmark`         | serif  | 30 / 34, Medium, roman                     | "Relive" header               |
| `type.coverTitle`       | serif  | 30 / 36, Medium                            | custom timeline cover heading  |
| `type.title`            | serif  | 24 / 30, SemiBold                          | moment title / screen title    |
| `type.dateLarge`        | serif  | 28 / 32, Medium                            | editorial day header (role provided; not wired into the timeline UI yet) |
| `type.subtitle`         | sans   | 15 / 22, italic                           | moment subtitle/summary line  |
| `type.body`             | sans   | 16 / 26                                    | content (long-form reading)   |
| `type.caption`          | sans   | 13 / 18                                    | small secondary text          |
| `type.eyebrow`          | sans   | 11 / 16, Medium, uppercase, wide tracking  | timeline metadata line (`DATE • TIME` / location)        |
| `type.tag`              | sans   | 12 / 16, Medium, medium tracking           | tag chips (rendered `#lowercase`; `#` is supplied by the UI, not stored on the tag label) |
| `type.action`           | sans   | 14 / 20, Medium                            | buttons                       |
| `type.prominentAction`  | sans   | 16 / 22, SemiBold                          | primary call-to-action         |

The composer location input uses `type.body` with muted/secondary text, the `icon.sm` location pin, and a `48dp` minimum row target. It is an inline editorial field directly below `DATE • TIME`, never a heavy outlined address form. Saved Moment locations use the same `type.eyebrow` role and left edge as saved date/time, but `color.text.secondary`; presentation trims whitespace and capitalizes only the first character.

Sizes are expressed in scalable units (`sp`) so they respect system font scaling (§16).

### 8.1 Material type scale mapping

All fifteen Material 3 `Typography` roles are populated from Relive tokens via `reliveMaterialTypography`, so any Material component reading `MaterialTheme.typography.*` renders in the bundled Relive families and never the Material default (Roboto). The `title`, `body`, and `label` roles map to the existing Relive tokens; the `display` and `headline` roles have no Relive equivalent and are derived from the serif `type.title` family. Do not rely on Material components using an unmapped role — every role is branded.

### 8.2 Optical sizing (structural)

The bundled fonts are static cuts, so there is no live variable `opsz` axis. Optical-size intent is met structurally instead: the serif is Fraunces' **72pt display optical cut** used only at large sizes (24–34sp) and the sans is a **text** face used only at small sizes (11–16sp), so each role already carries the stroke contrast appropriate to its size. Tracking is tuned per size the way an optical axis would tune it — tight (negative) on the large serif roles, open on the small-caps roles (`type.eyebrow`, `type.tag`). Adopting the Fraunces variable font with a live `opsz` axis is deferred (see [`DECISIONS.md`](DECISIONS.md) ADR-0055, ADR-0057) — KMP variable-font axis support is inconsistent across platforms, so static instances are the reliable path.

### 8.3 Dark-mode label weight (halation)

On a dark canvas, light-on-dark text glares and its strokes visually bloat, so a weight that looks right on light reads too heavy on dark. The label roles step one bundled weight lighter in dark mode so they carry the same typographic color in both modes, via two helpers: the standard labels (`type.eyebrow`, `type.tag`, `type.action`) use `labelWeightFor(isDark)` — **Medium on light, Regular on dark** — and the single heaviest control, the primary CTA (`type.prominentAction`), uses `prominentLabelWeightFor(isDark)` — **SemiBold on light, Medium on dark**. This is the same one-step mechanism the system has always used; its baselines dropped one weight when the "Kept" scale made metadata/controls calmer (ADR-0057). Body and serif roles are unchanged (no lighter cut is bundled).

### 8.4 Modular scale — the "Kept" direction

The scale is one modular editorial system. Its metrics were first professionalized in ADR-0056 (removing a muddy middle and broken rhythm); the family pairing and character were then redesigned into the **"Kept"** direction (see [`DECISIONS.md`](DECISIONS.md) ADR-0057). Reading `type.body` is **16 / 26** (comfortable long-form reading for a journaling app); the text roles step clearly (`caption` 13 < `subtitle` 15 < `body` 16); the serif brand roles run `title` 24 → `coverTitle`/`wordmark` 30 → `display` 34, with the optional `type.dateLarge` at 28; and every role sets an explicit line height and optical tracking. The pairing is **Fraunces (serif) + Inter (sans)** — a warm old-style soft-serif for identity/emotion against a neutral, highly legible workhorse sans for reading and UI. Fraunces replaced Playfair Display, whose high-contrast Didone hairlines read as fashion/wedding rather than intimate journal and fractured at small sizes over busy wallpapers. Two serif binaries in, two out — no net increase.

---

## 9. Spacing

Spacing scale (planned, aligned to the reference's rhythm):

| Token         | Value  |
| ------------- | ------ |
| `space.0`     | `0dp`  |
| `space.1`     | `4dp`  |
| `space.2`     | `8dp`  |
| `space.3`     | `12dp` |
| `space.4`     | `16dp` |
| `space.6`     | `24dp` |
| `space.8`     | `32dp` |
| `space.12`    | `48dp` |

Reference anchors: screen horizontal padding ≈ `24dp`; vertical gap between moments ≈ `48dp`. Generous whitespace is a requirement, not a nicety.

---

## 10. Radii

| Token          | Value   | Usage                     |
| -------------- | ------- | ------------------------- |
| `radius.sm`    | `8dp`   | inner media image corners |
| `radius.md`    | `12dp`  | media container, chips-as-pills baseline |
| `radius.lg`    | `20dp`  | expressive composer media-action container |
| `radius.dialog` | `28dp` | dialogs and modal-sheet top corners |
| `radius.menu` | aliases `radius.md` | popup menus and snackbars |
| `radius.pill`  | `999dp` | tag chips, circular buttons |

Search uses the same `radius.pill` container shape. `search.container.height` is `56dp`; the inner controls retain the global `48dp` minimum touch target.

The expanded composer keeps one `radius.lg` media-action container using `color.surface.card`, a hairline muted border, and tokenized internal spacing. Its **Voice**, **Camera**, and **Media** actions occupy equal width, use aligned `icon.lg` Relive/Material-style microphone, photo-camera, and gallery glyphs, and retain at least `48dp` touch targets. The actions share one surface rather than becoming three elevated or independently colored pills. Press feedback comes from the Material interaction indication under Relive colors.

---

## 11. Icon sizing

| Token          | Value  | Usage                        |
| -------------- | ------ | ---------------------------- |
| `icon.sm`      | `12dp` | inline metadata icons (pin)  |
| `icon.md`      | `20dp` | composer plus, add-media     |
| `icon.lg`      | `24dp` | header icons, favorite       |

---

## 12. Stroke widths

| Token              | Value   | Usage                          |
| ------------------ | ------- | ------------------------------ |
| `stroke.icon`      | `1.5px` | icon line weight (default)     |
| `stroke.icon.bold` | `2px`   | emphasized icons (planned)     |
| `stroke.hairline`  | `1px`   | timeline rail, borders         |

---

## 13. Timeline dimensions

From the reference:

| Token                 | Value   | Usage                                     |
| --------------------- | ------- | ----------------------------------------- |
| `timeline.rail.width` | `1px`   | thin vertical rail                        |
| `timeline.dot.size`   | `10dp`  | circular dot for an existing moment        |
| `timeline.plus.size`  | `32dp`  | plus-circle marker for the active composer |
| `timeline.cover.hero.height` | `300dp` | custom-timeline cover hero resting height |
| `timeline.item.gap`   | `48dp`  | vertical spacing between moments            |
| `timeline.content.inset` | `32dp` | left inset from rail to content (`pl-8`) |
| `timeline.dot.color`  | `#6F4E37` | dot fill (accent)                        |

The plus-circle is integrated into the rail (bordered, canvas fill). Because the All moments feed on Home is newest-first, the composer sits at the chronological end of the feed, which renders at its **head**, directly beneath the `All moments` heading; the rail terminates at the centre of the plus marker and no rail continues above it. Keeping a Moment collapses the composer back to this `+` marker and renders the saved Moment with a normal dot adjacent to it.

---

`profile.avatar.size` is `80dp` for the neutral circular Profile placeholder.

## 14. Media dimensions and presentation

~~`media.ratio.square` and `media.carousel.peek` are superseded by [`DECISIONS.md`](DECISIONS.md) ADR-0019 (adaptive visual collage).~~

Media uses an adaptive visual collage integrated into the timeline (see ADR-0019 for layout rules by attachment count). All media types — image, video, audio — are first-class visual tiles. Audio has no image frame; never show empty media placeholders. No horizontal carousel or pager in the timeline.

### Single-media adaptive sizing

Single-media Moments use adaptive natural sizing: the container shrink-wraps around the media's aspect ratio, subject to timeline max constraints only. Media is never stretched or distorted. Max bounds are ceilings, not targets.

| Token | Value | Usage |
| --- | --- | --- |
| `media.ratio.wide` | `2:1` | single landscape image/video (reference) |
| `media.treatment.sepia` | ~`0.3` | subtle sepia on media (theme-dependent) |
| `media.timelineSinglePreviewMaxHeight` | `420dp` | max height for single timeline media |
| `media.timelineSingleAudioHeight` | `200dp` | fixed height for single audio tile |
| `media.timelineSingleFallbackHeight` | `180dp` | fallback when natural size unknown |
| `media.composerPreviewMaxHeight` | `420dp` | max height for composer media preview |

### Multi-media collage tokens

| Token | Value | Usage |
| --- | --- | --- |
| `media.collageGap` | `4dp` | internal gap between tiles (acts as divider) |
| `media.collageBorder` | `4dp` | outer collage border thickness |
| `media.collageSingleMaxHeight` | `420dp` | max height for dominant tile in collage |
| `media.collageTileAspectSquare` | `1:1` | default tile aspect in 2/4-grid layouts |
| `media.collageDominantAspect` | `4:3` | dominant tile in 3-layout |
| `media.collageVideoAspect` | `16:9` | video tile aspect in collage |
| `media.collageAudioAspect` | `4:3` | audio tile aspect in collage |

### Border behavior

- Single-media and multi-media outer borders use the **same thickness** (`4dp`).
- Border color matches timeline-dot color (`color.accent` / `#6F4E37`).
- Multi-media internal gaps and outer border are the same weight, so adjacent tiles yield **one** ~4dp separator, not two overlapping strokes.
- Audio, video, and image tiles all participate in the same border/gap system.

### Collection-card media/surface boundary

Collection-card visual media and deterministic generated covers meet the opaque lower information surface directly. No media-to-surface fade, translucent overlap, or fake-shadow transition is used. Generated fallback covers remain unchanged. Lower information areas use semantic minimum-height tokens; title text receives the flexible vertical space with `Alignment.CenterStart`, while supporting metadata retains its established lower row/area and start/right alignment.

---

## 15. Animation durations

`ReliveMotion.durations` exposes the complete M3 duration scale. Components use these
tokens only; legacy `fastMillis`, `standardMillis`, and `slowMillis` aliases are
deprecated and must not be used for new work.

| Token | Value |
| --- | --- |
| `short1` / `short2` / `short3` / `short4` | 50 / 100 / 150 / 200ms |
| `medium1` / `medium2` / `medium3` / `medium4` | 250 / 300 / 350 / 400ms |
| `long1` / `long2` / `long3` / `long4` / `extraLong1` | 450 / 500 / 550 / 600 / 700ms |

Use `short4` with emphasized accelerate for permanent exits, `medium4` with
emphasized decelerate for entrances, and `long2` with emphasized easing for hero
container transforms. Reduced motion always uses the shared `short3` standard fade.

Moving between Home's two states — the welcome/Rediscover top state and focused All moments — is a **reversible in-surface scroll-state change**: the motion is the scroll itself. It uses no fade-through, no container transform, and no shared-axis pattern, because no navigation event, route change, or back-stack entry occurs. The fade-through pattern still governs the read-only collection routes opened from the Rediscover row.

The inline composer uses the same tokenized vertical expand/fade transition whether invoked from the timeline rail `+` or the floating `+ New`: `motion.slow` for entry and `motion.standard` for collapse with `ease.standard`. `+ New` moves Home into focused All moments and expands the **existing** rail composer in place, performing only the minimum scroll needed to seat the composer and its Keep Moment button in view; there is no screen entry, no settled-frame handoff, and no second composer. It does not request title focus or open the IME; the person taps a field when ready.

Collapse — whether from Back or from Keep Moment — closes the composer in place and leaves the surface in focused All moments at the same scroll offset, with the saved Moment visible in the timeline. Neither path scrolls toward, animates back to, or restores the welcome/Rediscover top state; that state returns only on a manual scroll to the top.

Android external-share capture uses the same restrained system: reading/error/picker states fade, choosing a timeline uses a short emphasized horizontal slide/fade. A selected **custom** timeline settles collapsed for one frame before the normal composer expansion begins; a share targeting All has no separate screen to render and settle, so it merges into the existing inline composer on the Home surface, which enters focused All moments with no settled-frame handoff and without restoring the welcome/Rediscover top state. The picker has one compact full-width All card first, then a deliberately larger tokenized break before compact custom cards continue in a two-column editorial grid. Its media and name-footer geometry comes from `shareTimelinePicker` tokens; the footer contains only the timeline name in the compact body style. It has no floating navigation or creation controls. Material press/progress/error behavior is used under Relive tokens, without a global expressive theme.

The return-to-newest arrow uses Material 3 Expressive scale/fade visibility motion with the semantic floating surface, accent glyph, `radius.pill`, existing `icon.lg`/`stroke.iconBold`, and the global `48dp` minimum touch target. It is scoped to focused All moments: because the feed is newest-first, movement toward older Moments is downward scroll and the control returns to the newest end at the head of the feed. Its return scroll is confined to the All moments band — it never scrolls the welcome block or Rediscover row back into view, and the control is hidden whenever the Rediscover row is visible, so it never competes with the upward scroll that restores the top state. It hides while its fast, visibly continuous return scroll runs and reappears if that scroll is cancelled away from the newest end. Snackbar feedback lifts above the visible control.

---

## 16. Easing

`ReliveMotion.easings` implements the M3 easing set: `standard`
`cubic(0.2, 0, 0, 1)`, `standardDecelerate` `cubic(0, 0, 0, 1)`,
`standardAccelerate` `cubic(0.3, 0, 1, 1)`, `emphasizedDecelerate`
`cubic(0.05, 0.7, 0.1, 1)`, `emphasizedAccelerate` `cubic(0.3, 0, 0.8, 0.15)`,
and the true two-part M3 `emphasized` path. Navigation uses the emphasized pair;
small utility motion uses the standard family.

### Shape scale and morph availability

`ReliveRadii` provides the M3 scale: `none`, `xs`, `small`, `medium`, `large`,
`largeIncreased`, `xl`, `xlIncreased`, `xxl`, and `full` (0, 4, 8, 12, 16, 20,
28, 32, 48, and fully rounded). `ReliveTheme.shapes` supplies semantic card,
dialog, sheet, chip, button, and pill shapes. Nested padded media uses optical
roundness (`inner = outer − padding`) when it has a separate clip; media sharing a
single parent clip is not double-clipped. `MaterialShapes`, `RoundedPolygon`, and
`Morph` are available from common code; shape morphs are disabled under reduced motion.

---

## 17. Opacity

| Token              | Value  | Usage                                     |
| ------------------ | ------ | ----------------------------------------- |
| `opacity.full`     | `1.0`  | primary content                           |
| `opacity.veryHigh` | `0.9`  | pinned header background (`color.bg.header`) |
| `opacity.high`     | `0.7`  | secondary metadata                        |
| `opacity.med`      | `0.5`  | muted borders, placeholders               |
| `opacity.low`      | `0.4`  | inactive dots, faint text                 |

Opacity variants for colors come from this scale rather than one-off alpha values.

---

## 18. Accessibility / touch-target requirements

- **Minimum touch target: 48×48dp** for all interactive elements (favorite, add-media, tag add, page controls, header buttons), even when the visible glyph is smaller. Expand the hit area with padding.
- Respect **system font scaling** — typography uses `sp`; layouts must not clip at larger scales.
- Maintain adequate **color contrast** for text against backgrounds; verify muted/opacity variants remain legible.
- Provide **content descriptions** for icon-only controls (e.g. Settings, Search, Favorite, Add media).
- Build on **Material 3** components so platform accessibility semantics (focus, roles, announcements) are inherited, then restyle with tokens.
- Honor reduced-motion preferences where the platform exposes them.
- Use semantic haptic cues only for direct interaction outcomes: `Action`, `Selection`, `ToggleOn`, `ToggleOff`, `Context`, `Confirm`, and `Reject`. Do not haptic-trigger typing, scrolling, passive animation, ordinary navigation, or routine Back/Close actions.
- Drive `Confirm` and `Reject` from one-shot success/failure outcomes, never from recomposition. Android camera capture retains its success-timed native vibration and must not receive a duplicate shared shutter haptic; the native iOS camera remains solely UIKit-controlled.

---

## 19. Themes

Themes are presentation-only token sets resolved by palette plus global appearance mode. Original preserves the approved Warm Journal light tokens exactly. Evergreen, Lilac Dusk, Crimson Keepsake, Blue Hour, and Rosewood use these ordered light/mid/strong/dark anchors:

| Palette | Light | Mid | Strong | Dark |
| --- | --- | --- | --- | --- |
| Evergreen | `#D1F2EB` | `#50C878` | `#0B6E4F` | `#013220` |
| Lilac Dusk | `#E6C7E6` | `#A3779D` | `#663399` | `#2E1A47` |
| Crimson Keepsake | `#FBE4E3` | `#D72638` | `#98111E` | `#3F0D12` |
| Blue Hour | `#D6E6F3` | `#A6C5D7` | `#0F52BA` | `#000926` |
| Rosewood | `#FADADD` | `#B66E79` | `#8C4E4F` | `#3B1F1B` |

Light schemes neutralize the light anchor into the canvas, use the strong anchor for accents, and the dark anchor for text. Dark schemes deepen the dark anchor into canvases/surfaces, use the mid anchor for accents, and lighten the first anchor for text. Supporting tones are opaque semantic blends; accent foregrounds select the highest-contrast candidate and destructive actions remain red. Primary text and accent content pairs meet WCAG AA. Theme changes interpolate semantic colors over `motion.duration.standard` without replacing screen composition.

The generated-cover fallback is a centralized mode-aware theme token. It derives curated rich gradient pairs from the active anchors, remains deterministic for a stable identity, and never modifies stored media. Global app appearance remains app-scoped. The editable All moments feed and every custom Timeline separately own a `TimelineAppearance`; staged rendering of its wallpaper and Moment treatment remains confined to timeline-owned regions — on Home that is the All moments band only, so the welcome block, the `Relive your memories` heading, and the Rediscover row always render on the plain canvas.

### Timeline wallpapers

Timeline wallpaper is independent from the global palette. The approved hand-drawn wallpaper artwork is bundled once per wallpaper identity and rendered behind Timeline content, so its doodles retain the exact supplied composition at every size. Light pairs are Warm Cream `#FAF3E9` / `#E7D5BF`, Blush Pink `#FDE7E7` / `#F0B8BA`, Sage Green `#E4E9DD` / `#B8C5AE`, Lavender `#EDE6F9` / `#CBBEE5`, Powder Blue `#E1EEFA` / `#B1CFEA`, and Soft Peach `#FEEBE1` / `#F5B99B`. Moment text, rail/dot, heart, media, and metadata retain their existing semantic colors until Moment treatment is introduced separately.

Profile archive-insights uses one restrained summary surface, then direct canvas sections with subtle dividers and proportional indicators. Its category distinctions derive only from semantic Relive colors and are always accompanied by labels and formatted values.

### 19.1 Visual-media fallback cover

Collection-card visual regions use `ReliveGeneratedCover` when their preview data contains no image or video. The cover uses a deterministic stable hash of the Timeline ID (or logical `timeline-all`) or Moment ID to choose a curated gradient; it is never persisted, random, time-based, or animated. A reactive image/video preview replaces it automatically. Audio-only and text-only collection cards use the cover, without a waveform, generic icon, or empty-state copy. Normal Timeline MomentCard media presentation remains unchanged.

The automatic bucketed collage cover is used by the `All Photos` card in Home's Rediscover row (§20) and by the `timeline-all` destination card in the external-share timeline picker: its available visual candidates remain a bounded reactive projection, while a deterministic three-hour bucket chooses 1–9 distinct candidates and a curated layout. When there are zero candidates, it uses the neutral no-cover placeholder rather than a generated cover. The `All moments` band on Home carries no cover hero; it begins at its section heading and is not entered from a card.

## 20. Home surface — welcome, Rediscover row, and navigation

Home is one continuous, vertically scrollable surface — one scroll container, one scroll position — and the app's single root. It renders, in order: the welcome block, the `Relive your memories` section heading, the horizontally scrollable Rediscover collection row, the `All moments` section heading, the inline composer collapsed to its rail `+` marker, and the All moments timeline. Rediscover is not a root, a destination, a route, or a screen; it is the collection row in the upper band of Home, and the timeline sits directly beneath it on the same scroll. Home's two states — the top state, where the welcome block and Rediscover row are in view, and focused All moments, where they have scrolled above the viewport and the pinned `All moments` heading tops the timeline — are scroll positions, never separate screens.

Home opens with the welcome block: `Welcome back, {name}` when a real profile display name exists and exactly `Welcome back` when none does, with no trailing punctuation, placeholder, or `Your Relive` fallback label, set in the serif brand role (`type.display` / `type.title`), followed by the subtitle `Your memories are waiting for you.` in `type.subtitle`. `Relive your memories` and `All moments` are section headings in `type.eyebrow`, separated by `space.6` / `space.8`.

The Rediscover row reuses Warm Journal typography, colors, radii, borders, and spacing. Its cards are `Favourites`, `On This Day`, `From Your Past`, and `All Photos`, sharing a single card family, visual region, and metadata layout; none of them is an entry point into the All moments timeline, which is already on this surface. That family uses the shared responsive 68%-width token, one-line ellipsized titles, and an attached `Show all` arrow action; On This Day keeps the same section-heading language, an editorial `day month` date, and its larger featured treatment with stable visual regions for image/video or generated covers; From Your Past keeps the same card family and dimensions, including its visual region and metadata layout, without a heart indicator. `All Photos` is a bounded, read-only system collection of Moments carrying at least one image or video attachment, read through the same bounded projection pattern as Favourites and From Your Past; its card cover is the automatic bucketed 1–9 collage described in §19.1. Search is a top-level surface alongside Home: its Material 3 search field, counter, and up/down controls use Relive theme colors, typography, minimum touch targets, and calm active treatment; results retain Timeline presentation. Components must not introduce raw visual values. The timeline reference is authoritative for the All moments timeline band only — rail, dots, Moment cards, and their metrics — not for Home's composition, while the Rediscover row follows the same calm editorial visual language without reproducing the timeline rail or Moment cards.

The bottom controls are a matched Material 3 `HorizontalFloatingToolbar` pair: navigation at bottom-left and quick capture at bottom-right. Both use the semantic `color.surface.floating` warm-stone container, `color.accent` actions, pill shape, equal height/baseline/bottom inset treatment, and one shared responsive layout. Expanded navigation consumes the remaining width after horizontal margins, the small `8dp` control gap, and quick capture's responsive width; its actions always retain equal minimum targets. A subdued accent-derived pill slides beneath the selected navigation icon using standard, non-overshooting Relive motion; the pill tracks destinations only and does not move when Home scrolls between its top and focused All moments states, since both are one destination. Vertical scrolling collapses navigation to the active destination icon; reverse scrolling expands it to the Home / Timelines / Search destinations. The centered `+ New` action, whose label uses the prominent semantic action type, is not driven by scroll direction. Rediscover is never a navigation destination — it is reached by scrolling Home back to the top. `+ New` presentation is defined per Home state rather than per raw scroll direction: it does not collapse to a bare Add icon merely because the person scrolled down into focused All moments. Tapping it moves Home into focused All moments and expands the existing inline composer from the timeline rail in place; it never navigates, opens a chooser, or presents a modal, dialog, bottom sheet, or separate composer screen. Touch exploration keeps both expanded. They reserve enough scroll-end padding that archive content can move clear of both controls, and appear on all three destinations — the Home surface in both its top and focused All moments states, Timeline Home, and Search. `+ New` is additionally hidden while the inline composer is expanded, Keep Moment being the primary action then, and remains absent from Profile, custom timeline detail, read-only Rediscover collections, media viewer, camera, recorder, and modal/detail surfaces.

## 21. Behavior preferences

Preferences uses an open canvas page rather than a stack of cards: a Back header, one quiet explanatory line, uppercase eyebrow section headings, consistent rows, subtle semantic dividers, and generous section whitespace. Labels use `type.body`; a row's current value uses `type.subtitle`; section labels use `type.eyebrow`. Rows retain the global `48dp` minimum target and allow text to wrap at large font scales.

There is no startup-destination row. With a single root there is nothing to choose between: Relive always launches at the top of the Home surface, and Rediscover — a row inside Home rather than a destination — cannot be selected as a start destination. Restoration and deep-link destinations keep their existing higher priority.

Material 3 provides `Switch`, `RadioButton`, the single-choice `AlertDialog`, pressed states, focus/selection semantics, and snackbar behavior. Relive tokens continue to provide canvas, overlay, typography, dividers, spacing, and feedback colors. Toggle interactions use `ToggleOn`/`ToggleOff`; single-choice preference selection uses `Selection`. Passive preference restoration and recomposition produce no haptic.
