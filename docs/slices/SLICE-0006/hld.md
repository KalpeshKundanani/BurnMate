# HLD: SLICE-0006 — Dashboard Read Model

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0006/prd.md`

---

## Purpose

This design defines a read-only aggregation service that collects data from BurnMate's four existing domain modules — logging, calorie debt, weight history, and user profile — and composes a single `DashboardSnapshot` for consumption by a future dashboard UI. The read model performs no mutations, holds no state, and persists nothing.

## System Context Diagram

```text
┌──────────────────────────────────────────────────────────────┐
│ SLICE-0006: Dashboard Read Model                             │
│                                                              │
│  DashboardReadModelService                                   │
│    ├── collectTodayLogs()                                    │
│    ├── calculateTodayTotals()                                │
│    ├── readCurrentDebt()                                     │
│    ├── readWeightProgress()                                  │
│    └── prepareDebtChart()                                    │
│                                                              │
│  Output: DashboardSnapshot                                   │
└──────┬──────────┬──────────┬──────────┬──────────────────────┘
       │          │          │          │
       v          v          v          v
┌──────────┐ ┌────────┐ ┌────────┐ ┌──────────┐
│ Logging  │ │ Calorie│ │ Weight │ │ Profile  │
│ Domain   │ │ Debt   │ │ Domain │ │ Domain   │
│ (SLICE-  │ │ Engine │ │ (SLICE-│ │ (SLICE-  │
│  0004)   │ │(SLICE- │ │  0005) │ │  0003)   │
│          │ │ 0002)  │ │        │ │          │
└──────────┘ └────────┘ └────────┘ └──────────┘
  consumed     consumed   consumed   consumed
  read-only    read-only  read-only  read-only
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `DashboardReadModelService` | Public entry point that orchestrates data collection from all domain modules and assembles a `DashboardSnapshot` |
| `DashboardSnapshot` | Top-level immutable container holding today's summary, debt summary, weight summary, and chart data |
| `TodaySummary` | Aggregated calorie totals for a single day: intake, burn, net, and remaining |
| `DebtSummary` | Current calorie debt value, severity classification, and trend direction |
| `WeightSummary` | Current weight, goal weight, kilograms remaining, and progress percentage |
| `DebtChartPoint` | A single chart-ready data point with date and cumulative debt value |

## Domain Model

| Entity / Value Object | Description |
|---|---|
| `DashboardSnapshot` | Top-level aggregate containing the snapshot date, today summary, debt summary, weight summary, and chart data |
| `TodaySummary` | Value object: total intake, total burn, net calories, remaining calories, and daily target |
| `DebtSummary` | Value object: current debt in calories, debt severity, and debt trend |
| `WeightSummary` | Value object: current weight in kg, goal weight in kg, kilograms remaining, and progress percentage |
| `DebtChartPoint` | Value object: date and cumulative debt value for chart rendering |

## Data Flow

```text
1. Caller invokes `getDashboardSnapshot(today)` on `DashboardReadModelService`.
2. Service fetches today's calorie log entries from `EntryRepository.fetchByDate(today)`.
3. Service partitions entries into intake (positive amounts) and burn (negative amounts), computing totals.
4. Service computes net calories and remaining calories relative to the daily target.
5. Service reads the current calorie debt result from `CalorieDebtCalculator` for a window ending at `today`.
6. Service reads the latest weight entry from `WeightHistoryService` and computes weight progress relative to the goal from `BodyMetrics`.
7. Service prepares a list of `DebtChartPoint` entries from the debt calculation's day-by-day data.
8. Service assembles all summaries into a `DashboardSnapshot` and returns it.
```

## Service Interfaces

| Interface | Responsibility |
|---|---|
| `DashboardReadModelService` | Orchestrates read-only data collection and snapshot assembly |

## Data Structures

| Structure | Notes |
|---|---|
| `DashboardSnapshot` | Immutable aggregate returned by the service |
| `TodaySummary` | Immutable value object for today's calorie activity |
| `DebtSummary` | Immutable value object for current debt status |
| `WeightSummary` | Immutable value object for weight progress |
| `List<DebtChartPoint>` | Chronologically ordered chart data points |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` domain layer | Owns all dashboard models and the read model service |
| Shared `commonTest` layer | Contains all unit tests for the dashboard read model |
| Android / iOS presentation layers | Will consume `DashboardSnapshot` to render UI in future slices |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Kotlin standard library | Internal | Collection operations and immutable models |
| `kotlinx-datetime` | Internal | `LocalDate` for date handling |
| Kotlin test | Internal | Shared unit tests |
| SLICE-0004 `EntryRepository` | Internal (read-only) | Fetches today's calorie log entries |
| SLICE-0004 `CalorieEntry`, `CalorieAmount`, `EntryDate` | Internal (read-only) | Logging domain models |
| SLICE-0002 `CalorieDebtCalculator` | Internal (read-only) | Computes calorie debt for the dashboard |
| SLICE-0002 `CalculationWindow`, `DailyCalorieEntry`, `CalorieDebtResult`, `CalorieDebtDay` | Internal (read-only) | Debt domain models |
| SLICE-0005 `WeightHistoryService` | Internal (read-only) | Fetches weight entries |
| SLICE-0005 `WeightEntry`, `WeightValue` | Internal (read-only) | Weight domain models |
| SLICE-0003 `BodyMetrics` | Internal (read-only) | Profile domain model for goal weight |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| No calorie log entries for today | Today summary shows all zeros, which is valid | Return valid zero-values summary |
| No weight entries recorded | Weight summary cannot determine current weight | Return `null` for `WeightSummary` in the snapshot |
| Debt calculation returns a failure | Debt summary cannot be computed | Return `null` for `DebtSummary` in the snapshot |
| Empty chart history window | No chart points to render | Return empty `DebtChartPoint` list |

## Observability

| Signal | Type | Description |
|---|---|---|
| `dashboard.snapshot.generated` | Metric | Count of successfully generated dashboard snapshots |
| `dashboard.snapshot.partial` | Metric | Count of snapshots with null debt or weight summaries |

## Security and Privacy Notes

- The dashboard read model aggregates data already in memory from upstream domain modules.
- No data is written to logs, external services, or persistent storage.
- No authentication or authorization logic is in scope because the module is an in-process domain component.

## Out of Scope

- UI state management, dashboard screens, or chart rendering.
- Persistence, caching, or memoization of snapshots.
- Real-time streaming or reactive observation.
- Modifications to any upstream domain module.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
