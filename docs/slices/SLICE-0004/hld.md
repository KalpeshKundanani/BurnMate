# HLD: SLICE-0004 — Daily Logging Domain + Persistence

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0004/prd.md`

---

## Purpose

This design defines a shared domain module for creating, validating, and persisting daily calorie log entries in BurnMate. It introduces the entry model, validation service, repository interface, and an in-memory persistence adapter so that future UI and integration slices have a trusted, deterministic logging contract to build on.

## System Context Diagram

```text
┌──────────────────────────────┐
│ Future Logging UI Slice      │
└──────────────┬───────────────┘
               │ CalorieEntry creation requests
               v
┌────────────────────────────────────────────────┐
│ SLICE-0004: Daily Logging Domain + Persistence │
│                                                │
│  Entry Validation                              │
│  CalorieEntry Model + Factory                  │
│  EntryRepository Interface                     │
│  LocalEntryRepository (in-memory adapter)      │
└──────────────┬─────────────────────────────────┘
               │ List<CalorieEntry>
               v
┌──────────────────────────────┐
│ SLICE-0002: Calorie Debt     │
│ Engine (consumer)            │
└──────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `CalorieEntryValidator` | Validates calorie amounts are non-negative and within the realistic upper bound; validates entry date is not null or malformed |
| `CalorieEntryFactory` | Public entry point that validates inputs and creates a `CalorieEntry` with a unique ID and creation timestamp |
| `EntryRepository` | Defines the persistence contract: create, delete by ID, fetch by date range, fetch by single date |
| `LocalEntryRepository` | In-memory implementation of `EntryRepository` for this slice; replaceable by a database-backed adapter later |
| Domain models | Carry entry data, validation results, and domain errors across layers |

## Domain Model

| Entity / Value Object | Description |
|---|---|
| `CalorieEntry` | Core entity: unique ID, date, calorie amount, and creation timestamp |
| `EntryId` | Value object wrapping a unique string identifier for an entry |
| `EntryDate` | Value object wrapping `kotlinx.datetime.LocalDate` for type-safe date handling |
| `CalorieAmount` | Value object wrapping a validated non-negative integer within the realistic bound |
| `EntryValidationError` | Sealed class representing structured validation failure reasons |
| `EntryRepositoryError` | Sealed class representing structured persistence operation failure reasons |

## Data Flow

```text
1. Caller sends raw inputs (date, calorie amount) to `CalorieEntryFactory`.
2. `CalorieEntryValidator` checks that the calorie amount is non-negative and within the 15,000 kcal upper bound.
3. The factory generates a unique `EntryId` and captures a creation timestamp.
4. The factory returns a validated `CalorieEntry` or a structured validation error.
5. Caller passes the `CalorieEntry` to `EntryRepository.create()` for persistence.
6. `LocalEntryRepository` stores the entry in memory and returns success or a duplicate-entry error.
7. Downstream consumers (e.g., calorie debt engine) call `EntryRepository.fetchByDateRange()` to retrieve entries.
```

## Service Interfaces

| Interface | Responsibility |
|---|---|
| `CalorieEntryFactory` | Creates validated entries from raw inputs |
| `CalorieEntryValidator` | Validates entry inputs against domain rules |
| `EntryRepository` | CRUD persistence contract for calorie entries |

## Data Structures

| Structure | Notes |
|---|---|
| `CalorieEntry` | Immutable entity stored and returned by the repository |
| `List<CalorieEntry>` | Ordered collection returned by date-range queries |
| `Map<EntryId, CalorieEntry>` | Internal storage structure in `LocalEntryRepository` |
| `Map<EntryDate, List<CalorieEntry>>` | Internal date-indexed lookup for efficient queries |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` domain layer | Owns all entry models, validation, factory, repository interface, and in-memory adapter |
| Shared `commonTest` layer | Contains all unit and contract tests for the logging domain |
| Android / iOS presentation layers | Will provide entry forms and pass user input to the factory in future UI slices |
| Future data layer | Will replace `LocalEntryRepository` with a database-backed adapter |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Kotlin standard library | Internal | Collection operations, UUID generation, and immutable models |
| `kotlinx-datetime` | Internal | `LocalDate` and `Instant` for entry dates and creation timestamps |
| Kotlin test | Internal | Shared unit tests for domain logic and repository contract compliance |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Negative calorie amount | Invalid entry would corrupt debt calculations | Fail fast with structured validation error before persistence |
| Calorie amount exceeding realistic bound | Unrealistic data could skew coaching summaries | Reject with `UNREALISTIC_CALORIE_AMOUNT` error |
| Duplicate entry ID on create | Data integrity violation | Return explicit `DUPLICATE_ENTRY` error from repository |
| Delete of non-existent entry | Caller may not know if deletion was effective | Return a result indicating whether the entry was actually removed |
| Inverted or invalid date range on fetch | Inconsistent query results | Validate range ordering and return structured error |

## Observability

| Signal | Type | Description |
|---|---|---|
| `logging_domain.entry.created` | Metric | Count of successfully created calorie entries |
| `logging_domain.entry.deleted` | Metric | Count of deleted calorie entries |
| `logging_domain.validation_failed` | Metric | Count of rejected entry creation attempts by error code |
| `logging_domain.fetch.invoked` | Metric | Count of date-range fetch operations |

## Security and Privacy Notes

- Calorie intake data is health-adjacent personal data and must remain in-memory only for this slice.
- This slice must not write calorie data to logs, external services, or unencrypted persistent storage.
- No authentication or authorization logic is in scope because the module is an in-process domain component.

## Out of Scope

- Database schemas, SQL migrations, or Room/SQLDelight integration.
- UI state management, entry editing forms, or calendar navigation.
- Calorie burn entries, macro tracking, or food database lookup.
- Networking, Google Fit, HealthKit, or external service integration.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
