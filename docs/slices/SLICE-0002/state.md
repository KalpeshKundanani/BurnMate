# Slice State

| Field | Value |
|---|---|
| **Slice** | SLICE-0002 |
| **Name** | Calorie Debt Engine |
| **Version** | 1 |
| **Current State** | `AUDIT_APPROVED` |
| **Owner Role** | Auditor |
| **Last Updated** | 2026-03-16 |
| **Links** | `feature/SLICE-0002/calorie-debt-engine` |
| **Blocking Issues** | None |
| **Notes** | Audit approved; slice cleared for merge |

## State History

| State | Role | Date | Artifact |
|---|---|---|---|
| `NOT_STARTED` | Planner | 2026-03-16 | slice initialized |
| `PRD_DEFINED` | Planner | 2026-03-16 | prd.md |
| `HLD_DEFINED` | Architect | 2026-03-16 | hld.md |
| `LLD_DEFINED` | Architect | 2026-03-16 | lld.md |
| `CODE_IN_PROGRESS` | Engineer | 2026-03-16 | engineering started |
| `CODE_COMPLETE` | Engineer | 2026-03-16 | calorie debt engine implemented |
| `REVIEW_REQUIRED` | Engineer | 2026-03-16 | submitted for review |
| `REVIEW_APPROVED` | Reviewer | 2026-03-16 | review.md |
| `QA_REQUIRED` | QA | 2026-03-16 | test-plan.md |
| `QA_APPROVED` | QA | 2026-03-16 | qa.md |
| `AUDIT_REQUIRED` | Auditor | 2026-03-16 | audit requested; artifacts prepared |
| `AUDIT_APPROVED` | Auditor | 2026-03-16 | audit-report.md |

<!-- Append a row for every state transition. This is the audit trail. -->

---

**Rule:** The `Current State` in this file must match the entry in `docs/slices/index.md` at all times. If they diverge, the slice is in an invalid state and work must stop until reconciled.
