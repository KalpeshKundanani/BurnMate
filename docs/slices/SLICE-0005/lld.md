# LLD: SLICE-0005 — Weight History + Debt Recalculation

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0005/hld.md`
**PRD Reference:** `docs/slices/SLICE-0005/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/weight/
  model/
    WeightEntry.kt
    WeightValue.kt
    WeightHistoryError.kt
    DebtRecalculationResult.kt
  domain/
    WeightEntryValidator.kt
    DefaultWeightEntryValidator.kt
    WeightHistoryService.kt
    DefaultWeightHistoryService.kt
    DebtRecalculationService.kt
    DefaultDebtRecalculationService.kt
  repository/
    WeightHistoryRepository.kt
    LocalWeightRepository.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/weight/
  DefaultWeightEntryValidatorTest.kt
  DefaultWeightHistoryServiceTest.kt
  LocalWeightRepositoryTest.kt
  DefaultDebtRecalculationServiceTest.kt
```

## Interfaces / APIs

### `WeightEntryValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface WeightEntryValidator {
    fun validate(weight: WeightValue): Result<Unit>
}
```

Behavior:
- Returns `Result.success(Unit)` when the weight value is within the valid range.
- Returns `Result.failure(WeightHistoryError.Validation)` when the weight is invalid.
- Never performs I/O, persistence, or platform calls.

### `WeightHistoryRepository`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.repository

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry

interface WeightHistoryRepository {
    fun save(entry: WeightEntry): Result<WeightEntry>
    fun update(entry: WeightEntry): Result<WeightEntry>
    fun deleteByDate(date: LocalDate): Result<Boolean>
    fun getByDate(date: LocalDate): Result<WeightEntry?>
    fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>>
    fun getAll(): Result<List<WeightEntry>>
}
```

Behavior:
- `save` returns the stored entry or `Result.failure(WeightHistoryError.DuplicateWeightDate)` if an entry for that date already exists.
- `update` replaces the entry for the given date or returns `Result.failure(WeightHistoryError.WeightEntryNotFound)` if no entry exists for that date.
- `deleteByDate` returns `Result.success(true)` when an entry was removed, `Result.success(false)` when no entry was found for that date.
- `getByDate` returns `Result.success(WeightEntry)` if found, `Result.success(null)` if not found.
- `getByDateRange` returns entries within the inclusive range in chronological order. Returns `Result.failure(WeightHistoryError.InvalidDateRange)` if `startDate` is after `endDate`. Returns an empty list if no entries exist in the range.
- `getAll` returns all entries in chronological order.

### `WeightHistoryService`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightEntry
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface WeightHistoryService {
    fun recordWeight(date: LocalDate, weight: WeightValue): Result<WeightEntry>
    fun editWeight(date: LocalDate, newWeight: WeightValue): Result<WeightEntry>
    fun deleteWeight(date: LocalDate): Result<Boolean>
    fun getWeightHistory(): Result<List<WeightEntry>>
    fun getWeightByDate(date: LocalDate): Result<WeightEntry?>
    fun getWeightByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<WeightEntry>>
}
```

Behavior:
- `recordWeight` validates the weight, checks for date uniqueness, persists via repository, and returns the saved entry.
- `editWeight` validates the new weight, updates the existing entry for the given date, and returns the updated entry.
- `deleteWeight` removes the entry for the given date and returns whether an entry was actually removed.
- All query methods delegate to the repository.

### `DebtRecalculationService`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalculationWindow
import org.kalpeshbkundanani.burnmate.caloriedebt.model.DailyCalorieEntry
import org.kalpeshbkundanani.burnmate.weight.model.DebtRecalculationResult
import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface DebtRecalculationService {
    fun recomputeDebt(
        newWeight: WeightValue,
        window: CalculationWindow,
        entries: List<DailyCalorieEntry>
    ): Result<DebtRecalculationResult>
}
```

Behavior:
- Derives an adjusted target calorie budget from the new weight value.
- Constructs an updated `CalculationWindow` with the adjusted target.
- Invokes `CalorieDebtCalculator.calculate()` from SLICE-0002 with the updated window and provided calorie entries.
- Returns the recalculated result wrapped in `DebtRecalculationResult`.
- Never modifies SLICE-0002 code; consumes its interface as a read-only dependency.

