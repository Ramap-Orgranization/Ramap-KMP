---
name: ramap-design-patterns
description: Reuse the established Ramap ViewModel, UI state, side effect, repository, data source, Koin, resource mapper, navigation, and platform integration patterns when implementing features.
---

# Ramap Design Patterns

Reuse these patterns before inventing a new abstraction. Keep architectural placement and module selection in the sibling skills.

## Feature state flow

- Model each feature with a typed `Intent`, `State`, and `SideEffect` contract.
- Extend `BaseViewModel<S, I, SE>` for feature ViewModels. Send UI events with `dispatch`, route them through `handleIntent`, update state with `reduce`, and emit one-off effects with `postSideEffect` or `trySideEffect`.
- Use `launchTask` or `launchResultTask` for cancellable work, duplicate-request policy, and common error handling. Do not reimplement those mechanics in each ViewModel.
- Give each operation a stable `taskKey` and choose the existing `TaskPolicy` for its actual interaction: `CancelPrevious` replaces work with the same key, while `IgnoreNew` skips a new request while that key is active. Keep cancellation propagation and loading cleanup owned by `BaseViewModel`.
- When an asynchronous task exposes loading in UI state, implement `LoadableState`, pass a typed `LoadKey` to the task, and derive loading from `loadState` even if the screen has only one loading operation. Do not toggle a parallel `loading` Boolean in `onStart`/`onFinish`. A task with no UI loading state does not need a `LoadKey`.
- When updating a legacy ViewModel or screen, compare it with a current feature using the shared pattern; nearby legacy code is not a reason to keep a custom state or loading mechanism.
- Keep ViewModels responsible for UI state and event orchestration. Put business rules in domain rules, stores, or use cases.
- Split large intent handlers by concern, as in viewport, shop, search, personalization, or account handlers.
- Read the current contracts in `core/ui/src/commonMain/kotlin/com/peto/ramap/ui/base` and `core/ui/src/commonMain/kotlin/com/peto/ramap/ui/loading` before wiring a new flow. Use `launchResultTask`'s existing result and retry hooks rather than adding a second network error or retry mechanism.

## Route and UI composition

- Let a feature `Route` obtain its ViewModel, collect state, observe side effects, and translate callbacks into typed intents.
- Keep rendering in `Content` or focused components. Pass state and callbacks down; do not inject repositories into leaf components.
- Resolve domain-to-UI resources through the existing mapper in the owning presentation module: `core/designsystem` for its shared components and `core/ui` for screen-facing area/filter mappings. Follow `ramap-architecture` and `ramap-module-structure` for ownership and paths; do not create screen-local duplicates.
- Keep transient UI-only state in the component when it does not belong to the feature's durable state.

## Repository and data source

- Declare repository interfaces in `domain` and implement them as `internal Default<Domain>Repository` classes in `data`.
- Put remote or local access behind a data-source interface. Keep serialized response/DTO types in `data.model` and convert them to domain types at the data boundary.
- Wrap network operations with the existing `invokeRequest`/`RamapResult` error path. Do not leak DTOs or network clients into features.
- Split repository interfaces by responsibility rather than adding unrelated methods to an existing contract.

## Koin composition

- Give each feature or infrastructure area a `di` module in its owning module.
- Register ViewModels with `viewModelOf`, stateless services with `singleOf`, and explicit platform implementations against their interfaces.
- Let `AppModule` collect feature modules with `includes()` and register only app-level objects. Let `KoinInitializer` combine app, network, platform, data-source, repository, use-case, design-system, and notification modules.
- Do not register feature ViewModels directly in `AppModule`.

## Navigation and platform bridges

- Represent destinations with typed `ScreenRoutes` and keep per-tab back stacks in `NavigationState`.
- Let `NavigationRouter` render route entries and delegate screen content to feature routes. Keep navigation mutation in `NavigationState`/router APIs rather than in leaf components.
- Define common platform contracts with `expect`/`actual` or a `core/platform` interface. Keep Android and iOS implementations in their platform source sets and bridge them through `shared` only when app composition requires it.

## Testing pattern

- Test domain rules and mappers as pure common tests.
- Test repositories with data-owned fakes and test doubles; use `core/testing` only for reusable cross-module fixtures and fakes.
- Test feature ViewModels through typed intents, emitted state, and side effects. Name tests after the subject and behavior.
- Before handoff, check changed ViewModels for `BaseViewModel` and, when they expose asynchronous loading, `LoadableState`/`LoadKey` usage. Tests and ktlint do not verify this pattern.
- For a dedicated implementation workflow, include these pattern checks alongside the Gradle commands in the handoff to `.codex/agents/test-validator.toml`. For ordinary implementation, the primary agent performs them as part of its own validation under `AGENTS.md`.
