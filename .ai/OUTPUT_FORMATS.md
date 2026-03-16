# Output Formats

This file defines the standardized output structure for each role. Every invocation must produce output matching the format for its role. These formats are designed to be copy-pasted directly into slice docs or PR comments.

Artifact metadata records role identity only. Use exactly one of these labels where applicable: `Planner`, `Architect`, `Engineer`, `Reviewer`, `QA`, `Auditor`. Do not append model names.

No free-form prose. No unstructured output. Every section is mandatory unless marked optional.

---

## Engineer Output Format

Used after: implementation work, change-request fixes.
Saved to: commit message body, or inline in PR description.

```markdown
## Engineer Output — SLICE-XXXX

### Summary
<!-- 1-2 sentences: what was implemented and which LLD sections it covers. -->

### Files Changed
| File | Action | LLD Section |
|---|---|---|
| `src/path/file.py` | Created / Modified | Section X.Y |
| `tests/path/test_file.py` | Created / Modified | Unit Test Cases |

### Tests Added
| LLD Test ID | Test Location | Status |
|---|---|---|
| T-01 | `tests/test_file.py::test_name` | PASS |
| T-02 | `tests/test_file.py::test_name` | PASS |

### Definition of Done Checklist
- [ ] All interfaces from LLD implemented
- [ ] All data models created
- [ ] All validation rules enforced
- [ ] All error handling contracts implemented
- [ ] All unit test cases from LLD written and passing
- [ ] No TODO/FIXME/HACK comments
- [ ] Code compiles/lints without errors
- [ ] No features beyond LLD scope

### State Transition Request
| Field | Value |
|---|---|
| Current State | `CODE_IN_PROGRESS` |
| Requested State | `CODE_COMPLETE` |
| Justification | All LLD requirements implemented and tests passing |
```

---

## Reviewer Output Format

Used after: code review.
Saved to: `docs/slices/SLICE-XXXX/review.md`

```markdown
## Reviewer Output — SLICE-XXXX

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | <!-- YYYY-MM-DD --> |
| Review Cycle | <!-- 1, 2, ... --> |
| LLD Reference | `docs/slices/SLICE-XXXX/lld.md` |

### Verdict: GO | CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | PASS / FAIL | <!-- evidence --> |
| R-02 | No unauthorized scope | PASS / FAIL | <!-- evidence --> |
| R-03 | Error handling | PASS / FAIL | <!-- evidence --> |
| R-04 | Tests present | PASS / FAIL | <!-- evidence --> |
| R-05 | Validation rules | PASS / FAIL | <!-- evidence --> |
| R-06 | No residual markers | PASS / FAIL | <!-- evidence --> |
| R-07 | Code compiles/lints | PASS / FAIL | <!-- evidence --> |
| R-08 | Security | PASS / FAIL | <!-- evidence --> |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | <!-- path --> | <!-- lines --> | Critical / Major / Minor | <!-- description --> | Required / Suggested |

### Rationale
<!-- 2-3 sentences: why GO or why CHANGES_REQUIRED. -->

### Required Actions (if CHANGES_REQUIRED)
1. <!-- Specific, actionable change -->
2. <!-- Specific, actionable change -->

### State Transition
| Field | Value |
|---|---|
| Current State | `REVIEW_REQUIRED` |
| Next State | `REVIEW_APPROVED` / `REVIEW_CHANGES` |
```

---

## QA Output Format

Used after: test execution and acceptance criteria validation.
Saved to: `docs/slices/SLICE-XXXX/qa.md`

```markdown
## QA Output — SLICE-XXXX

### QA Metadata
| Field | Value |
|---|---|
| QA | `QA` |
| Date | <!-- YYYY-MM-DD --> |
| QA Cycle | <!-- 1, 2, ... --> |
| PRD Reference | `docs/slices/SLICE-XXXX/prd.md` |
| Test Plan Reference | `docs/slices/SLICE-XXXX/test-plan.md` |

### Verdict: GO | CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| Q-01 | Acceptance criteria met | PASS / FAIL | <!-- evidence --> |
| Q-02 | Unit test coverage | PASS / FAIL | <!-- evidence --> |
| Q-03 | Edge cases tested | PASS / FAIL | <!-- evidence --> |
| Q-04 | Integration tests | PASS / FAIL / N/A | <!-- evidence --> |
| Q-05 | Regression safety | PASS / FAIL | <!-- evidence --> |
| Q-06 | No Critical/High defects | PASS / FAIL | <!-- evidence --> |
| Q-07 | Test data documented | PASS / FAIL | <!-- evidence --> |

### Test Execution Summary
| Layer | Total | Passed | Failed | Skipped |
|---|---|---|---|---|
| Unit | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |
| Integration | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |
| E2E | <!-- # --> | <!-- # --> | <!-- # --> | <!-- # --> |

### Acceptance Criteria Verification
| AC ID | Criterion | Test | Result |
|---|---|---|---|
| AC-01 | <!-- from prd.md --> | <!-- test name --> | PASS / FAIL |
| AC-02 | <!-- from prd.md --> | <!-- test name --> | PASS / FAIL |

### Edge Cases Tested
| ID | Edge Case | Expected Behavior | Result |
|---|---|---|---|
| EC-01 | <!-- description --> | <!-- expected --> | PASS / FAIL |

### Defects Found
| ID | Description | Severity | Status |
|---|---|---|---|
| D-01 | <!-- description --> | Critical / High / Medium / Low | Open / Fixed |

### Rationale
<!-- 2-3 sentences: why GO or why CHANGES_REQUIRED. -->

### Required Actions (if CHANGES_REQUIRED)
1. <!-- Specific defect to fix -->
2. <!-- Specific test to add -->

### State Transition
| Field | Value |
|---|---|
| Current State | `QA_REQUIRED` |
| Next State | `QA_APPROVED` / `QA_CHANGES` |
```

