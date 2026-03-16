# Test Plan: SLICE-0007 — Core UI

## Metadata

| Field | Value |
|---|---|
| Slice | `SLICE-0007` |
| Role | `QA` |
| Date | `2026-03-16` |
| Branch | `feature/SLICE-0007/core-ui` |
| Source State | `REVIEW_APPROVED` |

## Executed Checks

| Test ID | Requirement | Verification Method | Result |
|---|---|---|---|
| `T-01` | Onboarding form initializes deterministically with disabled submit | Verified `OnboardingViewModelTest` and inspected `OnboardingViewModel` / `OnboardingScreen` wiring | PASS |
| `T-02` | Onboarding parsing and validation errors map into UI state | Verified `OnboardingViewModelTest` and inspected `OnboardingErrorMapper` | PASS |
| `T-03` | Dashboard maps read-model output into content cards | Verified `DashboardViewModelTest` and inspected `DashboardViewModel` / `DashboardScreen` | PASS |
| `T-04` | Daily logging loads selected-date entries in deterministic order | Verified `DailyLoggingViewModelTest` and inspected `DailyLoggingViewModel` / `DailyLogScreen` | PASS |
| `T-05` | Daily logging add-intake path refreshes state and clears draft | Verified `DailyLoggingViewModelTest` | PASS |
| `T-06` | Daily logging burn path surfaces deterministic unsupported feedback | Verified `DailyLoggingViewModelTest` | PASS |
| `T-07` | Daily logging delete path refreshes visible state | Verified `DailyLoggingViewModelTest` | PASS |
| `T-08` | Dashboard and logging date navigation reload selected date state | Verified `DashboardViewModelTest` and `DailyLoggingViewModelTest` | PASS |
| `T-09` | Explicit empty-state handling exists for date navigation / no-entry scenarios | Verified `DailyLoggingViewModelTest` plus screen inspection | PASS |
| `T-10` | Shared selected-date context stays aligned across dashboard/logging flow | Verified `BurnMateNavigationHostTest` and inspected `SelectedDateCoordinator` host injection | PASS |

## Command Log

| Command | Result |
|---|---|
| `./gradlew --no-daemon assembleDebug` | PASS |
| `./gradlew --no-daemon clean test` | PASS |
| `rg -n "TODO|FIXME|HACK|XXX" composeApp/src` | PASS |
| `python3 scripts/validate_doc_freeze.py` | PASS |
| `python3 scripts/validate_slice_registry.py` | PASS |
| `python3 scripts/validate_required_artifacts.py` | PASS |
| `python3 scripts/validate_pr_checklist.py` | PASS |
| `python3 scripts/validate_state_machine_transitions.py` | PASS |
| `bash scripts/validate_all.sh` | PASS |

## Notes

- Required UI screens are present and wired into the app shell: `OnboardingScreen`, `DashboardScreen`, `DailyLogScreen`, `BurnMateNavigationHost`.
- Atomic structure is present under `ui/atoms`, `ui/molecules`, `ui/organisms`, and screens remain presentation-focused.
- `SelectedDateCoordinator` is injected once in `BurnMateNavigationHost` and shared by both dashboard and logging ViewModels.
