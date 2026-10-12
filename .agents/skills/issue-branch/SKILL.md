---
name: issue-branch
description: Create a GitHub issue and local branch from a short Korean work description. Use when the user invokes `/issue-branch` or asks to create an issue branch.
---

# Issue Branch

Create a GitHub issue and switch to a matching local branch for this repository.

## Agent routing

The primary agent follows `.codex/commands/issue-branch.md` and delegates to the named role `luna_git_issue_branch`. Use `.codex/agents/luna/luna-git-issue-branch.toml` as the source for its model and reasoning effort and `.codex/agents/luna/git-issue-branch.md` for its responsibilities. If the named role is unavailable, spawn an agent with the TOML's runtime settings.

The assigned LUNA agent executes the workflow below without delegating it again. An approved `source-command-impl` plan that includes issue and branch creation supplies the work description and authorization for this step.

## Usage

```text
/issue-branch <short description>
/issue-branch <type> <short description>
```

Examples:

```text
/issue-branch 카카오 맵 연동
/issue-branch feat 카카오 맵 연동
```

## Workflow

1. Read `AGENTS.md`, `.codex/agents/luna/luna-git-issue-branch.toml`, `.codex/agents/luna/git-issue-branch.md`, and the relevant issue template in `.github/ISSUE_TEMPLATE`. The TOML selects the runtime model and reasoning effort; the Markdown file supplies role-specific instructions.
2. Inspect the current branch and worktree with:

```bash
git branch --show-current
git status --short
```

3. Treat the first token as a work type only when it is one of:
   `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `design`, `rename`, or `remove`.
4. If no type is provided, infer the conservative type from the description. For new functionality, default to `feat`.
5. Create the GitHub issue in Korean with `gh issue create`, using the matching template when practical. For a multiline body, write the exact text to a temporary file and pass `--body-file`; preserve real newlines.
6. Create and switch to a local branch from the current branch:

```text
<type>/<issue-number>-<short-kebab-summary>
```

7. Do not overwrite, revert, stage, or commit unrelated local changes.
8. Report the issue URL, new branch name, and previous base branch.

If issue creation succeeds but branch creation fails, report the created issue and the unchanged branch state. Resume from that issue after resolving the failure rather than creating another issue. Do not stash, reset, or discard local changes to force a branch switch.

## Branch Examples

- `feat/23-kakao-map`
- `fix/24-supabase-config`
- `docs/25-update-conventions`
