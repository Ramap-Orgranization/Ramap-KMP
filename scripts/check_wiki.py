#!/usr/bin/env python3
"""Validate the tracked LLM wiki without third-party packages (Python 3.10+)."""

from __future__ import annotations

import argparse
from collections import Counter
from datetime import date
import html
import json
from pathlib import Path
import re
import subprocess
import unicodedata
from urllib.parse import unquote, urlsplit


REQUIRED_PAGES = (
    "README.md",
    "maintenance.md",
    "decisions.md",
    "product-policy.md",
    "domain.md",
    "inbox/README.md",
    "templates/knowledge-candidate.md",
)


def without_fences(text: str) -> str:
    """Preserve line numbers while omitting backtick and tilde code fences."""
    output = []
    fence = None
    for line in text.splitlines():
        match = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
        if fence is None and match:
            fence = match.group(1)
            output.append("")
        elif fence is not None:
            if match and match.group(1)[0] == fence[0] and len(match.group(1)) >= len(fence) and not match.group(2).strip():
                fence = None
            output.append("")
        else:
            output.append(line)
    return "\n".join(output)


def frontmatter(text: str, label: str, errors: list[str]) -> tuple[dict, str]:
    lines = text.splitlines()
    if not lines or lines[0] != "---":
        errors.append(f"{label}: missing frontmatter")
        return {}, text
    try:
        end = lines.index("---", 1)
    except ValueError:
        errors.append(f"{label}: unterminated frontmatter")
        return {}, ""
    metadata = {}
    for number, line in enumerate(lines[1:end], 2):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        match = re.fullmatch(r"([a-z][a-z0-9_]*):[ \t]*(.*)", line)
        if not match:
            errors.append(f"{label}:{number}: expected a scalar key: value")
            continue
        key, value = match.groups()
        if key in metadata:
            errors.append(f"{label}:{number}: duplicate metadata key {key}")
            continue
        try:
            if value.startswith('"'):
                value = json.loads(value)
            elif value.startswith("'"):
                if not re.fullmatch(r"'(?:[^']|'')*'", value):
                    raise ValueError("invalid single-quoted scalar")
                value = value[1:-1].replace("''", "'")
            else:
                value = re.split(r"\s+#", value, maxsplit=1)[0].strip()
                if value and (value[0] in "[{|>&*!" or ": " in value):
                    raise ValueError("unsupported non-scalar YAML")
                if value in ("", "null", "~"):
                    value = None
            metadata[key] = value
        except (ValueError, TypeError):
            errors.append(f"{label}:{number}: invalid scalar for {key}")
    return metadata, "\n" * (end + 1) + "\n".join(lines[end + 1:])


def valid_date(metadata: dict, key: str, label: str, errors: list[str]) -> date | None:
    value = metadata.get(key)
    try:
        if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
            raise ValueError
        return date.fromisoformat(value)
    except ValueError:
        errors.append(f"{label}: {key} must be an ISO date (YYYY-MM-DD)")
        return None


def heading_ids(text: str) -> set[str]:
    headings = []
    lines = without_fences(text).splitlines()
    for index, line in enumerate(lines):
        match = re.match(r"^ {0,3}#{1,6}[ \t]+(.+?)(?:[ \t]+#+)?[ \t]*$", line)
        if match:
            headings.append(match.group(1))
        elif index and lines[index - 1].strip() and re.fullmatch(r" {0,3}(?:=+|-+)[ \t]*", line):
            headings.append(lines[index - 1].strip())
    anchors = set()
    for heading in headings:
        heading = re.sub(r"!?\[([^]]*)\]\([^)]*\)", r"\1", heading)
        heading = re.sub(r"<[^>]*>", "", heading)
        heading = re.sub(r"(`+|\*+|~~)(.*?)\1", r"\2", heading)
        heading = re.sub(r"(?<!\w)(_+)(.*?)\1(?!\w)", r"\2", heading)
        heading = html.unescape(heading).lower()
        slug = "".join(
            character for character in heading
            if character in " -_" or unicodedata.category(character)[0] in "LNM"
        ).replace(" ", "-")
        anchor = slug
        suffix = 0
        while anchor in anchors:
            suffix += 1
            anchor = f"{slug}-{suffix}"
        anchors.add(anchor)
    return anchors


