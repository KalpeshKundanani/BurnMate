# QA: SLICE-0009 - Google Fit + Google Login

## QA Metadata

| Field | Value |
|---|---|
| QA | `QA` |
| Date | 2026-03-17 |
| Branch | `feature/SLICE-0009/google-fit-login` |
| Frozen Docs Reviewed | `contract.md`, `prd.md`, `hld.md`, `lld.md`, `review.md`, `test-plan.md`, `state.md`, `docs/slices/index.md` |

## Checks Performed

- Verified slice state and owner were `REVIEW_APPROVED` and `QA` before execution.
- Reviewed branch scope against the frozen contract and confirmed code changes stayed within allowed integration, presentation, UI, Android adapter, dependency/config, and slice-doc paths.
- Inspected shared integration contracts, Android auth/permission/Fit adapters, dashboard UI wiring, and app-root refresh integration.
- Validated `test-plan.md` coverage against the implemented tests and checked that tests remain fake-based rather than depending on real Google services.
- Ran `./gradlew --no-daemon assembleDebug`.
- Ran `./gradlew --no-daemon clean test`.
- Ran `rg -n "TODO|FIXME|HACK|XXX" composeApp/src` and confirmed no matches.
- Ran `python3 scripts/validate_doc_freeze.py`.
- Ran `python3 scripts/validate_slice_registry.py`.
- Ran `python3 scripts/validate_required_artifacts.py`.
- Ran `python3 scripts/validate_pr_checklist.py`.
- Ran `python3 scripts/validate_state_machine_transitions.py`.
- Ran `bash scripts/validate_all.sh`.

## Results

| Area | Result | Notes |
|---|---|---|
| Scope compliance | PASS | No unauthorized domain redesign, persistence redesign, Apple Health work, or unrelated integration changes were introduced. |
| Build and test gates | PASS | `assembleDebug` and `clean test` both passed locally. |
| Marker scan | PASS | No `TODO`, `FIXME`, `HACK`, or `XXX` markers were found under `composeApp/src`. |
| Validators | PASS | All required validators passed. |
| Adapter isolation | PASS | Raw Google Fit SDK and Android launcher types remain in `androidMain`; shared/common layers use sanitized contracts. |
| Dashboard/logging integration | PASS | Imports write through the existing `EntryRepository`, and successful sync emits the refresh signal consumed by dashboard/logging reloads. |
| Auth + permission flow correctness | FAIL | The permission adapter can switch to a different Google account than the one returned by Credential Manager sign-in, while the ViewModel/UI keeps showing the original authenticated session. |
| Test-plan completeness | FAIL | Existing tests do not cover the account-consistency regression between sign-in and permission grant, so the current plan does not fully protect the frozen auth-flow contract. |

## Findings

### 1. Authenticated account can diverge from the account granted Fit access

- Severity: High
- Evidence:
  - [GoogleAuthServiceAndroid.kt](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/integration/auth/GoogleAuthServiceAndroid.kt#L30) signs in through Credential Manager and returns a `GoogleAccountSession` for the selected Google account.
  - [AndroidPermissionCoordinator.kt](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/integration/permission/AndroidPermissionCoordinator.kt#L46) ignores the `session` parameter entirely, checks `GoogleSignIn.getLastSignedInAccount(...)`, and if needed launches a generic `GoogleSignIn` intent that can resolve against any Google account.
  - [GoogleIntegrationViewModel.kt](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration/GoogleIntegrationViewModel.kt#L185) treats a granted permission result as approval for the original session and continues importing with that stale `session` in UI state.
- Impact:
  - The UI can report one authenticated Google account while the Fit permission and imported data belong to another account.
  - This violates the frozen auth-flow requirement that the authenticated state be reflected correctly in ViewModel/UI and tied to the selected account.

### 2. Automated coverage does not exercise the auth/permission account-consistency boundary

- Severity: Medium
- Evidence:
  - [test-plan.md](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/docs/slices/SLICE-0009/test-plan.md#L11) through [test-plan.md](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/docs/slices/SLICE-0009/test-plan.md#L20) cover happy-path sign-in, cancel, denied, mapping, sync, disconnect, and boundary checks, but no case verifies that the permission grant remains bound to the same signed-in account.
  - [GoogleIntegrationViewModelTest.kt](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration/GoogleIntegrationViewModelTest.kt#L35) through [GoogleIntegrationViewModelTest.kt](/Users/kalpeshkundanani/AndroidStudioProjects/BurnMate/composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/integration/GoogleIntegrationViewModelTest.kt#L180) never model a mismatch between the signed-in session and the permission-authorized Google account.
- Impact:
  - The main auth/permission regression identified above can slip through `./gradlew test` while the slice appears fully covered.

## Verdict

`CHANGES_REQUIRED`

## Rationale

The slice passes the required build, test, marker, and validator gates, and its overall scope and adapter isolation remain compliant. However, the permission flow does not preserve account identity across Credential Manager sign-in and Google Fit authorization, which breaks the frozen auth contract. QA approval is therefore blocked until that defect and its missing regression coverage are addressed.
