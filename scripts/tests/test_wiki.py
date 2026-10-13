"""Regression checks for wiki publishing and knowledge promotion guardrails."""

import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest


SPEC = importlib.util.spec_from_file_location("check_wiki", Path(__file__).resolve().parents[1] / "check_wiki.py")
CHECK_WIKI = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECK_WIKI)

ACTIVE = "---\nauthority: canonical\nstatus: active\nlast_verified: 2026-10-11\n---\n"
CANDIDATE = """---
type: knowledge-candidate
authority: none
status: candidate
created_at: 2026-10-10
updated_at: 2026-10-11
target_path: null
decision_id: null
resolution_reason: null
---
# Candidate
"""


class WikiValidationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        for name in ("README.md", "maintenance.md", "decisions.md", "product-policy.md", "domain.md", "inbox/README.md"):
            self.write(f"docs/wiki/{name}", ACTIVE + "# 문서\n")
        self.write("docs/wiki/decisions.md", ACTIVE + "# Decisions\n\n## DEC-0001 Initial wiki\n")
        self.write("docs/wiki/templates/knowledge-candidate.md", "---\nauthority: none\nstatus: template\ncreated_at: YYYY-MM-DD\n---\n# Template\n")

    def write(self, relative, text):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def errors(self):
        return "\n".join(CHECK_WIKI.validate(self.root))

    def test_valid_wiki_and_pending_candidate(self):
        self.write("docs/wiki/inbox/example.md", CANDIDATE)
        self.assertEqual([], CHECK_WIKI.validate(self.root))

    def test_missing_required_page(self):
        (self.root / "docs/wiki/maintenance.md").unlink()
        self.assertIn("required page is missing", self.errors())

    def test_malformed_metadata_is_rejected(self):
        for replacement, expected in (
            ("authority: [canonical]", "invalid scalar"),
            ("authority: canonical\nstatus: rejected", "duplicate metadata key status"),
            ("authority canonical", "expected a scalar"),
        ):
            with self.subTest(replacement=replacement):
                self.write("docs/wiki/README.md", ACTIVE.replace("authority: canonical", replacement))
                self.assertIn(expected, self.errors())
        self.write("docs/wiki/README.md", ACTIVE.replace("2026-10-11", "2026-02-30"))
        self.assertIn("must be an ISO date", self.errors())

    def test_domain_reference_does_not_require_a_code_commit(self):
        self.write("docs/wiki/domain.md", ACTIVE.replace("authority: canonical", "authority: reference") + "# 도메인 용어\n")
        self.assertEqual([], CHECK_WIKI.validate(self.root))

    def test_optional_source_ref_is_validated_only_when_supplied(self):
        reference = ACTIVE.replace("authority: canonical", "authority: reference")
        self.write("docs/wiki/domain.md", reference.replace("status: active", "status: active\nsource_ref: abc123"))
        self.assertEqual([], CHECK_WIKI.validate(self.root))
        self.write("docs/wiki/domain.md", reference.replace("status: active", "status: active\nsource_ref: null"))
        self.assertIn("optional source_ref must be nonempty", self.errors())

    def test_broken_links_and_anchors_are_reported_in_entrypoints(self):
        self.write("README.md", "[wiki](docs/wiki/README.md#missing)\n")
        self.write(".github/PULL_REQUEST_TEMPLATE.md", "[gone](../docs/wiki/gone.md)\n")
        errors = self.errors()
        self.assertIn("README.md:1: missing Markdown anchor", errors)
        self.assertIn("PULL_REQUEST_TEMPLATE.md:1: broken local link", errors)

    def test_korean_duplicate_headings_and_encoded_paths(self):
        self.write("docs/wiki/한글 페이지.md", ACTIVE + "# 사용 방법\n\n## 사용 방법\n\n## `코드`와 **강조**\n")
        self.write("README.md", "[첫째](docs/wiki/%ED%95%9C%EA%B8%80%20%ED%8E%98%EC%9D%B4%EC%A7%80.md#사용-방법)\n[둘째](<docs/wiki/한글 페이지.md#사용-방법-1>)\n[서식](<docs/wiki/한글 페이지.md#코드와-강조>)\n")
        self.assertEqual([], CHECK_WIKI.validate(self.root))

    def test_fenced_examples_do_not_create_links_or_decisions(self):
        self.write("docs/wiki/README.md", ACTIVE + "# Intro\n\n```markdown\n[example](missing.md)\n# Invisible\n```\n~~~markdown\n[example](another.md)\n~~~\n`[inline example](missing.md)`\n")
        with (self.root / "docs/wiki/decisions.md").open("a") as file:
            file.write("\n```markdown\n## DEC-0001 example\n```\n")
        self.assertEqual([], CHECK_WIKI.validate(self.root))
        self.write("README.md", "[hidden](docs/wiki/README.md#invisible)\n")
        self.assertIn("missing Markdown anchor", self.errors())

    def test_duplicate_decision_ids_are_rejected(self):
        self.write("docs/wiki/decisions.md", ACTIVE + "## DEC-0001 First\n\n## DEC-0001 Second\n")
        self.assertIn("duplicate decision ID DEC-0001", self.errors())

    def test_invalid_candidate_transitions(self):
        for changes, expected in (
            ({"target_path: null": "target_path: docs/wiki/README.md"}, "pending candidates require null"),
            ({"status: candidate": "status: rejected"}, "rejected candidates require resolution_reason"),
            ({"status: candidate": "status: rejected", "resolution_reason: null": "resolution_reason: duplicate", "decision_id: null": "decision_id: DEC-0001"}, "rejected candidates require null"),
            ({"updated_at: 2026-10-11": "updated_at: 2026-10-09"}, "updated_at precedes created_at"),
            ({"status: candidate": "status: promoted"}, "promoted decision_id must exist"),
        ):
            with self.subTest(changes=changes):
                candidate = CANDIDATE
                for old, new in changes.items():
                    candidate = candidate.replace(old, new)
                self.write("docs/wiki/inbox/example.md", candidate)
                self.assertIn(expected, self.errors())

    def test_promoted_candidate_requires_active_target_and_existing_decision(self):
        promoted = CANDIDATE.replace("status: candidate", "status: promoted").replace("target_path: null", "target_path: docs/wiki/README.md").replace("decision_id: null", "decision_id: DEC-0001").replace("resolution_reason: null", "resolution_reason: Reviewed and merged")
        self.write("docs/wiki/inbox/example.md", promoted)
        self.assertEqual([], CHECK_WIKI.validate(self.root))
        for target in ("docs/wiki/inbox/README.md", "docs/wiki/templates/knowledge-candidate.md", "docs/wiki/missing.md", "../outside.md"):
            with self.subTest(target=target):
                self.write("docs/wiki/inbox/example.md", promoted.replace("target_path: docs/wiki/README.md", f"target_path: {target}"))
                self.assertIn("active wiki page outside inbox/templates", self.errors())
        self.write("docs/wiki/inbox/example.md", promoted.replace("DEC-0001", "DEC-9999"))
        self.assertIn("promoted decision_id must exist", self.errors())

    def test_links_cannot_escape_repository_even_through_symlinks(self):
        self.write("docs/wiki/README.md", ACTIVE + "[escape](../../../outside.md)\n[encoded](%2e%2e/%2e%2e/%2e%2e/outside.md)\n")
        self.assertEqual(2, self.errors().count("link escapes repository"))
        with tempfile.TemporaryDirectory() as outside:
            external = Path(outside) / "external.md"
            external.write_text("# External\n")
            (self.root / "linked.md").symlink_to(external)
            self.write("docs/wiki/README.md", ACTIVE + "[symlink](../../linked.md)\n")
            self.assertIn("link escapes repository", self.errors())

    def test_git_ignored_local_files_cannot_be_wiki_dependencies(self):
        subprocess.run(["git", "init", "--quiet", str(self.root)], check=True, capture_output=True)
        self.write(".gitignore", "private.md\n")
        self.write("private.md", "# Local only\n")
        self.write("docs/wiki/README.md", ACTIVE + "[local](../../private.md)\n")
        self.assertIn("git-ignored/local-only file", self.errors())


if __name__ == "__main__":
    unittest.main()
