# HLD: SLICE-0005 — Weight History + Debt Recalculation

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0005/prd.md`

---

## Purpose

This design defines a shared domain module for recording daily body weight, maintaining a chronological weight history, and triggering calorie debt recalculation when weight entries change. It addresses the PRD requirement that the debt trajectory must remain accurate as the user's body weight evolves over time.

## System Context Diagram

```text
┌──────────────────────────────┐
│ Future Weight Entry UI Slice │
└──────────────┬───────────────┘
               │ WeightEntry save/update/delete
               v
┌────────────────────────────────────────────────────┐
│ SLICE-0005: Weight History + Debt Recalculation    │
│                                                    │
│  Weight Validation                                 │
│  WeightEntry Model                                 │
│  WeightHistoryRepository Interface                 │
│  LocalWeightRepository (in-memory adapter)         │
│  WeightHistoryService (orchestrator)               │
│  DebtRecalculationService                          │
└──────────────┬──────────────────┬──────────────────┘
               │                  │
               │ Weight timeline  │ Recalculated debt
               v                  v
┌────────────────────┐  ┌──────────────────────────┐
│ Future Dashboard / │  │ SLICE-0002: Calorie Debt │
│ Chart Slices       │  │ Engine (consumed)        │
└────────────────────┘  └──────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `WeightEntryValidator` | Validates weight values are positive and within the realistic human range (0.5–500.0 kg); validates date uniqueness against the repository |
| `WeightHistoryService` | Public entry point that orchestrates validation, persistence, and debt recalculation on weight changes |
| `DebtRecalculationService` | Recomputes calorie debt by constructing an updated calculation window from the current weight and invoking the existing `CalorieDebtCalculator` |
| `WeightHistoryRepository` | Defines the persistence contract: save, update, delete by date, get by date, get by range, get all |
| `LocalWeightRepository` | In-memory implementation of `WeightHistoryRepository` for this slice; replaceable by a database-backed adapter later |
| Domain models | Carry weight data, validation results, and domain errors across layers |

## Domain Model

| Entity / Value Object | Description |
|---|---|
| `WeightEntry` | Core entity: date, weight in kg, and creation timestamp |
| `WeightValue` | Value object wrapping a validated positive `Double` in kilograms |
| `WeightHistoryError` | Sealed class representing structured validation and repository error reasons |
| `DebtRecalculationResult` | Aggregate result wrapping the recalculated `CalorieDebtResult` plus the triggering weight change metadata |

## Data Flow

```text
1. Caller sends a date and weight value to `WeightHistoryService`.
2. `WeightEntryValidator` checks that the weight is positive and within the 0.5–500.0 kg range.
3. For save: validator checks that no entry exists for the given date.
4. For update: validator checks that an entry exists for the given date.
5. The service persists or updates the entry via `WeightHistoryRepository`.
6. After successful save/update/delete, the service invokes `DebtRecalculationService`.
7. `DebtRecalculationService` derives an updated target calorie budget from the new weight and calls `CalorieDebtCalculator.calculate()`.
8. The service returns the weight operation result and the recalculated debt result.
```

## Service Interfaces

| Interface | Responsibility |
|---|---|
| `WeightHistoryService` | Orchestrates weight CRUD operations and triggers debt recalculation |
| `WeightEntryValidator` | Validates weight values and date constraints |
| `WeightHistoryRepository` | Persistence contract for weight entries |
| `DebtRecalculationService` | Recomputes calorie debt from a weight change |

## Data Structures

| Structure | Notes |
|---|---|
| `WeightEntry` | Immutable entity stored and returned by the repository |
| `List<WeightEntry>` | Chronologically ordered collection returned by range and getAll queries |
| `Map<LocalDate, WeightEntry>` | Internal storage structure in `LocalWeightRepository` for date-keyed uniqueness |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` domain layer | Owns all weight models, validation, services, repository interface, and in-memory adapter |
| Shared `commonTest` layer | Contains all unit tests for the weight domain |
| Android / iOS presentation layers | Will provide weight entry forms and display weight history in future UI slices |
| Future data layer | Will replace `LocalWeightRepository` with a database-backed adapter |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Kotlin standard library | Internal | Collection operations and immutable models |
| `kotlinx-datetime` | Internal | `LocalDate` and `Instant` for dates and timestamps |
| Kotlin test | Internal | Shared unit tests for domain logic |
| SLICE-0002 `CalorieDebtCalculator` | Internal (read-only) | Consumed by `DebtRecalculationService` to recompute debt; not modified |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Weight value outside realistic range | Invalid data could corrupt calculations | Fail fast with structured validation error |
| Duplicate date on save | Multiple weights for one day would create ambiguity | Reject with `DUPLICATE_WEIGHT_DATE` error |
| Update for non-existent date | Update cannot proceed without existing entry | Reject with `WEIGHT_ENTRY_NOT_FOUND` error |
| Delete for non-existent date | Caller may not know if deletion was effective | Return result indicating whether an entry was actually removed |
| Debt recalculation with empty calorie log | Recalculation produces zero debt, which is valid | Return valid zero-debt result |

## Observability

| Signal | Type | Description |
|---|---|---|
| `weight_history.entry.saved` | Metric | Count of successfully saved weight entries |
| `weight_history.entry.updated` | Metric | Count of successfully updated weight entries |
| `weight_history.entry.deleted` | Metric | Count of deleted weight entries |
| `weight_history.validation_failed` | Metric | Count of rejected weight operations by error code |
| `weight_history.debt_recalculation.invoked` | Metric | Count of debt recalculations triggered by weight changes |

## Security and Privacy Notes

- Body weight is health-adjacent personal data and must remain in-memory only for this slice.
- This slice must not write weight data to logs, external services, or unencrypted persistent storage.
- No authentication or authorization logic is in scope because the module is an in-process domain component.

## Out of Scope

- UI state management, weight entry forms, or history visualization.
- Database schemas, SQL migrations, or Room/SQLDelight integration.
- Google Fit, HealthKit, or any external health-data source.
- Analytics, coaching, or recommendation logic beyond deterministic debt recalculation.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
