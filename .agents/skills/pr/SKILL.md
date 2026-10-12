---
name: pr
description: Create a pull request following this repository's Korean PR convention. Use when the user invokes `/pr` or asks to open a PR.
---

# Pull Request

Create a pull request for this repository.

## Agent routing

For a PR request, the primary agent follows `.codex/commands/pr.md` and delegates to the named role `git_pr`. Use `.codex/agents/git-pr.toml` for its runtime settings and `developer_instructions`. If the named role is unavailable, spawn an agent with the TOML's runtime settings.

The assigned agent executes the workflow below without delegating it again. The release manager applies these PR rules directly when creating its own post-release PR, preserving its ownership of the release workflow.

## Usage

```text
/pr
/pr <target branch>
```

## Workflow

1. Read `AGENTS.md`, `.codex/agents/git-pr.toml`, and `.github/PULL_REQUEST_TEMPLATE.md`. The standalone TOML includes the runtime settings and role-specific instructions.
2. Confirm the current branch and target branch, defaulting to `develop` when no target is provided.
3. Inspect the final diff against the target branch and the task's local change records. At this final PR stage, follow `docs/wiki/maintenance.md` to consolidate verified facts, confirmed policy decisions, and unresolved candidates into the relevant wiki pages. Do not copy private logs, secrets, or unrelated task records into the wiki. Update existing pages before adding new ones and run `python3 scripts/check_wiki.py`. Wiki changes are not required at each implementation or validation step.
4. Ensure the intended code and wiki changes are committed before creating the PR. Report uncommitted files explicitly; they are not included in the PR. Preserve unrelated worktree changes and do not create a commit without authorization. If the wiki consolidation creates uncommitted files, resolve their inclusion through the authorized commit workflow before publishing the PR.
5. Run or report relevant checks. Distinguish checks that passed, failed, or were not run; do not turn an earlier result for different code into a current validation result.
6. Check for an existing PR with the same head and base. Reuse it for the requested work instead of creating a duplicate. Push the branch when needed without force-pushing or including unrelated commits.
7. Create the PR with a Korean title and a body based on `.github/PULL_REQUEST_TEMPLATE.md`. Describe the concrete problem, resulting behavior, changes, test results, related issue, and screenshots or recordings for UI changes. Fill out the wiki impact section with the consolidated pages, policy decision IDs when needed, and wiki check result, or a concrete reason for no wiki impact. If media is unavailable, report that gap. For multiline bodies, prefer a structured tool argument or `gh pr create --body-file` with an exact temporary file.
8. Verify the resulting PR URL, head/base branches, title, and body. In Codex, attach every created PR with `mcp__codex_app__attach_artifact`; also attach an existing PR when the user asks to continue working on it. Report its URL and validation gaps. Do not merge it unless the user requests a merge.

Do not use commit-style prefixes in the PR title. Include `Fixes #<issue>` when applicable.
