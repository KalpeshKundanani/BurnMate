# Review: SLICE-0009 - Google Fit + Google Login

## Reviewer Output — SLICE-0009

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | 2026-03-17 |
| Review Cycle | 1 |
| LLD Reference | `docs/slices/SLICE-0009/lld.md` |

### Verdict: CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | FAIL | `GoogleIntegrationConfiguration` requires manifest metadata for `burnmate.google.web_client_id` and `burnmate.google.fit_project_enabled`, but `AndroidManifest.xml` only adds `ACTIVITY_RECOGNITION`; there is no path in-repo for Android to reach `Available`, so the required sign-in/import happy path is blocked. |
| R-02 | No unauthorized scope | PASS | Implementation stays within the contract’s allowed directories and keeps Google-specific logic in `integration/`, `presentation/integration/`, `ui/`, `ui/navigation/`, `androidMain/integration/`, and `androidMain/platform/`. |
| R-03 | Error handling | PASS | `GoogleIntegrationViewModel` represents signed-out, authenticating, permission-required, syncing, imported, error, and unavailable phases, including sign-in cancellation and Fit-unavailable fallbacks. |
| R-04 | Tests present | PASS | LLD tests `T-01` through `T-10` are implemented across `GoogleIntegrationViewModelTest`, `DefaultBurnImportMapperTest`, and `DefaultImportedBurnSyncServiceTest`; `./gradlew --no-daemon test` passed. |
| R-05 | Validation rules | PASS | Mapping and sync enforce `INVALID_ACTIVE_CALORIES`, `INVALID_STEP_COUNT`, `INVALID_BURN_SAMPLE`, `INVALID_INTEGRATION_ID`, and invalid-window guards in shared code. |
| R-06 | No residual markers | PASS | `rg -n "TODO|FIXME|HACK|XXX" composeApp/src` returned no matches in slice scope. |
| R-07 | Code compiles/lints | PASS | `./gradlew --no-daemon assembleDebug` and `./gradlew --no-daemon test` both passed locally. |
| R-08 | Security | PASS | Raw Google SDK and Android framework types remain confined to `androidMain`; shared interfaces expose sanitized models only. |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | `composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/platform/GoogleIntegrationConfiguration.kt` | 11-31 | Critical | Android availability depends on manifest metadata for the web client id and Fit-project enablement, but the slice never wires those values into the manifest or any other Android config source. As shipped, `availability()` resolves to `ConfigurationMissing` or `FitProjectUnavailable`, so the app cannot complete the required sign-in/import happy path from the dashboard. | Required |
| 2 | `docs/slices/SLICE-0009/contract.md` | 45-53 | Major | The contract lists `test-plan.md` as a required execution artifact, but the slice folder currently contains only `contract.md`, `hld.md`, `lld.md`, `prd.md`, and `state.md`. That leaves the next QA handoff without the required test-plan artifact. | Required |

### Rationale
Build, unit tests, marker scan, and the shared-vs-Android adapter boundary are all in acceptable shape. The slice still cannot be approved because the Android configuration path needed to make Google integration actually available is missing, and the required `test-plan.md` artifact has not been produced.

### Required Actions (if CHANGES_REQUIRED)
1. Add the Android configuration wiring required by `GoogleIntegrationConfiguration` so a correctly configured build can resolve to `GoogleIntegrationAvailability.Available` and execute the dashboard sign-in/import flow.
2. Add the missing `docs/slices/SLICE-0009/test-plan.md` artifact so QA has the required handoff document for this slice.

### State Transition
| Field | Value |
|---|---|
| Current State | `REVIEW_REQUIRED` |
| Next State | `REVIEW_CHANGES` |
