# Audit Report: SLICE-XXXX — <!-- Slice Name -->

**Auditor:** `Auditor`
**Date:** <!-- YYYY-MM-DD -->
**Verdict:** `APPROVED` | `CHANGES_REQUIRED`

---

## Slice Summary

<!-- 1-2 sentences: what this slice does. -->

## Documents Reviewed

| Document | Path | Exists | Frozen |
|---|---|---|---|
| PRD | `docs/slices/SLICE-XXXX/prd.md` | Yes / No | Yes / No / N/A |
| HLD | `docs/slices/SLICE-XXXX/hld.md` | Yes / No | Yes / No / N/A |
| LLD | `docs/slices/SLICE-XXXX/lld.md` | Yes / No | Yes / No / N/A |
| Test Plan | `docs/slices/SLICE-XXXX/test-plan.md` | Yes / No | Yes / No / N/A |
| Review | `docs/slices/SLICE-XXXX/review.md` | Yes / No | — |
| QA | `docs/slices/SLICE-XXXX/qa.md` | Yes / No | — |
| State | `docs/slices/SLICE-XXXX/state.md` | Yes / No | — |

## Code Reviewed

| Item | Value |
|---|---|
| PR Link | <!-- e.g., #42 --> |
| Commit Hash | <!-- e.g., abc1234 --> |
| Branch | <!-- e.g., slice/SLICE-XXXX --> |

## Spec-to-Code Traceability

<!-- Map every MUST requirement from the PRD to the code that implements it. -->

| PRD Requirement | LLD Section | Code Location | Verified |
|---|---|---|---|
| <!-- AC-01: When invoice is 7 days overdue... --> | Interfaces / API | `src/notifications/overdue.py:45` | Yes / No |
| <!-- AC-02: Email contains invoice ID... --> | Data Models | `src/notifications/templates.py:12` | Yes / No |

## State Machine Compliance

- [ ] All state transitions followed the allowed transitions in `STATE_MACHINE.md`
- [ ] No states were skipped
- [ ] `state.md` history matches actual artifact creation order
- [ ] `state.md` current state matches `index.md`

## Role Isolation Compliance

- [ ] No role performed another role's duties within this slice
- [ ] Engineer did not self-review
- [ ] Reviewer did not modify code
- [ ] No artifacts were modified by unauthorized roles
- [ ] Artifacts record role labels only, with no model names
- [ ] Role ownership handoffs in `state.md` match the required transitions

## Deviations Found

| # | Description | Severity | Resolution |
|---|---|---|---|
| <!-- 1 --> | <!-- e.g., LLD test case T-03 not implemented --> | Critical / Minor | <!-- Required / Waived with justification --> |

## Required Follow-Ups

<!-- Actions that must be completed before or after merge. "None" if clean. -->

- <!-- e.g., None -->