---

## Auditor Output Format

Used after: final compliance audit.
Saved to: `docs/slices/SLICE-XXXX/audit-report.md`

```markdown
## Auditor Output — SLICE-XXXX

### Audit Metadata
| Field | Value |
|---|---|
| Auditor | `Auditor` |
| Date | <!-- YYYY-MM-DD --> |
| PR Link | <!-- # or N/A --> |
| Commit Hash | <!-- abc1234 --> |

### Verdict: APPROVED | CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Evidence |
|---|---|---|---|
| A-01 | Traceability complete | PASS / FAIL | <!-- see table below --> |
| A-02 | All artifacts present | PASS / FAIL | <!-- file listing --> |
| A-03 | State transitions valid | PASS / FAIL | <!-- state.md history --> |
| A-04 | Role isolation | PASS / FAIL | <!-- role labels and state ownership evidence --> |
| A-05 | Review APPROVED | PASS / FAIL | <!-- review.md verdict --> |
| A-06 | QA APPROVED | PASS / FAIL | <!-- qa.md verdict --> |
| A-07 | Doc freeze respected | PASS / FAIL | <!-- git log / timestamps --> |
| A-08 | index.md in sync | PASS / FAIL | <!-- compared values --> |
| A-09 | CI green | PASS / FAIL | <!-- pipeline URL/status --> |
| A-10 | No open blockers | PASS / FAIL | <!-- state.md blockers --> |

### Artifact Inventory
| Artifact | Path | Exists |
|---|---|---|
| PRD | `docs/slices/SLICE-XXXX/prd.md` | Yes / No |
| HLD | `docs/slices/SLICE-XXXX/hld.md` | Yes / No |
| LLD | `docs/slices/SLICE-XXXX/lld.md` | Yes / No |
| Review | `docs/slices/SLICE-XXXX/review.md` | Yes / No |
| QA | `docs/slices/SLICE-XXXX/qa.md` | Yes / No |
| Test Plan | `docs/slices/SLICE-XXXX/test-plan.md` | Yes / No |
| Audit Report | `docs/slices/SLICE-XXXX/audit-report.md` | Yes / No |

### Spec-to-Code Traceability
| PRD Requirement | LLD Section | Code Location | Verified |
|---|---|---|---|
| AC-01: <!-- criterion --> | <!-- section --> | `file.py:line` | Yes / No |
| AC-02: <!-- criterion --> | <!-- section --> | `file.py:line` | Yes / No |

### State Machine Compliance
- [ ] All transitions in `state.md` are allowed per `STATE_MACHINE.md`
- [ ] No states were skipped
- [ ] Transition history timestamps are sequential
- [ ] Current state matches `index.md`

### Role Isolation Compliance
- [ ] Engineer did not self-review
- [ ] Reviewer did not modify code
- [ ] No role performed another role's duties
- [ ] Artifacts record only the correct role labels
- [ ] Role ownership transitions are distinct and valid in `state.md`

### Rationale
<!-- 2-3 sentences: why APPROVED or why CHANGES_REQUIRED. -->

### Required Follow-Ups (if CHANGES_REQUIRED)
1. <!-- Specific compliance gap -->
2. <!-- Specific missing artifact -->

### State Transition
| Field | Value |
|---|---|
| Current State | `AUDIT_REQUIRED` |
| Next State | `AUDIT_APPROVED` |
```

---

## Planner Output Format

Used after: slice scoping.
Saved to: `docs/slices/SLICE-XXXX/prd.md`

The Planner output IS the PRD. Use the template at `docs/slices/_templates/prd.md` as the exact format. No additional wrapper is needed.

---

## Architect Output Format

Used after: design work.
Saved to: `docs/slices/SLICE-XXXX/hld.md` or `docs/slices/SLICE-XXXX/lld.md`

The Architect output IS the HLD or LLD. Use the templates at `docs/slices/_templates/hld.md` and `docs/slices/_templates/lld.md` as the exact formats. No additional wrapper is needed.

---

## Format Enforcement Rules

1. **Every section is mandatory.** If a section is not applicable, write `N/A` with a one-line justification. Do not omit the section.
2. **Tables must be complete.** Every row must be filled. No empty cells. Use `—` for truly empty values.
3. **Verdicts are exact strings.** Use `GO`, `CHANGES_REQUIRED`, `APPROVED`, or `CHANGES_REQUESTED` exactly. No synonyms, no qualifiers.
4. **Evidence is required.** Every rubric result must cite a file path, line number, test name, or observable fact. "Looks good" is not evidence.
5. **State transition requests are explicit.** The output must state the current state and the requested next state. The orchestrator uses this to update `state.md`.
6. **Formats are not customizable per-slice.** These formats apply to every slice uniformly.
