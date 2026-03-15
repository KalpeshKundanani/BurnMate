#!/usr/bin/env python3
"""
validate_slice_registry.py — Slice Registry Validator

Enforces docs/slices/index.md rules:
  1. Every SLICE-NNNN folder on disk is present in index.md
  2. Every entry in index.md has a corresponding folder on disk
  3. Owner Role matches vocabulary from .ai/ROLES.md
  4. Slice folder links are correct relative paths

Exit codes:
  0 = registry consistent
  1 = violations found
  2 = configuration/runtime error
"""

import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SLICES_DIR = REPO_ROOT / "docs" / "slices"
INDEX_FILE = SLICES_DIR / "index.md"
ROLES_FILE = REPO_ROOT / ".ai" / "ROLES.md"

VALID_ROLES = {"Planner", "Architect", "Engineer", "Reviewer", "QA", "Auditor"}


def parse_roles_from_file() -> set[str]:
    """Extract role names from ROLES.md headings as ground truth."""
    if not ROLES_FILE.exists():
        return VALID_ROLES  # fallback to hardcoded
    content = ROLES_FILE.read_text()
    roles = set()
    for match in re.finditer(r"^## (\w+)", content, re.MULTILINE):
        role = match.group(1)
        # Skip non-role headings
        if role not in ("Role", "State"):
            roles.add(role)
    return roles if roles else VALID_ROLES


def discover_slice_dirs() -> set[str]:
    """Find all SLICE-NNNN directories on disk."""
    if not SLICES_DIR.exists():
        return set()
    return {
        d.name
        for d in SLICES_DIR.iterdir()
        if d.is_dir() and re.match(r"SLICE-\d{4}", d.name)
    }


def parse_index() -> list[dict[str, str]]:
    """Parse index.md table rows into list of dicts."""
    if not INDEX_FILE.exists():
        return []

    content = INDEX_FILE.read_text()
    entries = []

    # Match table rows: | SLICE-NNNN | name | state | role | folder | date |
    for match in re.finditer(
        r"^\|\s*(SLICE-\d{4})\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|",
        content,
        re.MULTILINE,
    ):
        entries.append(
            {
                "slice_id": match.group(1).strip(),
                "name": match.group(2).strip(),
                "state": match.group(3).strip().strip("`"),
                "role": match.group(4).strip(),
                "folder": match.group(5).strip(),
                "updated": match.group(6).strip(),
            }
        )
    return entries


def validate() -> list[str]:
    """Run all registry checks. Returns list of violation messages."""
    violations: list[str] = []
    valid_roles = parse_roles_from_file()

    # Discover
    disk_slices = discover_slice_dirs()
    index_entries = parse_index()
    index_ids = {e["slice_id"] for e in index_entries}

    print(f"  Slices on disk: {sorted(disk_slices) if disk_slices else '(none)'}")
    print(f"  Slices in index: {sorted(index_ids) if index_ids else '(none)'}")

    # Check 1: Disk slices missing from index
    missing_from_index = disk_slices - index_ids
    for sid in sorted(missing_from_index):
        violations.append(
            f"{sid}: Folder exists on disk but missing from index.md"
            f"\n  Fix: Add row for {sid} to docs/slices/index.md"
        )

    # Check 2: Index entries missing from disk
    missing_from_disk = index_ids - disk_slices
    for sid in sorted(missing_from_disk):
        violations.append(
            f"{sid}: Listed in index.md but folder does not exist"
            f"\n  Fix: Create docs/slices/{sid}/ with at minimum state.md"
        )

    # Check 3: Owner Role vocabulary
    for entry in index_entries:
        role = entry["role"]
        if role and role not in valid_roles:
            violations.append(
                f"{entry['slice_id']}: Owner Role '{role}' is not a valid role"
                f"\n  Valid roles: {', '.join(sorted(valid_roles))}"
                f"\n  Fix: Update index.md to use a valid role name"
            )

    # Check 4: Slice folder links
    for entry in index_entries:
        folder_link = entry["folder"]
        if not folder_link:
            violations.append(
                f"{entry['slice_id']}: Slice Folder column is empty"
                f"\n  Fix: Set to docs/slices/{entry['slice_id']}/"
            )
            continue

        # Normalize the link (strip markdown link syntax if present)
        clean_path = re.sub(r"\[.*?\]\((.*?)\)", r"\1", folder_link)
        clean_path = clean_path.strip().rstrip("/")

        expected = f"docs/slices/{entry['slice_id']}"
        # Accept both relative forms
        if clean_path not in (expected, f"./{expected}", f"/{expected}", entry["slice_id"]):
            # Also accept just the folder path without docs/slices prefix
            if not clean_path.endswith(entry["slice_id"]):
                violations.append(
                    f"{entry['slice_id']}: Slice Folder link '{folder_link}' does not match expected path"
                    f"\n  Expected: docs/slices/{entry['slice_id']}/"
                    f"\n  Fix: Update the Slice Folder column in index.md"
                )

    # Check 5: Naming convention (SLICE-NNNN, zero-padded, sequential)
    all_ids = sorted(disk_slices | index_ids)
    for sid in all_ids:
        if not re.match(r"^SLICE-\d{4}$", sid):
            violations.append(
                f"{sid}: Does not match naming convention SLICE-NNNN"
                f"\n  Fix: Rename to SLICE-NNNN format (zero-padded four digits)"
            )

    return violations


def main() -> int:
    print("=" * 60)
    print("validate_slice_registry.py — Slice Registry Validator")
    print("=" * 60)

    if not INDEX_FILE.exists():
        print(f"\nERROR: {INDEX_FILE} not found", file=sys.stderr)
        return 2

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

    print("\nPASSED — Slice registry is consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
