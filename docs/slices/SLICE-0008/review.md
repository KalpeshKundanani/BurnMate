# Review: SLICE-0008 — Charts & Visual Progress

## Reviewer Output — SLICE-0008

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | 2026-03-17 |
| Review Cycle | 1 |
| LLD Reference | `docs/slices/SLICE-0008/lld.md` |
| Reviewed Commit | `ae5c430` |

### Verdict: CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | FAIL | The real app wiring does not honor the requested chart window size for debt-history loads, so 14D and 30D debt visuals cannot satisfy AC-01/AC-02 even though the selector and data-source abstraction exist. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationDependencies.kt:36-39`. |
| R-02 | No unauthorized scope | PASS | Branch diff is confined to `presentation/`, `ui/`, `commonTest/`, and slice docs. No forbidden `domain/`, `core/`, or `persistence/` paths changed in `git diff --name-only origin/main...HEAD`. |
| R-03 | Error and empty-state handling | FAIL | `DashboardVisualProgressSection` renders an explicit empty state for missing debt trend and weight trend, but silently drops the weekly deficit chart when `weeklyDeficit == null`, which violates the slice requirement for explicit partial-data states. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms/DashboardVisualProgressSection.kt:73-75`. |
| R-04 | Tests present | PASS | Shared tests exist for `DashboardChartStateAdapter`, `DefaultDashboardChartDataSource`, and `DashboardViewModel`, and `./gradlew test` passed. |
| R-05 | Validation rules | PASS | Range enums and adapter/data-source rules are implemented. One minor lifecycle deviation remains in visualization-state clearing, noted below as non-blocking follow-up. |
| R-06 | No residual markers | PASS | `rg -n "TODO|FIXME|HACK|XXX" composeApp/src` returned no matches. |
| R-07 | Code compiles/lints | PASS | `./gradlew assembleDebug` and `./gradlew test` both passed on 2026-03-17. |
| R-08 | Security | PASS | The slice remains local, read-only, and Canvas-based with no new external integrations or chart SDKs. |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationDependencies.kt` | `36-39` | Major | `createChartDataSource()` ignores the factory's requested `days` argument and always builds `DefaultDashboardReadModelService` with its default 7-day window. In production wiring, switching from 7D to 14D/30D therefore does not actually load enough debt history for the larger ranges, so the debt trend and weekly deficit charts cannot meet AC-01/AC-02. | Required |
| 2 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms/DashboardVisualProgressSection.kt` | `73-75` | Major | When `weeklyDeficit` is `null`, the section renders nothing instead of an explicit empty state. That is a silent omission of partial chart data, contrary to the PRD MUST requirement and the LLD weekly-deficit contract. | Required |
| 3 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/dashboard/DashboardViewModel.kt` | `102-145` | Minor | Visualization transitions leave `charts` populated during `Loading`, `Empty`, and `Error`. The LLD validation rule says non-content visualization states should have `charts == null`, so stale chart data can survive across transitions even though the UI currently hides it. | Suggested |

### Rationale
The slice is close, but the first finding is a functional defect in the real dependency wiring rather than a test-only issue: 14D and 30D debt ranges are exposed in UI yet the dashboard service behind the chart data source never receives those wider windows. The second finding is a direct UX/spec gap because one partial-data branch silently omits a required visualization instead of surfacing an explicit empty state. Build, test, marker, and validator gates are otherwise clean.

### Required Actions (if CHANGES_REQUIRED)
1. Thread the requested chart-window size through the real navigation dependency wiring so `DefaultDashboardChartDataSource` can create a `DefaultDashboardReadModelService` with the requested debt-history window, and add a regression test that exercises that wiring instead of only the isolated data source.
2. Render explicit empty-state copy when `weeklyDeficit` is unavailable so partial debt-history scenarios never silently remove the bar chart.

### State Transition
| Field | Value |
|---|---|
| Current State | `CODE_COMPLETE` |
| Next State | `REVIEW_CHANGES` |
| Next Owner | `Engineer` |
