# LLD: SLICE-0003 — User Profile + Goal Domain

**Author:** Architect
**Date:** 2026-03-16
**HLD Reference:** `docs/slices/SLICE-0003/hld.md`
**PRD Reference:** `docs/slices/SLICE-0003/prd.md`

---

## Package / File Layout

```text
composeApp/src/commonMain/kotlin/org/kalpeshbkundanani/burnmate/profile/
  model/
    BodyMetrics.kt
    BmiSnapshot.kt
    BmiCategory.kt
    GoalValidationReason.kt
    GoalValidationResult.kt
    UserProfileSummary.kt
    ProfileDomainError.kt
  domain/
    UserProfileFactory.kt
    DefaultUserProfileFactory.kt
    ProfileMetricsValidator.kt
    DefaultProfileMetricsValidator.kt
    BmiCalculator.kt
    DefaultBmiCalculator.kt
    HealthyGoalValidator.kt
    DefaultHealthyGoalValidator.kt

composeApp/src/commonTest/kotlin/org/kalpeshbkundanani/burnmate/profile/
  DefaultUserProfileFactoryTest.kt
  DefaultBmiCalculatorTest.kt
  DefaultHealthyGoalValidatorTest.kt
```

## Interfaces / APIs

### `UserProfileFactory`

```kotlin
package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.ProfileDomainError
import org.kalpeshbkundanani.burnmate.profile.model.UserProfileSummary

interface UserProfileFactory {
    fun create(metrics: BodyMetrics): Result<UserProfileSummary>
}
```

Behavior:
- Returns `Result.success(UserProfileSummary)` when all profile and goal rules pass.
- Returns `Result.failure(ProfileDomainError.Validation)` when any measurement or goal rule is invalid.
- Never performs I/O, persistence, network access, or platform calls.

### `ProfileMetricsValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics

interface ProfileMetricsValidator {
    fun validate(metrics: BodyMetrics): Result<Unit>
}
```

### `BmiCalculator`

```kotlin
package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot

interface BmiCalculator {
    fun calculate(heightCm: Double, weightKg: Double): BmiSnapshot
    fun classify(bmiValue: Double): BmiCategory
}
```

### `HealthyGoalValidator`

```kotlin
package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BodyMetrics
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot
import org.kalpeshbkundanani.burnmate.profile.model.GoalValidationResult

interface HealthyGoalValidator {
    fun validate(
        metrics: BodyMetrics,
        currentBmi: BmiSnapshot,
        goalBmi: BmiSnapshot
    ): GoalValidationResult
}
```

### `DefaultUserProfileFactory`

```kotlin
package org.kalpeshbkundanani.burnmate.profile.domain

class DefaultUserProfileFactory(
    private val validator: ProfileMetricsValidator = DefaultProfileMetricsValidator(),
    private val bmiCalculator: BmiCalculator = DefaultBmiCalculator(),
    private val healthyGoalValidator: HealthyGoalValidator = DefaultHealthyGoalValidator()
) : UserProfileFactory
```

## Classes

| Class | Type | Responsibility | Dependencies |
|---|---|---|---|
| `DefaultUserProfileFactory` | Class | Orchestrates validation, BMI calculation, healthy-goal validation, and summary mapping | `ProfileMetricsValidator`, `BmiCalculator`, `HealthyGoalValidator` |
| `DefaultProfileMetricsValidator` | Class | Rejects non-positive measurements and non-weight-loss goals | None |
| `DefaultBmiCalculator` | Class | Computes BMI using metric units and maps categories deterministically | None |
| `DefaultHealthyGoalValidator` | Class | Confirms goal BMI is within `18.5..24.9` inclusive and returns reasoned results | None |
| `BodyMetrics` | Data class | Immutable metric input aggregate | None |
| `BmiSnapshot` | Data class | Captures BMI value plus coarse category | None |
| `GoalValidationResult` | Data class | Validation outcome contract with reason and derived helpers | None |
| `UserProfileSummary` | Data class | Aggregate summary returned to callers | None |
| `ProfileDomainError` | Sealed class | Domain error contract used in `Result.failure` | None |

## Data Models

```kotlin
package org.kalpeshbkundanani.burnmate.profile.model

