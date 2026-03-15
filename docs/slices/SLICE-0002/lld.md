# LLD: SLICE-0002 — Calorie Debt Engine

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0002/hld.md`
**PRD Reference:** `docs/slices/SLICE-0002/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt/
  model/
    DailyCalorieEntry.kt
    CalculationWindow.kt
    CalorieDebtDay.kt
    CalorieDebtResult.kt
    CalorieDebtTrend.kt
    CalorieDebtSeverity.kt
    CalorieDebtError.kt
  domain/
    CalorieDebtCalculator.kt
    DefaultCalorieDebtCalculator.kt
    CalorieDebtValidator.kt
    DefaultCalorieDebtValidator.kt
    DebtTrendClassifier.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/caloriedebt/
  DefaultCalorieDebtCalculatorTest.kt
  DefaultCalorieDebtValidatorTest.kt
```

## Interfaces / APIs

### `CalorieDebtCalculator`

```kotlin
package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

interface CalorieDebtCalculator {
    fun calculate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<CalorieDebtResult>
}
```

Behavior:
- Returns `Result.success(CalorieDebtResult)` when validation passes.
- Returns `Result.failure(CalorieDebtError.Validation)` for invalid input.
- Never performs I/O, persistence, or platform calls.

### `CalorieDebtValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtError
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry

