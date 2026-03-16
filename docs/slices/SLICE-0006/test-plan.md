# Test Plan: SLICE-0006 — Dashboard Read Model

**Author:** Codex
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0006/prd.md`
**LLD Reference:** `docs/slices/SLICE-0006/lld.md`

---

## Test Scope

| Layer | In Scope | Notes |
|---|---|---|
| Unit | Yes | Shared-domain aggregation logic in `DefaultDashboardReadModelService` and dashboard read-model data shaping. |
| Integration | No | Slice is read-model only and uses in-memory test doubles rather than real cross-module integration wiring. |
| E2E | No | No UI, network, or end-to-end flow exists in this slice. |
| Manual | No | All acceptance criteria are covered by deterministic shared tests. |

## Test Data Requirements

| Data Set | Description | Source |
|---|---|---|
| Logging entries | Positive intake and negative burn entries across same-day and multi-day windows | Test factory helpers in `DefaultDashboardReadModelServiceTest` |
| Debt result fixtures | Explicit debt summaries and out-of-order debt days for chart verification | `StubDebtCalculator` in `DefaultDashboardReadModelServiceTest` |
| Weight history fixtures | Deterministic weight entries with fixed timestamps | `FakeWeightHistoryService` and `weightEntry(...)` helper |
| Profile metrics | Static `BodyMetrics(height=175.0, currentWeightKg=90.0, goalWeightKg=70.0)` | `createService(...)` default fixture |

## Test Cases

### Unit Tests

| ID | Test Case | Status |
|---|---|---|
| T-01 | Snapshot generation with all data available | PASS |
| T-02 | Intake aggregation sums all positive calorie entries | PASS |
| T-03 | Burn aggregation sums absolute values of all negative calorie entries | PASS |
| T-04 | Net calories equals intake minus burn | PASS |
| T-05 | Remaining calories equals target minus intake | PASS |
| T-06 | Weight progress computed correctly | PASS |
| T-07 | Chart dataset maps debt days to chart points chronologically | PASS |
| T-08 | Deterministic snapshot: two calls with same inputs yield identical results | PASS |
| T-09 | Empty logging day returns zero totals | PASS |
| T-10 | Multi-day chart data covers the full window | PASS |

### Integration Tests

| ID | Test Case | Components Involved | Status |
|---|---|---|---|
| N/A | No integration tests applicable for this slice | N/A | N/A |

### Edge Cases

| ID | Edge Case | Expected Behavior | Status |
|---|---|---|---|
| EC-01 | No calorie entries for today | Snapshot returns zeroed `TodaySummary` totals with intact target/remaining fields | PASS |
| EC-02 | Debt days arrive out of chronological order | Chart points are sorted by ascending date before returning | PASS |
| EC-03 | Sparse multi-day logging within a 7-day chart window | Chart output still covers the full calculator-produced window deterministically | PASS |

### Regression Focus

- Shared compile/test health for existing common-domain slices via `./gradlew --no-daemon clean test`
- Dashboard package boundary purity: no UI imports, network imports, persistence writes, or upstream domain mutations

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|
| None | No QA defects found during execution. | N/A | N/A |

## Exit Criteria for QA_APPROVED

All of the following must be true:

- [x] All unit tests from LLD are passing
- [x] All integration tests (if in scope) are passing
- [x] All edge cases are tested
- [x] No Critical or High severity defects remain open
- [x] All acceptance criteria from `prd.md` are verified
- [x] Regression areas show no new failures
