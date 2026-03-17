# LLD: SLICE-0008 - Charts & Visual Progress

**Author:** Architect
**Date:** 2026-03-17
**HLD Reference:** `docs/slices/SLICE-0008/hld.md`
**PRD Reference:** `docs/slices/SLICE-0008/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/
  presentation/
    dashboard/
      DashboardUiState.kt                 (modify)
      DashboardUiMapper.kt                (modify if summary formatting needs reuse only)
      DashboardViewModel.kt               (modify)
      charts/
        ChartRangeOption.kt               (new)
        DashboardVisualizationUiState.kt  (new)
        DashboardChartState.kt            (new)
        DashboardChartDataSource.kt       (new)
        DefaultDashboardChartDataSource.kt (new)
        DashboardChartStateAdapter.kt     (new)
  ui/
    components/
      charts/
        ChartSurface.kt                   (new)
        ChartRangeSelector.kt             (new)
        DebtTrendChart.kt                 (new)
        WeeklyDeficitBarChart.kt          (new)
        WeightTrendChart.kt               (new)
        GoalProgressRing.kt               (new)
    organisms/
      DashboardVisualProgressSection.kt   (new)
    screens/
      DashboardScreen.kt                  (modify)
    navigation/
      BurnMateNavigationDependencies.kt   (modify for chart data-source wiring only)

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/
  presentation/
    dashboard/
      DashboardViewModelTest.kt           (modify)
      charts/
        DashboardChartStateAdapterTest.kt (new)
        DefaultDashboardChartDataSourceTest.kt (new)
```

## Interfaces / APIs

### `ChartRangeOption`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

enum class ChartRangeOption(
    val days: Int,
    val label: String
) {
    Last7Days(days = 7, label = "7D"),
    Last14Days(days = 14, label = "14D"),
    Last30Days(days = 30, label = "30D")
}
```

Behavior:
- Defines the only valid dashboard visualization windows in this slice.
- `Last7Days` is the default selected range.
- Keeps range handling deterministic and avoids arbitrary custom input.

### `DashboardChartDataSource`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

interface DashboardChartDataSource {
    fun loadDebtChartSnapshot(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<DashboardSnapshot>

    fun loadWeightEntries(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<List<WeightEntry>>
}
```

Behavior:
- Reads only from existing dashboard and weight-history dependencies.
- Does not mutate repositories, domain models, or persisted state.
- Returns range-scoped read data for visualization only.

### `DefaultDashboardChartDataSource`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.domain.DashboardReadModelService
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

