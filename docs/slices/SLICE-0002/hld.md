# HLD: SLICE-0002 — Calorie Debt Engine

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0002/prd.md`

---

## Purpose

This design defines a shared domain module that converts daily calorie logs into a deterministic calorie-debt summary for BurnMate. It addresses all PRD MUST requirements while keeping the slice pure, portable, and ready for later UI and persistence slices.

## System Context Diagram

```text
┌──────────────────────────────┐
│ Future Log Capture / Storage │
└──────────────┬───────────────┘
               │ DailyCalorieEntry[]
               v
┌────────────────────────────────────────────┐
│ SLICE-0002: Shared Calorie Debt Engine     │
│                                            │
│  Input Validation                          │
│  Debt Calculation Service                  │
│  Debt Summary Mapping                      │
└──────────────┬─────────────────────────────┘
               │ CalorieDebtResult
               v
┌──────────────────────────────┐
│ Future UI / Coaching Slices  │
└──────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `CalorieDebtCalculator` | Public entry point that validates input, iterates the requested range, and returns a complete debt result |
| `CalorieDebtValidator` | Enforces range validity, non-negative numeric constraints, and duplicate-date rejection before calculation begins |
| `DebtTimelineBuilder` | Produces one normalized day record per requested date, filling missing days with zero-consumption entries |
| `DebtTrendClassifier` | Derives latest-day trend and optional severity band from the computed debt values |
| Domain models | Carry ordered inputs and explainable outputs used by future UI and tests |

## Domain Model

| Entity / Value Object | Description |
|---|---|
| `DailyCalorieEntry` | Caller-provided calories consumed for a specific `LocalDate` |
| `CalculationWindow` | Inclusive date range plus target calories used for a single calculation |
| `CalorieDebtDay` | Output row for one date with consumed calories, delta, starting debt, and ending debt |
| `CalorieDebtResult` | Aggregate response containing final debt, ordered breakdown, streak, latest trend, and severity |
| `CalorieDebtTrend` | Enum describing how the latest processed day changed debt |
| `CalorieDebtSeverity` | Enum representing optional debt intensity bands |
| `CalorieDebtValidationError` | Structured error used when input is invalid |

## Data Flow

```text
1. Caller sends `CalculationWindow` and `DailyCalorieEntry` list to `CalorieDebtCalculator`.
2. `CalorieDebtValidator` checks target calories, date ordering, duplicate dates, and non-negative values.
3. `DebtTimelineBuilder` expands the inclusive date range and joins any supplied entries by date.
4. The calculator walks days in chronological order:
   a. startingDebt = previous day ending debt
   b. dailyDelta = consumedCalories - targetCalories
   c. endingDebt = max(0, startingDebt + dailyDelta)
5. `DebtTrendClassifier` derives latest-day trend, severity band, and consecutive debt-day streak.
6. The calculator returns a `CalorieDebtResult` with final debt and full breakdown.
```

## Service Interfaces

| Interface | Responsibility |
|---|---|
| `CalorieDebtCalculator` | Synchronous pure-domain service for producing `CalorieDebtResult` |
| `CalorieDebtValidator` | Validation contract used by the calculator before any processing |
| `ClocklessDateRangeExpander` | Internal helper interface for generating contiguous days without relying on platform time APIs |

## Data Structures

| Structure | Notes |
|---|---|
| `List<DailyCalorieEntry>` | Input payload from future logging or persistence layers |
| `Map<LocalDate, DailyCalorieEntry>` | Internal deduped lookup keyed by day after validation |
| `List<CalorieDebtDay>` | Chronological output timeline matching the inclusive range |
| `Set<LocalDate>` | Temporary structure used to detect duplicate dates |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` domain layer | Owns all calculation rules, validation, enums, and result models |
| Android / iOS presentation layers | Supply already localized `LocalDate` values and render returned result fields |
| Future storage/integration layer | Loads daily logs and passes them into the calculator; not part of this slice |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Kotlin standard library | Internal | Core collection and numeric operations |
| `kotlinx-datetime` | Internal | Provides multiplatform `LocalDate` and date iteration support |
| Kotlin test | Internal | Pure unit tests for deterministic domain logic |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Inverted date range | Result cannot be trusted | Fail fast with validation error before processing |
| Duplicate entry dates | Ambiguous consumed calories for a day | Reject request with explicit duplicate-date error |
| Negative target or consumed calories | Invalid business input | Reject request with field-specific validation error |
| Large positive debt accumulation | Summary may be alarming but still valid | Preserve exact integer math and classify severity band |
| Missing dates in the range | Gaps would make timeline inconsistent | Synthesize zero-consumption rows for every missing day |

## Observability

| Signal | Type | Description |
|---|---|---|
| `calorie_debt.calculate.invoked` | Metric | Count of calculator invocations |
| `calorie_debt.calculate.validation_failed` | Metric | Count of rejected requests by validation reason |
| `calorie_debt.final_debt` | Log | Structured result summary for debugging in future app layers |
| `calorie_debt.range_days` | Metric | Distribution of requested range lengths |

## Security and Privacy Notes

- Calorie data is user health-adjacent information and should remain in memory only for the duration of calculation in this slice.
- This slice must not emit raw calorie logs to external services or persistent logs.
- No authentication or authorization logic is in scope because the engine is an in-process domain module.

## Out of Scope

- Persistence schema, migrations, or repository interfaces.
- UI state management and localized copy generation.
- Any recommendation engine that interprets debt beyond the exposed summary fields.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
