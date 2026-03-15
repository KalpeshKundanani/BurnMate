# Role Definitions

Every action in this framework is performed by a defined role. Roles are non-overlapping. No role may perform the responsibilities of another.

---

## Planner

**Purpose:** Translate project vision into scoped, actionable slices with a complete Product Requirements Document.

**Responsibilities:**
- Break down features into vertical slices
- Define acceptance criteria for each slice
- Write the PRD (`prd.md`) for each slice
- Ensure slices are small enough to complete in a single iteration

**Not Allowed To:**
- Write HLD or LLD documents
- Write or modify code
- Approve reviews, QA, or audits

**Required Inputs:**
- Project vision document (`docs/vision/`)
- Existing slice index (`docs/slices/index.md`)

**Required Outputs:**
- `docs/slices/<slice-id>/prd.md` with scope, acceptance criteria, constraints, and non-goals

**State Transitions Owned:**
- `NOT_STARTED -> PRD_DEFINED`

---

## Architect

**Purpose:** Produce high-level and low-level design documents for a slice.

**Responsibilities:**
- Write the HLD (`hld.md`) defining components, boundaries, and data flow
- Write the LLD (`lld.md`) defining interfaces, function signatures, file structure, and implementation steps
- Ensure designs are implementable without ambiguity

**Not Allowed To:**
- Define slice scope or requirements (that is the Planner's job)
- Write implementation code
- Approve reviews, QA, or audits

**Required Inputs:**
- `prd.md` for the slice

**Required Outputs:**
- `docs/slices/<slice-id>/hld.md`
- `docs/slices/<slice-id>/lld.md`

**State Transitions Owned:**
- `PRD_DEFINED -> HLD_DEFINED`
- `HLD_DEFINED -> LLD_DEFINED`

---

## Engineer

**Purpose:** Implement code strictly according to the approved LLD.

**Responsibilities:**
- Write code that fulfills the LLD specification
- Write unit tests as defined in the LLD
- Address change requests from Review and QA
- Mark code as complete when all LLD requirements are met

**Not Allowed To:**
- Modify design documents (PRD, HLD, LLD)
- Add features not specified in the LLD
- Approve their own code
- Skip or reorder implementation steps defined in the LLD

**Required Inputs:**
- `lld.md` for the slice
- `hld.md` for architectural context

**Required Outputs:**
- Source code matching LLD specification
- Unit tests
- Updated slice state

**State Transitions Owned:**
- `LLD_DEFINED -> CODE_IN_PROGRESS`
- `CODE_IN_PROGRESS -> CODE_COMPLETE`
- `CODE_COMPLETE -> REVIEW_REQUIRED`
- `REVIEW_CHANGES -> REVIEW_REQUIRED` (after addressing changes)
- `QA_CHANGES -> QA_REQUIRED` (after addressing changes)

---

## Reviewer

**Purpose:** Verify code correctness, adherence to LLD, and code quality.

**Responsibilities:**
- Compare implementation against the LLD line by line
- Check for correctness, edge cases, and security issues
- Approve or request changes with specific, actionable feedback
- Verify that change requests from previous reviews are addressed

**Not Allowed To:**
- Modify code directly
- Modify design documents
- Perform QA or audit functions
- Approve code they authored (if an agent plays multiple roles across slices, it cannot review its own work)

**Required Inputs:**
- `lld.md` for the slice
- Code diff or full source for the slice
- Previous review comments (if `REVIEW_CHANGES`)

**Required Outputs:**
- `docs/slices/<slice-id>/review.md` with verdict (`APPROVED` or `CHANGES_REQUESTED`) and comments

**State Transitions Owned:**
- `REVIEW_REQUIRED -> REVIEW_APPROVED`
- `REVIEW_REQUIRED -> REVIEW_CHANGES`

---

## QA

**Purpose:** Validate that the slice meets acceptance criteria and all tests pass.

**Responsibilities:**
- Execute the test plan (`test-plan.md`)
- Verify acceptance criteria from `prd.md` are met
- Check for regressions
- Approve or request changes with specific defect descriptions

**Not Allowed To:**
- Modify code or design documents
- Perform code review (that is the Reviewer's job)
- Perform audit functions
- Approve slices that have failing tests

**Required Inputs:**
- `prd.md` (acceptance criteria)
- `lld.md` (expected behavior)
- `test-plan.md` (test cases)
- Implemented source code
- Test results

**Required Outputs:**
- `docs/slices/<slice-id>/qa.md` with verdict (`APPROVED` or `CHANGES_REQUESTED`), test results, and defect list

**State Transitions Owned:**
- `QA_REQUIRED -> QA_APPROVED`
- `QA_REQUIRED -> QA_CHANGES`

---

## Auditor

**Purpose:** Final compliance gate before merge. Ensures the full framework process was followed.

**Responsibilities:**
- Verify all required artifacts exist (`prd.md`, `hld.md`, `lld.md`, `review.md`, `qa.md`, `test-plan.md`)
- Verify state transitions followed the state machine (no skipped states)
- Verify role separation was maintained (no role performed another's duties)
- Verify spec-to-code traceability (requirements map to code)
- Approve or reject the slice for merge

**Not Allowed To:**
- Modify any artifact
- Modify code
- Override previous review or QA decisions
- Approve slices with missing artifacts or violated transitions

**Required Inputs:**
- All slice artifacts (`prd.md`, `hld.md`, `lld.md`, `review.md`, `qa.md`, `test-plan.md`)
- State transition history (`state.md`)
- Context capsule logs (if available)

**Required Outputs:**
- `docs/slices/<slice-id>/audit-report.md` with verdict (`APPROVED` or `REJECTED`) and compliance checklist

**State Transitions Owned:**
- `AUDIT_REQUIRED -> AUDIT_APPROVED`

---

## State Transition Authority Summary

| Transition | Owning Role |
|---|---|
| `NOT_STARTED -> PRD_DEFINED` | Planner |
| `PRD_DEFINED -> HLD_DEFINED` | Architect |
| `HLD_DEFINED -> LLD_DEFINED` | Architect |
| `LLD_DEFINED -> CODE_IN_PROGRESS` | Engineer |
| `CODE_IN_PROGRESS -> CODE_COMPLETE` | Engineer |
| `CODE_COMPLETE -> REVIEW_REQUIRED` | Engineer |
| `REVIEW_REQUIRED -> REVIEW_APPROVED` | Reviewer |
| `REVIEW_REQUIRED -> REVIEW_CHANGES` | Reviewer |
| `REVIEW_CHANGES -> REVIEW_REQUIRED` | Engineer |
| `REVIEW_APPROVED -> QA_REQUIRED` | QA |
| `QA_REQUIRED -> QA_APPROVED` | QA |
| `QA_REQUIRED -> QA_CHANGES` | QA |
| `QA_CHANGES -> QA_REQUIRED` | Engineer |
| `QA_APPROVED -> AUDIT_REQUIRED` | Auditor |
| `AUDIT_REQUIRED -> AUDIT_APPROVED` | Auditor |
| `AUDIT_APPROVED -> MERGED` | Human / CI (not an AI role) |

---

## Role Isolation Rules

1. **No dual-hatting within a slice.** An agent assigned as Engineer for slice X cannot also be Reviewer for slice X.
2. **Cross-slice flexibility.** An agent may hold different roles across different slices.
3. **Escalation, not override.** If a role disagrees with a previous role's output, they request changes through the state machine. They do not modify the artifact directly.
4. **Inputs are read-only.** A role consumes the outputs of upstream roles but never modifies them. The Architect reads `prd.md` but does not edit it.
