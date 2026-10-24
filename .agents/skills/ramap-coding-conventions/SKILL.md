---
name: ramap-coding-conventions
description: Apply Ramap Kotlin formatting, naming, file, string, testing, security, and change-hygiene rules when writing, reviewing, or testing Kotlin Multiplatform code. Use ramap-architecture, ramap-module-structure, and ramap-design-patterns for those separate concerns.
---

# Ramap Team Coding Conventions

Apply this skill to code quality and team habits only. Read the sibling Ramap skills when a change also involves layer ownership, module placement, or an established implementation pattern.

## Source and scope

- Read `AGENTS.md`, the target files, direct callers, dependencies, and related tests before editing.
- Preserve existing user changes and avoid files outside the requested scope.
- Before editing, identify the requested screen or behavior and the minimum acceptance criteria. Give implementation agents the same bounded scope and exact files or responsibilities.
- Follow `AGENTS.md` for routing. Ordinary implementation and validation belong to the primary agent; using these conventions does not automatically start a planner, implementer, or validator workflow.
- Treat adjacent screens, new UI states, backend behavior, and other related improvements as separate work unless the requested behavior cannot function without them. Do not add them merely to make a feature feel complete.
- Follow nearby code when an existing convention is more specific than this general rule.
- Do not create commits, branches, issues, pull requests, or pushes unless explicitly requested.

## Kotlin style

- Use 4-space indentation, LF line endings, UTF-8, and a final newline.
- Keep packages under `com.peto.ramap`.
- Keep one class or interface per file, except for an established sealed-class or interface grouping. Do not declare private classes.
- Prefer ordinary classes with constructor injection for business rules and services. Let DI own their lifetime. Use `object` for singleton values such as sealed state variants when that is their intended role.
- Keep domain rules and state changes on the responsible type or collaborator. Use extension functions only for a necessary external-library adapter or pure presentation conversion when the owning type cannot be changed; explain the exception in the handoff.
- Prefer code that reveals intent. Add a short comment only when the code cannot express the intent clearly.
- Avoid nesting beyond two levels; use guard clauses, early returns, or focused functions.
- Use `when` for complex branching and explicit `for`/`if` flows when collection chains hide branching, side effects, or mutable accumulation.
- Extract functions when they expose a distinct responsibility, abstraction level, or reason to change. Do not split a cohesive method solely to meet a line count.
- Separate state changes, data calls, and result handling into focused functions.

## Formatting and layout

- Use 4-space indentation consistently across all files.
- Place each parameter on its own line for Composable declarations, function signatures, and calls whenever there are multiple parameters or line length is exceeded, and end with a trailing comma (`,`).
- Format `Modifier` chains across multiple lines with a 4-space indent for each chained method call (`.fillMaxWidth()`, `.padding(...)`, etc.).
- Use named arguments (`color =`, `radius =`, `center =`, etc.) for Canvas drawing methods, complex Composable calls, and function calls with multiple parameters.
- Use design system `AppText` with explicit `AppTextStyle` (`AppTextStyle.H1`, `T1`, `B1`, `C1`, etc.) for user-visible text, including new text in an existing screen that still uses Compose `Text`. Use `Text` directly only when `AppText` cannot express the required behavior; state that exception in the handoff.
- Reuse matching `core/designsystem` buttons and interaction components in feature UI. If a visual button pattern is repeated and no component fits, add the reusable variant to `core/designsystem` first. Use raw Material button primitives in a feature only for a genuine exception, and explain it in the handoff.
- **Code formatting & linting standards:**
  - Formatting is governed by the actual project `.editorconfig` and Ktlint configuration: 4-space indentation, LF line endings, final newline, and trimmed trailing whitespace. Kotlin currently has `max_line_length = off`; do not invent a 100-character limit.
  - Use the changed module's `ktlintFormat` task when formatting is needed and inspect its diff for unrelated changes. Use root `./gradlew ktlintFormat` only when formatting across modules is in scope. Formatting modifies source files; a read-only reviewer or validator uses `ktlintCheck` instead.

## Names and values

- Do not use abbreviations such as `cnt`, `calc`, or `usrId`.
- Name functions to expose their action and result. Avoid `get` and `set` prefixes:
  - reads: `fetch`, `find`, `search`
  - mutations: `update`, `change`, `save`
  - Boolean queries: `is`, `has`, `can`, `should`
- Include units and meaning in names such as `elapsedMs`, `remainingRatio`, and `retryCount`.
- Wrap primitives with domain meaning or validation rules in value classes when appropriate.
- Use first-class collection types when a collection owns behavior or invariants.
- Put magic numbers and non-user-facing debug error messages in `companion object` constants.

## Text, tests, and safety

- Put every user-visible string in `strings.xml`; use positional placeholders such as `%1$s` and `%2$d`.
- Preserve existing user-visible UI text unless the user explicitly requests a text change. A layout request alone does not authorize new labels or rewritten copy.
- Use `kotlin.test` for common tests and the repository's existing coroutine, Turbine, Compose UI, and Robolectric utilities as applicable.
- Put shared logic tests in `commonTest`, Android-specific behavior in `androidHostTest`, and iOS-specific behavior in `iosTest`.
- Name tests after the subject and behavior, such as `SampleRepositoryTest`.
- Write test-case function names, natural-language descriptions, and assertion messages in Korean. Use backtick function names to describe the condition and observable result; keep technical class names and fixture identifiers meaningful.
- Keep Supabase values, API keys, signing values, and machine-specific configuration out of source and commits.

## Finish the change

- Do not maintain a parallel per-task changelog. Use Git commits and pull requests as the shared record of changes, put validation results in the pull request or CI results, and record deployment state in the relevant pull request or release record. Keep credentials, personal data, and detailed security evidence within their existing local-only boundaries. The local-only `docs/change-history/` directory is optional and may be removed at the user's request; do not append new work to it, read it routinely, or recreate it when absent.
- At final PR preparation, update the wiki only when approved policy, domain meaning, user-visible behavior, an important decision reason, or a documented knowledge error changes. File moves, internal refactoring, implementation parameters, and verification/deployment results alone do not require a wiki update or a fresh `source_ref`. Run the wiki check after an actual wiki edit; run validator regression tests when its rules change.
- Inspect tracked and untracked changes against the acceptance criteria before reporting completion. Preserve pre-existing user changes, and call out any necessary supporting change that extends beyond the requested surface.
- Check changed Compose UI for direct `Text` usage and confirm any exception to the `AppText` rule. Formatting and compilation checks do not enforce this convention.
- Run the smallest relevant Gradle test and `git diff --check`. Android host tasks use `:<module>:testAndroidHostTest`; iOS simulator tasks use `:<module>:iosSimulatorArm64Test`. Select app builds or shared integration checks only when the affected platform or dependency path requires them.
- Use `./gradlew ktlintCheck` when formatting or broad Kotlin style is affected.
- Run app-level E2E flows or control a connected device only when the user explicitly requests that verification.
- Report the command, result, and whether any failure is from the implementation, environment, or pre-existing changes.
- For a dedicated implementation, follow `.codex/agents/implementer.toml` and hand the commands, expected results, and known constraints to the separate validator defined in `.codex/agents/test-validator.toml`. Test and lint success do not replace the read-only UI and ViewModel pattern checks required by those role documents.
