# LLD: SLICE-0009 - Google Fit + Google Login

**Author:** Architect
**Date:** 2026-03-17
**HLD Reference:** `docs/slices/SLICE-0009/hld.md`
**PRD Reference:** `docs/slices/SLICE-0009/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/
  integration/
    model/
      GoogleIntegrationAvailability.kt
      GoogleAccountSession.kt
      GoogleAuthState.kt
      FitPermissionState.kt
      ImportedActivitySample.kt
      ImportedBurnSample.kt
      GoogleIntegrationError.kt
      GoogleFitSyncSummary.kt
    auth/
      GoogleAuthService.kt
    fit/
      GoogleFitService.kt
    permission/
      PermissionCoordinator.kt
    mapping/
      BurnImportMapper.kt
      DefaultBurnImportMapper.kt
    sync/
      ImportedBurnSyncService.kt
      DefaultImportedBurnSyncService.kt
  presentation/
    integration/
      GoogleIntegrationUiState.kt
      GoogleIntegrationViewModel.kt
  ui/
    organisms/
      GoogleIntegrationStatusSection.kt
    screens/
      DashboardScreen.kt                          (modify)
    navigation/
      BurnMateNavigationDependencies.kt          (modify)
      BurnMateAppRoot.kt                         (modify)
      BurnMateNavigationHost.kt                  (modify)

composeApp/src/androidMain/kotlin/org/kalpeshbkundanani/burnmate/
  integration/
    auth/
      GoogleAuthServiceAndroid.kt
    fit/
      GoogleFitServiceAndroid.kt
    permission/
      AndroidPermissionCoordinator.kt
  platform/
    AndroidGoogleIntegrationLauncherHost.kt
    GoogleIntegrationConfiguration.kt
  MainActivity.kt                                (modify if activity-owned launcher host is required)

composeApp/src/androidMain/AndroidManifest.xml   (modify)

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/
  integration/
    mapping/
      DefaultBurnImportMapperTest.kt
    sync/
      DefaultImportedBurnSyncServiceTest.kt
  presentation/
    integration/
      GoogleIntegrationViewModelTest.kt
```

## Build Dependency Requirements

Add only the Android dependencies required by this slice:

| File | Alias / Coordinate | Version | Purpose |
|---|---|---|---|
| `gradle/libs.versions.toml` | `androidx-credentials = "1.6.0-rc02"` | `1.6.0-rc02` | Credential Manager core |
| `gradle/libs.versions.toml` | `googleid = "1.2.0"` | `1.2.0` | Sign in with Google for Credential Manager |
| `gradle/libs.versions.toml` | `play-services-auth = "21.5.1"` | `21.5.1` | Google account / Fit permission interoperability |
| `gradle/libs.versions.toml` | `play-services-fitness = "21.3.0"` | `21.3.0` | Google Fit history reads |

Required version-catalog entries:

```toml
[libraries]
androidx-credentials = { module = "androidx.credentials:credentials", version.ref = "androidx-credentials" }
androidx-credentials-play-services-auth = { module = "androidx.credentials:credentials-play-services-auth", version.ref = "androidx-credentials" }
googleid = { module = "com.google.android.libraries.identity.googleid:googleid", version.ref = "googleid" }
play-services-auth = { module = "com.google.android.gms:play-services-auth", version.ref = "play-services-auth" }
play-services-fitness = { module = "com.google.android.gms:play-services-fitness", version.ref = "play-services-fitness" }
```

Required `composeApp/build.gradle.kts` additions:

- `androidMain.dependencies { implementation(libs.androidx.credentials) }`
- `androidMain.dependencies { implementation(libs.androidx.credentials.play.services.auth) }`
- `androidMain.dependencies { implementation(libs.googleid) }`
- `androidMain.dependencies { implementation(libs.play.services.auth) }`
- `androidMain.dependencies { implementation(libs.play.services.fitness) }`

Required Android manifest addition:

- `<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />`

No new Compose UI dependency is required for this slice.

## Interfaces / APIs

### `GoogleAuthService`

