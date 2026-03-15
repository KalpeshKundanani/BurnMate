# State Machine Specification

This document defines the deterministic lifecycle of every development slice in the framework. No agent, human, or automation may violate these transitions.

## States

| State | Description |
|---|---|
| `NOT_STARTED` | Slice exists in backlog. No work has begun. |
| `PRD_DEFINED` | Product Requirements Document is complete. Scope, acceptance criteria, and constraints are locked. |
| `HLD_DEFINED` | High-Level Design document is complete and approved. |
| `LLD_DEFINED` | Low-Level Design document is complete and approved. |
| `CODE_IN_PROGRESS` | Implementation is actively underway based on approved LLD. |
| `CODE_COMPLETE` | All code for the slice is written. No known incomplete work. |
| `REVIEW_REQUIRED` | Code is submitted for peer/AI review. |
| `REVIEW_CHANGES` | Reviewer requested changes. Engineer must address them. |
| `REVIEW_APPROVED` | Reviewer approved the code. |
| `QA_REQUIRED` | Code is submitted for quality assurance validation. |
| `QA_CHANGES` | QA found issues. Engineer must fix them. |
| `QA_APPROVED` | QA validated the slice. All tests pass. |
| `AUDIT_REQUIRED` | Slice is submitted for final audit before merge. |
| `AUDIT_APPROVED` | Auditor confirmed compliance with all framework rules. |
| `MERGED` | Slice is merged into the main branch. Terminal state. |

## Allowed Transitions

```
NOT_STARTED       -> PRD_DEFINED
PRD_DEFINED       -> HLD_DEFINED
HLD_DEFINED       -> LLD_DEFINED
LLD_DEFINED       -> CODE_IN_PROGRESS
CODE_IN_PROGRESS  -> CODE_COMPLETE
CODE_COMPLETE     -> REVIEW_REQUIRED
REVIEW_REQUIRED   -> REVIEW_APPROVED
REVIEW_REQUIRED   -> REVIEW_CHANGES
REVIEW_CHANGES    -> REVIEW_REQUIRED
REVIEW_APPROVED   -> QA_REQUIRED
QA_REQUIRED       -> QA_APPROVED
QA_REQUIRED       -> QA_CHANGES
QA_CHANGES        -> QA_REQUIRED
QA_APPROVED       -> AUDIT_REQUIRED
AUDIT_REQUIRED    -> AUDIT_APPROVED
AUDIT_APPROVED    -> MERGED
```

## Transition Diagram

```
NOT_STARTED
    |
    v
PRD_DEFINED
    |
    v
HLD_DEFINED
    |
    v
LLD_DEFINED
    |
    v
CODE_IN_PROGRESS
    |
    v
CODE_COMPLETE
    |
    v
REVIEW_REQUIRED <--+
    |               |
    +-> REVIEW_CHANGES
    |
    v
REVIEW_APPROVED
    |
    v
QA_REQUIRED <------+
    |               |
    +-> QA_CHANGES
    |
    v
QA_APPROVED
    |
    v
AUDIT_REQUIRED
    |
    v
AUDIT_APPROVED
    |
    v
MERGED (terminal)
```

## Required Artifacts Per Transition

| Transition | Required Artifact |
|---|---|
| `NOT_STARTED -> PRD_DEFINED` | `prd.md` committed |
| `PRD_DEFINED -> HLD_DEFINED` | `hld.md` committed |
| `HLD_DEFINED -> LLD_DEFINED` | `lld.md` committed |
| `LLD_DEFINED -> CODE_IN_PROGRESS` | No new artifact; Engineer begins work |
| `CODE_IN_PROGRESS -> CODE_COMPLETE` | Source code + unit tests committed |
| `CODE_COMPLETE -> REVIEW_REQUIRED` | No new artifact; submission to review |
| `REVIEW_REQUIRED -> REVIEW_APPROVED` | `review.md` with verdict APPROVED |
| `REVIEW_REQUIRED -> REVIEW_CHANGES` | `review.md` with verdict CHANGES_REQUESTED |
| `REVIEW_CHANGES -> REVIEW_REQUIRED` | Code changes committed addressing feedback |
| `REVIEW_APPROVED -> QA_REQUIRED` | No new artifact; submission to QA |
| `QA_REQUIRED -> QA_APPROVED` | `test-plan.md` results with verdict APPROVED |
| `QA_REQUIRED -> QA_CHANGES` | QA defect list documented |
| `QA_CHANGES -> QA_REQUIRED` | Code fixes committed addressing defects |
| `QA_APPROVED -> AUDIT_REQUIRED` | No new artifact; submission to audit |
| `AUDIT_REQUIRED -> AUDIT_APPROVED` | `audit-report.md` with verdict APPROVED |
| `AUDIT_APPROVED -> MERGED` | PR merged to main |

## Rules

1. **Forward-only progression.** A slice moves forward through the pipeline. The only backward transitions are the change-request loops (`REVIEW_CHANGES -> REVIEW_REQUIRED` and `QA_CHANGES -> QA_REQUIRED`).
2. **No skipping.** Every state must be entered and exited through a valid transition. There is no shortcut from `PRD_DEFINED` to `CODE_IN_PROGRESS`.
3. **Single active state.** A slice is in exactly one state at any time.
4. **State is persisted.** The current state must be recorded in the slice tracking file (`docs/slices/<slice-id>/state.md`). State changes must be committed.
5. **Transition requires evidence.** Moving to the next state requires the output artifact of the current state to exist and be committed. See the artifacts table above.
6. **Terminal state is irreversible.** Once `MERGED`, no further transitions occur.

## Invalid Transition Examples

| From | To | Why Invalid |
|---|---|---|
| `NOT_STARTED` | `CODE_IN_PROGRESS` | Skips PRD, HLD, and LLD phases entirely. |
| `PRD_DEFINED` | `LLD_DEFINED` | Skips HLD. You cannot detail what you haven't outlined. |
| `NOT_STARTED` | `HLD_DEFINED` | Skips PRD. Requirements must be defined before design. |
| `CODE_COMPLETE` | `QA_REQUIRED` | Skips review. All code must be reviewed before QA. |
| `REVIEW_APPROVED` | `MERGED` | Skips QA and audit. Both are mandatory. |
| `QA_APPROVED` | `MERGED` | Skips audit. Audit is the final gate. |
| `MERGED` | `AUDIT_REQUIRED` | Terminal state. No backward movement. |
| `REVIEW_CHANGES` | `QA_REQUIRED` | Must go back through `REVIEW_REQUIRED` first. |

## Enforcement Philosophy

This state machine is the backbone of the framework. It exists to:

- **Prevent drift.** Without enforced states, agents will take shortcuts. Shortcuts compound into chaos.
- **Enable resumability.** Any agent can pick up a slice at its current state and know exactly what has been done and what remains.
- **Force artifact production.** Each transition gate requires a concrete output. No state change without proof of work.
- **Make auditing trivial.** The audit role can trace every slice through its full lifecycle by reading state history.

Enforcement is convention-based (agents must check state before acting). Automation scripts may be added in future phases to validate transitions programmatically.
