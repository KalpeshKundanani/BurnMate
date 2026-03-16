# LLD: SLICE-0004 — Daily Logging Domain + Persistence

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0004/hld.md`
**PRD Reference:** `docs/slices/SLICE-0004/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/logging/
  model/
    CalorieEntry.kt
    EntryId.kt
    EntryDate.kt
    CalorieAmount.kt
    EntryValidationError.kt
    EntryRepositoryError.kt
  domain/
    CalorieEntryValidator.kt
    DefaultCalorieEntryValidator.kt
    CalorieEntryFactory.kt
    DefaultCalorieEntryFactory.kt
    EntryRangeQuery.kt
  repository/
    EntryRepository.kt
    LocalEntryRepository.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/logging/
  DefaultCalorieEntryValidatorTest.kt
  DefaultCalorieEntryFactoryTest.kt
  LocalEntryRepositoryTest.kt
```

## Interfaces / APIs

### `CalorieEntryValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

interface CalorieEntryValidator {
    fun validate(date: EntryDate, amount: CalorieAmount): Result<Unit>
}
```

Behavior:
- Returns `Result.success(Unit)` when date and amount are valid.
- Returns `Result.failure(EntryValidationError)` when any validation rule fails.
- Never performs I/O, persistence, or platform calls.

### `CalorieEntryFactory`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryValidationError

interface CalorieEntryFactory {
    fun create(date: EntryDate, amount: CalorieAmount): Result<CalorieEntry>
}
```

Behavior:
- Validates inputs via `CalorieEntryValidator`.
- Generates a unique `EntryId` and captures a creation timestamp.
- Returns `Result.success(CalorieEntry)` when all rules pass.
- Returns `Result.failure(EntryValidationError)` when validation fails.

### `EntryRepository`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.repository

import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.model.EntryRepositoryError

interface EntryRepository {
    fun create(entry: CalorieEntry): Result<CalorieEntry>
    fun deleteById(id: EntryId): Result<Boolean>
    fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>>
    fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>>
}
```

Behavior:
- `create` returns the stored entry or `Result.failure(EntryRepositoryError.DuplicateEntry)` if the ID already exists.
- `deleteById` returns `Result.success(true)` when an entry was removed, `Result.success(false)` when no entry was found.
- `fetchByDateRange` returns entries within the inclusive range in chronological order. Returns an empty list if no entries exist. Returns `Result.failure(EntryRepositoryError.InvalidDateRange)` if `startDate` is after `endDate`.
- `fetchByDate` is a convenience method equivalent to `fetchByDateRange(date, date)`.

### `DefaultCalorieEntryValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.domain

class DefaultCalorieEntryValidator(
    private val maxCalorieAmount: Int = MAX_CALORIE_AMOUNT
) : CalorieEntryValidator {
    companion object {
        const val MAX_CALORIE_AMOUNT: Int = 15_000
    }
}
```

### `DefaultCalorieEntryFactory`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.domain

class DefaultCalorieEntryFactory(
    private val validator: CalorieEntryValidator = DefaultCalorieEntryValidator()
) : CalorieEntryFactory
```

### `LocalEntryRepository`

```kotlin
package org.kalpeshbkundanani.burnmate.logging.repository

class LocalEntryRepository : EntryRepository
```

## Classes

| Class | Type | Responsibility | Dependencies |
|---|---|---|---|
| `DefaultCalorieEntryValidator` | Class | Validates calorie amount is non-negative and within the realistic upper bound | None |
| `DefaultCalorieEntryFactory` | Class | Orchestrates validation, ID generation, timestamp capture, and entry creation | `CalorieEntryValidator` |
| `LocalEntryRepository` | Class | In-memory implementation of `EntryRepository` using maps for storage and date indexing | None |
| `CalorieEntry` | Data class | Immutable entity representing a single calorie log entry | None |
| `EntryId` | Value class | Wraps a unique string identifier | None |
| `EntryDate` | Value class | Wraps `kotlinx.datetime.LocalDate` for type-safe date handling | None |
| `CalorieAmount` | Value class | Wraps a non-negative integer calorie value | None |
| `EntryValidationError` | Sealed class | Domain validation error contract | None |
| `EntryRepositoryError` | Sealed class | Repository operation error contract | None |
| `EntryRangeQuery` | Data class | Encapsulates a date-range query with start and end dates | None |

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.logging.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@JvmInline
value class EntryId(val value: String)

@JvmInline
value class EntryDate(val value: LocalDate)

@JvmInline
value class CalorieAmount(val value: Int)

data class CalorieEntry(
    val id: EntryId,
    val date: EntryDate,
    val amount: CalorieAmount,
    val createdAt: Instant
)

sealed class EntryValidationError(message: String) : IllegalArgumentException(message) {
    data class InvalidCalorieAmount(
        val amount: Int,
        val detail: String
    ) : EntryValidationError("INVALID_CALORIE_AMOUNT: $detail")

    data class UnrealisticCalorieAmount(
        val amount: Int,
        val maxAllowed: Int,
        val detail: String
    ) : EntryValidationError("UNREALISTIC_CALORIE_AMOUNT: $detail")
}

sealed class EntryRepositoryError(message: String) : IllegalArgumentException(message) {
    data class DuplicateEntry(
        val id: EntryId,
        val detail: String
    ) : EntryRepositoryError("DUPLICATE_ENTRY: $detail")

    data class InvalidDateRange(
        val startDate: EntryDate,
        val endDate: EntryDate,
        val detail: String
    ) : EntryRepositoryError("INVALID_DATE_RANGE: $detail")
}
```

```kotlin
package org.kalpeshbkundanani.burnmate.logging.domain

import org.kalpeshbkundanani.burnmate.logging.model.EntryDate

