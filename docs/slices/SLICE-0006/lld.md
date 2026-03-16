# LLD: SLICE-0006 — Dashboard Read Model

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0006/hld.md`
**PRD Reference:** `docs/slices/SLICE-0006/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/dashboard/
  model/
    DashboardSnapshot.kt
    TodaySummary.kt
    DebtSummary.kt
    WeightSummary.kt
    DebtChartPoint.kt
  domain/
    DashboardReadModelService.kt
    DefaultDashboardReadModelService.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/dashboard/
  DefaultDashboardReadModelServiceTest.kt
```

## Interfaces / APIs

### `DashboardReadModelService`

```kotlin
package org.kalpeshbkundanani.burnmate.dashboard.domain

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.dashboard.model.DashboardSnapshot

interface DashboardReadModelService {
    fun getDashboardSnapshot(today: LocalDate): Result<DashboardSnapshot>
}
```

Behavior:
- Accepts a `LocalDate` representing the target day.
- Aggregates data from the logging, calorie debt, weight, and profile domains.
- Returns `Result.success(DashboardSnapshot)` with all available summaries.
- Null-safe: if weight or debt data is unavailable, the corresponding summary field is `null`.
- Never performs mutations on any upstream domain module.
- Never persists, caches, or stores the snapshot.
- Deterministic: identical inputs always produce identical output.

### `DefaultDashboardReadModelService`

```kotlin
package org.kalpeshbkundanani.burnmate.dashboard.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.domain.CalorieDebtCalculator
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.weight.domain.WeightHistoryService

class DefaultDashboardReadModelService(
    private val entryRepository: EntryRepository,
    private val debtCalculator: CalorieDebtCalculator,
    private val weightHistoryService: WeightHistoryService,
    private val bodyMetrics: BodyMetrics,
    private val dailyTargetCalories: Int,
    private val chartWindowDays: Int = DEFAULT_CHART_WINDOW_DAYS
) : DashboardReadModelService {
    companion object {
        const val DEFAULT_CHART_WINDOW_DAYS: Int = 7
    }
}
```

## Classes

| Class | Type | Responsibility | Dependencies |
|---|---|---|---|
| `DefaultDashboardReadModelService` | Class | Orchestrates read-only data collection from all domain modules and assembles a `DashboardSnapshot` | `EntryRepository` (SLICE-0004), `CalorieDebtCalculator` (SLICE-0002), `WeightHistoryService` (SLICE-0005), `BodyMetrics` (SLICE-0003) |
| `DashboardSnapshot` | Data class | Top-level immutable container for all dashboard data | None |
| `TodaySummary` | Data class | Today's calorie totals: intake, burn, net, remaining, and daily target | None |
| `DebtSummary` | Data class | Current debt value, severity, and trend | None |
| `WeightSummary` | Data class | Current weight, goal weight, remaining kg, and progress percentage | None |
| `DebtChartPoint` | Data class | Single chart data point with date and cumulative debt | None |

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.dashboard.model

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend

data class TodaySummary(
    val totalIntakeCalories: Int,
    val totalBurnCalories: Int,
    val netCalories: Int,
    val remainingCalories: Int,
    val dailyTargetCalories: Int
)

data class DebtSummary(
    val currentDebtCalories: Int,
    val severity: CalorieDebtSeverity,
    val trend: CalorieDebtTrend
)

data class WeightSummary(
    val currentWeightKg: Double,
    val goalWeightKg: Double,
    val remainingKg: Double,
    val progressPercentage: Double
)

data class DebtChartPoint(
    val date: LocalDate,
    val cumulativeDebtCalories: Int
)

data class DashboardSnapshot(
    val snapshotDate: LocalDate,
    val todaySummary: TodaySummary,
    val debtSummary: DebtSummary?,
    val weightSummary: WeightSummary?,
    val debtChartPoints: List<DebtChartPoint>
)
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `chartWindowDays` | Must be `>= 1` | `INVALID_CHART_WINDOW` |

No other validation is required — the read model consumes already-validated data from upstream domains.

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Chart window days less than 1 | N/A (domain module) | `INVALID_CHART_WINDOW` | Caller supplies a positive integer |
| Entry repository failure | N/A (domain module) | Propagated from upstream | Caller handles the upstream error |
| Debt calculation failure | N/A (domain module) | N/A | `debtSummary` set to `null`, `debtChartPoints` set to empty list |
| Weight data unavailable | N/A (domain module) | N/A | `weightSummary` set to `null` |

## Algorithms

### Collect today logs algorithm

```text
1. Construct an `EntryDate` from the `today` parameter.
2. Call `entryRepository.fetchByDate(EntryDate(today))`.
3. If the result is a failure, propagate the error.
4. Return the list of `CalorieEntry` for today.
```

### Calculate today totals algorithm

```text
1. Partition today's calorie entries by `CalorieAmount.value`:
   - Intake: entries where `amount.value > 0`
   - Burn: entries where `amount.value < 0`
