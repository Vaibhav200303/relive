# AGENTS.md

Operating rules for any AI agent or human contributor working on **Relive**.

This file is binding. If a request conflicts with these rules, stop and surface the conflict before acting.

Relive is a private, local-first personal memory timeline built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS. The documents in `docs/` are the source of truth for the product, architecture, and design. Read them before writing code.

---

## Golden rules

1. **Inspect before modifying.** Read the relevant source, docs, and existing patterns before changing anything. Never edit a file you have not read.
2. **Work phase by phase.** Follow the phases in [`docs/ROADMAP.md`](docs/ROADMAP.md). Do not start a later phase while an earlier one is incomplete unless explicitly instructed.
3. **No unrelated scope.** A change addresses exactly one requested task. Do not opportunistically refactor, rename, reformat, or "improve" unrelated code in the same change.
4. **No unrequested dependencies.** Do not add libraries, plugins, or Gradle modules unless the task explicitly calls for them. Prefer the standard library and what is already declared in `gradle/libs.versions.toml`.
5. **Preserve architecture boundaries.** Respect the layering defined in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): domain, data, presentation, and platform integrations stay separated. Shared business logic must not depend on Android or iOS APIs.
6. **Small changes.** Prefer focused, reviewable diffs over large rewrites. Split large tasks into steps.
7. **Tests for important behavior.** Any non-trivial behavior — especially domain rules, persistence, search, and the 4-day edit/forget rule — ships with tests. See [`docs/TESTING.md`](docs/TESTING.md).
8. **Formatting and static analysis before completion.** Run the project's formatting and static-analysis checks and fix issues before declaring a task done.
9. **Review `git diff`.** Read your own diff end to end before finishing. Confirm nothing unrelated changed.
10. **Never commit, merge, or push without explicit approval.** Leave changes staged or unstaged for human review. The human decides when history changes.
11. **Never silently change settled decisions.** Product behavior, architecture, and design tokens that are already written down are settled. To change one, propose it, get approval, and record it in [`docs/DECISIONS.md`](docs/DECISIONS.md).
12. **Document major architectural decisions** in [`docs/DECISIONS.md`](docs/DECISIONS.md) as they are made.

---

## Authoritative UI reference

For any timeline UI work, these files are authoritative:

- `docs/ui-reference/timeline-reference.png`
- `docs/ui-reference/timeline-reference.html`

Do **not** redesign, reinterpret, or replace the approved layout unless explicitly instructed to. The reference defines the intended spacing, typography, rail, dots, media presentation, and composer layout. Match it.

**When written design tokens in [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) conflict with the approved UI reference, stop and report the conflict.** Do not improvise a resolution.

---

## What Relive is (and is not)

Relive must feel like a **beautiful personal life archive**, never a notes app, database front-end, or CRUD tool. Every UI and product decision serves that feeling: warm editorial aesthetic, continuous scrolling, generous whitespace, premium media presentation.

See [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) for the full product definition. Do not invent behavior beyond what is specified there.

### Out of scope unless explicitly requested later

Do not add any of the following on your own initiative:

- backend or server
- cloud sync
- login or accounts
- social features
- AI, embeddings, recommendations
- background location tracking or location history separate from moments
- third-party analytics

RevenueCat (Pro entitlement), RevenueCat Funnels, and Stripe (web subscription) are planned for later phases. The architecture must **allow** them without implementing them now.

---

## Workflow for a typical task

1. Read the relevant `docs/` files and the code you will touch.
2. Confirm which roadmap phase the task belongs to.
3. Make the smallest change that satisfies the request.
4. Add or update tests for important behavior.
5. Run formatting, static analysis, and the relevant test tasks.
6. Review the full `git diff` and `git status`.
7. Report what changed and why. Wait for review before any commit.

---

## Conventions

- Language/style: Kotlin official code style (`kotlin.code.style=official`).
- Package root: `com.vaibhav.relive`.
- Shared code lives in `shared/`; platform entry points in `androidApp/` and `iosApp/`.
- Platform-specific capabilities (media capture, storage, location) are accessed in shared code only through interfaces; concrete implementations live in platform source sets. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## When in doubt

Stop and ask. It is always correct to surface ambiguity, a conflict between the design tokens and the UI reference, or a request that would change a settled decision, rather than guess.