interface CalorieDebtValidator {
    fun validate(
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<Unit>
}
```

### `DebtTrendClassifier`

```kotlin
package org.kalpeshbkundanani.burnmate.caloriedebt.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtDay
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtSeverity
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtTrend

interface DebtTrendClassifier {
    fun classifyLatestTrend(days: List<CalorieDebtDay>): CalorieDebtTrend
    fun classifySeverity(finalDebtCalories: Int): CalorieDebtSeverity
    fun calculateDebtStreak(days: List<CalorieDebtDay>): Int
}
```

### `DefaultCalorieDebtCalculator`

```kotlin
package org.kalpeshbkundanani.burnmate.caloriedebt.domain

class DefaultCalorieDebtCalculator(
    private val validator: CalorieDebtValidator = DefaultCalorieDebtValidator(),
    private val trendClassifier: DebtTrendClassifier = DefaultDebtTrendClassifier()
) : CalorieDebtCalculator
```

## Classes

| Class | Type | Responsibility | Dependencies |
|---|---|---|---|
| `DefaultCalorieDebtCalculator` | Class | Orchestrates validation, date expansion, daily debt iteration, and result mapping | `CalorieDebtValidator`, `DebtTrendClassifier` |
| `DefaultCalorieDebtValidator` | Class | Validates range ordering, non-negative values, and duplicate dates | None |
| `DefaultDebtTrendClassifier` | Class | Maps final debt and latest-day movement to trend and severity values | None |
| `DailyCalorieEntry` | Data class | Input value for one day's consumed calories | None |
| `CalculationWindow` | Data class | Inclusive calculation range and target calories | None |
| `CalorieDebtDay` | Data class | Explainable breakdown row for one day | None |
| `CalorieDebtResult` | Data class | Aggregate debt summary returned to callers | None |
| `CalorieDebtError` | Sealed class | Domain error contract used in `Result.failure` | None |

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.caloriedebt.model

import kotlinx.datetime.LocalDate

data class DailyCalorieEntry(
    val date: LocalDate,
    val consumedCalories: Int
)

data class CalculationWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val targetCalories: Int
)

data class CalorieDebtDay(
    val date: LocalDate,
    val consumedCalories: Int,
    val targetCalories: Int,
    val dailyDeltaCalories: Int,
    val startingDebtCalories: Int,
    val endingDebtCalories: Int
)

data class CalorieDebtResult(
    val finalDebtCalories: Int,
    val days: List<CalorieDebtDay>,
    val latestTrend: CalorieDebtTrend,
    val debtStreakDays: Int,
    val severity: CalorieDebtSeverity
)

enum class CalorieDebtTrend {
    INCREASED,
    REDUCED,
    UNCHANGED,
    CLEARED
}

enum class CalorieDebtSeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

sealed class CalorieDebtError(message: String) : IllegalArgumentException(message) {
    data class Validation(
        val code: String,
        val detail: String
    ) : CalorieDebtError("$code: $detail")
}
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `CalculationWindow.startDate` + `endDate` | `startDate` must be on or before `endDate` | `INVALID_DATE_RANGE` |
| `CalculationWindow.targetCalories` | Must be `>= 0` | `INVALID_TARGET_CALORIES` |
| `DailyCalorieEntry.consumedCalories` | Must be `>= 0` | `INVALID_CONSUMED_CALORIES` |
| `entries[].date` | No duplicate dates allowed in the input list | `DUPLICATE_ENTRY_DATE` |
| `entries[].date` | Entries outside the requested range are allowed but ignored during timeline generation | N/A |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Start date is after end date | N/A (domain module) | `INVALID_DATE_RANGE` | Caller corrects the range and retries |
| Target calories is negative | N/A (domain module) | `INVALID_TARGET_CALORIES` | Caller supplies a non-negative target |
| Consumed calories is negative | N/A (domain module) | `INVALID_CONSUMED_CALORIES` | Caller corrects the invalid entry |
| Duplicate dates in entries | N/A (domain module) | `DUPLICATE_ENTRY_DATE` | Caller deduplicates and retries |

## Algorithms

### Calculation algorithm

```text
1. Validate `window` and `entries`; return the first validation failure if invalid.
2. Filter entries to dates inside `[startDate, endDate]`.
3. Build a lookup map from date to consumed calories.
4. Generate every inclusive date in the range in chronological order.
5. Initialize `previousEndingDebt = 0`.
6. For each date:
   a. `consumed = lookup[date] ?: 0`
   b. `delta = consumed - targetCalories`
   c. `startingDebt = previousEndingDebt`
   d. `endingDebt = maxOf(0, startingDebt + delta)`
   e. append `CalorieDebtDay(...)`
   f. set `previousEndingDebt = endingDebt`
7. Derive `finalDebtCalories` from the last day, or `0` if no days exist.
8. Classify `latestTrend`, `debtStreakDays`, and `severity`.
9. Return `CalorieDebtResult`.
```

### Trend classification algorithm

```text
1. If there are no days, return `UNCHANGED`.
2. Let `last` be the latest `CalorieDebtDay`.
3. If `last.startingDebtCalories > 0` and `last.endingDebtCalories == 0`, return `CLEARED`.
4. If `last.endingDebtCalories > last.startingDebtCalories`, return `INCREASED`.
5. If `last.endingDebtCalories < last.startingDebtCalories`, return `REDUCED`.
6. Otherwise return `UNCHANGED`.
```

### Severity classification algorithm

```text
1. If `finalDebtCalories == 0`, return `NONE`.
2. If `finalDebtCalories in 1..299`, return `LOW`.
3. If `finalDebtCalories in 300..699`, return `MEDIUM`.
4. Otherwise return `HIGH`.
```

## Persistence Schema Changes

Not applicable. This slice is a pure domain engine and does not modify storage.

## External Integration Contracts

None. This slice does not call external services.

## Method Signatures

```kotlin
fun DefaultCalorieDebtCalculator.calculate(
    window: CalculationWindow,
    entries: List<DailyCalorieEntry>
): Result<CalorieDebtResult>

fun DefaultCalorieDebtValidator.validate(
    window: CalculationWindow,
    entries: List<DailyCalorieEntry>
): Result<Unit>

fun DefaultDebtTrendClassifier.classifyLatestTrend(
    days: List<CalorieDebtDay>
): CalorieDebtTrend

fun DefaultDebtTrendClassifier.classifySeverity(
    finalDebtCalories: Int
): CalorieDebtSeverity

fun DefaultDebtTrendClassifier.calculateDebtStreak(
    days: List<CalorieDebtDay>
): Int
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `kotlinx-datetime` | `LocalDate` modeling and date progression |
| `kotlin.test` | Shared unit tests |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Over-target day creates debt | Window `2026-03-01..2026-03-01`, target `2000`, entries `[2026-03-01 -> 2300]` | `finalDebtCalories=300`, one day, trend `INCREASED`, severity `MEDIUM` |
| T-02 | Under-target day with no prior debt stays at zero | Window `2026-03-01..2026-03-01`, target `2000`, entries `[2026-03-01 -> 1700]` | `finalDebtCalories=0`, one day, trend `UNCHANGED`, severity `NONE` |
| T-03 | Under-target day reduces existing debt but does not go negative | Window `2026-03-01..2026-03-02`, target `2000`, entries `[2026-03-01 -> 2400, 2026-03-02 -> 1500]` | Day 1 ending debt `400`, day 2 ending debt `0`, trend `CLEARED` |
| T-04 | Missing date inside range produces zero-consumption row | Window `2026-03-01..2026-03-03`, target `2000`, entries `[2026-03-01 -> 2100, 2026-03-03 -> 2200]` | Three output days present; middle day uses `consumedCalories=0` |
| T-05 | Duplicate dates are rejected | Window `2026-03-01..2026-03-02`, target `2000`, entries `[2026-03-01 -> 2000, 2026-03-01 -> 2200]` | `Result.failure` with code `DUPLICATE_ENTRY_DATE` |
| T-06 | Inverted range is rejected | Window `2026-03-03..2026-03-01`, target `2000`, entries `[]` | `Result.failure` with code `INVALID_DATE_RANGE` |
| T-07 | Negative consumed calories are rejected | Window `2026-03-01..2026-03-01`, target `2000`, entries `[2026-03-01 -> -1]` | `Result.failure` with code `INVALID_CONSUMED_CALORIES` |
| T-08 | Entries outside the range are ignored | Window `2026-03-02..2026-03-02`, target `2000`, entries `[2026-03-01 -> 2500, 2026-03-02 -> 2100]` | One output day for `2026-03-02`, `finalDebtCalories=100` |
| T-09 | Severity thresholds map correctly | Final debts `0`, `1`, `299`, `300`, `699`, `700` | Severity `NONE`, `LOW`, `LOW`, `MEDIUM`, `MEDIUM`, `HIGH` |
| T-10 | Debt streak counts trailing days with ending debt above zero | Window `2026-03-01..2026-03-04`, target `2000`, entries `[2100, 2200, 1800, 2300]` by day | Final streak is `1` because only the latest day ends above zero consecutively |

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
