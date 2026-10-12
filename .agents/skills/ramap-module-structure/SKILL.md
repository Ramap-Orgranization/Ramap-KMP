---
name: ramap-module-structure
description: Choose Ramap Gradle modules, source sets, package paths, resources, tests, and dependency edges for Kotlin Multiplatform changes. Use when adding or moving code between modules.
---

# Ramap Module Structure

Use this skill when a change creates, moves, or wires code across Gradle modules. `settings.gradle.kts` is the source for included modules; confirm the owning module's actual `build.gradle.kts` before editing. Read the working tree as well as tracked files so concurrent module additions are preserved.

## Module inventory

- App and composition: `:androidApp`, `:shared`
- Core: `:core:analytics`, `:core:common`, `:core:ui`, `:core:preview`, `:core:designsystem`, `:core:network`, `:core:platform`, `:core:notification`, `:core:navigation`, `:core:testing`
- Stable contracts and data: `:domain`, `:data`
- Main aggregator and tabs: `:feature:main`, `:feature:main:event:list`, `:feature:main:event:detail`, `:feature:main:notice`, `:feature:main:map`, `:feature:main:japan`, `:feature:main:ranking`, `:feature:main:my`
- Other features: `:feature:profile:edit`, `:feature:review:my`, `:feature:review:other`, `:feature:review:write`, `:feature:settings`, `:feature:account`, `:feature:bookmark:list`, `:feature:bookmark:importation`, `:feature:hidden`, `:feature:notification`, `:feature:report`, `:feature:subscribed`
- Debug tooling: `:debug:admin`, included by `androidApp` through `debugImplementation`

## Placement rules

- Keep Kotlin packages under `com.peto.ramap`.
- Put shared domain models and contracts in `domain/src/commonMain`.
- Put DTOs, data sources, repository implementations, and data-owned use-case implementations in `data/src/commonMain`.
- Put feature contracts, UI state, ViewModels, routes, and components in the owning feature's `src/commonMain`.
- Put Android-only APIs in the owning module's `src/androidMain`; put iOS-only APIs in `src/iosMain`.
- Put common tests in `src/commonTest`, Android host tests in `src/androidHostTest`, and iOS tests in `src/iosTest`.
- Put resource mappers consumed by shared design-system components in `core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/resource`; keep screen-facing area/filter mappers in `core/ui/src/commonMain/kotlin/com/peto/ramap/ui/resource`. Check existing consumers and reuse the owning mapper before adding a conversion.
- Put shared Compose components and theme resources in `core/designsystem`; put Android entry resources in `androidApp/src/main`; put Xcode assets in `iosApp/iosApp`.
- Put Compose preview parameter providers in `core/preview/src/commonMain`. Keep Android admin tooling in `debug/admin/src/androidMain`, host tests in `src/androidHostTest`, device UI tests in `src/androidDeviceTest`, and fakes shared by those admin test targets in `src/androidTestSupport` as configured by its build file.

## Dependency rules

- Features depend on `domain` and the required `core` modules. They must not depend directly on another feature.
- `data` depends on `domain` and core infrastructure such as `core/network`; `domain` must not depend on `data`.
- `domain` depends on `core/common` and the common API of `core/analytics`. Keep platform analytics SDK access in `core/analytics` platform source sets.
- `core/ui` consumes `core/designsystem`; design-system components must not depend back on `core/ui` to obtain resource mappings.
- `shared` may depend on features, data, domain, and core modules because it is the composition root.
- `feature/main` aggregates `feature/main/*` modules; do not bypass this structure by creating a new feature-to-feature edge.
- Keep `core/testing` test-only. Put data-specific fakes under `data/src/commonTest`; put reusable cross-module fakes and fixtures under `core/testing`.
- Keep `debug/admin` outside release dependencies. Its Android host tests may depend on `core/testing`; release app code must not depend on admin tooling.
- Add a module to `settings.gradle.kts`, apply the existing convention plugin, declare only the needed dependencies, and add its DI module when the module owns injectable runtime objects.

## Change checklist

1. Identify the owning responsibility and module.
2. Confirm the source set and package path.
3. Check the module's existing dependencies and nearby files.
4. Add the smallest dependency edge that preserves the direction above.
5. Register DI at the module boundary and include it from the composition root when needed.
6. Place any necessary tests beside the changed module and choose its narrowest relevant `testAndroidHostTest` or `iosSimulatorArm64Test` task. Follow `.codex/agents/implementer.toml` for a dedicated implementation handoff; module placement alone does not require delegation.
