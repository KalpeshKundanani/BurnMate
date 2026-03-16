# Slice Contract

## Slice Identifier

- `SLICE-0007`

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

- Compose Multiplatform UI for the first usable BurnMate shell
- ViewModels and immutable UI state models
- Core navigation wiring for onboarding, dashboard, daily logging, and date navigation
- Presentation mappers that adapt existing domain and dashboard read-model outputs into screen state

## Architecture Rules

- Stateless composables wherever possible
- ViewModel owns screen state and user-intent orchestration
- No business logic inside composables
- UI consumes existing `profile`, `logging`, `weight`, `caloriedebt`, and `dashboard` outputs as read-only dependencies
- No direct persistence work inside composables or UI mappers
- No redesign of completed domain or read-model slices

## Forbidden Scope

Modules or directories that must not be modified by the slice:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/weight`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/weight`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- Charts and advanced visualizations
- Google Fit integration
- Google login
- Settings, export, reset, or persistence redesign
- Any new business or domain logic beyond presentation mapping
