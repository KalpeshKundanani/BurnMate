# Go / No-Go Rubrics

This file defines the evaluation criteria for every gate in the slice lifecycle where a verdict is required. Each rubric produces exactly one of two outcomes:

- **GO** — The slice passes the gate and advances to the next state.
- **CHANGES_REQUIRED** — The slice is sent back for corrections.

There is no "partial pass." There is no "pass with caveats." The rubric is binary.

---

## Reviewer Rubric

**Gate:** `REVIEW_REQUIRED → REVIEW_APPROVED` or `REVIEW_REQUIRED → REVIEW_CHANGES`
**Evaluator:** Reviewer role
**Source of truth:** `lld.md` (the spec the code must match)

### Criteria

| # | Criterion | Weight | How to Evaluate |
|---|---|---|---|
| R-01 | **Spec alignment** | Critical | Every interface, function signature, data model, and validation rule in `lld.md` has a corresponding implementation. No missing items. |
| R-02 | **No unauthorized scope** | Critical | No code exists that is not specified in `lld.md`. No extra endpoints, functions, helpers, or features. |
| R-03 | **Error handling** | Major | Every error condition in the LLD "Error Handling Contracts" table is implemented with the specified HTTP status, error code, and recovery behavior. |
| R-04 | **Tests present** | Major | Every test case in the LLD "Unit Test Cases" table has a corresponding test. Test inputs and expected outputs match the LLD exactly. |
| R-05 | **Validation rules** | Major | Every validation rule in the LLD "Validation Rules" table is enforced in code with the specified error code. |
| R-06 | **No residual markers** | Minor | No `TODO`, `FIXME`, `HACK`, `XXX`, or `TEMP` comments in slice code. |
| R-07 | **Code compiles/lints** | Critical | Code compiles (or passes linting for interpreted languages) without errors. |
| R-08 | **Security** | Major | No hardcoded secrets, no SQL injection vectors, no unvalidated user input passed to dangerous operations. |

### Verdict Rules

| Condition | Verdict |
|---|---|
| All Critical criteria pass AND all Major criteria pass | **GO** |
| Any Critical criterion fails | **CHANGES_REQUIRED** |
| 1+ Major criteria fail but all Critical pass | **CHANGES_REQUIRED** |
| Only Minor criteria fail, all Critical and Major pass | **GO** (with advisory notes) |

### Required Output

```markdown
## Verdict: GO | CHANGES_REQUIRED

## Criteria Results
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | PASS / FAIL | ... |
| R-02 | No unauthorized scope | PASS / FAIL | ... |
| ... | ... | ... | ... |

## Rationale
<!-- 2-3 sentences explaining the verdict. -->

## Required Actions (if CHANGES_REQUIRED)
1. <!-- Specific, actionable fix -->
2. <!-- Specific, actionable fix -->
```

---

## QA Rubric

**Gate:** `QA_REQUIRED → QA_APPROVED` or `QA_REQUIRED → QA_CHANGES`
**Evaluator:** QA role
**Source of truth:** `prd.md` (acceptance criteria) + `test-plan.md` (test cases)

### Criteria

| # | Criterion | Weight | How to Evaluate |
|---|---|---|---|
| Q-01 | **Acceptance criteria met** | Critical | Every acceptance criterion in `prd.md` is tested and passes. Map AC-XX → test → result. |
| Q-02 | **Unit test coverage** | Major | Every LLD unit test case (T-XX) is implemented and passing. No missing test cases. |
| Q-03 | **Edge cases tested** | Major | Edge cases identified in `test-plan.md` are tested. Results recorded. |
| Q-04 | **Integration tests** | Major (if applicable) | If `test-plan.md` scope includes integration tests, they are executed and passing. |
| Q-05 | **Regression safety** | Major | Existing test suites outside this slice show no new failures. |
| Q-06 | **No Critical/High defects** | Critical | Zero open defects at Critical or High severity. Medium/Low may be documented. |
| Q-07 | **Test data documented** | Minor | Test data requirements from `test-plan.md` are satisfied. Fixtures or factories exist. |

### Verdict Rules

| Condition | Verdict |
|---|---|
| All Critical criteria pass AND all Major criteria pass | **GO** |
| Any Critical criterion fails | **CHANGES_REQUIRED** |
| 1+ Major criteria fail but all Critical pass | **CHANGES_REQUIRED** |
| Only Minor criteria fail, all Critical and Major pass | **GO** (with advisory notes) |

### Required Output