class DefaultDashboardChartDataSource(
    private val dashboardServiceFactory: (Int) -> DashboardReadModelService,
    private val weightHistoryService: WeightHistoryService
) : DashboardChartDataSource {
    override fun loadDebtChartSnapshot(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<DashboardSnapshot>

    override fun loadWeightEntries(
        selectedDate: LocalDate,
        range: ChartRangeOption
    ): Result<List<WeightEntry>>
}
```

Behavior:
- Builds a range-specific dashboard read-model service through the injected factory without modifying domain code.
- Requests `max(range.days, 8)` debt-history points so weekly bar deltas always have enough consecutive data.
- Uses `weightHistoryService.getWeightByDateRange(startDate, endDate)` for weight-chart data.

### `DashboardChartStateAdapter`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import org.kalpeshbkundanani.burnmate.dashboard.model.DebtChartPoint
import org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

class DashboardChartStateAdapter {
    fun map(
        range: ChartRangeOption,
        debtPoints: List<DebtChartPoint>,
        weightSummary: WeightSummary?,
        weightEntries: List<WeightEntry>
    ): DashboardChartState
}
```

Behavior:
- Pure mapping layer only.
- Formats labels, trims ranges, derives day-over-day deltas, and produces immutable chart state.
- Does not call repositories or services directly.

### Dashboard event extension

Add this event to the existing `DashboardEvent` sealed interface:

```kotlin
data class ChartRangeSelected(val range: ChartRangeOption) : DashboardEvent
```

Behavior:
- Updates visualization range only.
- Does not change the selected dashboard date.

## ViewModels

### `DashboardViewModel`

Dependencies:
- Existing `DashboardReadModelService` for base summary-card state
- `DashboardChartDataSource` for range-scoped chart inputs
- Existing `DashboardUiMapper` for summary cards
- New `DashboardChartStateAdapter` for visualization state
- Existing `SelectedDateCoordinator`

Required state changes:

```kotlin
data class DashboardUiState(
    val selectedDate: LocalDate,
    val status: LoadableUiState = LoadableUiState.Loading,
    val todaySummary: DashboardTodayCardState? = null,
    val debtSummary: DashboardDebtCardState? = null,
    val weightSummary: DashboardWeightCardState? = null,
    val visualization: DashboardVisualizationUiState = DashboardVisualizationUiState(),
    val emptyMessage: UiMessage? = null,
    val errorMessage: UiMessage? = null
)
```

Responsibilities:
- Load existing dashboard summary cards exactly as today for the selected date.
- Load or reload visualization state for the current `ChartRangeOption`.
- Preserve summary content while visualization-only range changes are in flight.
- Keep selected date and selected range synchronized across all dashboard visuals.

Private methods to add:

```kotlin
private fun loadDashboard(date: LocalDate = _uiState.value.selectedDate)
private fun loadVisualization(
    selectedDate: LocalDate,
    range: ChartRangeOption,
    weightSummary: org.kalpeshbkundanani.burnmate.dashboard.model.WeightSummary?
)
private fun updateVisualizationLoading(range: ChartRangeOption)
```

Event handling contract additions:

| Event | ViewModel Result |
|---|---|
| `ChartRangeSelected(range)` | Set `visualization.selectedRange`, mark visualization as loading, reload chart data only |
| `PreviousDayTapped` / `NextDayTapped` | Update selected date, reload summary cards, then reload charts for the same range |
| `Retry` | Retry summary and visualization loads for the current date/range |

## Data Models

### `DashboardVisualizationUiState`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState
import org.kalpeshbkundanani.burnmate.presentation.shared.UiMessage

data class DashboardVisualizationUiState(
    val selectedRange: ChartRangeOption = ChartRangeOption.Last7Days,
    val status: LoadableUiState = LoadableUiState.Loading,
    val charts: DashboardChartState? = null,
    val emptyMessage: UiMessage? = null,
    val errorMessage: UiMessage? = null
)
```

### `DashboardChartState`

```kotlin
package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

data class DashboardChartState(
    val debtTrend: DebtTrendChartState?,
    val weightTrend: WeightTrendChartState?,
    val weeklyDeficit: WeeklyDeficitChartState?,
    val progressRing: GoalProgressRingState?
)
```

### `DebtTrendChartState` and `DebtTrendPoint`

```kotlin
data class DebtTrendChartState(
    val points: List<DebtTrendPoint>,
    val minDebtCalories: Int,
    val maxDebtCalories: Int,
    val latestValueLabel: String
)

data class DebtTrendPoint(
    val date: LocalDate,
    val cumulativeDebtCalories: Int,
    val label: String
)
```

### `WeightTrendChartState` and `WeightTrendPoint`

```kotlin
data class WeightTrendChartState(
    val points: List<WeightTrendPoint>,
    val minWeightKg: Double,
    val maxWeightKg: Double,
    val latestValueLabel: String
)

data class WeightTrendPoint(
    val date: LocalDate,
    val weightKg: Double,
    val label: String
)
```

### `WeeklyDeficitChartState` and `WeeklyDeficitBar`

```kotlin
enum class DailyBalanceDirection {
    Deficit,
    Surplus,
    Neutral
}

data class WeeklyDeficitChartState(
    val bars: List<WeeklyDeficitBar>,
    val maxMagnitudeCalories: Int
)

data class WeeklyDeficitBar(
    val date: LocalDate,
    val deltaCalories: Int,
    val direction: DailyBalanceDirection,
    val label: String
)
```

### `GoalProgressRingState`

```kotlin
data class GoalProgressRingState(
    val progressFraction: Float,
    val progressLabel: String,
    val supportingLabel: String,
    val isGoalReached: Boolean
)
```

## Composable APIs

### `DashboardVisualProgressSection`

```kotlin
@Composable
fun DashboardVisualProgressSection(
    state: DashboardVisualizationUiState,
    onRangeSelected: (ChartRangeOption) -> Unit,
    modifier: Modifier = Modifier
)
```

Responsibilities:
- Render range selector, progress ring, debt chart, weekly bars, and weight trend inside the dashboard layout.
- Render loading, empty, and error states for the visualization section only.

### Reusable chart primitives

```kotlin
@Composable
fun ChartRangeSelector(
    selectedRange: ChartRangeOption,
    onRangeSelected: (ChartRangeOption) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun DebtTrendChart(
    state: DebtTrendChartState,
    modifier: Modifier = Modifier
)

@Composable
fun WeeklyDeficitBarChart(
    state: WeeklyDeficitChartState,
    modifier: Modifier = Modifier
)

@Composable
fun WeightTrendChart(
    state: WeightTrendChartState,
    modifier: Modifier = Modifier
)

@Composable
fun GoalProgressRing(
    state: GoalProgressRingState,
    modifier: Modifier = Modifier
)
```

## Data Mapping From Read Model to Chart Model

### Debt trend mapping

Input:
- `DashboardSnapshot.debtChartPoints`
- `ChartRangeOption`

Rules:
1. Sort `debtChartPoints` by `date` ascending.
2. Keep the last `range.days` points.
3. Map each point to `DebtTrendPoint(date, cumulativeDebtCalories, label)`.
4. `minDebtCalories` and `maxDebtCalories` come from the trimmed list.
5. `latestValueLabel` formats the last point's debt value as signed calories.

### Weekly deficit mapping

Input:
- Range-scoped `DashboardSnapshot.debtChartPoints`

Rules:
1. Sort points by `date` ascending.
2. Keep the last 8 points if available.
3. For each consecutive pair, compute `deltaCalories = current.cumulativeDebtCalories - previous.cumulativeDebtCalories`.
4. Map negative values to `Deficit`, positive values to `Surplus`, and `0` to `Neutral`.
5. Keep the last 7 computed deltas as `WeeklyDeficitBar` items.
6. If fewer than 2 points exist, return `null` for `weeklyDeficit` and let the visualization section render an empty state.

### Weight trend mapping

Input:
- `List<WeightEntry>`
- `ChartRangeOption`

Rules:
1. Sort entries by `(date, createdAt)`.
2. Group by `date` and keep the latest entry for each date.
3. Map each retained entry to `WeightTrendPoint(date, weight.kg, label)`.
4. `minWeightKg` and `maxWeightKg` come from the mapped list.
5. `latestValueLabel` uses the last point's weight value.

### Progress ring mapping

Input:
- Existing `WeightSummary`

Rules:
1. If `weightSummary == null`, return `null` for `progressRing`.
2. `progressFraction = (progressPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()`.
3. `progressLabel = "<rounded-progress>%"`.
4. `supportingLabel = "Goal reached"` when `remainingKg <= 0`, otherwise `"<remainingKg> kg to goal"`.
5. `isGoalReached = remainingKg <= 0`.

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `ChartRangeOption` | Must be one of the defined enum values only | `INVALID_CHART_RANGE` |
| `DashboardVisualizationUiState.status` | Must reflect `charts == null` whenever status is `Loading`, `Empty`, or `Error` | `INVALID_VISUALIZATION_STATE` |
| `WeeklyDeficitChartState.bars` | Must contain at most 7 bars and remain date-ordered | `INVALID_WEEKLY_DEFICIT_SERIES` |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Base dashboard snapshot load fails | N/A | propagated from upstream | Dashboard enters existing screen-level error state and supports retry |
| Chart data-source debt load fails | N/A | propagated from upstream | Summary cards stay visible; visualization state becomes `Error` |
| Weight-history range load fails | N/A | propagated from upstream | Weight chart becomes `Error` or `Empty` while debt visuals remain available |
| No debt points for selected range | N/A | `EMPTY_DEBT_HISTORY` | Visualization shows empty state copy for debt-based charts |
| No weight entries for selected range | N/A | `EMPTY_WEIGHT_HISTORY` | Visualization shows empty state copy for weight trend |

## Algorithms

### `DefaultDashboardChartDataSource.loadDebtChartSnapshot`

```text
1. Determine `requiredDays = max(range.days, 8)`.
2. Build a `DashboardReadModelService` using `dashboardServiceFactory(requiredDays)`.
3. Call `getDashboardSnapshot(selectedDate)`.
4. Return the result unchanged; no additional mutation occurs here.
```

### `DefaultDashboardChartDataSource.loadWeightEntries`

```text
1. Compute `startDate = selectedDate - (range.days - 1) days`.
2. Call `weightHistoryService.getWeightByDateRange(startDate, selectedDate)`.
3. Return the result unchanged to the adapter layer.
```

### `DashboardViewModel.loadDashboard`

```text
1. Mark top-level dashboard state as `Loading`.
2. Load the base `DashboardSnapshot` using the existing `dashboardService`.
3. Map summary cards through `DashboardUiMapper`.
4. Publish top-level dashboard content state.
5. Call `loadVisualization(selectedDate, selectedRange, baseSnapshot.weightSummary)`.
6. If base load fails, publish the existing dashboard error state and skip visualization mapping.
```

### `DashboardViewModel.loadVisualization`

```text
1. Mark `visualization.status = Loading` and preserve the selected range.
2. Request a range-scoped debt snapshot from `DashboardChartDataSource`.
3. Request range-scoped weight entries from `DashboardChartDataSource`.
4. Map both inputs plus base `weightSummary` through `DashboardChartStateAdapter`.
5. If all chart models are null/empty, publish `visualization.status = Empty`.
6. Otherwise publish `visualization.status = Content` with the new `DashboardChartState`.
7. On failure, publish `visualization.status = Error` and keep summary cards unchanged.
```

## Compose Chart Rendering Approach

- All chart primitives use Compose `Canvas` inside a shared `ChartSurface` container.
- X positions are distributed evenly across the drawable width based on point index.
- Y positions are normalized from the state's min/max value range; if min == max, render values on a centered horizontal guide.
- `DebtTrendChart` draws a single `Path` plus endpoint markers and textual latest-value annotation.
- `WeeklyDeficitBarChart` draws bars from a zero baseline; deficits extend below the baseline and surpluses above it.
- `GoalProgressRing` uses `drawArc` for track and progress, with the arc starting from the top-left quadrant for visual balance.
- Every chart includes visible text labels outside the canvas so data remains understandable without interpreting color alone.

## Persistence Schema Changes

Not applicable. This slice introduces no persistence changes.

## External Integration Contracts

None. This slice introduces no external integrations.

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Visualization state initializes with default range | Fresh `DashboardViewModel` | `visualization.selectedRange == Last7Days` and status enters loading/content deterministically |
| T-02 | Debt trend adapter maps chronological debt points | Unordered `DebtChartPoint` list across 7 days | `DebtTrendChartState.points` sorted ascending and trimmed correctly |
| T-03 | Weekly deficit adapter derives signed daily deltas | 8 consecutive cumulative debt points | 7 bars with correct positive/negative/neutral deltas |
| T-04 | Weight trend adapter de-duplicates same-day weights deterministically | Multiple `WeightEntry` records for one date | Latest `createdAt` entry for that date is used in the chart |
| T-05 | Progress ring adapter maps existing weight summary | `WeightSummary(progressPercentage=50.0, remainingKg=10.0)` | `progressFraction == 0.5f`, correct labels, `isGoalReached == false` |
| T-06 | Dashboard load populates summary cards and chart state | Successful base snapshot, debt snapshot, and weight entries | Dashboard state `Content` with non-null `visualization.charts` |
| T-07 | Chart range change reloads visualizations without changing selected date | Existing dashboard state, then `ChartRangeSelected(Last30Days)` | Same `selectedDate`, updated range, visualization reload invoked |
| T-08 | Missing weight history yields visualization empty state without screen failure | Successful debt snapshot, empty weight entries | Summary cards remain content; weight trend is null or empty-state driven |
| T-09 | Missing debt points yields explicit empty debt visualization | Debt snapshot with empty `debtChartPoints` | Debt trend and weekly bars are null; visualization handles empty state |
| T-10 | Identical dependency outputs yield identical visualization state | Same snapshots and weight entries across two ViewModels | Resulting `DashboardUiState` values are equal |

## Definition of Done - CODE_COMPLETE Checklist

The Engineer must satisfy ALL of the following before transitioning to `CODE_COMPLETE`:

- [ ] All interfaces/APIs above are implemented
- [ ] All chart and visualization data models are created
- [ ] All validation rules are enforced
- [ ] All error handling contracts are implemented
- [ ] All unit test cases above are written and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code
- [ ] Code compiles/lints without errors
- [ ] No features beyond this LLD are implemented
