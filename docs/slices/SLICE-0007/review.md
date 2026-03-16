# Review: SLICE-0007 — Core UI

## Reviewer Output — SLICE-0007

### Review Metadata
| Field | Value |
|---|---|
| Reviewer | `Reviewer` |
| Date | 2026-03-16 |
| Review Cycle | 1 |
| LLD Reference | `docs/slices/SLICE-0007/lld.md` |
| Reviewed Commit | `293b41c` |

### Verdict: CHANGES_REQUIRED

### Rubric Evaluation
| # | Criterion | Result | Notes |
|---|---|---|---|
| R-01 | Spec alignment | FAIL | `App.kt` contains placeholder business wiring and a stub `DashboardReadModelService` instead of only hosting the navigation shell required by the LLD. The LLD also requires a one-shot onboarding success effect and dedicated SLICE-0007 tests under `commonTest`, which are missing. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/App.kt:20-49`, `docs/slices/SLICE-0007/lld.md:73-79`, `docs/slices/SLICE-0007/lld.md:157-163`, `docs/slices/SLICE-0007/lld.md:289-302`. |
| R-02 | No unauthorized scope | FAIL | The bottom navigation introduces `STATS` and `PROFILE` tabs even though the frozen slice contract limits navigation to onboarding, dashboard, and daily logging only, with no settings/profile/chart route in scope. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms/BottomNavigationBar.kt:23-64`, `docs/slices/SLICE-0007/lld.md:81-86`, `docs/slices/SLICE-0007/contract.md`. |
| R-03 | Error handling | FAIL | The onboarding navigation host navigates on submit based on the pre-submit state snapshot, so failed submissions can still route to dashboard before the ViewModel publishes validation failure. `DailyLogScreen` also leaves the `LoadableUiState.Error` branch unimplemented. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationHost.kt:38-44`, `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/screens/DailyLogScreen.kt:76-84`. |
| R-04 | Tests present | FAIL | The LLD requires T-01 through T-10 and names four new SLICE-0007 test files, but no `presentation/...ViewModelTest.kt` or `ui/navigation/BurnMateNavigationHostTest.kt` files exist under the slice scope. The passing `./gradlew test` run only exercised pre-existing repository tests. Evidence: `docs/slices/SLICE-0007/lld.md:52-59`, `docs/slices/SLICE-0007/lld.md:289-302`, `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate` file tree. |
| R-05 | Validation rules | FAIL | `OnboardingViewModel` collapses profile-domain failures into a generic `submitError` and does not map validation failures into the required field-level error state. Evidence: `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/onboarding/OnboardingViewModel.kt:54-91`, `docs/slices/SLICE-0007/lld.md:162-163`, `docs/slices/SLICE-0007/prd.md:53-54`. |
| R-06 | No residual markers | PASS | `rg -n "TODO|FIXME|HACK|XXX" composeApp/src` returned no matches in the repository. |
| R-07 | Code compiles/lints | PASS | `./gradlew --no-daemon assembleDebug` and `./gradlew --no-daemon test` both completed successfully on 2026-03-16. |
| R-08 | Security | PASS | No secrets or unsafe external integrations were introduced in the reviewed scope. |

### Findings
| # | File | Line(s) | Severity | Description | Resolution |
|---|---|---|---|---|---|
| 1 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/navigation/BurnMateNavigationHost.kt` | 38-44 | Critical | On onboarding submit, navigation to dashboard is decided from the stale pre-submit `state` snapshot. A failed profile submission can therefore still leave onboarding, which breaks AC-01/AC-07 and the LLD requirement for navigation on success only. | Required |
| 2 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/App.kt` | 20-49 | Critical | `App.kt` contains direct domain/repository construction plus a hardcoded stub `DashboardReadModelService`, which violates the LLD rule that `App.kt` should only host `BurnMateNavigationHost` and means the dashboard does not consume the existing read model as specified. | Required |
| 3 | `composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate` | Missing LLD files | Major | The slice did not add the required SLICE-0007 tests (`OnboardingViewModelTest`, `DashboardViewModelTest`, `DailyLoggingViewModelTest`, `BurnMateNavigationHostTest`) or coverage for T-01 through T-10. | Required |
| 4 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/organisms/BottomNavigationBar.kt` | 23-64 | Major | The shared navigation introduces `STATS` and `PROFILE` affordances even though the frozen slice allows only onboarding, dashboard, and daily logging. This is unauthorized UI scope. | Required |
| 5 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/presentation/onboarding/OnboardingViewModel.kt` | 54-91 | Major | Onboarding validation errors are surfaced as a generic banner rather than mapped into `fieldErrors`, so the implementation does not meet the specified field-level validation behavior. | Required |
| 6 | `docs/slices/SLICE-0007/state.md` | 8-23 | Major | The slice state was moved to `ENGINEERING_COMPLETE` with owner `QA`, skipping the mandatory Reviewer gate. Reviewer sequencing was invalid and had to be normalized during this review. | Required |
| 7 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/theme/Typography.kt` | 9-54 | Minor | Typography is centralized, but it uses `FontFamily.Default` throughout, which falls short of the rulebook’s stated premium/intentional typography direction. | Suggested |
| 8 | `composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/ui/components/GlassCard.kt` | 27-28 | Minor | The shared card layer is centralized, but it still hardcodes `1.dp` and `2.dp` instead of using spacing/elevation tokens, which is a rulebook compliance gap. | Suggested |

### Rationale
The slice establishes a real shared UI structure (`ui/theme`, `atoms`, `molecules`, `organisms`, `components`, `screens`, plus `presentation` and a navigation host), and common UI elements such as `GlassCard`, `StatCard`, `ActionCard`, `SectionHeader`, `MetricDisplay`, and `BottomNavigationBar` are centralized rather than duplicated. Screens are mostly thin composables that assemble shared components, and the visual direction generally aligns with the dark AMOLED / glass-card design language.

The slice still fails review because the implementation does not match the frozen behavior contract in several material ways: onboarding success navigation is incorrect, `App.kt` injects placeholder/stub logic instead of consuming the existing read model cleanly, LLD-required tests are missing, unauthorized navigation scope was added, field-level onboarding validation is not implemented, and the reviewer gate was skipped in the persisted state. Reviewer/state sequencing was therefore invalid on entry and has been corrected to a proper review-change state.

### Required Actions (if CHANGES_REQUIRED)
1. Fix onboarding navigation so dashboard routing occurs only after an explicit success effect from `OnboardingViewModel`.
2. Remove placeholder service wiring from `App.kt` and align the app shell with the LLD requirement that `App.kt` only hosts the navigation shell while using the existing dashboard read model contract.
3. Add the SLICE-0007 `commonTest` coverage required by the LLD for T-01 through T-10, including the named ViewModel and navigation host tests.
4. Remove unauthorized `STATS` and `PROFILE` navigation affordances from the shared bottom navigation for this slice.
5. Map onboarding validation failures into deterministic `fieldErrors` as required by the PRD/LLD.
6. Preserve valid framework sequencing by resubmitting from `REVIEW_REQUIRED` after engineering changes; do not skip directly to QA.

### State Transition
| Field | Value |
|---|---|
| Current State | `REVIEW_REQUIRED` |
| Next State | `REVIEW_CHANGES` |
| Next Owner | `Engineer` |
