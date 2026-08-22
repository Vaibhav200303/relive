# Relive — Design System

A **strict, tokenized** design system. Every visual value used in the app comes from a named token. Do **not** invent random per-screen styling, and do not hardcode raw colors, sizes, or durations in components.

This document defines the planned **token categories** and seeds them with concrete values derived from the approved UI reference. Values marked *(planned)* are placeholders to be confirmed against the reference as the corresponding UI is built.

---

## 0. Authority and conflict rule

The approved UI reference is authoritative for timeline UI:

- `docs/ui-reference/timeline-reference.png`
- `docs/ui-reference/timeline-reference.html`

> **When written design tokens in this document conflict with the approved UI reference, STOP and report the conflict.** Do not improvise a resolution, do not silently pick one side. Surface it for a human decision, then record the resolution in [`DECISIONS.md`](DECISIONS.md).

The base color, typography, and dimension tokens below are transcribed from the reference stylesheet. They are the starting truth; the reference image remains the tie-breaker.

**Reference exception — fixed bottom Edit/Share controls:** The fixed bottom Edit and Share controls present in the reference HTML/screenshot are explicitly excluded from the approved Relive timeline design. They must not be implemented unless separately specified later. Tokens that existed only to support that bottom action bar (its background fade and filled-button color) are intentionally omitted from this document.

---

## 1. Design language

- Warm editorial / nostalgic aesthetic.
- Generous whitespace; continuous scrolling; not a database or list of records.
- Serif titles, sans-serif body/metadata.
- Subtle brown/sepia accents on a cream background.
- Thin timeline rail, small circular dots, integrated plus-circle for composing.
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
| `color.bg.header`  | `#F6F4F0` @ `opacity.veryHigh` (+ blur) | sticky header background |

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
| `color.surface.card` | `#EFECE5` | media container / tag chip surface |
| `color.surface.card.translucent` | `#EFECE5` @ ~50% | dashed "Add media" surface |

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

- **Serif** — `Playfair Display`, SIL Open Font License 1.1. Bundled weights/styles: Regular, Italic.
- **Sans** — `Inter`, SIL Open Font License 1.1. Bundled weights/styles: Regular, Italic, Medium, SemiBold.

Only the weights and styles actually referenced by the token mappings below are bundled. License files are stored alongside the font binaries in the design-system layer. Callers reference typography exclusively through `ReliveTheme.typography.*` — the underlying font resource names are an implementation detail of the design-system layer.

| Token                   | Family | Size / style                              | Usage                         |
| ----------------------- | ------ | ----------------------------------------- | ----------------------------- |
| `type.wordmark`         | serif  | ~30sp, italic                             | "Relive" header               |
| `type.title`            | serif  | ~24sp (2xl)                               | moment title                  |
| `type.subtitle`         | sans   | ~14sp, italic                             | moment subtitle/summary line  |
| `type.body`             | sans   | ~14–16sp                                  | content                       |
| `type.eyebrow`          | sans   | ~10sp, semibold, uppercase, wide tracking | timeline metadata line (`DATE • TIME` / location)        |
| `type.tag`              | sans   | ~10sp, semibold, wide tracking            | tag chips (rendered `#lowercase`; `#` is supplied by the UI, not stored on the tag label) |
| `type.action`           | sans   | ~14sp, semibold                           | buttons                       |

Sizes are expressed in scalable units (`sp`) so they respect system font scaling (§16).

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
| `radius.pill`  | `999dp` | tag chips, circular buttons |

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
| `timeline.item.gap`   | `48dp`  | vertical spacing between moments            |
| `timeline.content.inset` | `32dp` | left inset from rail to content (`pl-8`) |
| `timeline.dot.color`  | `#6F4E37` | dot fill (accent)                        |

The plus-circle is integrated into the rail (bordered, canvas fill) and becomes a normal dot after the moment is kept.

---

## 14. Media aspect ratios

| Token              | Value  | Usage                                  |
| ------------------ | ------ | -------------------------------------- |
| `media.ratio.wide` | `2:1`  | single landscape image/video           |
| `media.treatment.sepia` | ~`0.3` | subtle sepia on media (theme-dependent) |

~~`media.ratio.square` and `media.carousel.peek` are superseded by [`DECISIONS.md`](DECISIONS.md) ADR-0019 (adaptive visual collage).~~

Media uses an adaptive visual collage integrated into the timeline (see ADR-0019 for layout rules by attachment count). All media types — image, video, audio — are first-class visual tiles. Audio has no image frame; never show empty media placeholders. No horizontal carousel or pager in the timeline.

---

## 15. Animation durations

| Token             | Value   | Usage                              |
| ----------------- | ------- | ---------------------------------- |
| `motion.fast`     | `120ms` | hover/opacity, small state changes |
| `motion.standard` | `240ms` | expand/collapse (more/less), reveals |
| `motion.slow`     | `360ms` | larger transitions (planned)       |

Values *(planned)* — confirm against the reference feel during implementation. Motion is subtle and supports the calm, editorial tone.

---

## 16. Easing

| Token              | Curve                          | Usage                    |
| ------------------ | ------------------------------ | ------------------------ |
| `ease.standard`    | standard decelerate/accelerate | most transitions         |
| `ease.emphasized`  | emphasized                     | expressive moments (planned) |

Use Material 3 motion easing as the foundation; keep it restrained.

---

## 17. Opacity

| Token              | Value  | Usage                                     |
| ------------------ | ------ | ----------------------------------------- |
| `opacity.full`     | `1.0`  | primary content                           |
| `opacity.veryHigh` | `0.9`  | sticky header background (`color.bg.header`) |
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

---

## 19. Themes

Themes are token sets. The base is **Warm Journal** (values above). **Monochrome Archive** and **Film Memory** are additional token sets that may change color, typography, borders, surfaces, timeline styling, media treatment, and subtle texture — but never navigation, structure, hierarchy, composer interaction, or search behavior (see [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §11). A timeline may reference a theme; the UI resolves tokens from it via `ReliveTheme`.

Concrete palettes for Monochrome Archive and Film Memory are *(planned)* and will be transcribed/derived when the theming phase begins, and cross-checked against any updated reference.
