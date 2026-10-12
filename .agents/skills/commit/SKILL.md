---
name: commit
description: Create one or more atomic commits following this repository's Korean commit convention. Use when the user invokes `/commit` or asks to commit changes.
---

# Commit

Create atomic commits for the changes the user authorized in this repository.

## Agent routing

For a commit request, the primary agent follows `.codex/commands/commit.md` and delegates to the named role `luna_git_commit`. Read `.codex/agents/luna/luna-git-commit.toml` for its model and reasoning effort, and `.codex/agents/luna/git-commit.md` for its responsibilities. If the named role is unavailable, spawn an agent with the runtime settings from that TOML; a task label does not select the role.

The assigned LUNA agent executes the workflow below without delegating it again. When the release manager uses this skill as part of its own release workflow, it applies the commit rules directly and retains ownership of that workflow.

## Usage

```text
/commit
/commit <optional commit intent>
```

## Workflow

1. Read `AGENTS.md`, `.codex/agents/luna/luna-git-commit.toml`, and `.codex/agents/luna/git-commit.md`. The TOML selects the runtime model and reasoning effort; the Markdown file supplies role-specific instructions.
2. Inspect `git status --short` and focused diffs.
3. Split changes into the smallest practical atomic commits before staging.
   - Do not combine build/dependency changes, file moves, base architecture, feature behavior, tests, and formatting in one commit unless they are inseparable.
   - Prefer multiple small commits over one broad commit when a later revert or review would naturally target only part of the change.
   - If a staged commit touches many areas, stop and restate the intended commit groups before committing.
4. Exclude unrelated files, generated files, local config, and crash logs unless explicitly requested. Never commit secrets. Inspect existing staged changes and preserve anything outside the authorized commit scope; do not unstage or include it automatically.
5. Run only the checks explicitly requested by the user and report the results. If a requested check fails, report the command and failure before committing; continue only when the user has authorized committing despite that failure.
6. Stage only the intended files for the current atomic commit.
7. Commit with this format:

```text
<type>: <subject>
```

Use Korean for the subject. Allowed types are `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `design`, `rename`, and `remove`.

After each commit, verify its files and message. Report the commit hashes, subjects, requested check results, and remaining uncommitted changes. A commit request does not include push or PR creation.