data class EntryRangeQuery(
    val startDate: EntryDate,
    val endDate: EntryDate
)
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `CalorieAmount.value` | Must be `>= 0` | `INVALID_CALORIE_AMOUNT` |
| `CalorieAmount.value` | Must be `<= 15,000` | `UNREALISTIC_CALORIE_AMOUNT` |
| `EntryDate.value` | Must not be null (enforced by type system via `LocalDate`) | N/A (compile-time) |
| `fetchByDateRange.startDate` + `endDate` | `startDate` must be on or before `endDate` | `INVALID_DATE_RANGE` |
| `create(entry)` | `entry.id` must not already exist in the repository | `DUPLICATE_ENTRY` |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Calorie amount is negative | N/A (domain module) | `INVALID_CALORIE_AMOUNT` | Caller supplies a non-negative integer |
| Calorie amount exceeds 15,000 | N/A (domain module) | `UNREALISTIC_CALORIE_AMOUNT` | Caller supplies a realistic calorie value |
| Duplicate entry ID on create | N/A (domain module) | `DUPLICATE_ENTRY` | Caller generates a new unique ID or checks before creating |
| Start date after end date on fetch | N/A (domain module) | `INVALID_DATE_RANGE` | Caller corrects the range ordering |

## Algorithms

### Entry creation algorithm

```text
1. Validate `CalorieAmount` via `CalorieEntryValidator`; return the first validation failure if invalid.
2. Generate a unique `EntryId` using `kotlin.uuid.Uuid.random().toString()`.
3. Capture the creation timestamp using `kotlinx.datetime.Clock.System.now()`.
4. Construct `CalorieEntry(id, date, amount, createdAt)`.
5. Return `Result.success(CalorieEntry)`.
```

### Entry validation algorithm

```text
1. If `amount.value < 0`, return `Result.failure(EntryValidationError.InvalidCalorieAmount(...))`.
2. If `amount.value > maxCalorieAmount`, return `Result.failure(EntryValidationError.UnrealisticCalorieAmount(...))`.
3. Return `Result.success(Unit)`.
```

### Repository create algorithm

```text
1. Check if an entry with the same `id` already exists in the store.
2. If found, return `Result.failure(EntryRepositoryError.DuplicateEntry(...))`.
3. Store the entry indexed by both `id` and `date`.
4. Return `Result.success(entry)`.
```

### Repository fetchByDateRange algorithm

```text
1. If `startDate > endDate`, return `Result.failure(EntryRepositoryError.InvalidDateRange(...))`.
2. Collect all entries where `entry.date.value >= startDate.value && entry.date.value <= endDate.value`.
3. Sort entries by `date` ascending, then by `createdAt` ascending for same-date entries.
4. Return `Result.success(sortedEntries)`.
```

### Repository deleteById algorithm

```text
1. Look up the entry by `id`.
2. If found, remove it from all internal indexes and return `Result.success(true)`.
3. If not found, return `Result.success(false)`.
```

## Persistence Schema Changes

Not applicable for this slice. The `LocalEntryRepository` uses in-memory storage. A future slice will introduce database persistence.

## External Integration Contracts

None. This slice does not call external services.

## Method Signatures

```kotlin
fun DefaultCalorieEntryValidator.validate(
    date: EntryDate,
    amount: CalorieAmount
): Result<Unit>

fun DefaultCalorieEntryFactory.create(
    date: EntryDate,
    amount: CalorieAmount
): Result<CalorieEntry>

fun LocalEntryRepository.create(entry: CalorieEntry): Result<CalorieEntry>

fun LocalEntryRepository.deleteById(id: EntryId): Result<Boolean>

fun LocalEntryRepository.fetchByDateRange(
    startDate: EntryDate,
    endDate: EntryDate
): Result<List<CalorieEntry>>

fun LocalEntryRepository.fetchByDate(date: EntryDate): Result<List<CalorieEntry>>
```

## Dependencies

| Dependency | Purpose |
|---|---|
| Kotlin standard library | Collection operations, UUID generation, immutable models |
| `kotlinx-datetime` | `LocalDate` and `Instant` for dates and timestamps |
| `kotlin.test` | Shared unit tests |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Valid entry creation succeeds | `date=2026-03-15`, `amount=1500` | `Result.success(CalorieEntry)` with matching date and amount, non-empty ID |
| T-02 | Negative calorie amount is rejected | `amount=-1` | `Result.failure` with code `INVALID_CALORIE_AMOUNT` |
| T-03 | Unrealistic calorie amount is rejected | `amount=15001` | `Result.failure` with code `UNREALISTIC_CALORIE_AMOUNT` |
| T-04 | Deleting an existing entry removes it from repository | Create entry, then delete by ID | `deleteById` returns `Result.success(true)`, subsequent fetch returns empty list |
| T-05 | Deleting a non-existent entry returns false | Delete with unknown ID | `deleteById` returns `Result.success(false)` |
| T-06 | Fetch by date range returns entries in chronological order | Create entries for `2026-03-13`, `2026-03-15`, `2026-03-14` | Fetch `2026-03-13..2026-03-15` returns entries ordered by date |
| T-07 | Fetch for empty date range returns empty list | No entries in repository | `fetchByDateRange` returns `Result.success(emptyList())` |
| T-08 | Fetch with inverted date range is rejected | `startDate=2026-03-15`, `endDate=2026-03-10` | `Result.failure` with code `INVALID_DATE_RANGE` |
| T-09 | Duplicate entry creation is rejected | Create same entry twice | Second `create` returns `Result.failure` with code `DUPLICATE_ENTRY` |
| T-10 | Boundary calorie amounts are accepted | `amount=0` and `amount=15000` | Both create successfully |

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
