# LLD: SLICE-0010 - Settings + Final Polish

**Author:** Architect
**Date:** 2026-03-17
**HLD Reference:** `docs/slices/SLICE-0010/hld.md`
**PRD Reference:** `docs/slices/SLICE-0010/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/
  settings/
    preferences/
      AppPreferences.kt
      AppPreferencesStore.kt
      InMemoryAppPreferencesStore.kt
    export/
      AppExportSnapshot.kt
      AppExportCoordinator.kt
      DefaultAppExportCoordinator.kt
      AppExportLauncher.kt
    reset/
      AppResetCoordinator.kt
      DefaultAppResetCoordinator.kt
    state/
      AppSessionState.kt
      AppSessionStore.kt
  presentation/
    settings/
      SettingsUiState.kt
      SettingsViewModel.kt
      SettingsEvent.kt
      SettingsStateMapper.kt
  ui/
    screens/
      SettingsScreen.kt
    organisms/
      SettingsSectionCard.kt
      SettingsActionRow.kt
      SettingsPreferenceRow.kt
      SettingsConfirmationDialog.kt
    navigation/
      BurnMateRoute.kt                         (modify)
      BurnMateNavigationHost.kt               (modify)
      BurnMateNavigationDependencies.kt       (modify)
      BurnMateAppRoot.kt                      (modify)

composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/
  platform/
    AndroidAppExportLauncher.kt
  MainActivity.kt                              (modify only if launcher wiring is required)

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/
  settings/
    export/
      DefaultAppExportCoordinatorTest.kt
    reset/
      DefaultAppResetCoordinatorTest.kt
  presentation/
    settings/
      SettingsViewModelTest.kt
  ui/
    navigation/
      BurnMateNavigationHostTest.kt           (modify)
```

## Interfaces / APIs

### `AppPreferences`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.preferences

data class AppPreferences(
    val dailyTargetCalories: Int = 2000
)
```

Behavior:
- Represents the complete in-scope preference set for this slice.
- Only already-approved app configuration belongs here.
- `dailyTargetCalories` replaces hardcoded settings state where required, but no new diet-planning logic is introduced.

### `AppPreferencesStore`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.preferences

interface AppPreferencesStore {
    fun read(): AppPreferences
    fun update(transform: (AppPreferences) -> AppPreferences): AppPreferences
    fun reset(): AppPreferences
}
```

Behavior:
- Synchronous contract is sufficient because the current app architecture is fully local and deterministic.
- `reset()` restores the default preference snapshot used by a clean first run.

### `AppSessionStore`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.state

import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

data class AppSessionState(
    val activeProfile: UserProfileSummary? = null
)

interface AppSessionStore {
    fun read(): AppSessionState
    fun update(transform: (AppSessionState) -> AppSessionState): AppSessionState
    fun reset(): AppSessionState
}
```

Behavior:
- Centralizes the active-profile state currently held only in `BurnMateNavigationCoordinator`.
- Gives settings/reset/export a deterministic way to read or clear session state without hidden UI mutations.
- Must not become a generic business-logic container.

### `AppExportSnapshot`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.export

import kotlinx.datetime.Instant
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary
import org.kalpeshbkundanani.burnmate.settings.preferences.AppPreferences
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

data class AppExportSnapshot(
    val exportedAt: Instant,
    val profile: UserProfileSummary?,
    val preferences: AppPreferences,
    val calorieEntries: List<CalorieEntry>,
    val weightEntries: List<WeightEntry>,
    val integrationSummary: String?
)
```

Behavior:
- Snapshot must be built in deterministic order:
  1. profile
  2. preferences
  3. calorie entries sorted by date, `createdAt`, then id
  4. weight entries sorted by date
  5. integration summary string
- `integrationSummary` must be derived from sanitized settings/integration state only; raw tokens or SDK objects are forbidden.

### `AppExportLauncher`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.export

interface AppExportLauncher {
    suspend fun launch(snapshot: AppExportSnapshot): Result<Unit>
}
```

Behavior:
- Platform-only handoff. It may share, save, or otherwise hand the payload to the OS.
- Settings UI never calls platform APIs directly.

### `AppExportCoordinator`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.export

interface AppExportCoordinator {
    suspend fun export(): Result<AppExportSnapshot>
}
```

