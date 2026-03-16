# LLD: SLICE-0007 — Core UI

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0007/hld.md`
**PRD Reference:** `docs/slices/SLICE-0007/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/
  App.kt
  ui/
    navigation/
      BurnMateNavGraph.kt
      BurnMateNavigationHost.kt
      BurnMateRoute.kt
    onboarding/
      OnboardingScreen.kt
      OnboardingFormSection.kt
    dashboard/
      DashboardScreen.kt
      DashboardSummaryCard.kt
      DateNavigationHeader.kt
    logging/
      DailyLoggingScreen.kt
      EntryListItem.kt
      LoggingEntryEditor.kt
  presentation/
    onboarding/
      OnboardingViewModel.kt
      OnboardingUiState.kt
      OnboardingEvent.kt
      OnboardingErrorMapper.kt
    dashboard/
      DashboardViewModel.kt
      DashboardUiState.kt
      DashboardEvent.kt
      DashboardUiMapper.kt
    logging/
      DailyLoggingViewModel.kt
      DailyLoggingUiState.kt
      DailyLoggingEvent.kt
      DailyLoggingUiMapper.kt
    shared/
      LoadableUiState.kt
      UiMessage.kt
      DateNavigatorState.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/
  presentation/
    onboarding/OnboardingViewModelTest.kt
    dashboard/DashboardViewModelTest.kt
    logging/DailyLoggingViewModelTest.kt
  ui/
    navigation/BurnMateNavigationHostTest.kt
