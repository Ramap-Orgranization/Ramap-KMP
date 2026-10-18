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

## Small PRs

Make small PRs the default. Each PR should have one purpose that can be reviewed and reverted as a unit.

- Group changes by responsibility or behavior: API/schema, domain contracts, data integration, shared UI, individual screens, a focused bug fix, or documentation. Keep files required for the same change and its existing regression tests together; choose boundaries by cohesion and dependencies.
- Before committing or publishing, summarize each group's purpose, files, head/base branches, dependencies, and requested checks. Use atomic commits that match those groups.
- Start independent PRs from the target branch. When a PR depends on another, use the prerequisite branch as its base and include only the next group's changes in its diff.
- Keep every intermediate branch compilable. Use authorized checks against that PR's actual commit; adjust the split when a dependency breaks compilation. Record the commit and check scope, and distinguish source compilation from test execution and earlier integrated results.
- Link the prerequisite PR and state the merge order in each dependent PR body. Use `Refs #<issue>` for partial work and `Fixes #<issue>` in the PR that completes the issue.
- For a stack with policy/domain changes, consolidate those wiki updates once in the last relevant documentation PR and link it from the code PRs. Implementation-only stacks need no wiki update or source-ref refresh.
- Report and attach every PR, with its purpose and merge order.

## Workflow

1. Read `AGENTS.md`, `.codex/agents/git-pr.toml`, and `.github/PULL_REQUEST_TEMPLATE.md`. The standalone TOML includes the runtime settings and role-specific instructions.
2. Confirm the current branch and target branch, defaulting to `develop` when no target is provided.
3. Inspect the final diff and local change records, and define the groups under `Small PRs`. Under `docs/wiki/maintenance.md`, assess whether approved policy, domain meaning, user-visible behavior, an important decision reason, or a knowledge error changed. If so, update only the relevant wiki knowledge and run `python3 scripts/check_wiki.py`. Otherwise leave the wiki unchanged and report why there is no semantic impact. Do not copy code inventories, test/deployment reports, private logs, or secrets into the wiki; do not refresh source refs merely because commits changed.
4. Ensure the intended code and wiki changes are committed before creating the PR. Report uncommitted files explicitly; they are not included in the PR. Preserve unrelated worktree changes and do not create a commit without authorization. If the wiki consolidation creates uncommitted files, resolve their inclusion through the authorized commit workflow before publishing the PR.
5. Run or report authorized checks for each PR's actual commit. Distinguish checks that passed, failed, or were not run; do not turn an earlier result for different code into a current validation result.
6. For each group, check for an existing PR with the same head and base. Reuse it for the requested work instead of creating a duplicate. Push the branch when needed without force-pushing or including unrelated commits.
7. Create each PR with a Korean title and a body based on `.github/PULL_REQUEST_TEMPLATE.md`. Describe the concrete problem, resulting behavior, changes, test results, related issue, dependencies and merge order, and screenshots or recordings for UI changes. Fill out the wiki impact section with changed policy/domain rules, pages, decision IDs when needed, and the wiki check result, or a concrete reason for no semantic impact (mark a local wiki check not run when no edit was needed). If media is unavailable, report that gap. For multiline bodies, prefer a structured tool argument or `gh pr create --body-file` with an exact temporary file.
8. Verify every resulting PR's URL, head/base branches, title, body, and changed files against its planned group. In Codex, attach every created PR with `mcp__codex_app__attach_artifact`; also attach an existing PR when the user asks to continue working on it. Report the URLs, merge order, and validation gaps. Do not merge unless the user requests a merge.

Do not use commit-style prefixes in the PR title. Include `Fixes #<issue>` when applicable.
