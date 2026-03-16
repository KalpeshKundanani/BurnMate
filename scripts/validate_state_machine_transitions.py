#!/usr/bin/env python3
"""
validate_state_machine_transitions.py — State Machine Transition Validator

Validates that each slice's state history follows the allowed transitions from
.ai/STATE_MACHINE.md and that the current state matches the final history row.

Exit codes:
  0 = all slices compliant
  1 = violations found
  2 = configuration/runtime error
"""

import re
import sys
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
SLICES_DIR = REPO_ROOT / "docs" / "slices"

ALLOWED_TRANSITIONS = {
    "NOT_STARTED": {"PRD_DEFINED"},
    "PRD_DEFINED": {"HLD_DEFINED"},
    "HLD_DEFINED": {"LLD_DEFINED"},
    "LLD_DEFINED": {"CODE_IN_PROGRESS"},
    "CODE_IN_PROGRESS": {"CODE_COMPLETE"},
    "CODE_COMPLETE": {"REVIEW_REQUIRED"},
    "REVIEW_REQUIRED": {"REVIEW_APPROVED", "REVIEW_CHANGES"},
    "REVIEW_CHANGES": {"REVIEW_REQUIRED"},
    "REVIEW_APPROVED": {"QA_REQUIRED"},
    "QA_REQUIRED": {"QA_APPROVED", "QA_CHANGES"},
    "QA_CHANGES": {"QA_REQUIRED"},
    "QA_APPROVED": {"AUDIT_REQUIRED"},
    "AUDIT_REQUIRED": {"AUDIT_APPROVED"},
    "AUDIT_APPROVED": {"MERGED"},
    "MERGED": set(),
}


def discover_slice_dirs() -> list[Path]:
    if not SLICES_DIR.exists():
        return []
    return sorted(
        d for d in SLICES_DIR.iterdir() if d.is_dir() and re.match(r"SLICE-\d{4}", d.name)
    )


def parse_current_state(content: str) -> Optional[str]:
    match = re.search(r"\*\*Current State\*\*\s*\|\s*`?([A-Z_]+)`?", content)
    return match.group(1) if match else None


def parse_state_history(content: str) -> list[str]:
    states: list[str] = []
    in_history = False

    for line in content.splitlines():
        if line.strip() == "## State History":
            in_history = True
            continue
        if in_history and line.startswith("---"):
            break
        if not in_history:
            continue

        match = re.match(r"\|\s*`?([A-Z_]+)`?\s*\|", line)
        if match:
            states.append(match.group(1))

    return states


def validate() -> list[str]:
    violations: list[str] = []
    slice_dirs = discover_slice_dirs()

    if not slice_dirs:
        print("  No slice directories found. Nothing to validate.")
        return violations

    for slice_dir in slice_dirs:
        slice_id = slice_dir.name
        state_file = slice_dir / "state.md"

        if not state_file.exists():
            violations.append(f"{slice_id}: state.md is missing")
            continue

        content = state_file.read_text()
        current_state = parse_current_state(content)
        history = parse_state_history(content)

        if current_state is None:
            violations.append(f"{slice_id}: Cannot parse Current State from state.md")
            continue

        if current_state not in ALLOWED_TRANSITIONS:
            violations.append(f"{slice_id}: Current state '{current_state}' is not valid")
            continue

        if not history:
            violations.append(f"{slice_id}: State History is missing or has no valid rows")
            continue

        print(f"  {slice_id}: current_state={current_state}, history_entries={len(history)}")

        if history[-1] != current_state:
            violations.append(
                f"{slice_id}: Current State is '{current_state}' but final State History entry is '{history[-1]}'"
                "\n  Fix: Align Current State with the final history row"
            )

        for index, state in enumerate(history):
            if state not in ALLOWED_TRANSITIONS:
                violations.append(
                    f"{slice_id}: State History entry '{state}' is not a valid state"
                )
                continue

            if index == 0:
                if state != "NOT_STARTED":
                    violations.append(
                        f"{slice_id}: State History must begin with NOT_STARTED, found '{state}'"
                        "\n  Fix: Add the initial NOT_STARTED row or repair history"
                    )
                continue

            previous_state = history[index - 1]
            if state not in ALLOWED_TRANSITIONS.get(previous_state, set()):
                violations.append(
                    f"{slice_id}: Invalid transition {previous_state} -> {state}"
                    "\n  Fix: Repair State History to follow .ai/STATE_MACHINE.md"
                )

    return violations


def main() -> int:
    print("=" * 60)
    print("validate_state_machine_transitions.py — State Machine Transition Validator")
    print("=" * 60)

    try:
        violations = validate()
    except Exception as exc:
        print(f"\nERROR: {exc}", file=sys.stderr)
        return 2

    if violations:
        print(f"\nFAILED — {len(violations)} violation(s) found:\n")
        for index, violation in enumerate(violations, 1):
            print(f"  [{index}] {violation}")
        return 1

    print("\nPASSED — All slice state histories follow the state machine.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