def inline_links(text: str):
    """Read inline destinations, including angle brackets and balanced parentheses."""
    text = without_fences(text)
    text = re.sub(r"(`+)(.*?)\1", lambda match: " " * len(match.group()), text)
    for match in re.finditer(r"(?<!\\)\[[^\]\n]*\]\(", text):
        start = match.end()
        while start < len(text) and text[start].isspace():
            start += 1
        if start >= len(text):
            continue
        if text[start] == "<":
            end = text.find(">", start + 1)
            if end != -1:
                yield text[start + 1:end], text.count("\n", 0, match.start()) + 1
            continue
        depth = 0
        end = start
        while end < len(text):
            character = text[end]
            if character == "\\" and end + 1 < len(text):
                end += 2
                continue
            if character == "(":
                depth += 1
            elif character == ")":
                if depth == 0:
                    break
                depth -= 1
            elif character.isspace() and depth == 0:
                break
            end += 1
        yield re.sub(r"\\([() ])", r"\1", text[start:end]), text.count("\n", 0, match.start()) + 1


def contained_path(root: Path, base: Path, value: str) -> Path | None:
    path = (base / value).resolve()
    return path if path.is_relative_to(root) else None


def validate_candidate(path: Path, metadata: dict, pages: dict, root: Path, decisions: set[str], errors: list[str]):
    label = str(path.relative_to(root))
    for key in ("type", "authority", "status", "created_at", "updated_at", "target_path", "decision_id", "resolution_reason"):
        if key not in metadata:
            errors.append(f"{label}: missing {key}")
    if metadata.get("type") != "knowledge-candidate" or metadata.get("authority") != "none":
        errors.append(f"{label}: candidates require type knowledge-candidate and authority none")
    created = valid_date(metadata, "created_at", label, errors)
    updated = valid_date(metadata, "updated_at", label, errors)
    if created and updated and created > updated:
        errors.append(f"{label}: updated_at precedes created_at")
    status = metadata.get("status")
    target, decision, reason = (metadata.get(key) for key in ("target_path", "decision_id", "resolution_reason"))
    if status not in ("candidate", "promoted", "rejected"):
        errors.append(f"{label}: invalid candidate status {status!r}")
        return
    if status == "candidate":
        if any(value is not None for value in (target, decision, reason)):
            errors.append(f"{label}: pending candidates require null target_path, decision_id, and resolution_reason")
        return
    if not isinstance(reason, str) or not reason.strip():
        errors.append(f"{label}: {status} candidates require resolution_reason")
    if status == "rejected":
        if target is not None or decision is not None:
            errors.append(f"{label}: rejected candidates require null target_path and decision_id")
        return
    if not isinstance(decision, str) or not re.fullmatch(r"DEC-\d{4}", decision) or decision not in decisions:
        errors.append(f"{label}: promoted decision_id must exist in decisions.md")
    target_file = None
    if isinstance(target, str) and target and not Path(target).is_absolute():
        target_file = contained_path(root, root, target)
    target_metadata = pages.get(target_file, ({}, ""))[0]
    wiki = root / "docs/wiki"
    if (
        target_file is None
        or not target_file.is_relative_to(wiki)
        or target_file.is_relative_to(wiki / "inbox")
        or target_file.is_relative_to(wiki / "templates")
        or target_metadata.get("status") != "active"
        or target_metadata.get("authority") not in ("canonical", "reference")
    ):
        errors.append(f"{label}: promoted target_path must name an active wiki page outside inbox/templates")


def ignored_paths(root: Path, paths: set[Path]) -> set[str]:
    if not paths or not (root / ".git").exists():
        return set()
    relative_paths = sorted(str(path.relative_to(root)) for path in paths)
    try:
        result = subprocess.run(
            ["git", "-C", str(root), "check-ignore", "--stdin", "-z"],
            input="\0".join(relative_paths) + "\0", text=True, capture_output=True, check=False, timeout=10,
        )
    except FileNotFoundError:
        return set()
    if result.returncode not in (0, 1):
        raise RuntimeError(f"git check-ignore failed: {result.stderr.strip()}")
    return set(result.stdout.rstrip("\0").split("\0")) if result.stdout else set()


