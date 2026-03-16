# Slice Contract

## Slice Identifier

- `SLICE-0001`

## Canonical Verdict Words

| Role | Allowed Verdicts |
|---|---|
| Reviewer | `APPROVED` \| `CHANGES_REQUIRED` |
| QA | `GO` \| `CHANGES_REQUIRED` |
| Auditor | `AUDIT_APPROVED` \| `CHANGES_REQUIRED` |

## Required State Machine

Primary progression states:

- `NOT_STARTED`
- `PRD_DEFINED`
- `HLD_DEFINED`
- `LLD_DEFINED`
- `CODE_IN_PROGRESS`
- `CODE_COMPLETE`
- `REVIEW_APPROVED`
- `QA_REQUIRED`
- `QA_APPROVED`
- `AUDIT_REQUIRED`
- `AUDIT_APPROVED`

Compatibility states preserved for the existing framework and validators:

- `REVIEW_REQUIRED`
- `REVIEW_CHANGES`
- `QA_CHANGES`
- `MERGED`

## Artifact Requirements

Always required bootstrap artifacts:

- `state.md`
- `contract.md`

Execution artifacts:

- `prd.md`
- `hld.md`
- `lld.md`
- `review.md`
- `qa.md`
- `test-plan.md`
- `audit-report.md`

## Implementation Scope

Directory paths where the slice is allowed to create code:

- No implementation paths are authorized while `SLICE-0001` remains in `NOT_STARTED`.

## Residual Marker Scan Scope

Directory paths where `TODO` / `FIXME` / `HACK` / `XXX` checks must run:

- No code scan scope is defined while `SLICE-0001` remains in `NOT_STARTED`.

## Required Test IDs

- `T-01`
- `T-02`
- `T-03`
- `T-04`
- `T-05`
- `T-06`
- `T-07`
- `T-08`
- `T-09`
- `T-10`

## Forbidden Scope

Modules or directories that must not be modified by the slice:

- All repository code paths until a future PRD/HLD/LLD defines explicit implementation scope.
