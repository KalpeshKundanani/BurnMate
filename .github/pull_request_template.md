## Slice

- **Slice ID:** SLICE-XXXX
- **Slice Name:**
- **Current State:**
- **Branch:** `slice/SLICE-XXXX`

## Pre-Merge Checklist

### Registration & State

- [ ] Slice is registered in `docs/slices/index.md`
- [ ] `state.md` current state matches `index.md`
- [ ] `state.md` history has a complete, unbroken transition record

### Artifacts — Existence & Freeze

- [ ] `prd.md` exists and is frozen (unchanged since `LLD_DEFINED`)
- [ ] `hld.md` exists and is frozen (unchanged since `LLD_DEFINED`)
- [ ] `lld.md` exists and is frozen (unchanged since `LLD_DEFINED`)
- [ ] If any frozen doc was changed, a `change-request.md` exists with approval

### Implementation

- [ ] Code matches `lld.md` specification — no additions, no omissions
- [ ] All unit tests from `lld.md` are implemented and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code

### Review

- [ ] `review.md` exists with verdict: **APPROVED**
- [ ] All reviewer change requests are addressed (if `REVIEW_CHANGES` occurred)

### QA

- [ ] `test-plan.md` exists and was executed
- [ ] `qa.md` exists with verdict: **APPROVED**
- [ ] No Critical/High defects remain open
- [ ] All acceptance criteria from `prd.md` are verified

### Audit

- [ ] `audit-report.md` exists with verdict: **APPROVED**
- [ ] Spec-to-code traceability table is complete
- [ ] State machine compliance verified
- [ ] Role isolation compliance verified

### Context Capsule

- [ ] Context Capsule was used for all AI invocations on this slice

## Summary

<!-- One paragraph: what this slice does and why. -->

## Changes

<!-- List of files added or modified. -->
