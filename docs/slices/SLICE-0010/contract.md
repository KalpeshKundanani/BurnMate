# Slice Contract

## Slice Identifier

- `SLICE-0010`

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

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/settings`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/screens`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/platform`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/settings`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration`
- `composeApp/build.gradle.kts` for minimal settings/export platform wiring required by this slice only

## Residual Marker Scan Scope

Directory paths where `TODO` / `FIXME` / `HACK` / `XXX` checks must run:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/settings`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/screens`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/platform`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/settings`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration`

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

- A dedicated settings surface reachable from the existing app shell
- Presentation state and navigation needed to expose final app preferences and release-ready status copy
- Explicit reset and export orchestration behind app-scoped interfaces or coordinators
- Integration status management surfaces for already-built Google sign-in / Google Fit functionality
- Minimal release polish on existing dashboard or logging surfaces when required to route users into settings or keep status/copy consistent
- Minimal app-state coordination required to reset or export the already-built local profile, logging, weight, chart-range, and integration-owned state
- Planning-time slice visibility updates through the repository-approved registry/state mechanism only

## Architecture Rules

- Business logic must stay out of composables
- Destructive actions must require explicit confirmation state before execution
- Export and reset flows must be isolated behind explicit interfaces or use-case style coordinators
- Settings presentation may consume existing dashboard, logging, weight, and integration outputs but must not redesign those domains
- Existing slice behavior remains the baseline; this slice may polish already-built surfaces only where the PRD/HLD/LLD name the exact touchpoint
- Repository tracking visibility must use `docs/slices/index.md` plus `docs/slices/SLICE-0010/state.md`, because that is the repository mechanism enforced by `scripts/validate_slice_registry.py`; `.github/ISSUE_TEMPLATE/new-slice.md` is manual and no repo-managed GitHub Project sync automation exists

## Forbidden Scope

Modules or directories that must not be modified by the slice:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging/domain`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile/domain`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/weight/domain`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/weight`
- Any new health provider integration or backend sync path
- Arbitrary design-system rewrites, persistence redesign, analytics expansion, or unrelated GitHub automation changes
