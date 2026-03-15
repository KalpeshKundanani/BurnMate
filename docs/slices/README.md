# Slice Folder Convention

Every slice lives in `docs/slices/SLICE-NNNN/`. This document defines the required files, ownership, freeze rules, and update procedures.

## Required Files

| File | Purpose | Owner Role | Created At State | Frozen After |
|---|---|---|---|---|
| `contract.md` | Canonical slice execution rules: verdict words, artifact requirements, allowed scope, scan scope | Framework-owned per slice | `NOT_STARTED` | Updated only when slice contract changes are explicitly approved |
| `state.md` | Tracks current state, owner, and metadata | All (each role updates on transition) | `NOT_STARTED` | Never (always updated) |
| `prd.md` | Product requirements, acceptance criteria, constraints | Planner | `PRD_DEFINED` | `LLD_DEFINED` |
| `hld.md` | High-level design: components, data flow, boundaries | Architect | `HLD_DEFINED` | `LLD_DEFINED` |
| `lld.md` | Low-level design: interfaces, models, algorithms, test cases | Architect | `LLD_DEFINED` | `LLD_DEFINED` |
| `test-plan.md` | Test scope, edge cases, exit criteria | QA | `QA_REQUIRED` | `QA_APPROVED` |
| `review.md` | Review verdict and comments | Reviewer | `REVIEW_REQUIRED` | `REVIEW_APPROVED` |
| `qa.md` | QA verdict, test results, defect list | QA | `QA_REQUIRED` | `QA_APPROVED` |
| `audit-report.md` | Compliance audit verdict and traceability | Auditor | `AUDIT_REQUIRED` | `AUDIT_APPROVED` |
| `change-request.md` | Justification for modifying frozen docs | Any role (as needed) | Only when needed | N/A |

## Folder Naming

- Format: `SLICE-NNNN` (zero-padded four digits, sequential)
- Example: `SLICE-0001`, `SLICE-0042`
- No renaming after creation

## Doc Freeze Rules

1. **PRD, HLD, and LLD are frozen once the slice enters `LLD_DEFINED`.**
2. After freeze, these documents are read-only. No role may edit them directly.
3. Any change to a frozen document requires a `change-request.md` to be filed.
4. The change request must specify:
   - Which documents are impacted
   - The earliest state that must be rolled back to
   - Approval from the role that owns that state
5. After approval, the slice state rolls back to the earliest impacted state, and all downstream artifacts are invalidated.

## How Files Are Updated

- **state.md**: Updated by the role that owns the current transition. Must be committed with every state change.
- **contract.md**: Read before any slice execution work. It defines canonical verdict vocabulary, artifact expectations, implementation scope, and residual scan scope.
- **prd.md / hld.md / lld.md**: Written once by the owning role, then frozen. Changed only via change request.
- **review.md / qa.md**: Written by Reviewer / QA respectively. Overwritten on each review cycle (if `REVIEW_CHANGES` or `QA_CHANGES` loops occur, append a new section).
- **audit-report.md**: Written once by Auditor. If rejected, updated with follow-up after issues are resolved.
- **change-request.md**: Created only when a frozen doc must be modified. Can have multiple entries appended chronologically.

## Templates

Canonical templates for all files are in `docs/slices/_templates/`. When creating a new slice, copy the templates into the slice folder. Do not modify templates directly.
