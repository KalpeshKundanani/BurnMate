# HLD: SLICE-0008 - Charts & Visual Progress

**Author:** Architect
**Date:** 2026-03-17
**PRD Reference:** `docs/slices/SLICE-0008/prd.md`

---

## Purpose

This design adds a visualization layer to the existing dashboard by introducing chart-oriented presentation adapters, reusable chart composables, and dashboard integration points. The design stays strictly above the existing read-only services and does not modify domain logic, persistence, or external integrations.

## System Context Diagram

```text
┌────────────────────────────────────────────────────────────────────┐
│ SLICE-0008: Charts & Visual Progress                              │
│                                                                    │
│  DashboardScreen                                                   │
│    └── DashboardVisualProgressSection                              │
│          ├── ChartRangeSelector                                    │
│          ├── DebtTrendChart                                        │
│          ├── WeeklyDeficitBarChart                                 │
│          ├── WeightTrendChart                                      │
│          └── GoalProgressRing                                      │
│                                                                    │
│  DashboardViewModel                                                │
│    ├── existing summary-card mapping                               │
│    └── chart visualization loading + state orchestration           │
│                                                                    │
│  presentation/dashboard/charts/                                    │
│    ├── DashboardChartDataSource                                    │
│    └── DashboardChartStateAdapter                                  │
└───────────────────┬───────────────────────────────┬────────────────┘
                    │                               │
                    v                               v
       ┌──────────────────────────┐      ┌──────────────────────────┐
       │ DashboardReadModelService│      │ WeightHistoryService     │
       │ (existing read-only)     │      │ (existing read-only)     │
       └──────────────────────────┘      └──────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `DashboardViewModel` | Own selected date, selected chart range, summary-card state, and visualization load lifecycle for the dashboard screen |
| `DashboardChartDataSource` | Presentation-only adapter that reads existing dashboard and weight-history outputs for the requested range |
| `DashboardChartStateAdapter` | Converts read-only models into chart-ready presentation state with normalized labels and deterministic display deltas |
| `DashboardVisualizationUiState` / `DashboardChartState` | Immutable dashboard visualization state consumed by UI |
| `ui/components/charts/*` | Reusable Compose chart primitives for line, bar, range selector, and ring rendering |
| `DashboardVisualProgressSection` | Integrates the chart primitives into a cohesive dashboard section with empty and error states |

## Architecture Overview

- Keep the existing dashboard summary-card pipeline intact for the selected date.
- Add a chart-specific loading path under `presentation/dashboard/charts/` so range changes can refresh visualizations without redefining dashboard domain contracts.
- Use existing `DashboardReadModelService` output for debt-based visuals and the existing weight-history read API for weight trend data.
- Convert raw read-only data into stable chart state before it reaches composables.
- Render charts with reusable Compose primitives under `ui/components/charts/`; dashboard-specific layout belongs in a dashboard organism/section, not in the low-level chart primitives.

## Data Flow

```text
1. Dashboard opens with the existing selected date and default chart range `Last7Days`.
2. `DashboardViewModel` loads the base dashboard snapshot through the existing `DashboardReadModelService`.
3. `DashboardViewModel` asks `DashboardChartDataSource` for range-specific visualization inputs:
   - debt-history snapshot for the selected date and range
   - weight-history entries for the selected date and range
4. `DashboardChartStateAdapter` maps those inputs into:
   - debt trend points
   - weekly deficit bars
   - weight trend points
   - progress-ring state
5. `DashboardScreen` renders the existing summary cards plus `DashboardVisualProgressSection`.
6. When the user changes range or date, the ViewModel reloads only the affected visualization data and publishes a new immutable state snapshot.
```

## Range Handling Strategy

- The chart UI exposes fixed presets: 7, 14, and 30 days.
- Debt-based visuals continue to use the existing dashboard read-model implementation, but presentation requests a range-specific history window from a chart data source rather than changing domain contracts.
- Weekly deficit bars always show the latest 7 daily deltas ending on the selected date.
- The weight trend chart uses the same selected range as the debt trend chart so both visuals remain aligned in time.

## Chart Derivation Rules

| Visualization | Upstream Source | Presentation Derivation |
|---|---|---|
| Calorie debt trend line | `DashboardSnapshot.debtChartPoints` | Sort chronologically, trim to selected range, format labels, normalize for rendering |
| Weekly deficit bars | `DashboardSnapshot.debtChartPoints` | Compute day-over-day delta from consecutive cumulative-debt points and keep the latest 7 bars |
| Weight progress trend | Existing `WeightHistoryService` range read | Sort chronologically, select the latest entry for duplicate dates, map to chart points |
| Dashboard progress ring | `DashboardSnapshot.weightSummary` | Convert progress percentage to ring fraction and remaining-kg copy only |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Existing `DashboardReadModelService` | Internal | Read-only source for summary cards and debt-history chart inputs |
| Existing `WeightHistoryService` | Internal | Read-only source for historical weight points |
| Compose Multiplatform `foundation` / `ui` / `material3` | Internal | Existing UI stack used to render charts with Canvas and layout primitives |
| Shared dashboard presentation layer | Internal | Existing `DashboardViewModel`, `DashboardUiState`, and mapper classes are extended rather than replaced |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Debt-history read returns no points | Debt trend and weekly bars cannot render | Show chart-level empty state; keep the rest of the dashboard in content mode |
| Weight-history range returns no entries | Weight trend cannot render | Show weight-chart empty state and keep progress ring fallback copy if current progress is available |
| Range-specific chart load fails | Visualization section becomes partially unavailable | Keep summary cards visible, mark visualization state as error, and allow retry through the existing dashboard event path |
| Duplicate weight entries exist for the same date | Multiple points would overlap visually | Presentation selects the latest entry by `createdAt` for that date and documents the rule in LLD |
| Flat data series produces zero vertical range | Chart line or bars could collapse visually | Chart renderer uses centered/zero-baseline fallback rules so output stays readable |

## Observability

| Signal | Type | Description |
|---|---|---|
| `dashboard.visualization.loaded` | Event / metric hook | Visualization state successfully mapped for the selected date and range |
| `dashboard.visualization.empty` | Event / metric hook | One or more charts rendered an explicit empty state due to missing data |
| `dashboard.visualization.range_changed` | Event / metric hook | User switched between 7-, 14-, and 30-day visualization presets |

## Security and Privacy Notes

- The slice only visualizes data already available inside the app through existing read-only services.
- No new storage, logging destination, or outbound network traffic is introduced.
- No additional PII is surfaced beyond the weight and calorie values already shown in dashboard summaries.

## Out of Scope

- Changes to `DashboardSnapshot`, `DebtChartPoint`, `WeightEntry`, or any domain repository/service contract.
- Third-party chart frameworks or analytics SDKs.
- New routes, settings panels, export flows, or integration work.
- Predictive insights, badges, or coaching overlays.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