2. `totalIntakeCalories = sum of amount.value for all intake entries`
3. `totalBurnCalories = sum of abs(amount.value) for all burn entries`
4. `netCalories = totalIntakeCalories - totalBurnCalories`
5. `remainingCalories = dailyTargetCalories - totalIntakeCalories`
6. Construct `TodaySummary(totalIntakeCalories, totalBurnCalories, netCalories, remainingCalories, dailyTargetCalories)`.
```

### Read current debt algorithm

```text
1. Compute `startDate = today - (chartWindowDays - 1) days`.
2. Construct `CalculationWindow(startDate, today, dailyTargetCalories)`.
3. Collect `DailyCalorieEntry` list from today's calorie entries.
   - Group all entries by date and sum consumed calories per day.
4. Call `debtCalculator.calculate(window, dailyEntries)`.
5. If the result is a failure, return `null` for `DebtSummary` and empty list for chart points.
6. Extract `DebtSummary(finalDebtCalories, severity, latestTrend)` from the `CalorieDebtResult`.
```

### Read weight progress algorithm

```text
1. Call `weightHistoryService.getWeightHistory()`.
2. If the result is a failure or the list is empty, return `null`.
3. Take the last entry (most recent by chronological order) as the current weight.
4. Read `goalWeightKg` from `bodyMetrics`.
5. If `currentWeightKg <= goalWeightKg`, progress is 100%.
6. Otherwise:
   - `remainingKg = currentWeightKg - goalWeightKg`
   - `totalToLose = bodyMetrics.currentWeightKg - goalWeightKg`
   - If `totalToLose <= 0`, progress is 100%.
   - Else `progressPercentage = ((totalToLose - remainingKg) / totalToLose) * 100.0`
7. Construct `WeightSummary(currentWeightKg, goalWeightKg, remainingKg, progressPercentage)`.
```

### Prepare debt chart algorithm

```text
1. If the debt calculation result is available:
   - Map each `CalorieDebtDay` from the result's `days` list to a `DebtChartPoint(date, endingDebtCalories)`.
   - Return the list sorted by date ascending.
2. If the debt calculation result is unavailable, return an empty list.
```

### Get dashboard snapshot algorithm

```text
1. Validate `chartWindowDays >= 1`; return failure if not.
2. Call `collectTodayLogs(today)` to get today's entries. If failure, propagate.
3. Call `calculateTodayTotals(entries)` to build `TodaySummary`.
4. Call `readCurrentDebt(today, entries)` to get `DebtSummary?` and `List<DebtChartPoint>`.
5. Call `readWeightProgress()` to get `WeightSummary?`.
6. Assemble `DashboardSnapshot(today, todaySummary, debtSummary, weightSummary, debtChartPoints)`.
7. Return `Result.success(snapshot)`.
```

## Persistence Schema Changes

Not applicable for this slice. The dashboard read model has no persistence layer.

## External Integration Contracts

None. This slice does not call external services.

## Method Signatures

```kotlin
fun DefaultDashboardReadModelService.getDashboardSnapshot(
    today: LocalDate
): Result<DashboardSnapshot>
```

Internal methods (private):

```kotlin
private fun collectTodayLogs(
    today: LocalDate
): Result<List<CalorieEntry>>

