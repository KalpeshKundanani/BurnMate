# Slice Contract

## Slice Identifier

- `SLICE-0009`

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

Directory paths where the slice is allowed to create or modify code:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/platform`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/MainActivity.kt`
- `composeApp/src/androidMain/AndroidManifest.xml`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation`
- `composeApp/build.gradle.kts`
- `gradle/libs.versions.toml`

## Residual Marker Scan Scope

Directory paths where `TODO` / `FIXME` / `HACK` / `XXX` checks must run:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/integration`
- `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/platform`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/integration`
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

- Authentication UI and presentation wiring required to start, complete, cancel, or fail Google sign-in from the current BurnMate flow
- Common integration interfaces, mappers, and sync contracts for Google account state and Google Fit activity import
- Android-specific adapters for Credential Manager, Google account session handling, Google Fit permission checks, Google Fit history reads, and runtime permission launchers
- Permission request flow for Android runtime activity recognition access plus Google Fit read scopes
- Deterministic mapping of imported Google Fit steps and calorie data into existing BurnMate burn-facing structures
- Dashboard and daily logging presentation integration required to surface imported burn data and integration status without adding a separate settings redesign
- Dependency and manifest changes strictly required for Google auth, Google Fit, and Android permission wiring

## Allowed Dependencies

- `androidx.credentials:credentials`
- `androidx.credentials:credentials-play-services-auth`
- `com.google.android.libraries.identity.googleid:googleid`
- `com.google.android.gms:play-services-auth`
- `com.google.android.gms:play-services-fitness`

## Architecture Rules

- Google-specific logic must remain behind common interfaces and Android adapters.
- Composables render immutable UI state and emit intents only; no auth, permission, sync, or mapping logic belongs in UI code.
- Imported burn must flow through the existing `EntryRepository`, `DashboardReadModelService`, and daily logging read paths rather than a parallel integration-only store.
- Integration-owned entries must be deterministic and duplicate-safe so repeated imports do not create drift.
- Raw Google SDK types, account tokens, and Android framework launcher types must not escape `androidMain`.
- Android-only platform behavior is allowed; non-Android builds must surface an explicit unavailable state instead of silently attempting Google Fit integration.
- Completed domain logic in `caloriedebt`, `dashboard`, `logging`, `profile`, and `weight` must be treated as read-only contracts for this slice.

## Forbidden Scope

Modules or feature areas that must not be modified by the slice:

- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging/domain`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging/model`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging/repository`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/weight`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/dashboard`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile`
- `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/weight`
- Apple Health, HealthKit, Health Connect, or any non-Google health integration
- Full account management, backend identity, subscription, billing, analytics, export, reset, or settings redesign
- Any new unrelated domain feature or business-rule redesign outside the Google auth and Google Fit integration path
