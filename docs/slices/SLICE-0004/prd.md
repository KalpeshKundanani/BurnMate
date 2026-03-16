# PRD: SLICE-0004 — Daily Logging Domain + Persistence

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate's calorie debt engine (SLICE-0002) consumes caller-provided entry lists but has no shared domain model for creating, validating, or persisting daily calorie log entries. Without a canonical logging domain, future UI and integration slices would each invent their own entry models, validation rules, and storage paths, leading to inconsistent calorie tracking across platforms.

## Users

- BurnMate users who need to log daily calorie intake.
- The calorie debt engine (SLICE-0002), which will consume validated entries produced by this slice's repository.
- Future UI slices that will build entry forms and date-navigation views on top of this domain.
- Engineers and QA validating that entry creation, validation, and persistence are deterministic and consistent across Android and iOS.

## Non-Goals

- Food logging UI, entry editing forms, or calendar navigation screens.
- Network sync, remote API integration, or cloud backup of log entries.
- Analytics, coaching, or recommendation logic based on logged entries.
- Modifications to the calorie debt engine beyond consuming its existing `DailyCalorieEntry` model.

## Success Metrics

| Metric | Target |
|---|---|
| Entry creation and validation return identical results for the same inputs across platforms | 100% deterministic |
| All MUST acceptance criteria are covered by automated tests | 100% coverage |
| Repository operations (create, delete, fetch by date range) execute without platform-specific code in the domain layer | 100% pure-domain interface |
| Local persistence adapter passes all repository contract tests | 100% compliance |

## Constraints

- Domain models and validation must be implemented in shared Kotlin Multiplatform code with no Android- or iOS-specific APIs.
- The repository interface must be defined in shared code; only the persistence adapter implementation may use platform-adjacent libraries.
- Calorie amounts must be validated as non-negative integers with an upper bound to prevent unrealistic values.
- Entry dates must use `kotlinx.datetime.LocalDate` to remain timezone-safe and platform-portable.
- This slice must not modify any existing calorie debt engine code; it produces entries that the debt engine can consume.

## Non-Functional Requirements

- Deterministic: the same entry inputs must always produce the same validation and creation outputs.
- Testable: all domain rules must be unit-testable in shared code without device setup or real database access.
- Portable: repository interface and domain models must compile for both Android and iOS targets.
- Resilient: persistence failures must be surfaced as structured domain errors, not raw exceptions.

## Functional Requirements

### MUST

- [ ] Define a `CalorieEntry` model representing a single calorie log entry with unique ID, date, calorie amount, and creation timestamp.
- [ ] Define an `EntryDate` abstraction wrapping `kotlinx.datetime.LocalDate` for type-safe date handling.
- [ ] Validate that calorie amounts are non-negative integers.
- [ ] Reject calorie amounts exceeding a configurable upper bound (default: 15,000 kcal) as unrealistic.
- [ ] Define an `EntryRepository` interface with operations: create entry, delete entry by ID, and fetch entries by date range.
- [ ] Implement a `LocalEntryRepository` adapter that persists entries using in-memory storage for this slice (replaceable by a real database adapter in a future slice).
- [ ] Return structured domain errors for all validation failures and repository operation errors.

### SHOULD

- [ ] Support fetching all entries for a single date as a convenience method on the repository.
- [ ] Return entries in chronological order when fetching by date range.
- [ ] Surface whether a delete operation actually removed an entry or the entry was not found.

### COULD

- [ ] Support batch creation of multiple entries in a single repository call, provided all entries pass validation.
- [ ] Include a running daily total helper that sums calorie amounts for a given date from the repository.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given valid calorie amount and date, entry creation returns a `CalorieEntry` with a unique ID and the provided values | Yes |
| AC-02 | When calorie amount is negative, entry validation returns a structured error with code `INVALID_CALORIE_AMOUNT` | Yes |
| AC-03 | When calorie amount exceeds 15,000, entry validation returns a structured error with code `UNREALISTIC_CALORIE_AMOUNT` | Yes |
| AC-04 | When an entry is deleted by ID, the repository no longer returns it in subsequent queries | Yes |
| AC-05 | When entries are fetched for a date range, only entries within the inclusive range are returned in chronological order | Yes |
| AC-06 | When entries are fetched for an empty date range (no entries exist), the repository returns an empty list instead of an error | Yes |
| AC-07 | When a duplicate entry ID is created, the repository returns a structured error with code `DUPLICATE_ENTRY` | Yes |
| AC-08 | The `LocalEntryRepository` adapter passes all `EntryRepository` contract tests using in-memory storage | Yes |
| AC-09 | All domain models and validation logic compile and execute identically on Android and iOS shared test targets | Yes |
| AC-10 | No calorie debt engine code is modified by this slice | Yes |

## Out of Scope

- Compose or SwiftUI forms for logging calorie entries.
- Database migration scripts, SQL schemas, or Room/SQLDelight integration (future slice concern).
- Google Fit, HealthKit, or any external data source integration.
- Calorie burn entries (the ROADMAP mentions these but they are deferred to avoid scope expansion in this initial domain slice).

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | Should this slice include calorie burn entries alongside intake entries? Resolved: no, this slice covers calorie intake entries only. Burn entries are deferred. | RESOLVED |
| 2 | What upper bound prevents unrealistic calorie values? Resolved: 15,000 kcal per entry as a sensible maximum for a single log. | RESOLVED |
| 3 | Should the persistence adapter use a real database in this slice? Resolved: no, use in-memory storage to keep scope minimal. A future slice will introduce SQLDelight or equivalent. | RESOLVED |
