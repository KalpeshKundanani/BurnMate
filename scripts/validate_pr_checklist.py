#!/usr/bin/env python3
"""
validate_pr_checklist.py — PR Checklist Validator

Validates .github/pull_request_template.md structure:
  - Required sections are present
  - Checkbox sections exist
  - Slice folder + state.md references exist

In CI mode (PR_BODY env var set): validates the actual PR body.
In offline mode: validates the template itself.

Exit codes:
  0 = template/PR body is compliant
  1 = violations found
  2 = configuration/runtime error
"""

import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
PR_TEMPLATE = REPO_ROOT / ".github" / "pull_request_template.md"

# Required sections that must appear in the PR template/body
REQUIRED_SECTIONS = [
    "Slice",
    "Registration & State",
    "Artifacts",
    "Implementation",
    "Review",
    "QA",
    "Audit",
    "Context Capsule",
    "Summary",
    "Changes",
]

# Required checkbox items (substring match)
REQUIRED_CHECKBOXES = [
    "registered in",
    "state.md",
    "prd.md",
    "hld.md",
    "lld.md",
    "review.md",
    "test-plan.md",
    "qa.md",
    "audit-report.md",
    "Context Capsule",
]

# Required references
REQUIRED_REFERENCES = [
    r"state\.md",
    r"index\.md",
    r"lld\.md",
    r"prd\.md",
]


def validate_content(content: str, source_name: str) -> list[str]:
    """Validate PR template or PR body content. Returns violations."""
    violations: list[str] = []

    # Check required sections
    for section in REQUIRED_SECTIONS:
        # Look for section as heading or bold label
        pattern = rf"(^#+\s*.*{re.escape(section)}|^\*\*.*{re.escape(section)}|\-\s*\*\*.*{re.escape(section)})"
        if not re.search(pattern, content, re.MULTILINE | re.IGNORECASE):
            # Also try just as a substring in headings
            heading_pattern = rf"^#{1,4}\s+.*{re.escape(section)}"
            if not re.search(heading_pattern, content, re.MULTILINE | re.IGNORECASE):
                violations.append(
                    f"{source_name}: Missing required section '{section}'"
                    f"\n  Fix: Add section heading or label for '{section}'"
                )

    # Check checkbox items exist
    checkboxes = re.findall(r"- \[[ x]\] (.+)", content)
    checkbox_text = " ".join(checkboxes).lower()

    for item in REQUIRED_CHECKBOXES:
        if item.lower() not in checkbox_text:
            violations.append(
                f"{source_name}: Missing required checkbox item containing '{item}'"
                f"\n  Fix: Add a checkbox item referencing '{item}'"
            )

    # Check required references
    for ref_pattern in REQUIRED_REFERENCES:
        if not re.search(ref_pattern, content):
            violations.append(
                f"{source_name}: Missing reference to '{ref_pattern}'"
                f"\n  Fix: Ensure the template references {ref_pattern}"
            )

    # Check that at least one checkbox section exists
    if not checkboxes:
        violations.append(
            f"{source_name}: No checkbox items found (expected '- [ ] ...' format)"
            f"\n  Fix: Add checklist items in '- [ ] item' format"
        )

    return violations


def main() -> int:
    print("=" * 60)
    print("validate_pr_checklist.py — PR Checklist Validator")
    print("=" * 60)

    # Determine mode
    pr_body = os.environ.get("PR_BODY")

    if pr_body:
        print("  Mode: CI (validating PR body from PR_BODY env var)")
        source_name = "PR body"
        content = pr_body
    else:
        print("  Mode: Offline (validating template file)")
        source_name = str(PR_TEMPLATE.relative_to(REPO_ROOT))
        if not PR_TEMPLATE.exists():
            print(f"\nERROR: {PR_TEMPLATE} not found", file=sys.stderr)
            return 2
        content = PR_TEMPLATE.read_text()

    try:
        violations = validate_content(content, source_name)
    except Exception as e:
        print(f"\nERROR: {e}", file=sys.stderr)
        return 2

    if violations:
        print(f"\nFAILED — {len(violations)} violation(s) found:\n")
        for i, v in enumerate(violations, 1):
            print(f"  [{i}] {v}")
        return 1

    print(f"\nPASSED — {source_name} meets all checklist requirements.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
