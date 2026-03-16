# QA Report: SLICE-XXXX — <!-- Slice Name -->

**QA:** `QA`
**Date:** <!-- YYYY-MM-DD -->
**QA Cycle:** 1 <!-- Increment on each QA_CHANGES -> QA_REQUIRED loop -->
**Verdict:** `APPROVED` | `CHANGES_REQUESTED`

---

## References

- **PRD:** `docs/slices/SLICE-XXXX/prd.md`
- **LLD:** `docs/slices/SLICE-XXXX/lld.md`
- **Test Plan:** `docs/slices/SLICE-XXXX/test-plan.md`

## Test Execution Summary

| Layer | Total | Passed | Failed | Skipped |
|---|---|---|---|---|
| Unit | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |
| Integration | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |
| E2E | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |
| Manual | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |

## Acceptance Criteria Verification

<!-- Map each AC from prd.md to pass/fail. -->

| AC ID | Criterion | Result | Evidence |
|---|---|---|---|
| AC-01 | <!-- From prd.md --> | PASS / FAIL | <!-- Test ID or manual verification note --> |
| AC-02 | <!-- From prd.md --> | PASS / FAIL | <!-- Evidence --> |

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|
| D-01 | <!-- Description --> | Critical / High / Medium / Low | Open / Fixed |

## Regression Check

- [ ] Existing test suites pass with no new failures
- [ ] No regressions detected in areas listed in test-plan.md

## Change Requests (if CHANGES_REQUESTED)

<!-- Specific defects the Engineer must fix before re-QA. -->

1. <!-- Change request 1 -->

---

**Rule:** QA must not modify code or design documents. All defects are communicated through this document. The Engineer addresses them and resubmits.
