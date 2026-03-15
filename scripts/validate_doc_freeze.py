#!/usr/bin/env python3
"""
validate_doc_freeze.py — Doc Freeze Validator

Enforces Operating Principle #13: Once a slice enters LLD_DEFINED or later,
PRD/HLD/LLD are frozen. Any modification requires a change-request.md.

Checks:
  - For each slice in LLD_DEFINED or later state:
    - prd.md, hld.md, lld.md must exist (they were created before freeze)
    - If git is available: checks if frozen files were modified after the
      commit that set state to LLD_DEFINED
    - If modified post-freeze: change-request.md must exist

Exit codes:
  0 = all slices compliant
  1 = violations found
  2 = configuration/runtime error
"""

import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
SLICES_DIR = REPO_ROOT / "docs" / "slices"

FROZEN_FILES = ["prd.md", "hld.md", "lld.md"]

# States at which docs are frozen (LLD_DEFINED and everything after it)
FROZEN_STATES = [
    "LLD_DEFINED",
    "CODE_IN_PROGRESS",
    "CODE_COMPLETE",
    "REVIEW_REQUIRED",
    "REVIEW_CHANGES",
    "REVIEW_APPROVED",
    "QA_REQUIRED",
    "QA_CHANGES",
    "QA_APPROVED",
    "AUDIT_REQUIRED",
    "AUDIT_APPROVED",
    "MERGED",
]


def git_available() -> bool:
    """Check if git is available and we're in a repo."""
    try:
        subprocess.run(
            ["git", "rev-parse", "--git-dir"],
            cwd=REPO_ROOT,
            capture_output=True,
            check=True,
        )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def parse_current_state(state_file: Path) -> Optional[str]:
    """Extract Current State from state.md."""
    if not state_file.exists():
        return None
    content = state_file.read_text()
    match = re.search(r"\*\*Current State\*\*\s*\|\s*`?([A-Z_]+)`?", content)
    return match.group(1) if match else None


def get_lld_defined_commit(slice_dir: Path) -> Optional[str]:
    """Find the commit where state.md first recorded LLD_DEFINED."""
    state_file = slice_dir / "state.md"
    if not state_file.exists():
        return None
    try:
        result = subprocess.run(
            ["git", "log", "--all", "--format=%H", "--diff-filter=M", "--", str(state_file)],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
        )
        # Walk commits from oldest to newest looking for LLD_DEFINED
        commits = result.stdout.strip().split("\n") if result.stdout.strip() else []
        for commit in reversed(commits):
            show = subprocess.run(
                ["git", "show", f"{commit}:{state_file.relative_to(REPO_ROOT)}"],
                cwd=REPO_ROOT,
                capture_output=True,
                text=True,
            )
            if "LLD_DEFINED" in show.stdout:
                return commit
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass
    return None


def file_modified_after_commit(filepath: Path, since_commit: str) -> bool:
    """Check if a file was modified in any commit after since_commit."""
    try:
        result = subprocess.run(
            ["git", "log", f"{since_commit}..HEAD", "--oneline", "--", str(filepath)],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
        )
        return bool(result.stdout.strip())
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def discover_slice_dirs() -> list[Path]:
    """Find all SLICE-NNNN directories."""
    if not SLICES_DIR.exists():
        return []
    return sorted(
        d
        for d in SLICES_DIR.iterdir()
        if d.is_dir() and re.match(r"SLICE-\d{4}", d.name)
    )


def validate() -> list[str]:
    """Run all doc freeze checks. Returns list of violation messages."""
    violations: list[str] = []
    use_git = git_available()
    slice_dirs = discover_slice_dirs()

    if not slice_dirs:
        print("  No slice directories found. Nothing to validate.")
        return violations

    for slice_dir in slice_dirs:
        slice_id = slice_dir.name
        state_file = slice_dir / "state.md"
        current_state = parse_current_state(state_file)

        if current_state is None:
            violations.append(f"{slice_id}: Cannot parse current state from state.md")
            continue

        if current_state not in FROZEN_STATES:
            print(f"  {slice_id}: state={current_state} (pre-freeze, skipping)")
            continue

        print(f"  {slice_id}: state={current_state} (frozen)")

        # Check frozen files exist
        for fname in FROZEN_FILES:
            fpath = slice_dir / fname
            if not fpath.exists():
                violations.append(
                    f"{slice_id}: {fname} is missing but required (slice is in frozen state {current_state})"
                    f"\n  Fix: Create {fname} or rollback state to before LLD_DEFINED"
                )

        # If git available, check for post-freeze modifications
        if use_git:
            lld_commit = get_lld_defined_commit(slice_dir)
            if lld_commit:
                has_change_request = (slice_dir / "change-request.md").exists()
                for fname in FROZEN_FILES:
                    fpath = slice_dir / fname
                    if fpath.exists() and file_modified_after_commit(fpath, lld_commit):
                        if not has_change_request:
                            violations.append(
                                f"{slice_id}: {fname} was modified after doc freeze (commit {lld_commit[:8]})"
                                f"\n  Fix: Add change-request.md with rollback justification"
                            )

    return violations


def main() -> int:
    print("=" * 60)
    print("validate_doc_freeze.py — Doc Freeze Validator")
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

    print("\nPASSED — No doc freeze violations.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
