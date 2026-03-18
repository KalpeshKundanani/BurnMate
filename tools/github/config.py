#!/usr/bin/env python3
"""Static configuration for BurnMate GitHub execution tooling."""

from __future__ import annotations

PROJECT_TITLE = "BurnMate Execution"

LOCAL_EXECUTION_STATES = [
    "NOT_STARTED",
    "PRD_DEFINED",
    "HLD_DEFINED",
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

GITHUB_EXECUTION_STATES = [
    "Todo",
    "Planning",
    "Dev",
    "Review",
    "QA",
    "Audit",
    "Done",
    "Live",
]

ROLE_OWNER_VALUES = [
    "Planner",
    "Architect",
    "Engineer",
    "Reviewer",
    "QA",
    "Auditor",
    "Unassigned",
]

PRIORITY_VALUES = ["P0", "P1", "P2", "P3"]
EFFORT_VALUES = ["XS", "S", "M", "L", "XL"]

PROJECT_FIELDS = [
    {
        "name": "Execution State",
        "data_type": "SINGLE_SELECT",
        "options": GITHUB_EXECUTION_STATES,
    },
    {
        "name": "Role Owner",
        "data_type": "SINGLE_SELECT",
        "options": ROLE_OWNER_VALUES,
    },
    {
        "name": "Priority",
        "data_type": "SINGLE_SELECT",
        "options": PRIORITY_VALUES,
    },
    {
        "name": "Effort",
        "data_type": "SINGLE_SELECT",
        "options": EFFORT_VALUES,
    },
    {"name": "Sprint", "data_type": "TEXT"},
    {"name": "Target Release", "data_type": "TEXT"},
    {"name": "Epic", "data_type": "TEXT"},
    {"name": "Blocked Reason", "data_type": "TEXT"},
]

MANAGED_LABELS = {
    "type:slice": {
        "color": "1D76DB",
        "description": "BurnMate execution slice tracked from docs/slices.",
    },
    "blocked": {
        "color": "D73A4A",
        "description": "Execution is blocked by a documented issue or dependency.",
    },
    "roadmap": {
        "color": "5319E7",
        "description": "Included in the locked BurnMate roadmap order.",
    },
    "needs-triage": {
        "color": "FBCA04",
        "description": "Slice metadata is incomplete or still in backlog/placeholder state.",
    },
    "area:data": {
        "color": "0E8A16",
        "description": "Domain, persistence, repository, read-model, or sync work.",
    },
    "area:ui": {
        "color": "C2E0C6",
        "description": "Compose UI, screens, navigation, or presentation work.",
    },
    "area:dashboard": {
        "color": "0052CC",
        "description": "Dashboard or chart visibility work.",
    },
    "area:auth": {
        "color": "BFD4F2",
        "description": "Authentication, sign-in, permissions, or external account integration.",
    },
}

MANAGED_LABEL_NAMES = set(MANAGED_LABELS)
DOC_FILE_ORDER = [
    "state.md",
    "contract.md",
    "prd.md",
    "hld.md",
    "lld.md",
    "review.md",
    "qa.md",
    "test-plan.md",
    "audit-report.md",
    "change-request.md",
]

STATE_ALIASES = {
    "TODO": "Todo",
    "BACKLOG": "Todo",
    "NOTSTARTED": "Todo",
    "PLANNING": "Planning",
    "PRDDEFINED": "Planning",
    "HLDDEFINED": "Planning",
    "LLDDEFINED": "Planning",
    "DEV": "Dev",
    "DEVELOPMENT": "Dev",
    "CODEINPROGRESS": "Dev",
    "CODECOMPLETE": "Dev",
    "REVIEW": "Review",
    "REVIEWREQUIRED": "Review",
    "REVIEWCHANGES": "Review",
    "REVIEWAPPROVED": "Review",
    "QA": "QA",
    "QAREQUIRED": "QA",
    "QACHANGES": "QA",
    "QAAPPROVED": "QA",
    "AUDIT": "Audit",
    "AUDITREQUIRED": "Audit",
    "DONE": "Done",
    "AUDITAPPROVED": "Done",
    "LIVE": "Live",
    "MERGED": "Live",
}

ROLE_ALIASES = {
    "PLANNER": "Planner",
    "ARCHITECT": "Architect",
    "ENGINEER": "Engineer",
    "REVIEWER": "Reviewer",
    "QA": "QA",
    "AUDITOR": "Auditor",
    "AUDIT": "Auditor",
    "UNASSIGNED": "Unassigned",
    "PM": "Planner",
}

SLICE_MARKER_TEMPLATE = "<!-- burnmate:slice-id:{slice_id} -->"
SYNC_START_MARKER = "<!-- burnmate:slice-sync:start -->"
SYNC_END_MARKER = "<!-- burnmate:slice-sync:end -->"
PR_COMMENT_MARKER_TEMPLATE = "<!-- burnmate:pr-link:{slice_id} -->"
PR_REF_START_MARKER = "<!-- burnmate:slice-ref:start -->"
PR_REF_END_MARKER = "<!-- burnmate:slice-ref:end -->"

VALIDATOR_PATHS = [
    "scripts/validate_slice_registry.py",
    "scripts/validate_state_machine_transitions.py",
]
