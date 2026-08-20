# Relive — Architecture Decision Record

This is the running log of **major** architectural and product-structure decisions. Record a decision here when it settles something future work must respect. Once recorded, a decision is **settled**: do not silently change it (see [`../AGENTS.md`](../AGENTS.md)). To change a settled decision, add a new entry that supersedes the old one and explains why.

Format for each entry:

- **ID / date / status** (Accepted | Superseded | Proposed)
- **Context** — the situation forcing a choice
- **Decision** — what was chosen
- **Consequences** — what this enables/constrains

---

## ADR-0001 — Local-first, no backend

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Relive is a private personal memory archive. The product explicitly excludes backend, cloud sync, login, and social features for the current product.
- **Decision:** All data is stored on-device. No server, network data layer, authentication, or sync is built now.
- **Consequences:** Persistence is a local database plus on-device media files. No networking stack or auth. Any future sync would be a separate, explicitly approved decision. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §1, §5.

---

## ADR-0002 — Kotlin Multiplatform + Compose Multiplatform, shared UI

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Target Android and iOS with a small team and a distinctive, consistent UI.
- **Decision:** Use Kotlin Multiplatform for shared logic and Compose Multiplatform for shared UI. Platform apps (`androidApp`, `iosApp`) are thin entry points. Toolchain is pinned in `gradle/libs.versions.toml` (Kotlin 2.4.10, Compose MP 1.11.1, Material3 1.11.0-alpha07, AGP 9.0.1).
- **Consequences:** UI and business logic live in `shared/`. Platform code is minimized. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §2–§3.

---

## ADR-0003 — Material 3 foundation, custom Relive design system on top

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** The app must be accessible and behave correctly, but must **not** look like a default Material app. It must match the approved warm-editorial reference.
- **Decision:** Build on Material 3 for interaction, accessibility, and component behavior; apply a strict tokenized Relive design system for all visual styling. The approved UI reference in `docs/ui-reference/` is authoritative for timeline UI.
- **Consequences:** No raw styling in components — everything comes from tokens. When tokens conflict with the reference, work stops and the conflict is reported. See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

---

## ADR-0004 — Store each moment once; timelines reference moments

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** A moment can appear in multiple timelines; duplicating it would corrupt edits and waste storage.
- **Decision:** Persist each moment exactly once. Custom timelines reference moments via a many-to-many membership table. Attachments and tags reference their moment. The built-in **All** timeline is a logical query over all moments, not per-moment membership rows.
- **Consequences:** Editing/forgetting a moment affects all timelines consistently. Membership is cheap to change. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §2, §13 and [`ARCHITECTURE.md`](ARCHITECTURE.md) §5.

---

## ADR-0005 — 4-day edit/forget window keyed on immutable `createdAt`

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Memories should feel permanent shortly after capture, with a brief correction window, and must not be endlessly editable.
- **Decision:** A moment may be edited or forgotten only within 4 days of its **immutable `createdAt`**. `updatedAt` **never** extends the window. The rule is centralized in the domain layer using a `Clock` abstraction and governs long-press actions, inline editing, and forgetting. Forgetting requires confirmation.
- **Consequences:** Deterministic, testable rule. UI must hide Edit/Forget after the window closes. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §8 and [`TESTING.md`](TESTING.md).

---

## ADR-0006 — Timeline-scoped search

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Users navigate by timeline (chapter of life); search should respect the current context.
- **Decision:** Search always operates within the currently selected timeline, with All / Tags / Places filters scoped to that timeline. On the built-in All timeline, scope is every moment. All-search behaves like WhatsApp chat search (highlight, match count, up/down navigation, auto-scroll).
- **Consequences:** Search queries carry the active timeline scope. Places/Tags suggestions derive only from the current timeline's moments. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §9.

---

## ADR-0007 — Platform capabilities behind shared interfaces

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Location, media capture/storage, and similar capabilities differ per platform, but shared logic must stay platform-agnostic.
- **Decision:** Declare capability interfaces in shared code (e.g. `LocationProvider`, `PlaceResolver`, media capture/storage). Implement them in `androidMain` (Android APIs) and `iosMain` (Core Location, AVFoundation/UIKit). Shared domain/presentation code depends only on the interfaces and never imports platform APIs.
- **Consequences:** Business logic is testable with fakes; implementations can change without touching shared logic. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §6.

---

## ADR-0008 — Location is moment-scoped and local-only

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** Location adds value to a moment but must not become tracking or a privacy liability.
- **Decision:** Location is captured only at moment-creation time, on demand, via `LocationProvider` → `PlaceResolver`. No background tracking, no separate location history, no third-party location analytics. Permission is requested only when needed via the platform flow; denial never blocks saving. All failure states (denied, permanently denied, services disabled, unavailable, timeout) let the composer continue. `ReliveLocation` fields are all optional; raw coordinates are never shown in the normal timeline UI. Location data stays local with the moment. The location data model and interfaces are established **before** platform implementations.
- **Consequences:** Strong privacy posture; robust composer behavior. See [`PRODUCT_SPEC.md`](PRODUCT_SPEC.md) §7 and [`ARCHITECTURE.md`](ARCHITECTURE.md) §6.1.

---

## ADR-0009 — Monetization deferred but seam kept open

- **Date:** 2026-08-20 · **Status:** Accepted
- **Context:** RevenueCat (Pro), RevenueCat Funnels, and Stripe (web) are planned later but must not be built now.
- **Decision:** Represent Pro entitlement behind an interface in shared code, initially backed by a local stub. No RevenueCat/Stripe dependencies are added until the monetization phase. Gating logic lives in domain/presentation so a real source can be swapped in.
- **Consequences:** The app ships free-tier now and can add real entitlements later without restructuring. See [`ARCHITECTURE.md`](ARCHITECTURE.md) §9, [`ROADMAP.md`](ROADMAP.md) Phase 9, [`RELEASE.md`](RELEASE.md).

---

## Template for new decisions

```
## ADR-XXXX — <short title>
- **Date:** YYYY-MM-DD · **Status:** Proposed | Accepted | Superseded
- **Context:** …
- **Decision:** …
- **Consequences:** …
```
