# Change Request: SLICE-0007

**Requested By:** Architect
**Date:** 2026-03-16
**Current Slice State:** AUDIT_REQUIRED

---

## Reason for Change

The original frozen contract and LLD omitted two Compose UI dependencies that are required by the implemented navigation and icon usage in this slice. A planner repair updated the frozen design artifacts to explicitly allow `composeApp/build.gradle.kts` dependency declarations for the missing UI libraries so the branch can be brought back to a compiling state without widening functional scope.

## Impacted Documents

| Document | Change Type | Details |
|---|---|---|
| `prd.md` | No Change | No product requirements changed. |
| `hld.md` | No Change | No architectural behavior changed. |
| `lld.md` | Modified | Added explicit UI dependency requirements for `androidx.navigation.compose` and `androidx.compose.material.icons`. |

## State Rollback Required

| Field | Value |
|---|---|
| **Current State** | `AUDIT_REQUIRED` |
| **Rollback To** | `LLD_DEFINED` |
| **Invalidated States** | `CODE_IN_PROGRESS`, `CODE_COMPLETE`, `REVIEW_REQUIRED`, `REVIEW_CHANGES`, `REVIEW_APPROVED`, `QA_REQUIRED`, `QA_APPROVED`, `AUDIT_REQUIRED` |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Dependency repair diverges from approved slice scope | Low | Medium | Limit Gradle changes to the two required UI dependencies only. |
| Build remains blocked due to missing dependency declarations | High | High | Update allowed Gradle declarations and rerun compile, test, and validators. |

## Approval Required

| Role | Approved | Date |
|---|---|---|
| Planner | Yes | 2026-03-16 |
| Architect | Yes | 2026-03-16 |

## Updated Acceptance Criteria

No PRD acceptance criteria changed.

| ID | Original Criterion | Updated Criterion |
|---|---|---|
| AC-01 | No change | No change |

---

**Rule:** This change request documents the frozen-doc repair that authorized the dependency fix. This engineering repair does not alter the recorded slice state in `state.md`.
