# PRD: SLICE-0005 — Weight History + Debt Recalculation

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate's calorie debt engine (SLICE-0002) uses a fixed target calorie budget for debt calculation, but the user's effective calorie needs change when body weight changes. Without a weight history domain, there is no shared model for recording daily weigh-ins, detecting weight changes over time, or triggering debt recalculation when historical weight entries are added or edited. This means the debt trajectory can silently become stale whenever a user's weight shifts.

## Users

- BurnMate users who weigh themselves regularly and expect the app to reflect accurate calorie debt relative to their current body weight.
- The calorie debt engine (SLICE-0002), which will consume weight-adjusted target calories produced by this slice's recalculation service.
- Future dashboard and chart slices that need a chronological weight timeline to visualize progress.
- Engineers and QA validating that weight history operations and debt recalculation are deterministic across Android and iOS.

## Non-Goals

- Weight entry UI, chart rendering, or visual progress screens.
- Google Fit, HealthKit, or any external health-data source integration.
- Networking, cloud sync, or remote backup of weight records.
- Nutritional coaching, macro tracking, or body-fat analysis.
- Modifications to the calorie debt engine's internal calculation algorithm.

## Success Metrics

| Metric | Target |
|---|---|
| Weight recording and retrieval return identical results for the same inputs across platforms | 100% deterministic |
| All MUST acceptance criteria are covered by automated tests | 100% coverage |
| Debt recalculation produces deterministic results when triggered by a weight change | 100% deterministic |
| Weight history operations execute without platform-specific code in the domain layer | 100% pure-domain |

## Constraints

- Must be implemented in shared Kotlin Multiplatform domain code with no Android- or iOS-specific APIs.
- Must use `kotlinx.datetime.LocalDate` for all date handling to remain timezone-safe and platform-portable.
- Body weight values must be positive doubles expressed in kilograms.
- Only one weight entry per date is allowed; duplicate dates must be rejected on create and enforced on update.
- The debt recalculation service must consume the existing `CalorieDebtCalculator` interface from SLICE-0002 without modifying it.
- This slice must not modify any code in the `caloriedebt`, `profile`, or `logging` packages.

## Non-Functional Requirements

- Deterministic: the same weight inputs and operations must always produce identical outputs.
- Testable: all domain rules must be unit-testable in shared code without device setup or real storage.
- Portable: all models and interfaces must compile for both Android and iOS targets.
- Resilient: validation failures and repository errors must surface as structured domain errors.

## UX Notes

This slice is domain-only. No user-facing screens, forms, or visual components are produced.

## Functional Requirements

### MUST

- [ ] Define a `WeightEntry` model representing a single weight recording with date, weight in kilograms, and creation timestamp.
- [ ] Define a `WeightValue` value object wrapping a positive `Double` in kilograms.
- [ ] Validate that weight values are positive and within a realistic human range (0.5 kg to 500.0 kg).
- [ ] Enforce uniqueness of weight entries by date: reject creation when an entry for the same date already exists.
- [ ] Define a `WeightHistoryRepository` interface with operations: save entry, update entry by date, delete entry by date, get entry by date, get entries by date range, and get all entries.
- [ ] Implement a `LocalWeightRepository` adapter using in-memory storage for this slice.
- [ ] Define a `DebtRecalculationService` that recomputes calorie debt when a weight entry is added, updated, or deleted.
- [ ] Return structured domain errors for all validation failures and repository operation errors.

### SHOULD

- [ ] Return weight entries in chronological order when fetching all or by range.
- [ ] Support retrieval of the latest weight entry for convenience.
- [ ] Provide a weight change delta between two dates for derived summary use.

### COULD

- [ ] Compute a simple moving average of weight over a configurable window for future smoothing use.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given a valid weight value and date, saving a weight entry returns a `WeightEntry` with the provided values and a creation timestamp | Yes |
| AC-02 | When a weight entry already exists for the given date, saving a new entry for that date returns a structured error with code `DUPLICATE_WEIGHT_DATE` | Yes |
| AC-03 | Weight history is retrievable as a chronologically ordered list via `getAll()` | Yes |
| AC-04 | When a weight entry is updated for an existing date, the debt recalculation service is invoked and produces a recomputed debt result | Yes |
| AC-05 | When a weight value is edited for a historical date, the updated value is persisted and the old value is replaced | Yes |
| AC-06 | When a weight value is zero, negative, below 0.5 kg, or above 500.0 kg, validation returns a structured error with code `INVALID_WEIGHT_VALUE` | Yes |
| AC-07 | When entries are fetched for a date range, only entries within the inclusive range are returned in chronological order | Yes |
| AC-08 | When deleting a weight entry by date, the entry is removed from the repository and subsequent queries no longer return it | Yes |
| AC-09 | Debt recalculation is deterministic: the same weight history and calorie entries always produce the same debt result | Yes |
| AC-10 | No code in `caloriedebt`, `profile`, or `logging` packages is modified by this slice | Yes |

## Out of Scope

- Compose or SwiftUI screens for weight entry or history visualization.
- Charts, graphs, or any visual weight-trend rendering.
- Google Fit, HealthKit, or external weigh-in data imports.
- Database migration scripts, SQL schemas, or Room/SQLDelight integration (future slice concern).
- Analytics, coaching, or recommendation logic beyond debt recalculation.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | What weight range should be considered valid? Resolved: 0.5 kg to 500.0 kg covers realistic human body weight. | RESOLVED |
| 2 | Should multiple weight entries per day be allowed? Resolved: no, only one entry per date to maintain a clean timeline. | RESOLVED |
| 3 | Should this slice include a real database persistence adapter? Resolved: no, use in-memory storage. A future slice will introduce database persistence. | RESOLVED |
| 4 | How does debt recalculation consume the existing debt engine? Resolved: the recalculation service calls `CalorieDebtCalculator.calculate()` with an updated target derived from the new weight. | RESOLVED |