Behavior:
- Builds the snapshot, invokes `AppExportLauncher`, and returns the exact snapshot that was exported.
- Failure before launch or during launch must return `Result.failure(...)` and leave app state unchanged.

### `AppResetCoordinator`

```kotlin
package org.kalpeshbkundanani.burnmate.settings.reset

data class AppResetResult(
    val clearedCalorieEntries: Int,
    val clearedWeightEntries: Int,
    val disconnectedIntegration: Boolean
)

interface AppResetCoordinator {
    suspend fun reset(): Result<AppResetResult>
}
```

Behavior:
- Performs the full destructive clear for in-scope app-managed state.
- Reset must include:
  1. disconnect future Google integration access when currently connected
  2. clear local calorie entries
  3. clear local weight entries
  4. reset preferences
  5. clear active profile/session state
- Completion is reported only after all steps succeed.

## Concrete Collaborators

### `DefaultAppExportCoordinator`

Constructor dependencies:

```kotlin
class DefaultAppExportCoordinator(
    private val sessionStore: AppSessionStore,
    private val preferencesStore: AppPreferencesStore,
    private val entryRepository: EntryRepository,
    private val weightRepository: WeightHistoryRepository,
    private val integrationStatusProvider: () -> String?,
    private val exportLauncher: AppExportLauncher,
    private val nowProvider: () -> Instant
) : AppExportCoordinator
```

Algorithm:

```text
1. Read current session and preferences snapshots.
2. Fetch all calorie entries via repository range/all strategy defined for implementation.
3. Fetch all weight entries.
4. Sort entries deterministically.
5. Build `AppExportSnapshot`.
6. Pass snapshot to `AppExportLauncher`.
7. Return the same snapshot on success.
```

Error handling:

| Error Condition | Error Contract | Recovery |
|---|---|---|
| Entry fetch fails | `Result.failure(IllegalStateException("Failed to export calorie entries"))` | Surface settings error state; allow retry |
| Weight fetch fails | `Result.failure(IllegalStateException("Failed to export weight history"))` | Surface settings error state; allow retry |
| Export launcher fails | `Result.failure(IllegalStateException("Failed to hand off export"))` | Surface settings error state; no data mutation |

### `DefaultAppResetCoordinator`

Constructor dependencies:

```kotlin
class DefaultAppResetCoordinator(
    private val sessionStore: AppSessionStore,
    private val preferencesStore: AppPreferencesStore,
    private val entryRepository: EntryRepository,
    private val weightRepository: WeightHistoryRepository,
    private val integrationDisconnect: suspend () -> Result<Boolean>,
    private val dateRangeProvider: () -> Pair<EntryDate, EntryDate>,
    private val weightDatesProvider: () -> Result<List<LocalDate>>
) : AppResetCoordinator
```

Algorithm:

```text
1. Attempt integration disconnect through the injected callback.
2. Load all current calorie entries for the known app date range and delete them by id.
3. Load all stored weight-entry dates and delete each one.
4. Reset preferences to defaults.
5. Reset app session state to no active profile.
6. Return counts plus disconnect outcome.
```

Notes:
- Because the current repositories expose only date-based reads and item deletes, the implementation may add bounded helper accessors inside this slice if needed, but it must not redesign repository behavior outside the slice contract.
- Partial success is not reported as success. Any failure must short-circuit and return `Result.failure(...)`.

### `SettingsStateMapper`

Responsibilities:
- Map `AppPreferences`, session presence, and integration phase into lightweight row models for the screen.
- Convert export/reset progress into user-facing status text without embedding branching logic in composables.
- Provide stable labels for release-polish copy used by settings.

## Presentation Contracts

### `SettingsUiState`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.settings

import org.kalpeshbkundanani.burnmate.presentation.integration.GoogleIntegrationUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class SettingsUiState(
    val isLoading: Boolean = true,
    val dailyTargetCalories: String = "",
    val dailyTargetError: UiMessage? = null,
    val integration: GoogleIntegrationUiState? = null,
    val exportStatus: SettingsActionStatus = SettingsActionStatus.Idle,
    val resetStatus: SettingsActionStatus = SettingsActionStatus.Idle,
    val pendingConfirmation: SettingsConfirmationState? = null,
    val message: UiMessage? = null,
    val appInfo: String = "BurnMate"
)

