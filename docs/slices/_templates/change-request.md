# Change Request: SLICE-XXXX

**Requested By:** <!-- Role label only: Planner / Architect / Engineer / Reviewer / QA / Auditor -->
**Date:** <!-- YYYY-MM-DD -->
**Current Slice State:** <!-- e.g., CODE_IN_PROGRESS -->

---

## Reason for Change

<!-- Why is this change needed? What was discovered that the original doc did not account for? Be specific. -->

## Impacted Documents

| Document | Change Type | Details |
|---|---|---|
| `prd.md` | Modified / No Change | <!-- What changes --> |
| `hld.md` | Modified / No Change | <!-- What changes --> |
| `lld.md` | Modified / No Change | <!-- What changes --> |

## State Rollback Required

| Field | Value |
|---|---|
| **Current State** | <!-- e.g., CODE_IN_PROGRESS --> |
| **Rollback To** | <!-- Earliest impacted state, e.g., PRD_DEFINED --> |
| **Invalidated States** | <!-- All downstream states that must be re-done --> |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| <!-- e.g., Delays delivery --> | High / Medium / Low | High / Medium / Low | <!-- e.g., Scope reduction --> |

## Approval Required

| Role | Approved | Date |
|---|---|---|
| <!-- e.g., Planner (if PRD changed) --> | Yes / No / Pending | <!-- YYYY-MM-DD --> |
| <!-- e.g., Architect (if HLD/LLD changed) --> | Yes / No / Pending | <!-- YYYY-MM-DD --> |

## Updated Acceptance Criteria

<!-- If the PRD acceptance criteria changed, list the new/modified criteria here. -->

| ID | Original Criterion | Updated Criterion |
|---|---|---|
| AC-XX | <!-- Old --> | <!-- New --> |

---

**Rule:** Once approved, the slice state MUST be rolled back to the state listed above. All downstream artifacts are invalidated and must be re-produced through the normal state machine flow. There are no partial rollbacks.
