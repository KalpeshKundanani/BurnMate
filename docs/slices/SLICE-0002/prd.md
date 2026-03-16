# PRD: SLICE-0002 — Calorie Debt Engine

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate needs a deterministic way to convert logged daily calorie intake versus target calories into a single "calorie debt" signal that downstream features can trust. Without a shared engine, different screens and future coaching logic could calculate debt differently, creating inconsistent user feedback and making it hard to explain progress.

## Users

- BurnMate users who log daily calories and need a trustworthy debt summary.
- Future UI and coaching slices that will consume a single source of truth for calorie debt calculations.
- Engineers and QA validating calorie progress behavior across Android and iOS from shared Kotlin code.

## Non-Goals

- Food logging UI, editing workflows, or calendar navigation.
- Persistence, sync, or remote API integration for calorie entries.
- Weight-change forecasting, macro coaching, or workout recommendations.

## Success Metrics

| Metric | Target |
|---|---|
| Debt calculations return identical results for the same input across platforms | 100% deterministic |
| Engine handles a 30-day history on a mid-range device | < 10 ms per calculation |
| Business rules required by this slice are covered by automated tests | 100% of MUST acceptance criteria |

## Constraints

- Must be implemented in shared Kotlin Multiplatform code so Android and iOS use the same calculation path.
- Must operate entirely on caller-provided input for this slice; no database, network, or platform storage dependencies are allowed.
- Must be timezone-safe by requiring caller-provided `LocalDate` values that already represent the user's local day.
- Must keep scope limited to a single bounded context: calorie debt calculation and summary generation.

## Non-Functional Requirements

- Deterministic: identical input must always produce identical output with no hidden global state.
- Explainable: results must include enough breakdown detail for future UI slices to explain why debt changed.
- Testable: all edge cases in this slice must be expressible as pure unit tests without device or network setup.
- Portable: the design must avoid Android- or iOS-specific APIs in the domain contract.

## UX Notes

This slice is backend/domain-only. It must produce labels and summary fields that a future UI can render, but it does not define visual presentation.

## Functional Requirements

### MUST

- [ ] Calculate a per-day calorie delta from a logged calorie total and a target calorie budget.
- [ ] Accumulate positive calorie debt across an ordered date range so over-target days increase debt.
- [ ] Prevent negative carryover debt so under-target days can reduce existing debt to zero but never create negative debt.
- [ ] Return both the final debt total and a day-by-day breakdown showing starting debt, daily delta, and ending debt for each processed date.
- [ ] Ignore days outside the requested calculation range.
- [ ] Treat missing day logs as zero consumed calories for that date.
- [ ] Reject invalid input where the date range is inverted or where duplicate entries exist for the same date.

### SHOULD

- [ ] Return helper summary values for current streak information, including consecutive debt days ending on the latest processed date.
- [ ] Surface whether the latest processed day increased, reduced, or cleared debt so future UI can show concise status messaging.

### COULD

- [ ] Include a normalized debt severity band (`NONE`, `LOW`, `MEDIUM`, `HIGH`) derived from the final debt total for future coaching copy.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given an ordered range of dates and daily calorie entries, the engine returns a final debt equal to the cumulative sum of positive daily deltas with debt floored at zero after each day | Yes |
| AC-02 | When a day is over target, that day's ending debt increases by exactly `consumedCalories - targetCalories` | Yes |
| AC-03 | When a day is under target, that day's ending debt decreases by the deficit but never below zero | Yes |
| AC-04 | When a date in the requested range has no entry, the engine treats consumed calories as `0` for that day and still emits a breakdown row | Yes |
| AC-05 | When input contains duplicate dates or a start date after the end date, the engine returns a validation error instead of a partial result | Yes |
| AC-06 | The result includes one breakdown row per date in the requested range, ordered chronologically | Yes |
| AC-07 | The result exposes a latest-day trend value of `INCREASED`, `REDUCED`, `UNCHANGED`, or `CLEARED` based on the last processed date's effect on debt | Yes |
| AC-08 | If severity bands are included, a final debt of `0` maps to `NONE`, `1..299` to `LOW`, `300..699` to `MEDIUM`, and `700+` to `HIGH` | Yes |

## Out of Scope

- Visualizing the debt timeline in Compose or SwiftUI.
- Saving or merging logs from HealthKit, Google Fit, or any backend service.
- Personalized target-calorie recommendation logic.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | How should negative carryover be handled when multiple under-target days exceed current debt? Resolved: clamp debt at zero after each day. | RESOLVED |
| 2 | How should missing dates inside the requested range behave? Resolved: synthesize a zero-consumption day so the timeline stays contiguous. | RESOLVED |
| 3 | Does this slice include persistence or UI? Resolved: no, this slice is a pure shared-domain engine only. | RESOLVED |
