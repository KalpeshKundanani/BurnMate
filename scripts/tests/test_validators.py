#!/usr/bin/env python3
"""
test_validators.py — Unit tests for framework validation scripts.

Uses only stdlib unittest. No external dependencies.
Tests use fixture directories under scripts/tests/fixtures/.

Run:
  python3 -m pytest scripts/tests/test_validators.py -v
  OR
  python3 -m unittest scripts.tests.test_validators -v
  OR
  python3 scripts/tests/test_validators.py
"""

import os
import re
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

# Add scripts/ to path so we can import validators
SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))

FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures"

# ---------------------------------------------------------------------------
# Import validator modules
# ---------------------------------------------------------------------------
import validate_doc_freeze
import validate_required_artifacts
import validate_slice_registry
import validate_pr_checklist


class TestRequiredArtifacts(unittest.TestCase):
    """Tests for validate_required_artifacts.py"""

    def test_valid_slice_passes(self):
        """A slice at LLD_DEFINED with prd+hld+lld+state should pass."""
        fixture = FIXTURES_DIR / "valid_slice"
        with patch.object(validate_required_artifacts, "SLICES_DIR", fixture.parent):
            with patch.object(validate_required_artifacts, "discover_slice_dirs", return_value=[fixture]):
                violations = validate_required_artifacts.validate()
        self.assertEqual(violations, [], f"Expected no violations, got: {violations}")

    def test_missing_artifact_detected(self):
        """A slice at HLD_DEFINED missing hld.md should fail."""
        fixture = FIXTURES_DIR / "missing_artifacts_slice"
        with patch.object(validate_required_artifacts, "SLICES_DIR", fixture.parent):
            with patch.object(validate_required_artifacts, "discover_slice_dirs", return_value=[fixture]):
                violations = validate_required_artifacts.validate()
        self.assertTrue(len(violations) > 0, "Expected at least one violation for missing hld.md")
        self.assertTrue(
            any("hld.md" in v and "missing" in v.lower() for v in violations),
            f"Expected violation about missing hld.md, got: {violations}",
        )

    def test_invalid_state_detected(self):
        """A slice with an unrecognized state should fail."""
        fixture = FIXTURES_DIR / "invalid_state_slice"
        with patch.object(validate_required_artifacts, "SLICES_DIR", fixture.parent):
            with patch.object(validate_required_artifacts, "discover_slice_dirs", return_value=[fixture]):
                violations = validate_required_artifacts.validate()
        self.assertTrue(len(violations) > 0, "Expected violation for invalid state")
        self.assertTrue(
            any("INVALID_STATE" in v for v in violations),
            f"Expected violation mentioning INVALID_STATE, got: {violations}",
        )

    def test_no_slices_passes(self):
        """Empty slices directory should pass with no violations."""
        with tempfile.TemporaryDirectory() as tmpdir:
            with patch.object(validate_required_artifacts, "SLICES_DIR", Path(tmpdir)):
                violations = validate_required_artifacts.validate()
        self.assertEqual(violations, [])

    def test_state_artifact_mapping_complete(self):
        """Every state in the state machine must have an entry in STATE_REQUIRED_ARTIFACTS."""
        expected_states = {
            "NOT_STARTED", "PRD_DEFINED", "HLD_DEFINED", "LLD_DEFINED",
            "CODE_IN_PROGRESS", "CODE_COMPLETE", "REVIEW_REQUIRED",
            "REVIEW_CHANGES", "REVIEW_APPROVED", "QA_REQUIRED", "QA_CHANGES",
            "QA_APPROVED", "AUDIT_REQUIRED", "AUDIT_APPROVED", "MERGED",
        }
        actual_states = set(validate_required_artifacts.STATE_REQUIRED_ARTIFACTS.keys())
        self.assertEqual(expected_states, actual_states)