```kotlin
package org.kalpeshbkundanani.burnmate.integration.auth

import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState

sealed interface GoogleAuthLaunchResult {
    data class Success(val session: GoogleAccountSession) : GoogleAuthLaunchResult
    data object Cancelled : GoogleAuthLaunchResult
    data class Failure(val error: Throwable) : GoogleAuthLaunchResult
}

interface GoogleAuthService {
    fun readCachedState(): GoogleAuthState
    suspend fun signIn(): GoogleAuthLaunchResult
    suspend fun disconnect(): Result<Unit>
}
```

Behavior:

- `readCachedState()` returns `SignedOut` when no cached Google account exists.
- `signIn()` owns the Android sign-in launch and returns `Success`, `Cancelled`, or `Failure`.
- `disconnect()` clears Google account access for future imports only.
- No raw Credential Manager or Google SDK response type may escape this contract.

### `PermissionCoordinator`

```kotlin
package org.kalpeshbkundanani.burnmate.integration.permission

import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession

sealed interface FitPermissionRequestResult {
    data object Granted : FitPermissionRequestResult
    data object Denied : FitPermissionRequestResult
    data object Cancelled : FitPermissionRequestResult
    data class Failure(val error: Throwable) : FitPermissionRequestResult
}

interface PermissionCoordinator {
    fun readState(session: GoogleAccountSession?): FitPermissionState
    suspend fun requestPermissions(session: GoogleAccountSession): FitPermissionRequestResult
}
```

Behavior:

- Owns both Android runtime `ACTIVITY_RECOGNITION` permission and Google Fit OAuth permission checks.
- Returns `Required`, `Granted`, `Denied`, or `Unavailable` through `FitPermissionState`.
- Uses Android launchers internally; no activity result types may leave `androidMain`.

### `GoogleFitService`

```kotlin
package org.kalpeshbkundanani.burnmate.integration.fit

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample

interface GoogleFitService {
    fun availability(): GoogleIntegrationAvailability
    suspend fun readDailyActivity(
        session: GoogleAccountSession,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ImportedActivitySample>>

    suspend fun disconnect(session: GoogleAccountSession): Result<Unit>
}
```

Behavior:

- Reads daily Google Fit data only after auth and permissions succeed.
- Must request Google Fit data types for steps and calories read access only.
- Returns normalized daily samples with no raw SDK data types.

### `BurnImportMapper`

```kotlin
package org.kalpeshbkundanani.burnmate.integration.mapping

import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample

interface BurnImportMapper {
    fun map(samples: List<ImportedActivitySample>): List<ImportedBurnSample>
}
```

Behavior:

- Pure mapping layer only.
- Prefers Google Fit calories when present.
- Falls back to a documented deterministic step-based estimate when calories are absent.
- Produces deterministic IDs and timestamps for repository sync.

### `ImportedBurnSyncService`

```kotlin
package org.kalpeshbkundanani.burnmate.integration.sync

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.ImportedBurnSample

interface ImportedBurnSyncService {
    fun sync(
        startDate: LocalDate,
        endDate: LocalDate,
        samples: List<ImportedBurnSample>
    ): Result<GoogleFitSyncSummary>
}
```

Behavior:

- Deletes only integration-owned entries in the target date range.
- Writes imported burn as negative `CalorieAmount` values using existing `CalorieEntry` and `EntryRepository`.
- Preserves all user-created manual entries.

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.integration.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class GoogleIntegrationAvailability {
    Available,
    UnsupportedPlatform,
    ConfigurationMissing,
    FitProjectUnavailable
}

data class GoogleAccountSession(
    val subjectId: String,
    val displayName: String?,
    val email: String?
)

sealed interface GoogleAuthState {
    data object SignedOut : GoogleAuthState
    data object Authenticating : GoogleAuthState
    data class SignedIn(val session: GoogleAccountSession) : GoogleAuthState
}

enum class FitPermissionState {
    Unknown,
    Required,
    Requesting,
    Granted,
    Denied,
    Unavailable
}

data class ImportedActivitySample(
    val date: LocalDate,
    val stepCount: Int?,
    val activeCalories: Int?,
    val source: String = "google_fit"
)