```

## Navigation Contracts

### `BurnMateRoute`

- `Onboarding`
- `Dashboard`
- `DailyLogging`

### `BurnMateNavigationHost`

Responsibilities:

- Replace the template `App()` content with the core navigation shell.
- Select initial destination based on whether a valid onboarding/profile state is available.
- Share selected-date context between dashboard and logging only through ViewModel or route state, not composable-local business logic.
- Wire navigation callbacks:
  - onboarding success -> dashboard
  - dashboard open logging -> daily logging
  - logging back -> dashboard

Constraints:

- Navigation scope is limited to onboarding, dashboard, and daily logging.
- No settings route.
- No chart route.
- No Google login route.

## UI State Models

### `OnboardingUiState`

Fields:

- `heightInput: String`
- `currentWeightInput: String`
- `goalWeightInput: String`
- `isSubmitting: Boolean`
- `fieldErrors: Map<OnboardingField, UiMessage>`
- `submitError: UiMessage?`
- `isSubmitEnabled: Boolean`

Responsibilities:

- Hold raw text input and derived presentation flags.
- Surface mapped domain validation failures without embedding business rules in the UI.

### `DashboardUiState`

Fields:

- `selectedDate: LocalDate`
- `status: LoadableUiState`
- `todaySummary: DashboardTodayCardState?`
- `debtSummary: DashboardDebtCardState?`
- `weightSummary: DashboardWeightCardState?`
- `emptyMessage: UiMessage?`
- `errorMessage: UiMessage?`

Responsibilities:

- Represent loading, content, empty, and retry-ready error states for the selected date.
- Exclude chart rendering even if chart points are present upstream.

### `DailyLoggingUiState`

Fields:

- `selectedDate: LocalDate`
- `status: LoadableUiState`
- `entries: List<LogEntryItemState>`
- `entryDraft: LoggingEntryDraftState`
- `supportsBurnEntry: Boolean`
- `emptyMessage: UiMessage?`
- `actionError: UiMessage?`

Responsibilities:

- Hold the visible daily entry list and add/delete controls.
- Preserve a deterministic unsupported/error path for burn-entry attempts when upstream contracts cannot complete them.

### Shared UI Models

| Model | Responsibility |
|---|---|
| `LoadableUiState` | Shared enum/sealed type for `Loading`, `Content`, `Empty`, `Error` |
| `UiMessage` | View-safe text/message wrapper for validation, empty, and action feedback |
| `DateNavigatorState` | Selected date plus previous/next availability and formatted label |

## ViewModel Contracts

### `OnboardingViewModel`

Dependencies:

- Existing profile-domain entry point for creating/validating `UserProfileSummary`

Responsibilities:

- Initialize onboarding form state.
- Handle text input updates.
- Parse metric inputs into domain values at submit time.
- Call the profile domain and map failures into `fieldErrors` or `submitError`.
- Expose a one-shot navigation effect or callback when onboarding succeeds.

Events:

- `HeightChanged`
- `CurrentWeightChanged`
- `GoalWeightChanged`
- `Submit`
- `DismissError`

### `DashboardViewModel`

Dependencies:

- `DashboardReadModelService`

Responsibilities:

- Load dashboard data for the selected date.
- Refresh state when date changes or retry is requested.
- Map `DashboardSnapshot` into dashboard card state objects.
- Omit chart UI while still consuming debt and weight summaries.

Events:

- `Load`
- `PreviousDayTapped`
- `NextDayTapped`
- `Retry`
- `OpenLogging`

### `DailyLoggingViewModel`

Dependencies:

- `EntryRepository`
- `CalorieEntryFactory`
- Presentation mapper(s) for entries and errors

Responsibilities:

- Fetch entries for the selected date.
- Maintain draft input for add-entry actions.
- Convert intake/burn UI intents into repository create/delete operations without putting rules in composables.
- Reload state after successful create/delete actions.
- Surface deterministic action errors for unsupported or invalid operations.

Events:

- `Load`
- `CalorieInputChanged`
- `AddIntakeTapped`
- `AddBurnTapped`
- `DeleteEntryTapped`
- `PreviousDayTapped`
- `NextDayTapped`
- `Retry`

## Composable Screen Contracts

### `OnboardingScreen`

- Receives `OnboardingUiState` plus event callbacks only.
- Renders input groups for height, current weight, and goal weight.
- Renders validation messages from state.
- Contains no BMI or goal-validation calculations.

### `DashboardScreen`

- Receives `DashboardUiState` plus event callbacks only.
- Renders summary cards/rows for today, debt, and weight.
- Uses a reusable `DateNavigationHeader`.
- Does not render any chart widget in this slice.

### `DailyLoggingScreen`

- Receives `DailyLoggingUiState` plus event callbacks only.
- Renders date navigation, draft entry editor, action buttons, and entry list.
- Supports add intake, add burn, and delete entry actions from UI intent callbacks.
- Burn action path must be presentation-driven and testable even if upstream services reject it.

## Smaller UI Components

| Component | Responsibility |
|---|---|
| `DateNavigationHeader` | Previous/next day controls and current date label |
| `DashboardSummaryCard` / summary rows | Compact read-only dashboard metrics |
| `EntryListItem` | Render one calorie entry and delete affordance |
| `LoggingEntryEditor` | Draft calorie input and add-action controls |
| `OnboardingFormSection` | Group onboarding text fields and inline errors |

## Presentation Mappers

| Mapper | Responsibility |
|---|---|
| `OnboardingErrorMapper` | Map profile-domain validation failures into field-level or screen-level UI messages |
| `DashboardUiMapper` | Convert `DashboardSnapshot` into card-ready dashboard state |
| `DailyLoggingUiMapper` | Convert `CalorieEntry` lists and repository/domain failures into list state and action feedback |

Rules:

- Mappers may format and rename data for UI consumption.
- Mappers may not add or alter business rules.
- Mappers must preserve deterministic output for identical inputs.

## Event Handling Contracts

| Event | ViewModel Result |
|---|---|
| Onboarding submit/update | Validate inputs, submit through profile domain, update errors or navigate |
| Add intake | Create a non-negative calorie entry and refresh selected-date entries |
| Add burn | Execute the burn-intent path and update UI with success or deterministic unsupported/error result |
| Delete entry | Remove by `EntryId` and refresh selected-date entries |
| Previous/next day navigation | Update selected date and reload affected screen state |
| Retry / refresh | Re-run the latest load path from the current selected date |

## Implementation Rules

- `App.kt` is updated only to host `BurnMateNavigationHost`; no business logic is placed there.
- Stateless composables are preferred everywhere possible.
- ViewModels own the authoritative screen state.
- No direct persistence or repository logic inside composables.
- No chart UI in this slice.
- No Google login or Google Fit in this slice.
- No settings UI in this slice.

## Test Cases

| ID | Test Case | Expected Outcome |
|---|---|---|
| T-01 | Onboarding state initialization | Empty form fields, no errors, submit disabled or neutral initial state |
| T-02 | Onboarding validation error mapping | Domain/profile validation failures map to deterministic UI error state |
| T-03 | Dashboard state mapping from read model | `DashboardSnapshot` maps to stable dashboard card state |
| T-04 | Daily logging state shows entries for selected date | Repository results map to correct ordered entry list |
| T-05 | Add intake action updates state correctly | Successful create path refreshes entries and clears draft/error state |
| T-06 | Add burn action updates state correctly | Burn intent yields deterministic success or deterministic unsupported/error state |
| T-07 | Delete entry action updates state correctly | Entry disappears after successful delete and state refresh |
| T-08 | Date navigation changes selected day state | Previous/next actions update selected date and reload data |
| T-09 | Empty state rendering/model behavior | No data produces explicit empty state rather than null/implicit behavior |
| T-10 | Deterministic ViewModel state transitions | Same inputs and dependency outputs yield identical state sequences |

## Out of Scope Guardrails

- No chart UI in this slice.
- No Google login or Google Fit in this slice.
- No settings UI in this slice.
- No export/reset flows in this slice.
- No new domain logic or persistence redesign in this slice.
