# Onboarding design blueprint

This document defines the shared presentation shell for the five-chapter first-launch onboarding. It records the approved Capture, Organize, and Organize → Relive handoff so the remaining chapters extend the same scene rather than introducing a new page treatment. It supplements PRODUCT_SPEC §1A and ADR-0094; those documents remain authoritative for onboarding behavior, privacy, and completion.

## Shared chapter shell

Every chapter uses one edge-to-edge, dark atmospheric canvas. Its gradient, glow, tint, shadow, and the subtle timeline curve interpolate with chapter progress; there is no top, bottom, or card-area background boundary. Chapter-specific colors come from the ordered dark Relive palettes already defined in ADR-0093.

The layout is stable across chapters:

1. A compact top header reserves the Skip action on chapters one through four.
2. A generous visual stage contains the chapter's native product preview and any supporting controls.
3. The lower shell contains, in order, a five-segment progress indicator, heading, short caption, generously separated primary forward button, and Back when that chapter supports backward navigation.

The lower shell is deliberately minimal: no secondary page dots, continuous progress bar, or additional navigation treatment. Progress stays above the heading and interpolates its active segment during an adjacent-chapter handoff. Continue is the primary action for chapters that advance the story; final completion actions, Skip, and Back retain their existing behavior.

## Moment-led visual language

Use the real Relive `MomentCard` presentation for a sample memory, including its pinned-paper treatment, metadata, media area, rail, and dots where timeline context is shown. Preview content stays in memory, cannot open or mutate the archive, and uses themed placeholder media when photography is not required by the chapter.

The carried sample Moment is square, with one shared focused size and focus-zone position. When a chapter hands it to a timeline, the destination card is measured and placed at the same bounds before motion begins; the rail and first dot appear with that handoff, not afterward. Surrounding timeline cards remain visible, smaller, and softened so the focused card is unmistakable. Metadata, rail, dots, and pins use a visible color derived from the active atmospheric palette.

## Capture and Organize pattern

Capture introduces the square “A quiet morning” sample Moment in the fixed focus zone. Its visual stage also has four unlabeled, elevated rounded-square controls: Photo, Note, Video, and Audio. They are scattered above and below the card, never clipped or overlapping it, and enter with a soft staggered rise, fade, slight scale, and small rotation settle.

Organize receives that exact Moment as its first focused timeline item, at the same position and dimensions. The timeline forms around it without a cut, then begins a short, smooth automatic upward scroll after settling. Focus emphasis continuously transfers to the card in the fixed focus zone. Back returns the original focused Moment and Capture shell before reversing the chapter transition.

## Organize → Relive: timeline becomes a custom timeline

Continue on Organize is the only trigger for this handoff. The lower shell does not enter the vacuum: only the Organize hero timeline visualization behaves as the flexible sheet.

The future Relive custom-timeline card already sits directly beneath that sheet, fully covered at rest. It uses the production custom-card structure and the content `Mountain escapes`, `12 moments`, and `Aug 2026`; its card boundary, title, metadata, and media area are uncovered only as the sheet retreats. The card never fades or scales in as a separate object.

The card's full-bleed media preview is the single suction destination. Until absorption completes it is an empty, dark recessed rounded rectangle with subtle inner depth — a hole, not a black media placeholder. The pull begins at the lower center of the hero timeline; nearby rail, cards, and content follow, its lower corners curve inward, and the upper portion initially remains largely intact. The retreating boundary stays curved and continuous, never a triangular/trapezoidal funnel or uniform timeline scale-down. Content that reaches the media bounds is clipped exactly there, so it reads as disappearing behind the fixed opening.

The reveal advances continuously from a small exposed custom-card edge to its surface, title/metadata strip, and finally the media-hole boundary. The two parts of the sheet deformation overlap so the uncovering boundary never pauses midway. The last flexible remnant enters the same media opening; it does not fade away.

As soon as the final sheet fragment is absorbed, the empty opening materializes the themed placeholder thumbnail over a short blur-to-clear, opacity, and scale reveal. It uses abstract landscape/media shapes rather than a photograph. The custom timeline card remains still after this reveal: it has no bounce or heartbeat settle.

During the late part of suction, the Relive heading/caption and final progress segment crossfade quietly into place so the thumbnail remains the final visual beat. Continue and Back brighten progressively but stay non-interactive until the entire Screen 3 scene has rendered; their enabled state must not produce a visible jump.

## Relive → Privacy: shared-card takeover

Privacy retains the same shared chapter shell: segmented progress, heading, caption, Continue, and Back. Its copy is **“Your memories stay yours.”** and **“A personal archive, stored on your device.”**

The Privacy outer panel uses the exact measured x/y position, width, and height of the completed `Mountain escapes` custom timeline card. It is not independently positioned, resized, or substituted with a new hero container. On Continue from Relive, the custom timeline card stays fixed while the dark Privacy surface grows over the same bounds as a layer; the card beneath progressively softens and obscures. The takeover is continuous and deliberately unhurried, never a slide-away, abrupt screen switch, hard crossfade, or rectangular flash.

The Privacy surface is a single rounded dark panel with its shadow and border clipped to the same rounded outline during both forward and reverse motion. No partially revealed square, old timeline-sheet fragment, or other intermediate compositing artifact may appear. Back reverses the same ownership transfer: Privacy content clears, the rounded surface recedes over the shared bounds, and the unchanged Relive card returns beneath it.

Only after the privacy surface is mostly dominant does the lock reveal in the upper-center area. It uses opacity plus a soft `0.88 → 1` scale and a small upward settle, with no bounce or pop. Beneath it are three individual rounded horizontal bars, clipped by the outer Privacy panel so they physically emerge from behind its edges. Their initial directions are left, right, then left; their spring is high-damping with only imperceptible overshoot. Each bar preserves its settled size, position, and a subtly distinct rose/purple/dark tint.

The bars contain these title/caption pairs:

1. **Stored on your device** — “Your archive begins locally.”
2. **No account needed to begin** — “Start without signing in.”
3. **You control what you keep** — “Edit or forget your own moments.”

## Remaining chapters

Relive, Privacy, and Ready retain their approved chapter-specific native previews, copy, and actions. Relive begins with the custom-timeline handoff above. All three chapters reuse the shared header, visual stage, lower shell, segmented progress, palette interpolation, typography, spacing, and motion behavior above. A chapter may replace the visual-stage content, but must not add a separate surface, alternate navigation arrangement, or a different primary-action hierarchy.

## Motion and responsive rules

Adjacent chapters overlap gently: the persistent Moment, background, progress, copy, and supporting native groups retarget continuously from their current state. Supporting groups use soft opacity, 8–12dp travel, and slight scale; card focus and auto-scroll use a longer, non-overshooting emphasized curve. There are no harsh cuts, delayed chrome, or discrete palette swaps.

At compact heights, preserve the header, focused Moment, lower controls, and their touch targets first. Reduce decorative spacing and supporting content before clipping any control. At tall heights, retain the same anchors and add breathing room rather than increasing the card beyond its shared focused size. Reduced motion removes travel, stagger, scale, and auto-scroll while preserving concise copy/action fades and the correct stable state.