### `DefaultWeightEntryValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

class DefaultWeightEntryValidator(
    private val minWeightKg: Double = MIN_WEIGHT_KG,
    private val maxWeightKg: Double = MAX_WEIGHT_KG
) : WeightEntryValidator {
    companion object {
        const val MIN_WEIGHT_KG: Double = 0.5
        const val MAX_WEIGHT_KG: Double = 500.0
    }
}
```

### `DefaultWeightHistoryService`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.weight.repository.WeightHistoryRepository

class DefaultWeightHistoryService(
    private val validator: WeightEntryValidator = DefaultWeightEntryValidator(),
    private val repository: WeightHistoryRepository
) : WeightHistoryService
```

### `DefaultDebtRecalculationService`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.caloriedebt.domain.CalorieDebtCalculator

class DefaultDebtRecalculationService(
    private val calculator: CalorieDebtCalculator
) : DebtRecalculationService
```

### `LocalWeightRepository`

```kotlin
package org.kalpeshbkundanani.burnmate.weight.repository

class LocalWeightRepository : WeightHistoryRepository
```

## Classes

| Class | Type | Responsibility | Dependencies |
|---|---|---|---|
| `DefaultWeightEntryValidator` | Class | Validates weight values are within the realistic human range (0.5–500.0 kg) | None |
| `DefaultWeightHistoryService` | Class | Orchestrates validation, persistence, and query operations for weight entries | `WeightEntryValidator`, `WeightHistoryRepository` |
| `DefaultDebtRecalculationService` | Class | Recomputes calorie debt using a weight-adjusted target via `CalorieDebtCalculator` | `CalorieDebtCalculator` (SLICE-0002, read-only) |
| `LocalWeightRepository` | Class | In-memory implementation of `WeightHistoryRepository` using a `MutableMap<LocalDate, WeightEntry>` | None |
| `WeightEntry` | Data class | Immutable entity representing a single weight recording | None |
| `WeightValue` | Value class | Wraps a validated positive `Double` in kilograms | None |
| `WeightHistoryError` | Sealed class | Domain error contract for weight operations | None |
| `DebtRecalculationResult` | Data class | Wraps the recalculated `CalorieDebtResult` plus the triggering weight metadata | None |

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.weight.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.caloriedebt.model.CalorieDebtResult

@JvmInline
value class WeightValue(val kg: Double)

data class WeightEntry(
    val date: LocalDate,
    val weight: WeightValue,
    val createdAt: Instant
)

data class DebtRecalculationResult(
    val triggeringWeight: WeightValue,
    val adjustedTargetCalories: Int,
    val debtResult: CalorieDebtResult
)

sealed class WeightHistoryError(message: String) : IllegalArgumentException(message) {
    data class Validation(
        val code: String,
        val detail: String
    ) : WeightHistoryError("$code: $detail")

    data class DuplicateWeightDate(
        val date: LocalDate,
        val detail: String
    ) : WeightHistoryError("DUPLICATE_WEIGHT_DATE: $detail")

    data class WeightEntryNotFound(
        val date: LocalDate,
        val detail: String
    ) : WeightHistoryError("WEIGHT_ENTRY_NOT_FOUND: $detail")

    data class InvalidDateRange(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val detail: String
    ) : WeightHistoryError("INVALID_DATE_RANGE: $detail")
}
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `WeightValue.kg` | Must be `>= 0.5` | `INVALID_WEIGHT_VALUE` |
| `WeightValue.kg` | Must be `<= 500.0` | `INVALID_WEIGHT_VALUE` |
| `save(entry)` | `entry.date` must not already exist in the repository | `DUPLICATE_WEIGHT_DATE` |
| `update(entry)` | `entry.date` must already exist in the repository | `WEIGHT_ENTRY_NOT_FOUND` |
| `getByDateRange.startDate` + `endDate` | `startDate` must be on or before `endDate` | `INVALID_DATE_RANGE` |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Weight below 0.5 kg | N/A (domain module) | `INVALID_WEIGHT_VALUE` | Caller supplies a weight within the valid range |
| Weight above 500.0 kg | N/A (domain module) | `INVALID_WEIGHT_VALUE` | Caller supplies a weight within the valid range |
| Duplicate date on save | N/A (domain module) | `DUPLICATE_WEIGHT_DATE` | Caller uses `editWeight` instead of `recordWeight` |
| Update for non-existent date | N/A (domain module) | `WEIGHT_ENTRY_NOT_FOUND` | Caller uses `recordWeight` instead of `editWeight` |
| Start date after end date on range query | N/A (domain module) | `INVALID_DATE_RANGE` | Caller corrects the range ordering |

## Algorithms

### Weight validation algorithm

```text
1. If `weight.kg < MIN_WEIGHT_KG (0.5)`, return `Result.failure(WeightHistoryError.Validation(code="INVALID_WEIGHT_VALUE", ...))`.
2. If `weight.kg > MAX_WEIGHT_KG (500.0)`, return `Result.failure(WeightHistoryError.Validation(code="INVALID_WEIGHT_VALUE", ...))`.
3. Return `Result.success(Unit)`.
```

### Record weight algorithm

```text
1. Validate `WeightValue` via `WeightEntryValidator`; return the validation failure if invalid.
2. Check repository via `getByDate(date)`; if an entry exists, return `Result.failure(WeightHistoryError.DuplicateWeightDate(...))`.
3. Capture the creation timestamp using `kotlinx.datetime.Clock.System.now()`.
4. Construct `WeightEntry(date, weight, createdAt)`.
5. Persist via `repository.save(entry)`.
6. Return `Result.success(entry)`.
```

### Edit weight algorithm

```text
1. Validate the new `WeightValue` via `WeightEntryValidator`; return the validation failure if invalid.
2. Check repository via `getByDate(date)`; if no entry exists, return `Result.failure(WeightHistoryError.WeightEntryNotFound(...))`.
3. Construct an updated `WeightEntry` with the new weight and a fresh timestamp.
4. Persist via `repository.update(updatedEntry)`.
5. Return `Result.success(updatedEntry)`.
```

### Delete weight algorithm

```text
1. Call `repository.deleteByDate(date)`.
2. Return the result indicating whether an entry was actually removed.
```

### Repository save algorithm

```text
1. Check if an entry with the same `date` already exists in the internal map.
2. If found, return `Result.failure(WeightHistoryError.DuplicateWeightDate(...))`.
3. Store the entry keyed by `date`.
4. Return `Result.success(entry)`.
```

### Repository update algorithm

```text
1. Check if an entry with the given `date` exists in the internal map.
2. If not found, return `Result.failure(WeightHistoryError.WeightEntryNotFound(...))`.
3. Replace the stored entry with the updated entry.
4. Return `Result.success(updatedEntry)`.
```

### Repository getByDateRange algorithm

```text
1. If `startDate > endDate`, return `Result.failure(WeightHistoryError.InvalidDateRange(...))`.
2. Collect all entries where `entry.date >= startDate && entry.date <= endDate`.
3. Sort entries by `date` ascending.
4. Return `Result.success(sortedEntries)`.
```

### Debt recalculation algorithm

```text
1. Derive an adjusted target calorie budget from `newWeight.kg`.
   Formula: `adjustedTarget = (newWeight.kg * 22.0).toInt()`
   (Uses a simplified BMR-approximation multiplier for domain-level recalculation.
    This is a deterministic proxy; the actual BMR/TDEE formula can be refined in a future slice.)