data class BodyMetrics(
    val heightCm: Double,
    val currentWeightKg: Double,
    val goalWeightKg: Double
)

data class BmiSnapshot(
    val value: Double,
    val category: BmiCategory
)

enum class BmiCategory {
    UNDERWEIGHT,
    HEALTHY,
    OVERWEIGHT,
    OBESE
}

enum class GoalValidationReason {
    VALID,
    INVALID_HEIGHT,
    INVALID_CURRENT_WEIGHT,
    INVALID_GOAL_WEIGHT,
    GOAL_NOT_BELOW_CURRENT_WEIGHT,
    GOAL_BMI_BELOW_HEALTHY_RANGE,
    GOAL_BMI_ABOVE_HEALTHY_RANGE
}

data class GoalValidationResult(
    val isValid: Boolean,
    val reason: GoalValidationReason,
    val kilogramsToLose: Double?,
    val bmiDelta: Double?
)

data class UserProfileSummary(
    val metrics: BodyMetrics,
    val currentBmi: BmiSnapshot,
    val goalBmi: BmiSnapshot,
    val kilogramsToLose: Double,
    val bmiDelta: Double,
    val goalValidation: GoalValidationResult
)

sealed class ProfileDomainError(message: String) : IllegalArgumentException(message) {
    data class Validation(
        val code: String,
        val detail: String
    ) : ProfileDomainError("$code: $detail")
}
```

## Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| `BodyMetrics.heightCm` | Must be `> 0` | `INVALID_HEIGHT` |
| `BodyMetrics.currentWeightKg` | Must be `> 0` | `INVALID_CURRENT_WEIGHT` |
| `BodyMetrics.goalWeightKg` | Must be `> 0` | `INVALID_GOAL_WEIGHT` |
| `BodyMetrics.goalWeightKg` | Must be strictly less than `currentWeightKg` for this weight-loss slice | `GOAL_NOT_BELOW_CURRENT_WEIGHT` |
| Goal BMI | Must be within `18.5..24.9` inclusive | `GOAL_BMI_OUT_OF_HEALTHY_RANGE` |

## Error Handling Contracts

| Error Condition | HTTP Status | Error Code | Recovery |
|---|---|---|---|
| Height is zero or negative | N/A (domain module) | `INVALID_HEIGHT` | Caller supplies a positive centimeter value |
| Current weight is zero or negative | N/A (domain module) | `INVALID_CURRENT_WEIGHT` | Caller supplies a positive kilogram value |
| Goal weight is zero or negative | N/A (domain module) | `INVALID_GOAL_WEIGHT` | Caller supplies a positive kilogram value |
| Goal weight is greater than or equal to current weight | N/A (domain module) | `GOAL_NOT_BELOW_CURRENT_WEIGHT` | Caller supplies a lower goal weight |
| Goal BMI is below `18.5` | N/A (domain module) | `GOAL_BMI_BELOW_HEALTHY_RANGE` | Caller raises the goal weight to a healthier range |
| Goal BMI is above `24.9` | N/A (domain module) | `GOAL_BMI_ABOVE_HEALTHY_RANGE` | Caller lowers the goal weight or revisits the goal later |

## Algorithms

### Profile creation algorithm

```text
1. Validate `BodyMetrics`; return the first validation failure if invalid.
2. Compute `currentBmi = bmiCalculator.calculate(heightCm, currentWeightKg)`.
3. Compute `goalBmi = bmiCalculator.calculate(heightCm, goalWeightKg)`.
4. Evaluate `goalValidation = healthyGoalValidator.validate(metrics, currentBmi, goalBmi)`.
5. If `goalValidation.isValid` is false, return `Result.failure(ProfileDomainError.Validation(...))` using the validation reason code.
6. Compute `kilogramsToLose = currentWeightKg - goalWeightKg`.
7. Compute `bmiDelta = currentBmi.value - goalBmi.value`.
8. Return `UserProfileSummary` with normalized metrics, both BMI snapshots, derived helpers, and `goalValidation`.
```

### BMI calculation algorithm

```text
1. Convert height in centimeters to meters: `heightMeters = heightCm / 100.0`.
2. Compute `rawBmi = weightKg / (heightMeters * heightMeters)`.
3. Round to one decimal place using deterministic half-up rounding.
4. Classify BMI:
   a. `< 18.5` -> `UNDERWEIGHT`
   b. `18.5..24.9` -> `HEALTHY`
   c. `25.0..29.9` -> `OVERWEIGHT`
   d. `>= 30.0` -> `OBESE`
