# Review: SLICE-0007 — Core UI

## Reviewer Output — SLICE-0007

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | 2026-03-16 |
| Review Cycle | 2 |
| LLD Reference | `docs/slices/SLICE-0007/lld.md` |
| Reviewed Commit | `401db9e` |

### Verdict: CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | FAIL | The previous onboarding-submit, `App.kt`, required-test, bottom-nav-scope, field-level-validation, and reviewer-gate findings are repaired. The slice still violates AC-06 / HLD data-flow rules because dashboard and daily logging do not share selected-date context across navigation. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationHost.kt:73-85`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/dashboard/DashboardViewModel.kt:36-49`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/logging/DailyLoggingViewModel.kt:42-68`, `docs/slices/SLICE-0007/hld.md`, `docs/slices/SLICE-0007/lld.md`. |
| R-02 | No unauthorized scope | PASS | Navigation is now limited to onboarding, dashboard, and daily logging. No `STATS` or `PROFILE` tab remains. |
| R-03 | Error handling | PASS | Onboarding navigation is now success-driven through a one-shot success event, failed onboarding stays on onboarding, and `DailyLogScreen` renders an explicit error state with retry affordance. |
| R-04 | Tests present | PASS | The required slice tests now exist: `OnboardingViewModelTest`, `DashboardViewModelTest`, `DailyLoggingViewModelTest`, and `BurnMateNavigationHostTest`. |
| R-05 | Validation rules | PASS | `OnboardingViewModel` maps parse/domain validation failures into `fieldErrors` instead of collapsing field-level failures into `submitError`. |
| R-06 | No residual markers | PASS | `rg -n "TODO|FIXME|HACK|XXX"` returned no matches in the slice-owned UI/presentation/navigation scope. |
| R-07 | Code compiles/lints | PASS | `./gradlew --no-daemon assembleDebug` and `./gradlew --no-daemon test` both completed successfully on 2026-03-16. |
| R-08 | Security | PASS | No secrets or unsafe external integrations were introduced in the reviewed scope. |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationHost.kt` | 73-85 | Major | Opening daily logging from the dashboard only triggers `DailyLoggingEvent.Load`; it does not transfer the dashboard's currently selected date into the logging ViewModel. If the user navigates to a prior day on dashboard and then opens logging, logging still shows its own internal date (typically today). This breaks AC-06 and the HLD/LLD requirement to share selected-date context between dashboard and logging through navigation/ViewModel state rather than screen-local state. | Required |
| 2 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationHost.kt` | 98-104 | Major | Returning from daily logging to dashboard also performs route navigation only; it does not synchronize the logging-selected date back into `DashboardViewModel`. Cross-screen date navigation is therefore not round-trippable, so the visible daily state is not consistent across the two in-scope screens. | Required |

### Previously Reported Findings Recheck
| Finding | Status | Notes |
|---|---|---|
| Onboarding navigation must be success-driven | RESOLVED | `BurnMateNavigationHost` now reacts to `successEvent` in `LaunchedEffect` and only navigates after a successful submit. |
| `App.kt` must be shell-only | RESOLVED | `App.kt` now only applies theme and hosts `BurnMateNavigationHost`. |
| Required slice tests must exist | RESOLVED | All four named test files are present under `commonTest`. |
| Bottom nav must stay in scope | RESOLVED | Bottom nav now exposes only dashboard/home and logging/activity. |
| Field-level validation must populate `fieldErrors` | RESOLVED | `OnboardingErrorMapper` + `OnboardingViewModel` now preserve field-level error mapping. |
| Reviewer gate/state discipline must be preserved | RESOLVED | Slice resubmitted from `REVIEW_REQUIRED` with `Owner Role = Reviewer`. |

### Rationale
The engineer repaired the original review blockers: onboarding no longer navigates on stale pre-submit state, `App.kt` is shell-only, the required tests exist, unauthorized bottom-navigation tabs were removed, field-level onboarding validation is mapped correctly, and the persisted state returned to a valid reviewer handoff.

The slice still fails review because the navigation shell does not preserve the shared selected-date context between dashboard and daily logging. The HLD/LLD explicitly require date context sharing across those two screens, and the current host only switches routes while each ViewModel keeps its own independent date state. That is a user-visible behavior defect in one of the slice's core flows.

### Required Actions (if CHANGES_REQUIRED)
1. Propagate the currently selected date from dashboard into daily logging when opening the logging route.
2. Propagate the currently selected date from daily logging back into dashboard when returning, or centralize the shared date state so both screens observe the same source of truth.
3. Add or extend slice tests so cross-screen date synchronization is covered at the navigation/presentation boundary, not only within each ViewModel independently.

### State Transition
| Field | Value |
|---|---|
| Current State | `REVIEW_REQUIRED` |
| Next State | `REVIEW_CHANGES` |
| Next Owner | `Engineer` |