class TestDocFreeze(unittest.TestCase):
    """Tests for validate_doc_freeze.py"""

    def test_frozen_slice_missing_prd_detected(self):
        """A slice past LLD_DEFINED missing prd.md should be caught."""
        fixture = FIXTURES_DIR / "frozen_violation_slice"
        with patch.object(validate_doc_freeze, "SLICES_DIR", fixture.parent):
            with patch.object(validate_doc_freeze, "discover_slice_dirs", return_value=[fixture]):
                with patch.object(validate_doc_freeze, "git_available", return_value=False):
                    violations = validate_doc_freeze.validate()
        self.assertTrue(len(violations) > 0, "Expected violation for missing prd.md in frozen slice")
        self.assertTrue(
            any("prd.md" in v and "missing" in v.lower() for v in violations),
            f"Expected violation about missing prd.md, got: {violations}",
        )

    def test_valid_frozen_slice_passes(self):
        """A slice at LLD_DEFINED with all frozen docs should pass."""
        fixture = FIXTURES_DIR / "valid_slice"
        with patch.object(validate_doc_freeze, "SLICES_DIR", fixture.parent):
            with patch.object(validate_doc_freeze, "discover_slice_dirs", return_value=[fixture]):
                with patch.object(validate_doc_freeze, "git_available", return_value=False):
                    violations = validate_doc_freeze.validate()
        self.assertEqual(violations, [], f"Expected no violations, got: {violations}")

    def test_pre_freeze_slice_skipped(self):
        """A slice at PRD_DEFINED (pre-freeze) should be skipped entirely."""
        # Create a temporary slice at PRD_DEFINED
        with tempfile.TemporaryDirectory() as tmpdir:
            slice_dir = Path(tmpdir) / "SLICE-0099"
            slice_dir.mkdir()
            (slice_dir / "state.md").write_text(
                "# Slice State\n\n| Field | Value |\n|---|---|\n| **Current State** | `PRD_DEFINED` |"
            )
            (slice_dir / "prd.md").write_text("# PRD: SLICE-0099\nTest")
            with patch.object(validate_doc_freeze, "SLICES_DIR", Path(tmpdir)):
                with patch.object(validate_doc_freeze, "discover_slice_dirs", return_value=[slice_dir]):
                    with patch.object(validate_doc_freeze, "git_available", return_value=False):
                        violations = validate_doc_freeze.validate()
        self.assertEqual(violations, [], "Pre-freeze slice should not trigger violations")

    def test_frozen_states_list_complete(self):
        """FROZEN_STATES should include all states from LLD_DEFINED onward."""
        expected = {
            "LLD_DEFINED", "CODE_IN_PROGRESS", "CODE_COMPLETE",
            "REVIEW_REQUIRED", "REVIEW_CHANGES", "REVIEW_APPROVED",
            "QA_REQUIRED", "QA_CHANGES", "QA_APPROVED",
            "AUDIT_REQUIRED", "AUDIT_APPROVED", "MERGED",
        }
        actual = set(validate_doc_freeze.FROZEN_STATES)
        self.assertEqual(expected, actual)


class TestSliceRegistry(unittest.TestCase):
    """Tests for validate_slice_registry.py"""

    def test_empty_repo_passes(self):
        """No slices on disk, no entries in index → pass."""
        with tempfile.TemporaryDirectory() as tmpdir:
            # Create an empty index
            index = Path(tmpdir) / "index.md"
            index.write_text(
                "# Slice Index\n\n| Slice ID | Name | Current State | Owner Role | Slice Folder | Last Updated |\n"
                "|---|---|---|---|---|---|\n"
            )
            with patch.object(validate_slice_registry, "SLICES_DIR", Path(tmpdir)):
                with patch.object(validate_slice_registry, "INDEX_FILE", index):
                    violations = validate_slice_registry.validate()
        self.assertEqual(violations, [])

    def test_disk_without_index_detected(self):
        """A slice folder on disk not in index.md should fail."""
        with tempfile.TemporaryDirectory() as tmpdir:
            # Create slice dir
            (Path(tmpdir) / "SLICE-0001").mkdir()
            # Create empty index
            index = Path(tmpdir) / "index.md"
            index.write_text(
                "# Slice Index\n\n| Slice ID | Name | Current State | Owner Role | Slice Folder | Last Updated |\n"
                "|---|---|---|---|---|---|\n"
            )
            with patch.object(validate_slice_registry, "SLICES_DIR", Path(tmpdir)):
                with patch.object(validate_slice_registry, "INDEX_FILE", index):
                    violations = validate_slice_registry.validate()
        self.assertTrue(len(violations) > 0)
        self.assertTrue(any("SLICE-0001" in v and "missing from index" in v for v in violations))

    def test_index_without_disk_detected(self):
        """An index entry without a folder on disk should fail."""
        fixture_index = FIXTURES_DIR / "registry_mismatch" / "index.md"
        with tempfile.TemporaryDirectory() as tmpdir:
            # Create only SLICE-0001, not SLICE-9999
            (Path(tmpdir) / "SLICE-0001").mkdir()
            with patch.object(validate_slice_registry, "SLICES_DIR", Path(tmpdir)):
                with patch.object(validate_slice_registry, "INDEX_FILE", fixture_index):
                    violations = validate_slice_registry.validate()
        self.assertTrue(
            any("SLICE-9999" in v and "does not exist" in v for v in violations),
            f"Expected violation about SLICE-9999, got: {violations}",
        )

    def test_invalid_role_detected(self):
        """An invalid Owner Role in index should fail."""
        with tempfile.TemporaryDirectory() as tmpdir:
            (Path(tmpdir) / "SLICE-0001").mkdir()
            index = Path(tmpdir) / "index.md"
            index.write_text(
                "# Slice Index\n\n"
                "| Slice ID | Name | Current State | Owner Role | Slice Folder | Last Updated |\n"
                "|---|---|---|---|---|---|\n"
                "| SLICE-0001 | Test | `PRD_DEFINED` | InvalidRole | docs/slices/SLICE-0001/ | 2026-02-27 |\n"
            )
            with patch.object(validate_slice_registry, "SLICES_DIR", Path(tmpdir)):
                with patch.object(validate_slice_registry, "INDEX_FILE", index):
                    violations = validate_slice_registry.validate()
        self.assertTrue(
            any("InvalidRole" in v and "not a valid role" in v for v in violations),
            f"Expected role violation, got: {violations}",
        )