data class ImportedBurnSample(
    val entryId: String,
    val date: LocalDate,
    val burnCalories: Int,
    val createdAt: Instant,
    val source: String = "google_fit"
)

sealed class GoogleIntegrationError(message: String) : IllegalStateException(message) {
    data object Unavailable : GoogleIntegrationError("integration unavailable")
    data object SignInCancelled : GoogleIntegrationError("sign in cancelled")
    data object PermissionDenied : GoogleIntegrationError("permission denied")
    data class SignInFailed(val detail: String) : GoogleIntegrationError(detail)
    data class ImportFailed(val detail: String) : GoogleIntegrationError(detail)
}

data class GoogleFitSyncSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val importedEntries: Int,
    val importedDays: Int
)
```

## Presentation Contracts

### `GoogleIntegrationUiState`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.integration

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.integration.model.FitPermissionState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAuthState
import org.kalpeshbkundanani.burnmate.integration.model.GoogleFitSyncSummary
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

enum class GoogleIntegrationPhase {
    SignedOut,
    Authenticating,
    SignedIn,
    PermissionRequired,
    Syncing,
    Imported,
    Error,
    Unavailable
}

data class GoogleIntegrationUiState(
    val phase: GoogleIntegrationPhase = GoogleIntegrationPhase.SignedOut,
    val availability: GoogleIntegrationAvailability = GoogleIntegrationAvailability.Available,
    val authState: GoogleAuthState = GoogleAuthState.SignedOut,
    val permissionState: FitPermissionState = FitPermissionState.Unknown,
    val importAnchorDate: LocalDate,
    val importWindowDays: Int = 30,
    val syncSummary: GoogleFitSyncSummary? = null,
    val message: UiMessage? = null
)
```

### `GoogleIntegrationViewModel`

Dependencies:

- `GoogleAuthService`
- `PermissionCoordinator`
- `GoogleFitService`
- `BurnImportMapper`
- `ImportedBurnSyncService`
- `SelectedDateCoordinator`

Output contract:

```kotlin
class GoogleIntegrationViewModel(
    private val authService: GoogleAuthService,
    private val permissionCoordinator: PermissionCoordinator,
    private val fitService: GoogleFitService,
    private val burnImportMapper: BurnImportMapper,
    private val importedBurnSyncService: ImportedBurnSyncService,
    initialDate: LocalDate,
    private val selectedDateCoordinator: SelectedDateCoordinator
) : ViewModel() {
    val uiState: StateFlow<GoogleIntegrationUiState>
    val importAppliedEvent: StateFlow<GoogleFitSyncSummary?>
    fun onEvent(event: GoogleIntegrationEvent)
    fun consumeImportAppliedEvent()
}
```

Required events:

```kotlin
sealed interface GoogleIntegrationEvent {
    data object Load : GoogleIntegrationEvent
    data object SignInClicked : GoogleIntegrationEvent
    data object GrantPermissionsClicked : GoogleIntegrationEvent
    data object RefreshImportClicked : GoogleIntegrationEvent
    data object DisconnectClicked : GoogleIntegrationEvent
    data object DismissMessage : GoogleIntegrationEvent
}
```

Responsibilities:

- Read cached auth and permission state when the dashboard or selected date changes.
- Transition to `Authenticating`, `PermissionRequired`, `Syncing`, `Imported`, `Error`, or `Unavailable` deterministically.
- Publish an `importAppliedEvent` after successful sync so `BurnMateAppRoot` can reload the existing dashboard and daily logging view models.
- Avoid all raw Android and Google SDK types.

## Android-Specific Implementations

### `GoogleAuthServiceAndroid`

Responsibilities:

- Use Credential Manager plus Sign in with Google to obtain a Google account session.
- Normalize the Android result into `GoogleAccountSession`.
- Return `Cancelled` when the user backs out or closes the sign-in flow.
- Translate adapter errors into `GoogleIntegrationError.SignInFailed`.

### `AndroidPermissionCoordinator`

Responsibilities:

- Check `ACTIVITY_RECOGNITION` runtime permission on Android.
- Build a Google Fit `FitnessOptions` request for:
  - `DataType.TYPE_STEP_COUNT_DELTA`
  - `DataType.TYPE_CALORIES_EXPENDED`
- Request Google Fit read access only when missing.
- Map runtime or Google Fit denial to `FitPermissionRequestResult.Denied`.

### `GoogleFitServiceAndroid`

Responsibilities:

- Report `FitProjectUnavailable` when Android configuration or Google Fit project access is missing.
- Read daily aggregated Google Fit data for each day in the requested range.
- Normalize each day into `ImportedActivitySample(date, stepCount, activeCalories)`.
- Own any Google Fit SDK pagination, request objects, and disconnect calls internally.

## Algorithms

### Integration load algorithm

```text
1. Read `fitService.availability()`.
2. If not `Available`, publish `Unavailable` state with explanatory message and stop.
3. Read cached auth from `authService.readCachedState()`.
4. If signed out, publish `SignedOut`.
5. If signed in, read permission state from `permissionCoordinator.readState(session)`.
6. If permission is `Granted`, publish `SignedIn`.
7. If permission is `Required` or `Denied`, publish `PermissionRequired`.
```

### Sign-in algorithm

```text
1. Publish `Authenticating`.
2. Call `authService.signIn()`.
3. On `Cancelled`, publish `SignedOut` plus non-error message.
4. On `Failure`, publish `Error`.
5. On `Success`, read permission state.
6. If permissions are missing, publish `PermissionRequired`.
7. If permissions are already granted, start import for the current 30-day window.
```

### Permission request algorithm

```text
1. Require a signed-in session.
2. Publish `PermissionRequired` with in-progress marker.
3. Call `permissionCoordinator.requestPermissions(session)`.
4. On `Granted`, start import for the current 30-day window.
5. On `Denied` or `Cancelled`, remain in `PermissionRequired` with user-visible message.
6. On `Failure`, publish `Error`.
```

### Burn mapping algorithm

```text
1. Sort Google Fit samples by `date`.
2. For each sample:
   - If `activeCalories` is non-null and > 0, use that as `burnCalories`.
   - Else if `stepCount` is non-null and > 0, compute `burnCalories = round(stepCount * 0.04)`.
   - Else skip the sample for burn import.
3. Build `entryId = "googlefit:${date}:burn"`.
4. Build `createdAt` as a deterministic midday UTC instant for the sample date.
5. Return `ImportedBurnSample` list in chronological order.
```

### Sync algorithm

```text
1. Determine `startDate = anchorDate - 29 days` and `endDate = anchorDate`.
2. Fetch existing repository entries for the range.
3. Delete only entries whose `EntryId.value` starts with `googlefit:`.
4. For each mapped burn sample, create `CalorieEntry` with:
   - `id = EntryId(sample.entryId)`
   - `date = EntryDate(sample.date)`
   - `amount = CalorieAmount(-sample.burnCalories)`
   - `createdAt = sample.createdAt`
5. Insert each new entry through `EntryRepository.create`.
6. Return `GoogleFitSyncSummary(startDate, endDate, importedEntries, importedDays)`.
7. If any delete or create operation fails, return failure and do not publish `importAppliedEvent`.
```

### Disconnect algorithm

```text
1. Read the current signed-in session.
2. Call `fitService.disconnect(session)` and `authService.disconnect()`.
3. Do not delete repository entries.
4. Publish `SignedOut` with a non-error message indicating sync is disconnected.
```

## Navigation / UI Wiring

- `DashboardScreen` gains:
  - `integrationState: GoogleIntegrationUiState`
  - `onIntegrationEvent: (GoogleIntegrationEvent) -> Unit`
