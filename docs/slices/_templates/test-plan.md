# Test Plan: SLICE-XXXX — <!-- Slice Name -->

**Author:** `QA`
**Date:** <!-- YYYY-MM-DD -->
**PRD Reference:** `docs/slices/SLICE-XXXX/prd.md`
**LLD Reference:** `docs/slices/SLICE-XXXX/lld.md`

---

## Test Scope

| Layer | In Scope | Notes |
|---|---|---|
| Unit | Yes / No | <!-- What is covered --> |
| Integration | Yes / No | <!-- What is covered --> |
| E2E | Yes / No | <!-- What is covered --> |
| Manual | Yes / No | <!-- What requires manual verification --> |

## Test Data Requirements

| Data Set | Description | Source |
|---|---|---|
| <!-- e.g., Valid invoice --> | <!-- e.g., Invoice with all required fields --> | Fixture / Factory / Seed |

## Test Cases

### Unit Tests

<!-- Reference LLD test cases. Add any additional cases discovered during QA. -->

| ID | Test Case | Status |
|---|---|---|
| T-01 | <!-- From LLD --> | PASS / FAIL / SKIP |
| T-02 | <!-- From LLD --> | PASS / FAIL / SKIP |

### Integration Tests

| ID | Test Case | Components Involved | Status |
|---|---|---|---|
| IT-01 | <!-- Description --> | <!-- e.g., API + DB --> | PASS / FAIL / SKIP |

### Edge Cases

| ID | Edge Case | Expected Behavior | Status |
|---|---|---|---|
| EC-01 | <!-- e.g., Empty input --> | <!-- e.g., Returns 400 --> | PASS / FAIL |
| EC-02 | <!-- e.g., Max length input --> | <!-- e.g., Accepted --> | PASS / FAIL |

### Regression Focus

<!-- Areas of the codebase that might break due to this slice's changes. -->

- <!-- e.g., Existing invoice list endpoint -->
- <!-- e.g., Shared validation library -->

## Defects Found

| ID | Description | Severity | Status |
|---|---|---|---|
| <!-- D-01 --> | <!-- Description --> | Critical / High / Medium / Low | Open / Fixed |

## Exit Criteria for QA_APPROVED

All of the following must be true:

- [ ] All unit tests from LLD are passing
- [ ] All integration tests (if in scope) are passing
- [ ] All edge cases are tested
- [ ] No Critical or High severity defects remain open
- [ ] All acceptance criteria from `prd.md` are verified
- [ ] Regression areas show no new failures
