# PRD: SLICE-0008 - Charts & Visual Progress

**Author:** Planner
**Date:** 2026-03-17
**Status:** APPROVED

---

## Problem Statement

BurnMate's dashboard currently renders only textual summary cards, even though the product vision emphasizes long-term trend visibility and visual progress. Users can see current numbers, but they cannot quickly understand whether debt is improving, how their weight is trending, or how close they are to their goal without mentally reconstructing history from raw values. This slice adds a visualization layer on top of existing read-only outputs so the dashboard communicates progress at a glance without changing any domain or persistence behavior.

## Users

- BurnMate users who want a quick visual read of recent calorie-debt recovery and weight direction.
- Returning dashboard users who need a richer progress view without leaving the main screen.
- Engineers and QA who need deterministic chart state derived from already-audited read-only data sources.

## Non-Goals

- Any changes to `caloriedebt`, `dashboard`, `logging`, `profile`, or `weight` domain models.
- Any persistence, storage, migration, or repository redesign.
- Google Fit, Google login, analytics integrations, or any external API work.
- Coaching, prediction, recommendations, or any new health-scoring logic.
- A separate full-screen analytics route; charts remain part of the existing dashboard.

## Success Metrics

| Metric | Target |
|---|---|
| Dashboard can render all four visualization types when underlying read-only data exists | 100% of supported data scenarios |
| Chart and progress state transitions are covered by shared presentation tests | 100% of MUST acceptance criteria |
| Unauthorized domain/persistence changes introduced by this slice | 0 |
| Default chart range interaction completes without leaving the dashboard | 100% of in-scope flows |

## Constraints

- Implementation must stay in shared Compose Multiplatform `ui` and `presentation` code only.
- Visualizations must consume existing read-only outputs; no new domain logic, persistence behavior, or external integration may be introduced.
- Debt trend and weight trend visualizations must support recent ranges in the 7-30 day window through fixed dashboard presets.
- Weekly deficit visualization must be derived from existing calorie-debt history data already available to presentation.
- The dashboard must continue to function when any single visualization lacks data; missing chart data is not a screen-level failure.
- No third-party chart SDK is required for this slice; built-in Compose drawing primitives are sufficient.

## Non-Functional Requirements

- Deterministic: identical upstream outputs and user actions must yield identical chart state.
- Thin presentation: mapping may sort, normalize, label, and derive display deltas, but it may not invent new business rules.
- Testable: chart adapters and ViewModel range/date transitions must be unit-testable in `commonTest`.
- Extensible: the chart component layer must be reusable for later dashboard polish without reworking upstream domains.

## UX Notes

- Charts live on the existing dashboard screen below the summary cards and above the quick-action cards.
- The default chart range is the last 7 days, with dashboard-local presets for 14 and 30 days.
- Missing data uses explicit empty-state copy inside the visualization section instead of hiding the section entirely.
- The progress ring should visually reinforce goal completion using the existing BurnMate visual language rather than introducing a separate chart theme.

## Functional Requirements

### MUST

- [ ] Render a calorie debt trend line chart for the selected dashboard date using recent debt history from existing read-only outputs.
- [ ] Support dashboard-local chart range presets covering the last 7, 14, and 30 days.
- [ ] Render a weight progress chart showing recorded weights across the selected range using existing read access only.
- [ ] Render a weekly deficit visualization as a bar chart of daily deficit/surplus values derived from existing debt history.
- [ ] Render a dashboard progress ring showing goal progress from existing weight progress data.
- [ ] Keep dashboard summary cards and visualization state aligned to the same selected date.
- [ ] Represent missing debt history, missing weight history, and partial chart data with explicit visualization states instead of crashing or silently omitting the section.
- [ ] Keep all chart normalization, labeling, and day-over-day delta derivation in presentation adapters only.

### SHOULD

- [ ] Reuse shared chart surface, legend, and axis-label patterns so the dashboard visuals feel cohesive.
- [ ] Preserve the existing dashboard navigation and action-card flow while adding the visualization section.
- [ ] Highlight the latest point or current progress value so users can quickly anchor themselves in the chart.

### COULD

- [ ] Add subtle range-change transitions using existing Compose UI capabilities if they do not require new libraries or hidden state.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given a selected range of 7, 14, or 30 days and existing debt history, the dashboard renders a line chart with chronologically ordered debt points for that range | Yes |
| AC-02 | When the user changes the chart range preset, the debt trend and weight trend refresh to the requested window without changing the selected dashboard date | Yes |
| AC-03 | Given consecutive debt-history points, the weekly deficit visualization renders daily bars whose values reflect the day-over-day change in cumulative debt | Yes |
| AC-04 | Given an existing `WeightSummary`, the progress ring reflects the same progress percentage and remaining-to-goal messaging already available in read-only outputs | Yes |
| AC-05 | Given weight entries in the selected range, the weight progress chart renders them in chronological order using deterministic point selection for duplicate dates | Yes |
| AC-06 | If debt history is unavailable, the dashboard keeps summary content visible and shows an explicit empty or error state for debt-based charts | Yes |
| AC-07 | If weight history is unavailable, the dashboard keeps debt and summary content visible and shows an explicit empty state for the weight chart and progress ring fallback copy | Yes |
| AC-08 | Visualization state is derived entirely from existing read-only data paths; the slice introduces no domain-model or persistence changes | Yes |
| AC-09 | ViewModel and chart adapter behavior is deterministic across repeated runs with identical dependency outputs | Yes |
| AC-10 | The slice remains limited to dashboard visualization work only: no settings, integrations, domain rewrites, or persistence redesign are added | Yes |

## Out of Scope

- Editing or creating weight entries from the visualization section.
- Changing dashboard read-model contracts or adding new persisted chart tables.
- Health recommendations, alerts, badges, or predictive timelines.
- Export/share flows, settings, onboarding changes, or route-level navigation changes outside dashboard integration.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | How is historical weight data sourced without changing domain models? Resolved: the slice uses the existing read-only `WeightHistoryService` range query from presentation and does not alter any weight contracts. | RESOLVED |
| 2 | How is daily deficit/surplus derived without adding new debt read-model fields? Resolved: the slice computes day-over-day deltas from consecutive `DebtChartPoint.cumulativeDebtCalories` values already exposed by the dashboard read model. | RESOLVED |
| 3 | Which recent-history windows are in scope for the dashboard chart controls? Resolved: fixed presets for 7, 14, and 30 days are in scope, with 7 days as the default. | RESOLVED |
