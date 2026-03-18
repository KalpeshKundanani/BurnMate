#!/usr/bin/env python3
"""Unit tests for BurnMate GitHub execution tooling."""

from __future__ import annotations

import subprocess
import sys
import unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch

TOOL_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOL_DIR))

import github_execution


REPO = github_execution.RepoInfo(
    owner="KalpeshKundanani",
    name="BurnMate",
    default_branch="main",
    url="https://github.com/KalpeshKundanani/BurnMate",
)


class RecordingRunner:
    def __init__(self, response_map: dict[tuple[str, ...], subprocess.CompletedProcess[str]] | None = None) -> None:
        self.calls: list[list[str]] = []
        self.response_map = response_map or {}

    def run(self, args, *, input_text=None, check=True, cwd=github_execution.REPO_ROOT):
        self.calls.append(list(args))
        key = tuple(args)
        return self.response_map.get(key, subprocess.CompletedProcess(args, 0, stdout="", stderr=""))


def load_real_slices() -> dict[str, github_execution.SliceMetadata]:
    return {
        slice_meta.slice_id: slice_meta
        for slice_meta in github_execution.build_slice_metadata(
            REPO,
            github_execution.parse_index_entries(),
            github_execution.parse_roadmap_entries(),
            [],
        )
    }


class TestParsingAndInference(unittest.TestCase):
    def test_discover_slice_dirs(self):
        discovered = github_execution.discover_slice_dirs()
        self.assertGreaterEqual(len(discovered), 9)
        self.assertEqual(discovered[0].name, "SLICE-0001")
        self.assertEqual(discovered[-1].name, "SLICE-0009")

    def test_slice_0001_title_falls_back_to_roadmap(self):
        slices = load_real_slices()
        self.assertEqual(slices["SLICE-0001"].title, "Architecture Bootstrap")
        self.assertIn("Name", slices["SLICE-0001"].placeholder_fields)
        self.assertIn("needs-triage", slices["SLICE-0001"].inferred_labels)

    def test_extract_prd_summary_and_acceptance_preview(self):
        prd_path = github_execution.SLICES_DIR / "SLICE-0008" / "prd.md"
        summary = github_execution.extract_prd_summary(prd_path)
        preview = github_execution.extract_acceptance_criteria(prd_path, limit=5)
        self.assertIsNotNone(summary)
        self.assertIn("dashboard currently renders only textual summary cards", summary)
        self.assertEqual(len(preview), 5)
        self.assertIn("selected range of 7, 14, or 30 days", preview[0])

    def test_infer_labels(self):
        labels = github_execution.infer_labels(
            title="Google Fit + Google Login",
            current_state="REVIEW_APPROVED",
            prd_summary="Dashboard sign-in and Google auth integration work.",
            notes="Google permissions and dashboard refresh.",
            blocking_issues="None",
            placeholder_fields=[],
            in_roadmap=True,
        )
        self.assertIn("type:slice", labels)
        self.assertIn("roadmap", labels)
        self.assertIn("area:auth", labels)
        self.assertIn("area:dashboard", labels)

    def test_render_managed_issue_section_contains_total_acceptance_count(self):
        slices = load_real_slices()
        section = github_execution.render_managed_issue_section(REPO, slices["SLICE-0008"])
        self.assertIn("Showing 5 of 10 inferred criteria.", section)
        self.assertIn("<!-- burnmate:slice-id:SLICE-0008 -->", section)
        self.assertEqual(slices["SLICE-0008"].github_execution_state, "Done")
        self.assertTrue(slices["SLICE-0008"].issue_should_be_closed)
        self.assertEqual(slices["SLICE-0009"].github_execution_state, "Done")


class TestNormalizationAndMerging(unittest.TestCase):
    def test_normalize_state_aliases(self):
        self.assertEqual(github_execution.normalize_state_input("Review Required"), "Review")
        self.assertEqual(github_execution.normalize_state_input("Done"), "Done")

    def test_normalize_owner_aliases(self):
        self.assertEqual(github_execution.normalize_role_input("Audit"), "Auditor")
        self.assertEqual(github_execution.normalize_role_input("QA"), "QA")

    def test_merge_managed_issue_section_preserves_manual_content(self):
        managed = "\n".join(
            [
                "<!-- burnmate:slice-id:SLICE-0008 -->",
                "<!-- burnmate:slice-sync:start -->",
                "updated",
                "<!-- burnmate:slice-sync:end -->",
            ]
        )
        existing = "\n".join(
            [
                "<!-- burnmate:slice-id:SLICE-0008 -->",
                "<!-- burnmate:slice-sync:start -->",
                "old",
                "<!-- burnmate:slice-sync:end -->",
                "",
                "Manual notes stay here.",
            ]
        )
        merged = github_execution.merge_managed_issue_section(existing, managed)
        self.assertIn("updated", merged)
        self.assertIn("Manual notes stay here.", merged)
        self.assertNotIn("old", merged)

    def test_find_existing_issue_prefers_marker(self):
        issues = [
            github_execution.IssueRecord(
                id="1",
                number=11,
                title="SLICE-0008: Wrong title",
                body="<!-- burnmate:slice-id:SLICE-0008 -->",
                url="https://example.com/11",
                state="OPEN",
                labels=[],
            ),
            github_execution.IssueRecord(
                id="2",
                number=12,
                title="SLICE-0008: Charts & Visual Progress",
                body="",
                url="https://example.com/12",
                state="OPEN",
                labels=[],
            ),
        ]
        issue = github_execution.find_existing_issue(
            issues,
            slice_id="SLICE-0008",
            issue_title="SLICE-0008: Charts & Visual Progress",
        )
        self.assertEqual(issue.number, 11)