- `GoogleIntegrationStatusSection` is rendered in the existing dashboard content column below summary cards and above action cards.
- `BurnMateAppRoot` instantiates `GoogleIntegrationViewModel` with the same `SelectedDateCoordinator` used by dashboard and daily logging.
- `BurnMateAppRoot` listens to `importAppliedEvent`; when received, it calls:
  - `dashboardViewModel?.onEvent(DashboardEvent.Retry)`
  - `dailyLoggingViewModel.onEvent(DailyLoggingEvent.Load)`

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| Google Fit availability | Must be `Available` before sign-in or import proceeds | `INTEGRATION_UNAVAILABLE` |
| Import window | Must always be exactly 30 days ending on the selected anchor date | `INVALID_IMPORT_WINDOW` |
| `ImportedActivitySample.activeCalories` | If present, must be `>= 0` | `INVALID_ACTIVE_CALORIES` |
| `ImportedActivitySample.stepCount` | If present, must be `>= 0` | `INVALID_STEP_COUNT` |
| `ImportedBurnSample.burnCalories` | Must be `> 0` before repository sync | `INVALID_BURN_SAMPLE` |
| Integration-owned repository IDs | Must start with `googlefit:` | `INVALID_INTEGRATION_ID` |

## Error Handling Contracts

| Error Condition | Error Code | Recovery |
|---|---|---|
| Google Fit unavailable for this build or project | `INTEGRATION_UNAVAILABLE` | Show `Unavailable` state; user cannot proceed in this slice |
| Sign-in cancelled by user | `SIGN_IN_CANCELLED` | Return to `SignedOut` with neutral message |
| Sign-in failed | `SIGN_IN_FAILED` | Show `Error` with retry CTA |
| Runtime or Google Fit permission denied | `PERMISSION_DENIED` | Show `PermissionRequired` with retry CTA |
| Google Fit read failed | `IMPORT_FAILED` | Show `Error` and preserve prior repository contents |
| Sync delete/create failure | `SYNC_FAILED` | Show `Error`; do not emit dashboard/logging refresh event |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Signed-out state initialization | `availability=Available`, no cached session | `GoogleIntegrationUiState.phase == SignedOut` |
| T-02 | Successful sign-in transitions to permission evaluation | `GoogleAuthLaunchResult.Success(session)` and permission `Required` | `phase == PermissionRequired`, session retained |
| T-03 | Sign-in cancellation handling | `GoogleAuthLaunchResult.Cancelled` | `phase == SignedOut`, non-error message shown |
| T-04 | Permission denied handling | `FitPermissionRequestResult.Denied` | `phase == PermissionRequired`, retry available |
| T-05 | Permission granted starts import and publishes imported state | `FitPermissionRequestResult.Granted`, valid samples, successful sync | `phase == Imported`, `importAppliedEvent` emitted |
| T-06 | Activity samples map deterministically to burn samples | Mixed calories and steps-only samples | Chronological `ImportedBurnSample` list with stable IDs and expected calorie values |
| T-07 | Sync replaces only Google-owned entries | Existing manual entries plus existing `googlefit:` entries in range | Manual entries preserved, prior Google-owned entries replaced, no duplicates |
| T-08 | Dashboard/logging refresh event only fires after successful sync | Successful sync vs failed sync | Refresh event emitted only on success |
| T-09 | Disconnect clears connection state without deleting imported history | Signed-in session and existing imported entries | `phase == SignedOut`, repository untouched |
| T-10 | Android adapter boundary hides raw SDK types | Common/presentation compilation surface | No Google SDK or Android launcher types appear in common interfaces/models |

## Explicit Guards

- No Apple Health, HealthKit, or Health Connect implementation in this slice.
- No settings redesign, export, or reset behavior in this slice.
- No unrelated profile, logging, weight, dashboard, or calorie-debt redesign.
- No business logic in composables.
- No raw Google SDK types escaping adapter boundaries.

## Definition of Done - CODE_COMPLETE Checklist

The Engineer must satisfy ALL of the following before transitioning to `CODE_COMPLETE`:

- [ ] All interfaces and Android adapters above are implemented
- [ ] All data models and UI states are created
- [ ] All validation rules are enforced
- [ ] All error handling contracts are implemented
- [ ] All unit test cases above are written and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code
- [ ] Code compiles without errors on Android and shared targets
- [ ] No features beyond this LLD are implemented
