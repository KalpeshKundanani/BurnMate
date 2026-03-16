# Test Plan: SLICE-0004 — Daily Logging Domain + Persistence

**Author:** Codex
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0004/prd.md`
**LLD Reference:** `docs/slices/SLICE-0004/lld.md`

---

## Test Scope

| Layer | In Scope | Notes |
|---|---|---|
| Unit | Yes | Shared logging domain validation, factory, and in-memory repository behavior in `commonTest` |
| Integration | No | No external storage, network, or platform adapter integration is in scope for this slice |
| E2E | No | No UI or end-user workflow exists in this slice |
| Manual | No | Slice scope is fully covered by deterministic shared-code automated tests |

## Test Data Requirements

| Data Set | Description | Source |
|---|---|---|
| Valid entry inputs | `EntryDate(2026-03-15)` with calorie amounts `1500`, `0`, and `15000` | Inline test fixtures in `DefaultCalorieEntryFactoryTest` and `DefaultCalorieEntryValidatorTest` |
| Invalid calorie inputs | Calorie amounts `-1` and `15001` | Inline test fixtures in validator and factory tests |
| Repository fixtures | Deterministic `CalorieEntry` instances with fixed IDs, dates, and timestamps | Inline factory helpers in `LocalEntryRepositoryTest` |
| Date-range fixtures | Inclusive ranges `2026-03-13..2026-03-15` and inverted range `2026-03-15..2026-03-10` | Inline test fixtures in `LocalEntryRepositoryTest` |

## Test Cases

### Unit Tests

| ID | Test Case | Status |
|---|---|---|
| T-01 | Valid entry creation succeeds via `DefaultCalorieEntryFactoryTest.validEntryCreationSucceeds` | PASS |
| T-02 | Negative calorie amount is rejected via `DefaultCalorieEntryValidatorTest.negativeCalorieAmountIsRejected` | PASS |
| T-03 | Unrealistic calorie amount is rejected via `DefaultCalorieEntryValidatorTest.unrealisticCalorieAmountIsRejected` and `DefaultCalorieEntryFactoryTest.unrealisticCalorieAmountIsRejectedDuringCreation` | PASS |
| T-04 | Deleting an existing entry removes it from repository via `LocalEntryRepositoryTest.deletingAnExistingEntryRemovesItFromRepository` | PASS |
| T-05 | Deleting a non-existent entry returns false via `LocalEntryRepositoryTest.deletingANonExistentEntryReturnsFalse` | PASS |
| T-06 | Fetch by date range returns entries in chronological order via `LocalEntryRepositoryTest.fetchByDateRangeReturnsEntriesInChronologicalOrder` | PASS |
| T-07 | Fetch for empty date range returns empty list via `LocalEntryRepositoryTest.fetchForEmptyDateRangeReturnsEmptyList` | PASS |
| T-08 | Fetch with inverted date range is rejected via `LocalEntryRepositoryTest.invertedDateRangeIsRejected` | PASS |
| T-09 | Duplicate entry creation is rejected via `LocalEntryRepositoryTest.duplicateEntryCreationIsRejected` | PASS |
| T-10 | Boundary calorie amounts are accepted via `DefaultCalorieEntryValidatorTest.boundaryCalorieAmountsAreAccepted` and `DefaultCalorieEntryFactoryTest.boundaryCalorieAmountsCreateSuccessfully` | PASS |

### Integration Tests

| ID | Test Case | Components Involved | Status |
|---|---|---|---|
| IT-01 | Not applicable for this slice | N/A | SKIP |

### Edge Cases

| ID | Edge Case | Expected Behavior | Status |
|---|---|---|---|
| EC-01 | Negative calorie amount | Returns `INVALID_CALORIE_AMOUNT` and does not create an entry | PASS |
| EC-02 | Calorie amount above 15,000 | Returns `UNREALISTIC_CALORIE_AMOUNT` and does not create an entry | PASS |
| EC-03 | Inverted fetch range | Returns `INVALID_DATE_RANGE` instead of partial data | PASS |
| EC-04 | Duplicate entry ID | Returns `DUPLICATE_ENTRY` on second create | PASS |
| EC-05 | Empty repository/date range | Returns `emptyList()` instead of an error | PASS |

### Regression Focus

- Shared calorie logging domain package under `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging`
- Shared logging tests under `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging`
- Build health for the existing multiplatform app via `./gradlew --no-daemon assembleDebug` and `./gradlew --no-daemon clean test`

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|
| D-01 | No defects found during QA execution. | Low | Fixed |

## Exit Criteria for QA_APPROVED

All of the following are true:

- [x] All unit tests from LLD are passing
- [x] All integration tests (if in scope) are passing
- [x] All edge cases are tested
- [x] No Critical or High severity defects remain open
- [x] All acceptance criteria from `prd.md` are verified
- [x] Regression areas show no new failures
