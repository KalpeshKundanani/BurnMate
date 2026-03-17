# Test Plan: SLICE-0009 - Google Fit + Google Login

## Scope

This test plan covers the frozen LLD verification set for the Android Google sign-in and Google Fit import integration slice. Execution remains limited to shared integration and presentation behavior plus the Android adapter boundary contract.

## Traceability Matrix

| Test ID | Traceability | Validates | Expected Execution Scope |
|---|---|---|---|
| `T-01` | LLD `GoogleIntegrationViewModel` state initialization | When `GoogleIntegrationAvailability.Available` is reported and no cached Google session exists, the integration UI initializes in `SignedOut` without altering dashboard state. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-02` | LLD `GoogleIntegrationViewModel` sign-in flow | A successful Google auth result retains the session and transitions immediately into permission evaluation, landing in `PermissionRequired` when Fit/runtime access is still missing. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-03` | LLD auth cancellation handling | Google sign-in cancellation returns the UI to `SignedOut` with a non-error cancellation message and no corrupted integration state. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-04` | LLD permission request flow | A denied runtime/Fit permission request keeps the flow recoverable by staying in `PermissionRequired` and exposing retry state instead of proceeding to import. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-05` | LLD import orchestration | A granted permission request starts the 30-day import flow, maps/syncs valid samples, and publishes `Imported` state plus the import-applied event used for dashboard/logging refresh. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-06` | LLD `DefaultBurnImportMapper` | Mixed Google Fit samples map deterministically into chronological `ImportedBurnSample` records with stable IDs, noon UTC timestamps, calorie preference, and steps-only fallback estimation. | Automated unit test in `composeApp/src/commonTest/.../integration/mapping/DefaultBurnImportMapperTest.kt` |
| `T-07` | LLD `DefaultImportedBurnSyncService` | Sync deletes and replaces only `googlefit:`-owned entries inside the requested window while preserving manual entries and avoiding duplicates. | Automated unit test in `composeApp/src/commonTest/.../integration/sync/DefaultImportedBurnSyncServiceTest.kt` |
| `T-08` | LLD post-sync refresh contract | The dashboard/logging refresh signal is emitted only after a successful sync and is withheld when sync fails. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-09` | LLD disconnect behavior | Disconnect clears the active connection state and future sync ability without deleting previously imported history. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |
| `T-10` | LLD Android boundary rule | Shared/common contracts and presentation models do not expose Android framework, launcher, or raw Google SDK types beyond `androidMain`. | Automated unit test in `composeApp/src/commonTest/.../presentation/integration/GoogleIntegrationViewModelTest.kt` |

## Execution Summary

| Test ID Range | Execution Scope |
|---|---|
| `T-01` to `T-05`, `T-08` to `T-10` | Shared presentation/integration unit coverage via `./gradlew --no-daemon test` |
| `T-06` to `T-07` | Shared integration deterministic mapping and sync unit coverage via `./gradlew --no-daemon test` |
| Build verification | Android artifact wiring and manifest/config integration via `./gradlew --no-daemon assembleDebug` |
