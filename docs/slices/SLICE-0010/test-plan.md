# Test Plan: SLICE-0010 - Settings + Final Polish

## Scope

This test plan covers the frozen LLD verification set for the settings route, integration-state presentation, deterministic export, destructive reset confirmation, and the final navigation polish needed to reach settings and recover cleanly after reset.

## Traceability Matrix

| Test ID | Traceability | Validates | Expected Execution Scope |
|---|---|---|---|
| `T-01` | LLD settings initialization | Settings loads deterministic preference and integration state from the current app session. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-02` | LLD daily target validation | Blank daily target input is rejected and does not mutate persisted preferences. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-03` | LLD daily target validation | Non-positive daily target input is rejected and does not mutate persisted preferences. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-04` | LLD daily target persistence | Valid daily target input persists deterministically and surfaces a success message. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-05` | LLD export coordinator ordering | Export assembles a deterministic snapshot with the required profile/preferences/calorie/weight/integration ordering. | Automated unit test in `composeApp/src/commonTest/.../settings/export/DefaultAppExportCoordinatorTest.kt` |
| `T-06` | LLD export failure path | Export launcher failure surfaces `Failure` state and leaves preferences, session, and repositories unchanged. | Automated unit tests in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` and `composeApp/src/commonTest/.../settings/export/DefaultAppExportCoordinatorTest.kt` |
| `T-07` | LLD reset confirmation | Reset does not execute until the destructive confirmation is explicitly accepted. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-08` | LLD reset success path | Confirmed reset clears in-scope app-managed state and triggers the post-reset completion callback. | Automated unit tests in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` and `composeApp/src/commonTest/.../settings/reset/DefaultAppResetCoordinatorTest.kt` |
| `T-09` | LLD integration disconnect | Disconnect delegates to the existing integration contract and refreshes the settings integration state. | Automated unit test in `composeApp/src/commonTest/.../presentation/settings/SettingsViewModelTest.kt` |
| `T-10` | LLD navigation polish | Settings is reachable from the app shell and successful reset returns navigation to onboarding. | Automated unit test in `composeApp/src/commonTest/.../ui/navigation/BurnMateNavigationHostTest.kt` |

## Edge and Failure Focus

| Area | Verification Focus | Evidence |
|---|---|---|
| Integration presentation truthfulness | Signed-out, authenticating, syncing, unavailable, and unsupported/error-backed states must not fabricate a fake connected action. | Deterministic settings integration summary mapping plus settings screen action enablement contract reviewed against `SettingsStateMapper` and `SettingsScreen`; full suite passed via `./gradlew --no-daemon clean test`. |
| Export safety | Failure must surface explicit failure state and leave app-managed state unchanged. | `T-06` coverage in `SettingsViewModelTest` and `DefaultAppExportCoordinatorTest.kt`. |
| Reset safety | Disconnect or clear failures must not partially mutate unrelated app-managed state. | `DefaultAppResetCoordinatorTest.kt` disconnect-failure coverage plus full suite pass. |
| Release routing | Existing dashboard/logging flows must stay intact while exposing settings and post-reset onboarding recovery. | `BurnMateNavigationHostTest.kt` and `./gradlew --no-daemon assembleDebug`. |

## Regression Focus

- Existing dashboard and daily logging navigation behavior.
- Existing Google integration status/disconnect contracts.
- Existing calorie-entry and weight-history repositories used by export/reset.
- Existing Android debug assembly and shared unit test execution path.

## Defects Found

No QA defects remained open at execution close.

## Exit Criteria for QA_APPROVED

All of the following must be true:

- [x] All unit tests from LLD are passing
- [x] All integration tests (if in scope) are passing
- [x] All edge cases are tested
- [x] No Critical or High severity defects remain open
- [x] All acceptance criteria from `prd.md` are verified
- [x] Regression areas show no new failures