2. Construct a new `CalculationWindow` with the original date range but the adjusted target.
3. Call `CalorieDebtCalculator.calculate(adjustedWindow, entries)`.
4. Wrap the result in `DebtRecalculationResult(triggeringWeight, adjustedTargetCalories, debtResult)`.
5. Return `Result.success(recalculationResult)`.
```

## Persistence Schema Changes

Not applicable for this slice. The `LocalWeightRepository` uses in-memory storage. A future slice will introduce database persistence.

## External Integration Contracts

None. This slice does not call external services.

## Method Signatures

```kotlin
fun DefaultWeightEntryValidator.validate(
    weight: WeightValue
): Result<Unit>

fun DefaultWeightHistoryService.recordWeight(
    date: LocalDate,
    weight: WeightValue
): Result<WeightEntry>

fun DefaultWeightHistoryService.editWeight(
    date: LocalDate,
    newWeight: WeightValue
): Result<WeightEntry>

fun DefaultWeightHistoryService.deleteWeight(
    date: LocalDate
): Result<Boolean>

fun DefaultWeightHistoryService.getWeightHistory(): Result<List<WeightEntry>>

fun DefaultWeightHistoryService.getWeightByDate(
    date: LocalDate
): Result<WeightEntry?>

fun DefaultWeightHistoryService.getWeightByDateRange(
    startDate: LocalDate,
    endDate: LocalDate
): Result<List<WeightEntry>>

