#!/usr/bin/env python3
"""
validate_required_artifacts.py — Required Artifacts Validator

Enforces STATE_MACHINE.md "Required Artifacts Per Transition" table:
  For each slice, based on its current state in state.md, verifies that
  all artifacts required up to that state exist and have minimal structure.

Exit codes:
  0 = all slices have required artifacts
  1 = violations found
  2 = configuration/runtime error
"""

import re
import sys
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
SLICES_DIR = REPO_ROOT / "docs" / "slices"

# Maps each state to the artifacts that MUST exist when a slice is in that state.
# Built from the "Required Artifacts Per Transition" table in STATE_MACHINE.md.
STATE_REQUIRED_ARTIFACTS: dict[str, list[str]] = {
    "NOT_STARTED": ["state.md"],
    "PRD_DEFINED": ["state.md", "prd.md"],
    "HLD_DEFINED": ["state.md", "prd.md", "hld.md"],
    "LLD_DEFINED": ["state.md", "prd.md", "hld.md", "lld.md"],
    "CODE_IN_PROGRESS": ["state.md", "prd.md", "hld.md", "lld.md"],
    "CODE_COMPLETE": ["state.md", "prd.md", "hld.md", "lld.md"],
    "REVIEW_REQUIRED": ["state.md", "prd.md", "hld.md", "lld.md"],
    "REVIEW_CHANGES": ["state.md", "prd.md", "hld.md", "lld.md", "review.md"],
    "REVIEW_APPROVED": ["state.md", "prd.md", "hld.md", "lld.md", "review.md"],
    "QA_REQUIRED": ["state.md", "prd.md", "hld.md", "lld.md", "review.md"],
    "QA_CHANGES": ["state.md", "prd.md", "hld.md", "lld.md", "review.md"],
    "QA_APPROVED": ["state.md", "prd.md", "hld.md", "lld.md", "review.md", "test-plan.md"],
    "AUDIT_REQUIRED": ["state.md", "prd.md", "hld.md", "lld.md", "review.md", "test-plan.md"],
    "AUDIT_APPROVED": [
        "state.md", "prd.md", "hld.md", "lld.md",
        "review.md", "test-plan.md", "audit-report.md",
    ],
    "MERGED": [
        "state.md", "prd.md", "hld.md", "lld.md",
        "review.md", "test-plan.md", "audit-report.md",
    ],
}

ALL_VALID_STATES = set(STATE_REQUIRED_ARTIFACTS.keys())

# Minimal structural validation: each artifact must have a top-level heading
REQUIRED_HEADINGS: dict[str, str] = {
    "state.md": r"#\s+Slice State",
    "prd.md": r"#\s+PRD:",
    "hld.md": r"#\s+HLD:",
    "lld.md": r"#\s+LLD:",
    "review.md": r"#\s+Review:",
    "qa.md": r"#\s+QA Report:",
    "test-plan.md": r"#\s+Test Plan:",
    "audit-report.md": r"#\s+Audit Report:",
    "change-request.md": r"#\s+Change Request:",
}


def parse_current_state(state_file: Path) -> Optional[str]:
    """Extract Current State from state.md."""
    if not state_file.exists():
        return None
    content = state_file.read_text()
    match = re.search(r"\*\*Current State\*\*\s*\|\s*`?([A-Z_]+)`?", content)
    return match.group(1) if match else None


def discover_slice_dirs() -> list[Path]:
    """Find all SLICE-NNNN directories."""
    if not SLICES_DIR.exists():
        return []
    return sorted(
        d
        for d in SLICES_DIR.iterdir()
        if d.is_dir() and re.match(r"SLICE-\d{4}", d.name)
    )


def check_heading(filepath: Path, artifact_name: str) -> bool:
    """Check if a file has the expected heading pattern."""
    pattern = REQUIRED_HEADINGS.get(artifact_name)
    if not pattern:
        return True  # No heading requirement defined
    content = filepath.read_text()
    return bool(re.search(pattern, content))


def validate() -> list[str]:
    """Run artifact validation. Returns list of violation messages."""
    violations: list[str] = []
    slice_dirs = discover_slice_dirs()

    if not slice_dirs:
        print("  No slice directories found. Nothing to validate.")
        return violations

    for slice_dir in slice_dirs:
        slice_id = slice_dir.name
        state_file = slice_dir / "state.md"

        # Check state.md exists
        if not state_file.exists():
            violations.append(
                f"{slice_id}: state.md is missing"
                f"\n  Fix: Create state.md from _templates/state.md"
            )
            continue

        current_state = parse_current_state(state_file)
        if current_state is None:
            violations.append(
                f"{slice_id}: Cannot parse Current State from state.md"
                f"\n  Fix: Ensure state.md has '**Current State** | `STATE_NAME`' format"
            )
            continue

        if current_state not in ALL_VALID_STATES:
            violations.append(
                f"{slice_id}: State '{current_state}' is not a valid state"
                f"\n  Valid states: {', '.join(sorted(ALL_VALID_STATES))}"
                f"\n  Fix: Update state.md to use a valid state from STATE_MACHINE.md"
            )
            continue

        required = STATE_REQUIRED_ARTIFACTS[current_state]
        print(f"  {slice_id}: state={current_state}, checking {len(required)} required artifact(s)")

        for artifact_name in required:
            artifact_path = slice_dir / artifact_name
            if not artifact_path.exists():
                violations.append(
                    f"{slice_id}: {artifact_name} is missing (required at state {current_state})"
                    f"\n  Fix: Create {artifact_name} from _templates/{artifact_name} or rollback state"
                )
            elif not check_heading(artifact_path, artifact_name):
                expected_pattern = REQUIRED_HEADINGS.get(artifact_name, "")
                violations.append(
                    f"{slice_id}: {artifact_name} is missing required heading (expected pattern: {expected_pattern})"
                    f"\n  Fix: Ensure {artifact_name} starts with the correct heading per template"
                )

    return violations


def main() -> int:
    print("=" * 60)
    print("validate_required_artifacts.py — Required Artifacts Validator")
    print("=" * 60)

    try:
        violations = validate()
    except Exception as e:
        print(f"\nERROR: {e}", file=sys.stderr)
        return 2

    if violations:
        print(f"\nFAILED — {len(violations)} violation(s) found:\n")
        for i, v in enumerate(violations, 1):
            print(f"  [{i}] {v}")
        return 1

    print("\nPASSED — All required artifacts present and structurally valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
