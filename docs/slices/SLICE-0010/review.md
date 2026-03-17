# Review: SLICE-0010 - Settings + Final Polish

## Reviewer Output — SLICE-0010

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | 2026-03-17 |
| Review Cycle | 1 |
| LLD Reference | `docs/slices/SLICE-0010/lld.md` |
| Reviewed Commit | `1809d62` |

### Verdict: CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | FAIL | The integrations section does not faithfully present the mapped settings state. When `integrationSummary.actionLabel` is null, `SettingsScreen` injects a fallback `"CONNECTED"` label and keeps a disabled action visible, which misstates signed-out, syncing, unavailable, and other no-action states instead of reflecting the actual status model from `SettingsStateMapper`. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/screens/SettingsScreen.kt:71-80`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings/SettingsStateMapper.kt:8-56`. |
| R-02 | No unauthorized scope | PASS | The branch diff stays within the slice's allowed settings/navigation/platform/doc touchpoints. The unrelated untracked `.vscode/`, `mcp_config.json`, and `tools/` paths were ignored. |
| R-03 | Error handling | PASS | Export and reset failures are surfaced through `SettingsActionStatus.Failure` plus a `UiMessage`, and the coordinators return explicit `Result.failure(...)` contracts for repository or launcher failures. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings/SettingsViewModel.kt:99-150`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/settings/export/DefaultAppExportCoordinator.kt:20-52`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/settings/reset/DefaultAppResetCoordinator.kt:20-58`. |
| R-04 | Tests present | FAIL | T-01 through T-10 are named, but the failure-path coverage is not meaningful enough to satisfy the frozen LLD. `T-06` is defined to prove `exportStatus=Failure`, yet the implemented `T-06` only exercises `DefaultAppExportCoordinator` and therefore cannot validate any `SettingsUiState` transition; it also "verifies" repository immutability by instantiating a new fake repository instead of asserting against the repository used by the coordinator. There is also no reset failure-path coverage despite the review instructions requiring reset/export error handling checks. Evidence: `docs/slices/SLICE-0010/lld.md:461-470`, `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/settings/export/DefaultAppExportCoordinatorTest.kt:64-90`, `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings/SettingsViewModelTest.kt:32-207`, `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/settings/reset/DefaultAppResetCoordinatorTest.kt:30-72`. |
| R-05 | Validation rules | PASS | Positive-integer validation for the daily target is enforced and prevents preference mutation on invalid input, matching the LLD rules. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings/SettingsViewModel.kt:76-96`, `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/presentation/settings/SettingsViewModelTest.kt:54-113`. |
| R-06 | No residual markers | PASS | `rg -n "TODO|FIXME|HACK|XXX"` returned no matches in the slice scan scope. |
| R-07 | Code compiles/lints | PASS | `./gradlew --no-daemon assembleDebug` and `./gradlew --no-daemon test` both passed on 2026-03-17. |
| R-08 | Security | PASS | The slice keeps export/reset behind explicit interfaces and confines the Android share intent to `androidMain`; no secrets or raw Google SDK objects are exposed in settings state. |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/screens/SettingsScreen.kt` | 71-80 | Major | The integrations row fabricates a `"CONNECTED"` action label whenever the mapper returns no available action. That makes signed-out or unavailable states render as a disabled "connected" action, which is misleading and bypasses the presentation contract that `SettingsStateMapper` owns the user-facing status mapping. | Required |
| 2 | `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/settings/export/DefaultAppExportCoordinatorTest.kt` | 64-90 | Major | The implemented `T-06` does not test the LLD's expected `exportStatus=Failure` state transition and does not meaningfully verify state immutability on the exercised repository instances. Combined with the absence of reset failure-path tests, the slice lacks meaningful coverage for the required reset/export error handling. | Required |

### Scope Findings
- Settings, reset/export coordination, navigation wiring, and Android export launching stay within the slice's allowed scope.
- No out-of-scope domain redesign or unrelated integrations were introduced in the branch diff.

### Architecture Findings
- `SettingsViewModel` remains the orchestration owner for preference save, export, reset confirmation, and Google disconnect.
- Reset and export logic is isolated behind explicit coordinators, and destructive reset still requires explicit confirmation before execution.
- `SettingsScreen` is not fully presentation-only for the integrations section because it applies extra action-label/action-enabled branching instead of rendering a fully mapped state object.

### Behavior Findings
- Settings is reachable from the existing dashboard header route, and post-reset navigation still targets onboarding.
- Export and reset coordinators are deterministic in ordering and all-or-nothing sequencing.
- The integrations section currently miscommunicates no-action states by showing a disabled `"CONNECTED"` button.

### Test / Build Results
- `./gradlew --no-daemon assembleDebug` — PASS
- `./gradlew --no-daemon test` — PASS
- `rg -n "TODO|FIXME|HACK|XXX" ...` in slice scan scope — no matches
- `python3 scripts/validate_doc_freeze.py` — PASS
- `python3 scripts/validate_slice_registry.py` — PASS
- `python3 scripts/validate_required_artifacts.py` — PASS
- `python3 scripts/validate_pr_checklist.py` — PASS
- `python3 scripts/validate_state_machine_transitions.py` — PASS
- `bash scripts/validate_all.sh` — PASS

### Rationale
The slice is close, but it is not ready for approval. One review blocker is a user-facing correctness issue in the integrations section, where settings can show a disabled `"CONNECTED"` action even when Google Fit is signed out or otherwise not actionable. The second blocker is that the frozen LLD's failure-path expectations are not covered meaningfully enough to support approval, especially around export/reset error-state transitions.

### Required Actions
1. Make the integrations section render the exact mapped settings presentation for no-action states instead of synthesizing a fallback `"CONNECTED"` action in the composable.
2. Add meaningful tests for the required export/reset failure paths, including the LLD-defined failure state transition for export and a reset failure-path assertion that proves deterministic non-success behavior.

### State Transition
| Field | Value |
|---|---|
| Current State | `REVIEW_REQUIRED` |
| Next State | `REVIEW_CHANGES` |
| Next Owner | `Engineer` |