fun LocalWeightRepository.save(entry: WeightEntry): Result<WeightEntry>

fun LocalWeightRepository.update(entry: WeightEntry): Result<WeightEntry>

fun LocalWeightRepository.deleteByDate(date: LocalDate): Result<Boolean>

fun LocalWeightRepository.getByDate(date: LocalDate): Result<WeightEntry?>

fun LocalWeightRepository.getByDateRange(
    startDate: LocalDate,
    endDate: LocalDate
): Result<List<WeightEntry>>

fun LocalWeightRepository.getAll(): Result<List<WeightEntry>>

fun DefaultDebtRecalculationService.recomputeDebt(
    newWeight: WeightValue,
    window: CalculationWindow,
    entries: List<DailyCalorieEntry>
): Result<DebtRecalculationResult>
```

## Dependencies

| Dependency | Purpose |
|---|---|
| Kotlin standard library | Collection operations, immutable models |
| `kotlinx-datetime` | `LocalDate` and `Instant` for dates and timestamps |
| `kotlin.test` | Shared unit tests |
| SLICE-0002 `CalorieDebtCalculator` | Consumed by `DebtRecalculationService` for debt recomputation (read-only) |
| SLICE-0002 `CalculationWindow` | Input model consumed by the debt calculator |
| SLICE-0002 `DailyCalorieEntry` | Input model consumed by the debt calculator |
| SLICE-0002 `CalorieDebtResult` | Output model returned by the debt calculator |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Valid weight entry is saved | `date=2026-03-15`, `weight=75.0 kg` | `Result.success(WeightEntry)` with matching date and weight, non-empty `createdAt` |
| T-02 | Duplicate date on save is rejected | Save `date=2026-03-15` twice | Second save returns `Result.failure` with code `DUPLICATE_WEIGHT_DATE` |
| T-03 | Weight history is retrievable chronologically | Save entries for `2026-03-13`, `2026-03-15`, `2026-03-14` | `getAll()` returns entries ordered by date ascending |
| T-04 | Editing an existing weight entry succeeds and replaces the old value | Save `date=2026-03-15, weight=75.0`, then edit to `weight=74.5` | `editWeight` returns updated entry with `weight=74.5` |
| T-05 | Deleting a weight entry removes it from the repository | Save entry, then delete by date | `deleteByDate` returns `Result.success(true)`, subsequent `getByDate` returns `null` |
| T-06 | Weight below 0.5 kg is rejected | `weight=0.4 kg` | `Result.failure` with code `INVALID_WEIGHT_VALUE` |
| T-07 | Weight above 500.0 kg is rejected | `weight=500.1 kg` | `Result.failure` with code `INVALID_WEIGHT_VALUE` |
| T-08 | Fetch by date range returns entries in chronological order within inclusive range | Entries for `2026-03-10` through `2026-03-20`, fetch `2026-03-12..2026-03-18` | Only entries within range returned, ordered by date |
| T-09 | Empty history retrieval returns empty list | No entries in repository | `getAll()` returns `Result.success(emptyList())` |
| T-10 | Debt recalculation produces deterministic result from weight change | `newWeight=74.0 kg`, window `2026-03-01..2026-03-07`, target derived as `74.0 * 22 = 1628`, entries `[day1=1700, day2=1500]` | `Result.success(DebtRecalculationResult)` with `adjustedTargetCalories=1628` and a deterministic `CalorieDebtResult` matching SLICE-0002 behavior |

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
