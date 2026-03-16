# Test Plan: SLICE-0002 — Calorie Debt Engine

**Author:** QA
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0002/prd.md`
**LLD Reference:** `docs/slices/SLICE-0002/lld.md`

---

## Test Scope

| Layer | In Scope | Notes |
|---|---|---|
| Unit | Yes | Calculation logic, trend, severity, streaks |
| Integration | No | N/A for pure domain logic |
| E2E | No | N/A for pure domain logic |
| Manual | No | Pure domain logic is fully unit tested |

## Test Data Requirements

| Data Set | Description | Source |
|---|---|---|
| Daily Entries | Various sequences of daily calorie intakes (over, under target) | Unit Test Fixtures |

## Test Cases

### Unit Tests

| ID | LLD Requirement | Executed Test Case | Status |
|---|---|---|---|
| T-01 | Over-target day creates debt | `overTargetDayCreatesDebt` | PASS |
| T-02 | Under-target day with no prior debt stays at zero | `underTargetWithNoDebtStaysZero` | PASS |
| T-03 | Under-target day reduces existing debt but does not go negative | `underTargetReducesDebtWithoutGoingNegative` | PASS |
| T-04 | Missing date inside range produces zero-consumption row | `missingDateProducesZeroConsumptionRow` | PASS |
| T-05 | Duplicate dates are rejected | `duplicateDatesAreRejected` | PASS |
| T-06 | Inverted range is rejected | `invertedRangeIsRejected` | PASS |
| T-07 | Negative consumed calories are rejected | `negativeConsumedCaloriesAreRejected` | PASS |
| T-08 | Entries outside the range are ignored | `entriesOutsideRangeAreIgnored` | PASS |
| T-09 | Severity thresholds map correctly | `severityThresholdsMapCorrectly` | PASS |
| T-10 | Debt streak counts trailing days with ending debt above zero | `debtStreakCountsTrailingPositiveDebtWithPositiveDelta` | PASS |

### Integration Tests

| ID | Test Case | Components Involved | Status |
|---|---|---|---|
| IT-01 | N/A | N/A | SKIP |

### Edge Cases

| ID | Edge Case | Expected Behavior | Status |
|---|---|---|---|
| EC-01 | Inverted Date Range | Returns Failure with `INVALID_DATE_RANGE` | PASS |
| EC-02 | Missing dates in window | Generates zero consumption row | PASS |
| EC-03 | Negative consumed calories | Returns Failure with `INVALID_CONSUMED_CALORIES` | PASS |

### Regression Focus

- Base `CalorieDebtResult` dependencies
- Multiplatform date calculations

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|

## Exit Criteria for QA_APPROVED

All of the following must be true:

- [x] All unit tests from LLD are passing
- [x] All integration tests (if in scope) are passing
- [x] All edge cases are tested
- [x] No Critical or High severity defects remain open
- [x] All acceptance criteria from `prd.md` are verified
- [x] Regression areas show no new failures
