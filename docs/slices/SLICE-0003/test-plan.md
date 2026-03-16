# Test Plan: SLICE-0003 — User Profile + Goal Domain

**Author:** Codex
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0003/prd.md`
**LLD Reference:** `docs/slices/SLICE-0003/lld.md`

---

## Test Scope

| Layer | In Scope | Notes |
|---|---|---|
| Unit | Yes | Shared-domain validation, BMI calculation, healthy-goal evaluation, and aggregate mapping in the profile package |
| Integration | No | Slice is pure domain only; no persistence, network, or cross-module integration contract exists in scope |
| E2E | No | No UI or flow orchestration exists in this slice |
| Manual | No | All acceptance criteria are deterministic and covered by automated verification |

## Test Data Requirements

| Data Set | Description | Source |
|---|---|---|
| DS-01 | Valid weight-loss profile: `heightCm=175.0`, `currentWeightKg=82.0`, `goalWeightKg=72.0` | Inline unit test fixture |
| DS-02 | Invalid height profile: `heightCm=0.0`, `currentWeightKg=82.0`, `goalWeightKg=72.0` | Inline unit test fixture |
| DS-03 | Invalid current weight profile: `heightCm=175.0`, `currentWeightKg=-1.0`, `goalWeightKg=72.0` | Inline unit test fixture |
| DS-04 | Non-weight-loss goals: `goalWeightKg=82.0` and `goalWeightKg=90.0` with current weight `82.0` | Inline unit test fixture |
| DS-05 | Deterministic BMI inputs: `heightCm=180.0`, `currentWeightKg=90.0`, `goalWeightKg=78.0` | Inline unit test fixture |
| DS-06 | Unsafe low-BMI goal: `heightCm=175.0`, `currentWeightKg=70.0`, `goalWeightKg=54.0` | Inline unit test fixture |
| DS-07 | Healthy-range goal: `heightCm=175.0`, `currentWeightKg=84.0`, `goalWeightKg=72.0` | Inline unit test fixture |
| DS-08 | Derived-helper verification: `heightCm=165.0`, `currentWeightKg=78.0`, `goalWeightKg=65.0` | Inline unit test fixture |

## Test Cases

### Unit Tests

| ID | Test Case | Status |
|---|---|---|
| T-01 | Valid profile creates summary | PASS |
| T-02 | Zero height is rejected | PASS |
| T-03 | Negative current weight is rejected | PASS |
| T-04 | Goal weight equal to current weight is rejected | PASS |
| T-05 | Goal weight above current weight is rejected | PASS |
| T-06 | BMI helper returns deterministic current and goal values | PASS |
| T-07 | Goal BMI below healthy range is rejected | PASS |
| T-08 | Goal BMI inside healthy range succeeds | PASS |
| T-09 | BMI categories map correctly at thresholds | PASS |
| T-10 | Derived helpers include kilograms-to-lose and BMI delta | PASS |

### Integration Tests

| ID | Test Case | Components Involved | Status |
|---|---|---|---|
| IT-01 | Not applicable for this pure-domain slice | N/A | N/A |

### Edge Cases

| ID | Edge Case | Expected Behavior | Status |
|---|---|---|---|
| EC-01 | Zero height input | Return `INVALID_HEIGHT` domain failure with no summary | PASS |
| EC-02 | Negative current weight input | Return `INVALID_CURRENT_WEIGHT` domain failure with no summary | PASS |
| EC-03 | Goal equal to or above current weight | Return `GOAL_NOT_BELOW_CURRENT_WEIGHT` domain failure | PASS |
| EC-04 | Goal BMI below `18.5` | Return `GOAL_BMI_BELOW_HEALTHY_RANGE` domain failure | PASS |
| EC-05 | BMI classification thresholds `18.4`, `18.5`, `24.9`, `25.0`, `30.0` | Classify deterministically to `UNDERWEIGHT`, `HEALTHY`, `HEALTHY`, `OVERWEIGHT`, `OBESE` | PASS |

### Regression Focus

- Shared `commonMain` compilation for the KMP module via `./gradlew --no-daemon assembleDebug`
- Existing unit suites outside the slice via `./gradlew --no-daemon clean test`
- Slice-scope architecture hygiene via residual-marker and forbidden-import scans in the profile package

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|
| D-01 | No functional or structural defects found during QA execution | Low | Fixed |

## Exit Criteria for QA_APPROVED

All of the following must be true:

- [x] All unit tests from LLD are passing
- [x] All integration tests (if in scope) are passing
- [x] All edge cases are tested
- [x] No Critical or High severity defects remain open
- [x] All acceptance criteria from `prd.md` are verified
- [x] Regression areas show no new failures
