# PRD: SLICE-XXXX — <!-- Slice Name -->

**Author:** `Planner`
**Date:** <!-- YYYY-MM-DD -->
**Status:** DRAFT | APPROVED

---

## Problem Statement

<!-- 2-3 sentences. What problem does this slice solve? Why does it matter now? -->

## Users

<!-- Who is affected? Be specific: end users, internal services, other slices. -->

## Non-Goals

<!-- What this slice explicitly does NOT do. Be precise to prevent scope creep. -->

- <!-- Non-goal 1 -->
- <!-- Non-goal 2 -->

## Success Metrics

<!-- How do we know this slice succeeded? Must be measurable. -->

| Metric | Target |
|---|---|
| <!-- e.g., API response time --> | <!-- e.g., < 200ms p95 --> |

## Constraints

<!-- Technical, business, or regulatory constraints. -->

- <!-- Constraint 1 -->
- <!-- Constraint 2 -->

## UX Notes

<!-- If this slice has a user-facing component. Remove section if purely backend. -->

## Functional Requirements

### MUST

- [ ] <!-- Requirement. Use imperative voice. -->
- [ ] <!-- Requirement -->

### SHOULD

- [ ] <!-- Requirement -->

### COULD

- [ ] <!-- Requirement -->

## Acceptance Criteria

<!-- Each criterion must be independently testable. -->

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | <!-- When X, then Y --> | Yes |
| AC-02 | <!-- When X, then Y --> | Yes |

## Out of Scope

<!-- Anything that might be confused as in-scope but isn't. -->

- <!-- Item 1 -->

## Open Questions

<!-- Unresolved items. Must be resolved before state transitions to HLD_DEFINED. -->

| # | Question | Status |
|---|---|---|
| 1 | <!-- Question --> | OPEN |

---

## Example (for reference — delete this section in actual slices)

**PRD: SLICE-0003 — Email Notification on Invoice Overdue**

**Problem Statement:** Customers with overdue invoices are not notified, leading to delayed payments and manual follow-up by the finance team.

**Users:** End customers with active accounts; finance team (receives fewer manual escalations).

**Non-Goals:**
- SMS notifications (future slice)
- Payment processing changes

**Functional Requirements — MUST:**
- [ ] System sends an email when an invoice is 7 days overdue
- [ ] Email includes invoice ID, amount, and due date
- [ ] Email uses the approved template from the design system

**Acceptance Criteria:**
| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | When an invoice is 7 days past due, an email is sent to the customer within 1 hour | Yes |
| AC-02 | Email body contains invoice ID, amount, and original due date | Yes |
| AC-03 | No email is sent if the invoice is already paid | Yes |