sealed interface SettingsActionStatus {
    data object Idle : SettingsActionStatus
    data object InProgress : SettingsActionStatus
    data class Success(val detail: String) : SettingsActionStatus
    data class Failure(val detail: String) : SettingsActionStatus
}

sealed interface SettingsConfirmationState {
    data object ResetAppData : SettingsConfirmationState
}
```

Rules:
- `pendingConfirmation` is the only way reset can become executable from UI state.
- Export does not require destructive confirmation.
- `integration` reuses the existing integration UI model or a stable subset mapped from it.

### `SettingsEvent`

```kotlin
sealed interface SettingsEvent {
    data object Load : SettingsEvent
    data class DailyTargetChanged(val value: String) : SettingsEvent
    data object SaveDailyTarget : SettingsEvent
    data object ExportTapped : SettingsEvent
    data object ResetTapped : SettingsEvent
    data object ConfirmReset : SettingsEvent
    data object DismissConfirmation : SettingsEvent
    data object DisconnectGoogleTapped : SettingsEvent
    data object DismissMessage : SettingsEvent
}
```

### `SettingsViewModel`

Constructor dependencies:

```kotlin
class SettingsViewModel(
    private val preferencesStore: AppPreferencesStore,
    private val sessionStore: AppSessionStore,
    private val exportCoordinator: AppExportCoordinator,
    private val resetCoordinator: AppResetCoordinator,
    private val integrationStateProvider: () -> GoogleIntegrationUiState,
    private val disconnectGoogle: suspend () -> Result<Unit>,
    private val onResetCompleted: () -> Unit,
    private val stateMapper: SettingsStateMapper = SettingsStateMapper()
) : ViewModel()
```

Responsibilities:
- Initialize settings state from preferences/session/integration sources.
- Validate and save `dailyTargetCalories` as a positive integer only.
- Open and close reset confirmation state.
- Run export and reset actions one at a time.
- Invoke `onResetCompleted()` only after reset succeeds so navigation can return to onboarding.

Event handling contract:

| Event | ViewModel Result |
|---|---|
| `Load` | Read sources and publish initial `SettingsUiState` |
| `DailyTargetChanged(value)` | Update draft text, clear inline error |
| `SaveDailyTarget` | Validate positive integer, update preferences, publish success or error |
| `ExportTapped` | Set `exportStatus=InProgress`, run export, then publish `Success` or `Failure` |
| `ResetTapped` | Set `pendingConfirmation=ResetAppData` |
| `ConfirmReset` | Require pending reset confirmation, set `resetStatus=InProgress`, run reset, then call `onResetCompleted` on success |
| `DismissConfirmation` | Clear `pendingConfirmation` without side effects |
| `DisconnectGoogleTapped` | Delegate to existing disconnect contract and refresh integration state |
| `DismissMessage` | Clear transient message only |

Determinism rules:
- Repeated `ConfirmReset` with no `pendingConfirmation` is a no-op.
- `SaveDailyTarget` with non-numeric or non-positive input sets `dailyTargetError` and does not mutate stored preferences.
- While `exportStatus` or `resetStatus` is `InProgress`, a second action request of the same type is ignored.

## Navigation Integration

### `BurnMateRoute`

Add:

```kotlin
data object Settings : BurnMateRoute
```

Route name:

```kotlin
BurnMateRoute.Settings -> "settings"
```

### `BurnMateNavigationHost`

Required changes:
- Register the `settings` composable.
- Pass `SettingsViewModel` state/events into `SettingsScreen`.
- Preserve existing onboarding, dashboard, and daily logging routes without behavior changes.

### `BurnMateAppRoot`

Required changes:
- Replace ad hoc `BurnMateNavigationCoordinator.activeProfile` ownership with `AppSessionStore` as the shared source of truth.
- Create `SettingsViewModel` with dependencies shared from navigation.
- Wire dashboard header `onProfileClick` to navigate to `BurnMateRoute.Settings`.
- After `onResetCompleted`, clear navigation back stack to onboarding.

### `BurnMateNavigationDependencies`

Required additions:
- `appPreferencesStore`
- `appSessionStore`
- `weightHistoryRepository` reference if not already retained
- `appExportCoordinator`
- `appResetCoordinator`
- `appExportLauncher`

## UI Contracts

### `SettingsScreen`

Responsibilities:
- Render section cards in this order:
  1. preferences
  2. integrations
  3. export
  4. reset
  5. app info
- Render confirmation dialog when `pendingConfirmation` is non-null.
- Emit `SettingsEvent` values only; no repository or platform calls.

### Reusable row/section components

| Component | Responsibility |
|---|---|
| `SettingsSectionCard` | Shared section container for the settings screen |
| `SettingsPreferenceRow` | Labeled input + save action for daily target calories |
| `SettingsActionRow` | Row with title, subtitle, action label, and optional danger emphasis |
| `SettingsConfirmationDialog` | Shared destructive confirmation shell used by reset |

## Error Handling Contracts

| Error Condition | UI State Result | Recovery |
|---|---|---|
| Invalid daily target input | `dailyTargetError = UiMessage("Enter a valid positive calorie target.", true)` | User edits and retries |
| Export failure | `exportStatus = Failure(detail)` and `message = UiMessage(detail, true)` | Retry export |
| Reset failure | `resetStatus = Failure(detail)` and confirmation dismissed | Retry reset from explicit flow |
| Disconnect failure | `message = UiMessage("Failed to disconnect Google Fit.", true)` | Retry disconnect |
| Integration unavailable | Reuse existing integration unavailable state in settings section | No retry beyond existing integration controls |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Settings state initializes from stored preferences and current integration state | preferences `2000`, signed-out integration | `SettingsUiState` shows `dailyTargetCalories="2000"` and integration summary |
| T-02 | Daily target save rejects blank input | `DailyTargetChanged("")`, `SaveDailyTarget` | inline validation error, no preference mutation |
| T-03 | Daily target save rejects non-positive values | `DailyTargetChanged("0")`, `SaveDailyTarget` | inline validation error, no preference mutation |
| T-04 | Daily target save persists a valid value deterministically | `DailyTargetChanged("2300")`, `SaveDailyTarget` | preferences updated to `2300`, success message present |
| T-05 | Export assembles deterministic snapshot ordering | known profile/preferences/entries/weights | snapshot fields and list ordering match contract exactly |
| T-06 | Export failure does not mutate app state | export launcher returns failure | `exportStatus=Failure`, preferences/session/repository contents unchanged |
| T-07 | Reset request requires explicit confirmation | `ResetTapped` only | `pendingConfirmation=ResetAppData`, reset coordinator not called |
| T-08 | Confirmed reset clears app state and triggers reset completion callback | `ResetTapped`, `ConfirmReset` | reset coordinator called once, session cleared, callback invoked |
| T-09 | Disconnect action delegates to existing integration contract and refreshes state | `DisconnectGoogleTapped` | disconnect callback invoked, integration state reloaded |
| T-10 | Navigation exposes settings route and returns to onboarding after successful reset | navigate from dashboard header, then successful reset | settings route present, post-reset route is onboarding |

## Definition of Done — CODE_COMPLETE Checklist

The Engineer must satisfy ALL of the following before transitioning to `CODE_COMPLETE`:

- [ ] All interfaces and collaborators above are implemented
- [ ] Settings navigation route is wired from the existing app shell
- [ ] Daily target preference flow is implemented with validation
- [ ] Export flow generates deterministic snapshots and uses the platform launcher abstraction
- [ ] Reset flow requires confirmation and clears all in-scope app-managed state
- [ ] Existing Google integration disconnect/status is surfaced through settings without raw SDK leakage
- [ ] All unit test cases above are written and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code
- [ ] Code compiles/lints without errors
- [ ] No features beyond this LLD are implemented

## Tracking Visibility Note

For this slice, the repository-approved planning-time visibility mechanism is:

1. `docs/slices/SLICE-0010/state.md`
2. `docs/slices/index.md`
3. `scripts/validate_slice_registry.py`
4. `.github/workflows/validators.yml`

No repository script or workflow was found that creates or synchronizes GitHub Project items or GitHub Issues automatically. Implementation must therefore preserve registry/state correctness, and any manual GitHub Issue creation remains outside repository automation.
