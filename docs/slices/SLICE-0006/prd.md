# PRD: SLICE-0006 — Dashboard Read Model

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate has four independent domain modules — calorie debt engine, user profile, daily logging, and weight history — each producing isolated data. Users need a unified dashboard summary that aggregates today's calorie activity, current debt status, weight progress, and historical debt trends into a single, deterministic snapshot. Without a read model layer, any future dashboard UI would have to duplicate aggregation logic, creating fragile coupling and inconsistent calculations.

## Users

- Future dashboard UI slices that will consume the `DashboardSnapshot` to render summaries and charts.
- BurnMate users who expect a single view of today's calorie intake, burn, net calories, remaining budget, current debt, weight progress, and debt history.
- Engineers and QA validating that dashboard aggregation is deterministic across Android and iOS.

## Non-Goals

- Dashboard UI, Compose screens, or any visual rendering.
- Persistence or caching of the dashboard snapshot.
- Real-time streaming or reactive observation patterns.
- Modifications to the calorie debt, profile, logging, or weight domain modules.
- Analytics, coaching, or recommendation logic.
- Network requests or cloud sync.

## Success Metrics

| Metric | Target |
|---|---|
| Dashboard snapshot produces identical output for identical inputs across platforms | 100% deterministic |
| All MUST acceptance criteria are covered by automated tests | 100% coverage |
| Read model executes without platform-specific code in the domain layer | 100% pure-domain |
| No mutation of upstream domain state occurs during snapshot generation | 100% read-only |

## Constraints

- Must be implemented in shared Kotlin Multiplatform domain code with no Android- or iOS-specific APIs.
- Must use `kotlinx.datetime.LocalDate` for all date handling.
- Must consume existing domain interfaces as read-only dependencies without modifying them.
- Must not persist, cache, or store the dashboard snapshot — it is computed fresh on each invocation.
- This slice must not modify any code in the `caloriedebt`, `profile`, `logging`, or `weight` packages.

## Non-Functional Requirements

- Deterministic: the same inputs must always produce identical dashboard snapshots.
- Testable: all aggregation logic must be unit-testable in shared code without device setup or real storage.
- Portable: all models and interfaces must compile for both Android and iOS targets.
- Stateless: the read model service must hold no internal mutable state between invocations.

## UX Notes

This slice is domain-only. No user-facing screens, forms, or visual components are produced.

## Functional Requirements

### MUST

- [ ] Define a `DashboardSnapshot` model aggregating all dashboard data for a given date.
- [ ] Define a `TodaySummary` model containing today's total intake calories, total burn calories, net calories, and remaining calories relative to the daily target.
- [ ] Define a `DebtSummary` model containing the current calorie debt value, debt severity, and debt trend.
- [ ] Define a `WeightSummary` model containing the current weight, goal weight, kilograms remaining, and progress percentage.
- [ ] Define a `DebtChartPoint` model representing a single data point for chart rendering with date and cumulative debt value.
- [ ] Define a `DashboardReadModelService` interface with a `getDashboardSnapshot(today: LocalDate)` method.
- [ ] Implement the service to aggregate data from the logging, calorie debt, weight, and profile domains.
- [ ] Produce deterministic output: identical inputs must always yield identical snapshots.

### SHOULD

- [ ] Return a chart-ready list of `DebtChartPoint` entries for debt history visualization.
- [ ] Support configurable chart history window length (number of days).

### COULD

- [ ] Include a convenience field for the snapshot date in the `DashboardSnapshot` model.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given calorie log entries for today, the dashboard snapshot's `TodaySummary.totalIntakeCalories` equals the sum of all intake entries for that date | Yes |
| AC-02 | Given calorie log entries for today, the dashboard snapshot's `TodaySummary.totalBurnCalories` equals the sum of all burn entries for that date | Yes |
| AC-03 | The dashboard snapshot's `TodaySummary.netCalories` equals `totalIntakeCalories - totalBurnCalories` | Yes |
| AC-04 | The dashboard snapshot's `TodaySummary.remainingCalories` equals `dailyTargetCalories - totalIntakeCalories` | Yes |
| AC-05 | The dashboard snapshot's `WeightSummary` reflects the current weight, goal weight, and progress percentage | Yes |
| AC-06 | The dashboard snapshot's `DebtSummary` contains the current debt value, severity, and trend from the calorie debt engine | Yes |
| AC-07 | The dashboard snapshot includes a chart-ready list of `DebtChartPoint` entries ordered chronologically | Yes |
| AC-08 | The read model produces deterministic output: calling `getDashboardSnapshot` twice with the same inputs yields identical snapshots | Yes |

## Out of Scope

- Compose or SwiftUI screens for dashboard rendering.
- Charts, graphs, or any visual rendering of dashboard data.
- Persistence, caching, or local storage of snapshots.
- Real-time or reactive observation of dashboard state changes.
- Modifications to the calorie debt engine, profile, logging, or weight domains.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | How should intake vs. burn calories be distinguished in the logging domain? Resolved: `CalorieAmount.value` positive = intake, negative = burn (existing convention). | RESOLVED |
| 2 | What chart history window should be used? Resolved: configurable, defaulting to 7 days. | RESOLVED |
| 3 | Should the read model persist snapshots? Resolved: no, compute fresh on each call. | RESOLVED |
