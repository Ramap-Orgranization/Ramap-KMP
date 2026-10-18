---
name: ramap-architecture
description: Apply the Ramap Kotlin Multiplatform layer responsibilities and dependency direction when deciding where behavior, contracts, UI state, platform code, or composition logic belongs.
---

# Ramap Architecture

Use this skill to place behavior in the correct architectural layer. Keep business meaning independent from UI, data access, and platform APIs.

## Responsibility map

- `domain`: business models, repository contracts, domain stores, and use-case contracts or domain-facing rules.
- `data`: data sources, serialized responses/DTOs, repository implementations, data-owned mapping, and data implementations of domain use cases.
- `core`: reusable cross-feature foundations and infrastructure:
  - `common`: shared primitives and result/error foundations
  - `network`: Supabase, Ktor, configuration, and network execution
  - `platform`: platform-facing abstractions and implementations
  - `analytics`: analytics contracts and implementations
  - `notification`: notification infrastructure
  - `navigation`: typed routes, navigation state, and navigation rendering
  - `ui`: shared UI state infrastructure, base ViewModels, and screen-facing resource mappers such as area filters
  - `preview`: Compose preview parameter providers and sample domain data
  - `designsystem`: reusable Compose components, theme, resources, and the domain-to-resource mappers used by those components
  - `testing`: test-only fakes and fixtures
- `feature`: user-facing screen behavior, feature contracts, ViewModels, routes, and components. Features consume domain contracts and core services; they do not own data access.
- `shared`: composition root, root UI, app navigation wiring, Koin assembly, deep-link orchestration, and common platform bridges.
- `androidApp`: Android entry point, manifest, Android resources, and Android-only app integration.
- `iosApp/iosApp`: SwiftUI entry point and Xcode-owned assets.
- `debug/admin`: Android admin tools, composed only through `androidApp`'s `debugImplementation` dependency.

## Dependency direction

Keep production flow directed toward stable abstractions:

`androidApp` / `iosApp` → `shared` → `feature`, `data`, `domain`, `core`

`feature` → `domain`, `core`

`data` → `domain`, `core/network`, `core/common`

`domain` → `core/common`, `core/analytics`

`domain` uses the common analytics API for login orchestration in `SignInUseCase` (`LoginAnalytics`, `AnalyticsSource`, `LoginMethod`). Keep Firebase and other platform SDK calls inside the platform implementations of `core/analytics`.

Resource presentation follows `core/ui` → `core/designsystem` → `domain`. Do not introduce a reverse dependency from `core/designsystem` to `core/ui`.

Do not add direct feature-to-feature dependencies or production dependencies on `core/testing`. Use `feature/main` only as the existing aggregator for the main-tab feature modules.

`debug/admin` may use the existing domain and core contracts for admin tooling. Keep it outside release app dependencies. `core/preview` may use domain models for preview fixtures; do not put runtime business rules in preview providers.

## Ownership rules

- Keep business rules out of ViewModels and composables. Put them in domain rules, use cases, stores, or the appropriate data implementation.
- Keep repository contracts in `domain`; keep repository implementations and remote/local data access in `data`.
- Keep resource mapping with its consumers: `core/designsystem` owns mappings shared by its components (business hours, events, categories, shop information, and operating notices); `core/ui` owns screen-facing area/filter mappings. Let features compose their UI state using those existing mappers instead of duplicating conversions.
- Keep platform APIs behind `expect`/`actual` or a core platform abstraction. Put platform implementations in `androidMain` or `iosMain`.
- Keep app-wide assembly in `shared`; do not make individual features assemble unrelated features.

## Boundary changes

Before moving behavior across layers, inspect all callers, dependency declarations, DI modules, platform source sets, and tests. Treat a new dependency edge or a new cross-layer contract as an architectural change, not a local refactor.

Architecture analysis does not itself require a planner or implementation subagent. Follow `AGENTS.md` for routing; use `source-command-impl` only for an explicitly requested staged plan and approval workflow. When a dedicated implementer is assigned, `.codex/agents/implementer.toml` defines its role and this skill remains the source for layer rules.
