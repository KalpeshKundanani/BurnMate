# Slice Contract

## Slice Identifier

- `SLICE-0008`

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

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation`

## Residual Marker Scan Scope

Directory paths where `TODO` / `FIXME` / `HACK` / `XXX` checks must run:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation`

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

## Allowed Scope

- Dashboard visualization work only: chart state, chart adapters, reusable chart composables, and dashboard-screen integration
- Presentation-level range handling for 7-, 14-, and 30-day dashboard visualizations
- Read-only use of existing `DashboardReadModelService` and `WeightHistoryService`
- Shared empty, loading, and error states for chart rendering

## Allowed Dependencies

- Existing Compose Multiplatform UI libraries already used by the app, including `foundation`, `ui`, `material3`, and related drawing primitives needed for Canvas-based charts
- Existing lifecycle/runtime dependencies already present in the dashboard presentation layer

## Architecture Rules

- No business logic inside composables
- Chart rendering must consume immutable presentation state only
- No new domain-model fields, repository methods, or persistence behaviors may be introduced
- Debt trend and weekly deficit visuals must derive from existing debt-history outputs already available through the current read-model path
- Weight trend visuals must use existing read-only weight history access; no new storage or sync layer is allowed
- Third-party charting libraries are not authorized for this slice

## Forbidden Scope

Modules or directories that must not be modified by the slice:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/weight`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/weight`
- Any persistence schema, repository storage layer, or external API integration path