def validate(root: Path | str) -> list[str]:
    root = Path(root).resolve()
    wiki = root / "docs/wiki"
    errors = []
    for required in REQUIRED_PAGES:
        if not (wiki / required).is_file():
            errors.append(f"docs/wiki/{required}: required page is missing")
    pages = {}
    for path in sorted(wiki.rglob("*.md")):
        if not path.resolve().is_relative_to(root):
            errors.append(f"{path.relative_to(root)}: page resolves outside repository")
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as error:
            errors.append(f"{path.relative_to(root)}: cannot read page: {error}")
            continue
        pages[path.resolve()] = frontmatter(text, str(path.relative_to(root)), errors)
    decisions_text = pages.get(wiki / "decisions.md", ({}, ""))[1]
    decision_ids = re.findall(r"^##[ \t]+(DEC-\d{4})(?=\s|$)", without_fences(decisions_text), re.MULTILINE)
    for decision, count in Counter(decision_ids).items():
        if count > 1:
            errors.append(f"docs/wiki/decisions.md: duplicate decision ID {decision}")
    for path, (metadata, _) in pages.items():
        label = str(path.relative_to(root))
        if path.is_relative_to(wiki / "templates"):
            if metadata.get("authority") != "none" or metadata.get("status") != "template":
                errors.append(f"{label}: templates require authority none and status template")
        elif path.is_relative_to(wiki / "inbox") and path.name != "README.md":
            validate_candidate(path, metadata, pages, root, set(decision_ids), errors)
        else:
            if metadata.get("authority") not in ("canonical", "reference") or metadata.get("status") != "active":
                errors.append(f"{label}: active pages require authority canonical/reference and status active")
            valid_date(metadata, "last_verified", label, errors)
            if "source_ref" in metadata and not str(metadata.get("source_ref") or "").strip():
                errors.append(f"{label}: optional source_ref must be nonempty when provided")
    documents = {path: body for path, (_, body) in pages.items()}
    for relative in ("README.md", ".github/PULL_REQUEST_TEMPLATE.md"):
        path = root / relative
        if path.is_file():
            documents[path] = path.read_text(encoding="utf-8")
    links_to_check = []
    anchor_cache = {}
    for source, body in documents.items():
        for destination, line in inline_links(body):
            label = f"{source.relative_to(root)}:{line}"
            try:
                parsed = urlsplit(destination)
            except ValueError:
                errors.append(f"{label}: invalid link {destination!r}")
                continue
            if parsed.scheme in ("http", "https", "mailto") or parsed.netloc:
                continue
            if parsed.scheme:
                errors.append(f"{label}: unsupported local link scheme in {destination!r}")
                continue
            target = contained_path(root, source.parent, unquote(parsed.path)) if parsed.path else source
            if target is None:
                errors.append(f"{label}: link escapes repository: {destination}")
                continue
            if not target.exists():
                errors.append(f"{label}: broken local link: {destination}")
                continue
            links_to_check.append((target, label, destination))
            if parsed.fragment and target.is_file() and target.suffix.lower() == ".md":
                if target not in anchor_cache:
                    target_text = documents.get(target)
                    if target_text is None:
                        target_text = target.read_text(encoding="utf-8")
                        if target_text.startswith("---\n"):
                            target_text = frontmatter(target_text, str(target.relative_to(root)), errors)[1]
                    anchor_cache[target] = heading_ids(target_text)
                if unquote(parsed.fragment) not in anchor_cache[target]:
                    errors.append(f"{label}: missing Markdown anchor: {destination}")
    try:
        ignored = ignored_paths(root, {target for target, _, _ in links_to_check})
        for target, label, destination in links_to_check:
            if str(target.relative_to(root)) in ignored:
                errors.append(f"{label}: link targets a git-ignored/local-only file: {destination}")
    except (RuntimeError, subprocess.TimeoutExpired) as error:
        errors.append(str(error))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    arguments = parser.parse_args()
    errors = validate(arguments.root)
    if errors:
        print("Wiki validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print("Wiki validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
