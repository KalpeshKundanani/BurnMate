# Phase 4 — Validation Scripts

This document describes the deterministic validation scripts that enforce the AI Development Framework governance rules.

## Overview

| Script | Enforces | Source of Truth |
|---|---|---|
| `validate_slice_registry.py` | index.md ↔ disk consistency | `docs/slices/index.md` rules |
| `validate_required_artifacts.py` | Artifacts exist per state | `STATE_MACHINE.md` Required Artifacts table |
| `validate_doc_freeze.py` | Frozen docs not modified | `OPERATING_PRINCIPLES.md` Principle #13 |
| `validate_pr_checklist.py` | PR template structure | `.github/pull_request_template.md` |
| `validate_all.sh` | Runs all above in sequence | — |

All scripts use Python 3.12+ stdlib only. No external dependencies.

## How to Run Locally

```bash
# Run all validators
./scripts/validate_all.sh

# Run individual validators
python3 scripts/validate_slice_registry.py
python3 scripts/validate_required_artifacts.py
python3 scripts/validate_doc_freeze.py
python3 scripts/validate_pr_checklist.py

# Run tests
python3 -m unittest scripts.tests.test_validators -v
```

## Exit Codes

| Code | Meaning |
|---|---|
| 0 | All checks passed |
| 1 | One or more violations found |
| 2 | Configuration or runtime error |

## What Each Validator Checks

### validate_slice_registry.py

- Every `SLICE-NNNN` folder on disk has a matching row in `index.md`
- Every row in `index.md` has a matching folder on disk
- Owner Role values match the vocabulary in `.ai/ROLES.md`
- Slice folder links are valid relative paths
- Slice IDs follow `SLICE-NNNN` naming convention

### validate_required_artifacts.py

- Reads `state.md` in each slice to determine current state
- Checks that all artifacts required for that state exist (per the `STATE_MACHINE.md` table)
- Validates minimal structural correctness (top-level heading pattern)
- Detects invalid or unrecognized states

### validate_doc_freeze.py

- Identifies slices at `LLD_DEFINED` or later (frozen)
- Verifies `prd.md`, `hld.md`, and `lld.md` exist
- If git is available: checks if frozen files were modified after the freeze commit
- If modified post-freeze: requires `change-request.md` to be present

### validate_pr_checklist.py

- Validates the PR template (offline) or actual PR body (CI via `PR_BODY` env var)
- Checks for required sections: Slice, Registration, Artifacts, Implementation, Review, QA, Audit, Context Capsule, Summary, Changes
- Checks for required checkbox items referencing key artifacts
- Checks for required file references (`state.md`, `index.md`, `lld.md`, `prd.md`)

## Common Failure Fixes

| Failure | Fix |
|---|---|
| Slice folder missing from index | Add a row to `docs/slices/index.md` |
| Index entry missing folder | Create `docs/slices/SLICE-NNNN/` with `state.md` |
| Invalid Owner Role | Use one of: Planner, Architect, Engineer, Reviewer, QA, Auditor |
| Missing artifact for state | Create from `_templates/` or rollback state in `state.md` |
| Invalid state value | Use one of the 15 states from `STATE_MACHINE.md` |
| Frozen doc modified without CR | File `change-request.md` in the slice folder |
| Missing frozen doc | Create the doc or rollback state to before `LLD_DEFINED` |
| PR template section missing | Add the missing section/checkbox to the PR template |

## CI Integration

The GitHub Actions workflow `.github/workflows/validators.yml`:

- Triggers on: `pull_request` and `push` to `main`/`dev`
- Uses `python 3.12` with no external dependencies
- Runs `validate_all.sh` (exits on first failure)
- On PRs: additionally validates the PR body via `PR_BODY` env var

## Test Suite

Test fixtures live in `scripts/tests/fixtures/`. The test suite (`scripts/tests/test_validators.py`) covers:

- Valid slice passes all checks
- Missing artifacts detected per state
- Invalid states rejected
- Doc freeze violations caught (missing frozen docs)
- Pre-freeze slices correctly skipped
- Registry mismatches detected (both directions)
- Invalid role names caught
- PR template structural validation
- State parsing edge cases
