# /release

Prepare a paired Ramap Android and iOS release from `release`, submit both builds for store review, and prepare the follow-up PR. Public store release stays manual.

## Usage

```text
/release
/release <version>
/release [<version>] --play-internal
/release --dry-run
```

Examples:

```text
/release
/release 1.1.0
/release --dry-run
```

## Instructions

Use the `release` skill and the dedicated `release_manager` subagent.

The command must:

1. Read `AGENTS.md`, `.agents/skills/release/SKILL.md`, and `.codex/agents/release-manager.toml`.
2. Require a clean worktree and synchronized `release` branch.
3. By default, release Android and iOS together with the same `versionName` / `MARKETING_VERSION`; increment Android `versionCode` by one and choose an iOS build number above every build already received by App Store Connect. If the user explicitly requests only one platform, restrict the scope to that platform.
4. Stop as a no-op when there are no commits after the latest stable tag unless the user explicitly requests a version-only release.
5. Merge `origin/develop` into `release`. When Android is in scope, update its version, run Android host tests, and build the signed release AAB. When iOS is in scope, also create a signed iOS Archive and IPA from the same release HEAD without changing the iOS project version files.
6. For each selected platform, verify the artifact signature and metadata, size, and SHA-256.
7. Draft Korean user-facing release notes from changes since the previous stable tag for GitHub Release, Play, and App Store Connect.
8. When Android is in scope, include the current Supabase minimum build number and proposed `versionCode`; ask whether to update it. Update only after store availability and explicit approval.
9. Show one complete preview covering every selected platform and obtain explicit approval before remote push, tag, upload, publication, App Store metadata changes, review submission, or PR creation.
10. After approval, push `release` when the prepared source or version commit requires it. When Android is in scope, create the annotated tag, attach the approved AAB to a GitHub draft, verify it, publish it as `Latest`, and verify the public release and asset digest.
11. By default, follow `.agents/skills/release/references/store-review-deployment.md` and submit the approved AAB to Play production after public GitHub Release verification. When iOS is in scope, upload the approved IPA, wait for Apple processing, associate the approved build, set the approved Korean app name and notes, submit for App Store review, and verify the actual review lifecycle state. Keep Play Managed publishing ON and App Store release MANUAL. `--play-internal` changes only the Android destination; the default paired scope still includes iOS App Store review. Read `play-internal-deployment.md` and dispatch that workflow with the exact approved tag, source SHA, versionCode, and AAB SHA-256. Never rebuild an approved artifact.
12. Report GitHub publication, Play result, App Store result, and the follow-up PR separately. If one platform fails, retry only its failed stage with the same approved artifact; do not roll back a successful platform. Internal testing does not authorize a forced-update policy change.

Never commit secrets, keystores, `local.properties`, generated build outputs, or unrelated changes. Stop on conflicts, failed tests, missing signing configuration, divergent remote state, or mismatched existing tags.