class TestPRChecklist(unittest.TestCase):
    """Tests for validate_pr_checklist.py"""

    def test_real_template_passes(self):
        """The actual PR template in the repo should pass validation."""
        template = SCRIPTS_DIR.parent / ".github" / "pull_request_template.md"
        if not template.exists():
            self.skipTest("PR template not found")
        content = template.read_text()
        violations = validate_pr_checklist.validate_content(content, "template")
        self.assertEqual(violations, [], f"Real template should pass, got: {violations}")

    def test_empty_template_fails(self):
        """An empty PR body should fail."""
        violations = validate_pr_checklist.validate_content("", "empty")
        self.assertTrue(len(violations) > 0, "Empty template should have violations")

    def test_missing_section_detected(self):
        """A template missing the Audit section should fail."""
        content = (
            "## Slice\n- **Slice ID:** SLICE-XXXX\n"
            "### Registration & State\n- [ ] registered in index.md\n- [ ] state.md matches\n"
            "### Artifacts\n- [ ] prd.md\n- [ ] hld.md\n- [ ] lld.md\n"
            "### Implementation\n- [ ] Code matches lld.md\n"
            "### Review\n- [ ] review.md exists\n"
            "### QA\n- [ ] test-plan.md\n- [ ] qa.md\n"
            "### Context Capsule\n- [ ] Context Capsule used\n"
            "## Summary\n\n## Changes\n"
        )
        violations = validate_pr_checklist.validate_content(content, "test")
        # Should flag missing Audit section and missing audit-report.md checkbox
        audit_violations = [v for v in violations if "udit" in v]
        self.assertTrue(len(audit_violations) > 0, f"Expected audit violations, got: {violations}")

    def test_required_checkboxes_detected(self):
        """All required checkbox items must be present."""
        # Minimal content with some checkboxes missing
        content = (
            "## Slice\n### Registration & State\n- [ ] registered in foo\n"
            "### Artifacts\n### Implementation\n### Review\n### QA\n### Audit\n"
            "### Context Capsule\n## Summary\n## Changes\n"
        )
        violations = validate_pr_checklist.validate_content(content, "test")
        self.assertTrue(len(violations) > 0, "Should detect missing checkbox items")


class TestParseCurrentState(unittest.TestCase):
    """Tests for state.md parsing used across validators."""

    def test_parse_backtick_format(self):
        """Parse state with backticks: `LLD_DEFINED`"""
        with tempfile.NamedTemporaryFile(mode="w", suffix=".md", delete=False) as f:
            f.write("| **Current State** | `CODE_COMPLETE` |")
            f.flush()
            result = validate_required_artifacts.parse_current_state(Path(f.name))
        os.unlink(f.name)
        self.assertEqual(result, "CODE_COMPLETE")

    def test_parse_no_backtick_format(self):
        """Parse state without backticks: CODE_COMPLETE"""
        with tempfile.NamedTemporaryFile(mode="w", suffix=".md", delete=False) as f:
            f.write("| **Current State** | REVIEW_APPROVED |")
            f.flush()
            result = validate_required_artifacts.parse_current_state(Path(f.name))
        os.unlink(f.name)
        self.assertEqual(result, "REVIEW_APPROVED")

    def test_parse_nonexistent_file(self):
        """Non-existent file should return None."""
        result = validate_required_artifacts.parse_current_state(Path("/nonexistent/state.md"))
        self.assertIsNone(result)


if __name__ == "__main__":
    unittest.main(verbosity=2)
