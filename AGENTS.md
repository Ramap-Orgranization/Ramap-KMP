# Repository Guidelines

## Project structure and implementation source of truth

This is a modularized Kotlin Multiplatform project targeting Android and iOS. Keep the detailed rules in separate skills so they can be evaluated and updated independently:

- `.agents/skills/ramap-coding-conventions/SKILL.md`: formatting, naming, tests, security, and change hygiene.
- `.agents/skills/ramap-architecture/SKILL.md`: layer responsibility, ownership, and dependency direction.
- `.agents/skills/ramap-module-structure/SKILL.md`: Gradle modules, source sets, package paths, and placement.
- `.agents/skills/ramap-design-patterns/SKILL.md`: established ViewModel, data, DI, navigation, resource, and platform patterns.

Read the relevant skills before changing production code. Do not recreate their rules in another agent document; update the owning skill when a team rule changes.

## LLM Wiki maintenance

Start with `docs/wiki/README.md` and read only the pages relevant to the task. During implementation and validation, keep findings, results, and pending wiki changes in the local-only `docs/change-history/CHANGELOG.md`; do not update wiki pages, decisions, or candidates after each task. At final PR preparation, consolidate the final diff and accumulated records under `docs/wiki/maintenance.md`: update verified implementation facts, record explicitly confirmed policy changes in `docs/wiki/decisions.md`, and keep unresolved reusable knowledge in `docs/wiki/inbox/`. Confirmed instructions in their owning source take effect immediately; an explicit request to edit wiki content or its maintenance rules can be handled when requested. Do not promote suggestions, repeated observations, or another repository's rules into approved Ramap policy. The four project skills above remain the source of truth for implementation rules; the wiki connects their responsibilities without duplicating them.

Run `python3 scripts/check_wiki.py` after an explicitly requested wiki edit and at final PR preparation; report affected pages or why no update is needed in the PR. Routine task completion does not require a wiki edit or wiki check. Do not publish local-only documents, secrets, or unrelated working-tree changes. `AGENTS.md`, `.codex/`, project skills, and the reusable `tools/menu-refresh/` skill are shared through Git; personal settings, runtime data, and historical outputs remain local-only. Shared maintenance instructions live in the tracked wiki and PR template.

## Build, Test, and Development Commands