class TestAuthScopes(unittest.TestCase):
    def test_write_flows_accept_project_scope_alone(self):
        with patch.object(github_execution, "get_gh_auth_scopes", return_value={"repo", "project"}):
            github_execution.require_project_scopes(RecordingRunner())

    def test_read_only_project_access_accepts_project_or_read_project(self):
        self.assertTrue(github_execution.has_project_read_scope({"project"}))
        self.assertTrue(github_execution.has_project_read_scope({"read:project"}))
        self.assertFalse(github_execution.has_project_read_scope({"repo"}))

    def test_should_close_issue_for_todo_done_and_live(self):
        self.assertTrue(github_execution.should_close_issue_for_state("Todo"))
        self.assertTrue(github_execution.should_close_issue_for_state("Done"))
        self.assertTrue(github_execution.should_close_issue_for_state("Live"))
        self.assertFalse(github_execution.should_close_issue_for_state("QA"))


class TestCommandPayloads(unittest.TestCase):
    def test_update_project_field_uses_single_select_option_id(self):
        runner = RecordingRunner()
        project = github_execution.ProjectInfo(
            id="PVT_kw",
            number=1,
            title="BurnMate Execution",
            url="https://example.com/project",
            fields={
                "Execution State": github_execution.ProjectField(
                    id="field-1",
                    name="Execution State",
                    data_type="SINGLE_SELECT",
                    options={"Review": "opt-1"},
                )
            },
        )
        item = github_execution.ProjectItem(id="item-1", content_id="issue-1", url="https://example.com/issue")

        changed = github_execution.update_project_field(
            runner,
            project,
            item,
            field_name="Execution State",
            value="Review",
            dry_run=False,
        )

        self.assertTrue(changed)
        self.assertEqual(
            runner.calls[0],
            [
                "gh",
                "project",
                "item-edit",
                "--id",
                "item-1",
                "--project-id",
                "PVT_kw",
                "--field-id",
                "field-1",
                "--single-select-option-id",
                "opt-1",
            ],
        )

    def test_managed_label_delta_removes_only_managed_labels(self):
        add_labels, remove_labels = github_execution.managed_label_delta(
            ["bug", "blocked", "type:slice"],
            ["type:slice", "roadmap"],
        )
        self.assertEqual(add_labels, ["roadmap"])
        self.assertEqual(remove_labels, ["blocked"])


class TestDryRunAndValidation(unittest.TestCase):
    def test_sync_command_dry_run_all_syncs_all_discovered_slices(self):
        slice_one = load_real_slices()["SLICE-0001"]
        slice_two = load_real_slices()["SLICE-0002"]
        calls: list[str] = []

        def fake_sync_slice(runner, repo, project, slice_meta, issues, *, dry_run):
            calls.append(slice_meta.slice_id)
            return project, github_execution.IssueRecord(
                id=f"id-{slice_meta.slice_id}",
                number=len(calls),
                title=slice_meta.issue_title,
                body="",
                url=f"https://example.com/{slice_meta.slice_id}",
                state="OPEN",
                labels=[],
            )

        args = Namespace(all=True, slice_id=None, dry_run=True, no_validate=True)
        with patch.object(github_execution, "ensure_dependencies"), patch.object(
            github_execution, "detect_repo_info", return_value=REPO
        ), patch.object(
            github_execution,
            "load_slice_lookup",
            return_value={"SLICE-0001": slice_one, "SLICE-0002": slice_two},
        ), patch.object(
            github_execution, "load_issues", return_value=[]
        ), patch.object(
            github_execution, "sync_slice", side_effect=fake_sync_slice
        ), patch.object(
            github_execution, "get_gh_auth_scopes", return_value=set()
        ):
            result = github_execution.sync_command(args)

        self.assertEqual(result, 0)
        self.assertEqual(calls, ["SLICE-0001", "SLICE-0002"])

    def test_sync_command_dry_run_single_slice_filters_correctly(self):
        slice_one = load_real_slices()["SLICE-0001"]
        slice_two = load_real_slices()["SLICE-0002"]
        calls: list[str] = []

        def fake_sync_slice(runner, repo, project, slice_meta, issues, *, dry_run):
            calls.append(slice_meta.slice_id)
            return project, github_execution.IssueRecord(
                id=f"id-{slice_meta.slice_id}",
                number=1,
                title=slice_meta.issue_title,
                body="",
                url=f"https://example.com/{slice_meta.slice_id}",
                state="OPEN",
                labels=[],
            )

        args = Namespace(all=False, slice_id="SLICE-0002", dry_run=True, no_validate=True)
        with patch.object(github_execution, "ensure_dependencies"), patch.object(
            github_execution, "detect_repo_info", return_value=REPO
        ), patch.object(
            github_execution,
            "load_slice_lookup",
            return_value={"SLICE-0001": slice_one, "SLICE-0002": slice_two},
        ), patch.object(
            github_execution, "load_issues", return_value=[]
        ), patch.object(
            github_execution, "sync_slice", side_effect=fake_sync_slice
        ), patch.object(
            github_execution, "get_gh_auth_scopes", return_value=set()
        ):
            result = github_execution.sync_command(args)

        self.assertEqual(result, 0)
        self.assertEqual(calls, ["SLICE-0002"])

    def test_ensure_local_status_rejects_drift(self):
        slice_meta = load_real_slices()["SLICE-0008"]
        with self.assertRaises(github_execution.ValidationError):
            github_execution.ensure_local_status(slice_meta, "Review")

    def test_ensure_local_owner_rejects_drift(self):
        slice_meta = load_real_slices()["SLICE-0009"]
        with self.assertRaises(github_execution.ValidationError):
            github_execution.ensure_local_owner(slice_meta, "Reviewer")


if __name__ == "__main__":
    unittest.main()
