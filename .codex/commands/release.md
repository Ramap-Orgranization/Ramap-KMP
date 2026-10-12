# /release

Prepare and publish a Ramap Android release from the `release` branch.

## Usage

```text
/release
/release <version>
/release --dry-run
```

Examples:

```text
/release
/release 1.1.0
/release --dry-run
```

## Instructions

Use the `release` skill and the dedicated `release-manager` subagent.

The command must:

1. Read `AGENTS.md`, `.agents/skills/release/SKILL.md`, and `.codex/agents/sol/release-manager.md`.
2. Require a clean worktree and synchronized `release` branch.
3. Default to the next patch version and increment Android `versionCode` by one.
4. Stop as a no-op when there are no commits after the latest stable tag unless the user explicitly requests a version-only release.
5. Merge `origin/develop` into `release`, update the Android version, run Android host tests, and build the signed release AAB.
6. Verify the AAB signature, size, and SHA-256.
7. Draft Korean user-facing patch notes from changes since the previous stable tag.
8. Include the current Supabase Android minimum build number in the preview and ask whether to update it to the new `versionCode`; update it only after store availability and explicit approval.
9. Show one complete release preview and obtain explicit approval before any remote push, tag, upload, or publication.
10. After approval, push `release`, create an annotated `vX.Y.Z` tag, attach `androidApp-release.aab` to a draft, verify it, publish it as `Latest`, and verify the public release and asset digest.

Never commit secrets, keystores, `local.properties`, generated build outputs, or unrelated changes. Stop on conflicts, failed tests, missing signing configuration, divergent remote state, or mismatched existing tags.