- `./gradlew testAndroidHostTest`: run Android host tests across all KMP modules; this is the CI unit-test command.
- `./gradlew iosSimulatorArm64Test`: run iOS simulator tests across all KMP modules.
- `./gradlew :<module>:testAndroidHostTest`: run Android host tests for one module, for example `:feature:main:map`.
- `./gradlew :shared:iosSimulatorArm64Test`: run shared iOS integration tests, including transitive Compose resources.
- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`: link the iOS Simulator framework.
- `./gradlew :androidApp:assembleDebug`: build the Android debug APK.

Run the iOS app from Xcode by opening `iosApp`.

## Implementation Agent Routing

The primary agent owns ordinary implementation requests from investigation through code changes and validation. Read the relevant project skills, inspect the existing behavior, make routine implementation decisions, and complete the request. Do not spawn a planner, implementer, or validator merely because a request spans modules, touches Supabase, or needs a plan to organize the work.

Use subagents when the user explicitly requests delegation or a dedicated workflow below requires one. Choose by the actual task:

- **Planning and code review:** `planner` for substantial unresolved product or architectural decisions and explicitly requested staged plans; `code_reviewer` for dedicated code reviews.
- **Assigned implementation and validation:** `implementer`, `test_validator`, and `release_manager` for their dedicated workflows. Content planning and post-registration menu checks use `instagram_marketer` and `menu_registration_reviewer`.
- **Git workflows:** `git_commit`, `git_issue_branch`, and `git_pr`, together with their corresponding task skills.

Each role has one standalone TOML file directly under `.codex/agents/`, such as `planner.toml` or `implementer.toml`. Its `name` is the role ID; its `description`, `model`, `model_reasoning_effort`, and `developer_instructions` define discovery, runtime settings, and responsibilities together. Use role-based names without model prefixes or model-specific directories, and do not duplicate these instructions in a paired Markdown file. The project skills remain the owners of implementation rules.

When a subagent is required, spawn the matching named role. A `task_name` is only the displayed task label and does not select the role or model. If a named role is unavailable in the current session, read its TOML and use those instructions, model, and reasoning effort when spawning a new agent. Existing agent turns keep the model selected when they started.

An implementation subagent must read its own instruction file, this file, the four Ramap project skills above, and any approved planner handoff before editing code. Give it exact files or responsibility and acceptance criteria; it must preserve concurrent changes.

When the user invokes `/impl` or explicitly requests a plan-approval and implementation workflow, use `.agents/skills/source-command-impl/SKILL.md`. Follow the `planner` → user approval → `git_issue_branch` → `implementer` → `test_validator` sequence and do not begin implementation before approval.

For a direct implementation request that does not require that staged workflow, the primary agent implements and runs checks appropriate to the change. Documentation-only, research, review, Git, database registration, and event registration requests continue to use their dedicated workflows.

When the user invokes `/menu-refresh`, `$menu-refresh`, or asks to refresh existing shop menus by database date or shop list, use `tools/menu-refresh/SKILL.md`. Reuse the tools, templates, and data in `tools/menu-refresh/`; `skills/menu-refresh` is a compatibility symlink. This is the complete workflow; `.agents/skills/menu-refresh/SKILL.md` is an unfinished placeholder. Keep menu registration policy in `docs/menu-registration-policy.md` and use the refresh skill's collection, guarded writes, and independent verification procedure.

When the user invokes `/ramap-review`, `$ramap-review`, or requests a code review, use `.agents/skills/ramap-review/SKILL.md` and route the review to `code_reviewer`, defined in `.codex/agents/code-reviewer.toml`. The review agent is read-only by default and must inspect the diff, related call paths, state transitions, tests, design-system reuse opportunities, and object-oriented design concerns before reporting severity-ranked findings.

When the user invokes `/release`, `$release`, or asks to prepare or publish a new Android release, use `.agents/skills/release/SKILL.md` and route the entire workflow to `release_manager`, defined in `.codex/agents/release-manager.toml`. The release manager owns the `develop` → `release` merge, version bump, tests, signed AAB, patch notes, GitHub Release verification, and the post-release `release` → `develop` pull request. It must stop for explicit approval after the local preview and before any remote push, tag, asset upload, publication, or pull request creation.

## Coding style and testing

Use `.agents/skills/ramap-coding-conventions/SKILL.md` for coding style, naming, test placement, security, and validation. Use `.agents/skills/ramap-design-patterns/SKILL.md` for the established implementation patterns.

## Modification records

For every code, database, deployment, or documentation modification, follow the change-record requirement owned by `.agents/skills/ramap-coding-conventions/SKILL.md` under `Finish the change`. Keep the continuing local history in `docs/change-history/CHANGELOG.md`.

## User-visible UI content

- Do not add, remove, or rewrite user-visible UI text unless the user explicitly requests that text change. When a visual request leaves copy ambiguous, preserve existing text and ask before introducing new labels.

## Commit & Pull Request Guidelines

Commit messages follow `<type>: <subject>` and are normally written in Korean. Common types include `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `design`, `rename`, and `remove`.

PR titles should summarize the work in Korean without `feat:` or `fix:` prefixes. Fill out `.github/PULL_REQUEST_TEMPLATE.md`, including overview, change list, test results, related issues, and screenshots or recordings for UI changes.

Follow the `Small PRs` section in `.agents/skills/pr/SKILL.md` for PR scope, commit grouping, branch dependencies, and merge-order reporting.

## Security & Configuration Tips

Supabase values are injected through BuildKonfig from `SUPABASE_URL` and `SUPABASE_ANON_KEY`, or from `local.properties` keys `supabase.url` and `supabase.anon_key`. Do not commit secrets or machine-specific local configuration.
Supabase credentials are already configured in the ignored `local.properties`; do not ask for or print the anon key again unless an actual command fails.