private fun calculateTodayTotals(
    entries: List<CalorieEntry>
): TodaySummary

private fun readCurrentDebt(
    today: LocalDate,
    todayEntries: List<CalorieEntry>
): Pair<DebtSummary?, List<DebtChartPoint>>

private fun readWeightProgress(): WeightSummary?

private fun prepareDebtChart(
    days: List<CalorieDebtDay>
): List<DebtChartPoint>
```

## Dependencies

| Dependency | Purpose |
|---|---|
| Kotlin standard library | Collection operations, immutable models |
| `kotlinx-datetime` | `LocalDate` for date handling, `DatePeriod` for chart window |
| `kotlin.test` | Shared unit tests |
| SLICE-0004 `EntryRepository` | Read today's calorie log entries |
| SLICE-0004 `CalorieEntry`, `CalorieAmount`, `EntryDate` | Logging domain models |
| SLICE-0002 `CalorieDebtCalculator` | Compute calorie debt for the dashboard |
| SLICE-0002 `CalculationWindow`, `DailyCalorieEntry`, `CalorieDebtResult`, `CalorieDebtDay` | Debt domain models |
| SLICE-0002 `CalorieDebtSeverity`, `CalorieDebtTrend` | Debt enums used in `DebtSummary` |
| SLICE-0005 `WeightHistoryService` | Fetch weight history |
| SLICE-0005 `WeightEntry`, `WeightValue` | Weight domain models |
| SLICE-0003 `BodyMetrics` | Profile model for goal weight and starting weight |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Snapshot generation with all data available | Today's entries: intake 500, 300; burn -200. Weight: 80kg, goal: 70kg. Debt result: 150 cal debt. Target: 2000. | `Result.success(DashboardSnapshot)` with `TodaySummary(800, 200, 600, 1200, 2000)`, valid `DebtSummary`, valid `WeightSummary`, non-empty chart points |
| T-02 | Intake aggregation sums all positive calorie entries | Entries: [+500, +300, +200] | `TodaySummary.totalIntakeCalories == 1000` |
| T-03 | Burn aggregation sums absolute values of all negative calorie entries | Entries: [-200, -150] | `TodaySummary.totalBurnCalories == 350` |
| T-04 | Net calories equals intake minus burn | Intake: 800, Burn: 350 | `TodaySummary.netCalories == 450` |
| T-05 | Remaining calories equals target minus intake | Target: 2000, Intake: 800 | `TodaySummary.remainingCalories == 1200` |
| T-06 | Weight progress computed correctly | Current: 80kg, Goal: 70kg, Starting: 90kg | `WeightSummary(80.0, 70.0, 10.0, 50.0)` — 50% progress |
| T-07 | Chart dataset maps debt days to chart points chronologically | Debt days for 3 dates with ending debts 100, 200, 150 | `debtChartPoints` has 3 entries ordered by date with matching cumulative values |
| T-08 | Deterministic snapshot: two calls with same inputs yield identical results | Same entries, same weight, same debt | `snapshot1 == snapshot2` |
| T-09 | Empty logging day returns zero totals | No calorie entries for today | `TodaySummary(0, 0, 0, 2000, 2000)` — all zeros except target and remaining |
| T-10 | Multi-day chart data covers the full window | 7-day window with entries on days 1, 3, 5, 7 | `debtChartPoints` list has entries matching the debt calculator's day output for the window |

## Definition of Done — CODE_COMPLETE Checklist

The Engineer must satisfy ALL of the following before transitioning to `CODE_COMPLETE`:

- [ ] All interfaces/APIs above are implemented
- [ ] All data models are created
- [ ] All validation rules are enforced
- [ ] All error handling contracts are implemented
- [ ] All unit test cases above are written and passing
- [ ] No TODO/FIXME/HACK comments remain in slice code
- [ ] Code compiles/lints without errors
- [ ] No features beyond this LLD are implemented