```markdown
## Verdict: GO | CHANGES_REQUIRED

## Criteria Results
| # | Criterion | Result | Notes |
|---|---|---|---|
| Q-01 | Acceptance criteria met | PASS / FAIL | ... |
| Q-02 | Unit test coverage | PASS / FAIL | ... |
| ... | ... | ... | ... |

## Acceptance Criteria Map
| AC ID | Criterion | Test | Result |
|---|---|---|---|
| AC-01 | ... | test_xyz | PASS / FAIL |

## Rationale
<!-- 2-3 sentences explaining the verdict. -->

## Required Actions (if CHANGES_REQUIRED)
1. <!-- Specific defect to fix -->
2. <!-- Specific test to add -->
```

---

## Auditor Rubric

**Gate:** `AUDIT_REQUIRED → AUDIT_APPROVED`
**Evaluator:** Auditor role
**Source of truth:** All slice artifacts + `STATE_MACHINE.md` + `ROLES.md`

### Criteria

| # | Criterion | Weight | How to Evaluate |
|---|---|---|---|
| A-01 | **Traceability complete** | Critical | Every MUST requirement in `prd.md` traces to an LLD section, which traces to a code location. Table is complete with no gaps. |
| A-02 | **All artifacts present** | Critical | `prd.md`, `hld.md`, `lld.md`, `review.md`, `qa.md`, `test-plan.md` all exist in the slice folder. |
| A-03 | **State transitions valid** | Critical | `state.md` history shows only allowed transitions per `STATE_MACHINE.md`. No skipped states. |
| A-04 | **Role isolation maintained** | Critical | No evidence that a role performed another role's duties. Engineer did not self-review. Reviewer did not modify code. |
| A-05 | **Review verdict is APPROVED** | Critical | `review.md` final verdict is `APPROVED`. |
| A-06 | **QA verdict is APPROVED** | Critical | `qa.md` final verdict is `APPROVED`. |
| A-07 | **Doc freeze respected** | Major | `prd.md`, `hld.md`, `lld.md` were not modified after `LLD_DEFINED`. If modified, `change-request.md` exists with approval. |
| A-08 | **index.md in sync** | Major | `state.md` current state matches the entry in `docs/slices/index.md`. |
| A-09 | **CI green** | Major | All CI checks pass (validators, tests). No pipeline failures. |
| A-10 | **No open blockers** | Critical | `state.md` blocking issues field is "None" or all blockers are resolved. |

### Verdict Rules

| Condition | Verdict |
|---|---|
| All Critical criteria pass AND all Major criteria pass | **GO** (APPROVED) |
| Any Critical criterion fails | **CHANGES_REQUIRED** (REJECTED) |
| 1+ Major criteria fail but all Critical pass | **CHANGES_REQUIRED** (REJECTED) |

There is no conditional approval at audit. The slice either passes all criteria or it does not merge.

### Required Output

```markdown
## Verdict: APPROVED | CHANGES_REQUIRED

## Criteria Results
| # | Criterion | Result | Evidence |
|---|---|---|---|
| A-01 | Traceability complete | PASS / FAIL | Traceability table below |
| A-02 | All artifacts present | PASS / FAIL | File listing |
| A-03 | State transitions valid | PASS / FAIL | state.md history |
| A-04 | Role isolation | PASS / FAIL | Commit authorship |
| A-05 | Review APPROVED | PASS / FAIL | review.md verdict |
| A-06 | QA APPROVED | PASS / FAIL | qa.md verdict |
| A-07 | Doc freeze respected | PASS / FAIL | Git history / timestamps |
| A-08 | index.md in sync | PASS / FAIL | Compared values |
| A-09 | CI green | PASS / FAIL | Pipeline status |
| A-10 | No open blockers | PASS / FAIL | state.md blockers field |

## Traceability Table
| PRD Requirement | LLD Section | Code Location | Verified |
|---|---|---|---|
| AC-01: ... | Section X | file.py:line | Yes / No |

## Rationale
<!-- 2-3 sentences explaining the verdict. -->

## Required Follow-Ups (if CHANGES_REQUIRED)
1. <!-- Specific compliance gap to fix -->
2. <!-- Specific missing artifact -->
```

---

## Rubric Usage Rules

1. **Rubrics are mandatory.** Every Reviewer, QA, and Auditor invocation must evaluate against the applicable rubric. Producing a verdict without evaluating each criterion is invalid.
2. **Every criterion must be evaluated.** Skipping a criterion is not permitted. If a criterion is not applicable (e.g., no integration tests), mark it `N/A` with justification.
3. **Evidence is required.** Each criterion result must include evidence: file paths, line numbers, test names, or specific observations. "Looks fine" is not evidence.
4. **Verdicts are final for the cycle.** Once a verdict is issued, it stands until the Engineer addresses the required actions and resubmits.
5. **Rubrics cannot be modified per-slice.** These criteria apply uniformly. A slice does not get a custom rubric.
