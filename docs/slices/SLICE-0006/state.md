# Slice State

| Field | Value |
|---|---|
| **Slice** | SLICE-0006 |
| **Name** | Dashboard Read Model |
| **Version** | 1 |
| **Current State** | `QA_APPROVED` |
| **Owner Role** | Auditor |
| **Last Updated** | 2026-03-16 |
| **Links** | `feature/SLICE-0006/dashboard-read-model` |
| **Blocking Issues** | None |
| **Notes** | QA approved; handed to Auditor. |

## State History

| State | Role | Date | Artifact |
|---|---|---|---|
| `NOT_STARTED` | Planner | 2026-03-16 | slice initialized |
| `PRD_DEFINED` | Planner | 2026-03-16 | prd.md |
| `HLD_DEFINED` | Architect | 2026-03-16 | hld.md |
| `LLD_DEFINED` | Architect | 2026-03-16 | lld.md |
| `CODE_IN_PROGRESS` | Engineer | 2026-03-16 | engineering started |
| `CODE_COMPLETE` | Engineer | 2026-03-16 | dashboard read model implemented |
| `REVIEW_REQUIRED` | Engineer | 2026-03-16 | submitted for review |
| `REVIEW_APPROVED` | Reviewer | 2026-03-16 | review approved; handed to QA |
| `QA_REQUIRED` | QA | 2026-03-16 | submitted to QA |
| `QA_APPROVED` | QA | 2026-03-16 | qa approved; handed to Auditor |

<!-- Append a row for every state transition. This is the audit trail. -->

---

**Rule:** The `Current State` in this file must match the entry in `docs/slices/index.md` at all times. If they diverge, the slice is in an invalid state and work must stop until reconciled.