5. Return `BmiSnapshot(value, category)`.
```

### Healthy-goal validation algorithm

```text
1. If goal weight is greater than or equal to current weight, return invalid with `GOAL_NOT_BELOW_CURRENT_WEIGHT`.
2. If goal BMI is below `18.5`, return invalid with `GOAL_BMI_BELOW_HEALTHY_RANGE`.
3. If goal BMI is above `24.9`, return invalid with `GOAL_BMI_ABOVE_HEALTHY_RANGE`.
4. Otherwise return valid with `GoalValidationReason.VALID`.
5. For valid results, include `kilogramsToLose` and `bmiDelta`; for invalid results, those fields may be null.
```

## Persistence Schema Changes

Not applicable. This slice is a pure domain module and does not modify storage.

## External Integration Contracts

None. This slice does not call external services.

## Method Signatures

```kotlin
fun DefaultUserProfileFactory.create(metrics: BodyMetrics): Result<UserProfileSummary>

fun DefaultProfileMetricsValidator.validate(metrics: BodyMetrics): Result<Unit>

fun DefaultBmiCalculator.calculate(heightCm: Double, weightKg: Double): BmiSnapshot

fun DefaultBmiCalculator.classify(bmiValue: Double): BmiCategory

fun DefaultHealthyGoalValidator.validate(
    metrics: BodyMetrics,
    currentBmi: BmiSnapshot,
    goalBmi: BmiSnapshot
): GoalValidationResult
```

## Dependencies

| Dependency | Purpose |
|---|---|
| Kotlin standard library | Numeric operations and immutable models |
| `kotlin.test` | Shared unit tests |

## Unit Test Cases

| ID | Test Case | Input | Expected Output |
|---|---|---|---|
| T-01 | Valid profile creates summary | `heightCm=175.0`, `currentWeightKg=82.0`, `goalWeightKg=72.0` | `Result.success`, positive `kilogramsToLose`, valid goal |
| T-02 | Zero height is rejected | `heightCm=0.0`, `currentWeightKg=82.0`, `goalWeightKg=72.0` | `Result.failure` with code `INVALID_HEIGHT` |
| T-03 | Negative current weight is rejected | `heightCm=175.0`, `currentWeightKg=-1.0`, `goalWeightKg=72.0` | `Result.failure` with code `INVALID_CURRENT_WEIGHT` |
| T-04 | Goal weight equal to current weight is rejected | `heightCm=175.0`, `currentWeightKg=82.0`, `goalWeightKg=82.0` | `Result.failure` with code `GOAL_NOT_BELOW_CURRENT_WEIGHT` |
| T-05 | Goal weight above current weight is rejected | `heightCm=175.0`, `currentWeightKg=82.0`, `goalWeightKg=90.0` | `Result.failure` with code `GOAL_NOT_BELOW_CURRENT_WEIGHT` |
| T-06 | BMI helper returns deterministic current and goal values | `heightCm=180.0`, `currentWeightKg=90.0`, `goalWeightKg=78.0` | Current BMI and goal BMI rounded to one decimal place consistently on repeated calls |
| T-07 | Goal BMI below healthy range is rejected | `heightCm=175.0`, `currentWeightKg=70.0`, `goalWeightKg=54.0` | `Result.failure` with code `GOAL_BMI_BELOW_HEALTHY_RANGE` |
| T-08 | Goal BMI inside healthy range succeeds | `heightCm=175.0`, `currentWeightKg=84.0`, `goalWeightKg=72.0` | `Result.success`, `goalValidation.reason=VALID` |
| T-09 | BMI categories map correctly at thresholds | BMI inputs producing `18.4`, `18.5`, `24.9`, `25.0`, `30.0` | Categories `UNDERWEIGHT`, `HEALTHY`, `HEALTHY`, `OVERWEIGHT`, `OBESE` |
| T-10 | Derived helpers include kilograms-to-lose and BMI delta | `heightCm=165.0`, `currentWeightKg=78.0`, `goalWeightKg=65.0` | `kilogramsToLose=13.0`, positive `bmiDelta` |

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
