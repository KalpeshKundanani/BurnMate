# PRD: SLICE-0007 — Core UI

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate has completed domain and read-model layers for profile setup, logging, weight history, calorie debt, and dashboard aggregation, but the app still ships with the default template UI. Users cannot complete onboarding, view their daily summary, or log calories from the product. BurnMate needs a first usable Compose Multiplatform interface that exposes the existing capabilities without adding new business logic.

## Users

- BurnMate users who need a simple first-run onboarding flow for profile and goal input.
- BurnMate users who need a dashboard view of today's summary and current debt/weight progress.
- BurnMate users who need to add and delete calorie entries and move between dates.
- Engineers and QA who need deterministic presentation behavior built on the audited domain layers.

## Non-Goals

- Charts or advanced visual progress views.
- Google Fit integration, Google login, or any external account flow.
- Settings polish, reset/export tools, or persistence redesign.
- New business rules in profile, logging, weight, calorie debt, or dashboard domains.

## Success Metrics

| Metric | Target |
|---|---|
| Users can complete the first-run UI flow from onboarding into the dashboard without placeholder screens | 100% of core flow |
| Core screen state is deterministic and covered by presentation-layer tests | 100% of MUST acceptance criteria |
| UI stays limited to presentation/navigation work on top of existing slice outputs | 0 unauthorized domain changes |

## Constraints

- Must be implemented in shared Compose Multiplatform UI and presentation code.
- Must consume existing `profile`, `logging`, `weight`, and `dashboard` interfaces as-is.
- Must represent validation, loading, and empty states without moving business rules into composables.
- Must keep scope limited to onboarding UI, dashboard UI, daily logging UI, and date navigation UI.

## Non-Functional Requirements

- Deterministic: identical upstream data and user intents must produce identical screen state.
- Thin UI: composables render state and emit intents only.
- Testable: ViewModel state transitions and presentation mappers must be unit-testable in shared code.
- Extensible: screen structure must leave room for later chart, settings, and integration slices without redesign.

## Functional Requirements

### MUST

- [ ] Provide an onboarding flow that collects height, current weight, and goal weight.
- [ ] Validate onboarding submissions through the existing profile domain and surface resulting errors in UI state.
- [ ] Provide a dashboard screen that renders today's summary from the dashboard read model.
- [ ] Show current weight/debt summary information in dashboard form.
- [ ] Provide a daily logging screen that supports creating calorie entries from the UI.
- [ ] Support both intake and burn entry intent paths in the UI layer while keeping business rules outside composables.
- [ ] Allow deleting an existing calorie entry from the daily logging screen.
- [ ] Allow moving backward and forward by date so the visible daily state updates.
- [ ] Represent loading, empty, and validation-error states explicitly in screen models.
- [ ] Keep all business logic outside composables.

### SHOULD

- [ ] Reuse common UI patterns for summary rows, date headers, and call-to-action sections.
- [ ] Route first-run users to onboarding and returning users to dashboard through a simple navigation host.
- [ ] Expose retry/refresh intents for dashboard and logging state reloads.

### COULD

- [ ] Include lightweight presentation copy or helper labels that make validation and empty states clearer, as long as they are derived from existing domain outputs.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Onboarding captures valid height, current weight, and goal weight inputs and can submit them through the existing profile domain contract | Yes |
| AC-02 | Dashboard renders today's summary using `DashboardReadModelService` output for the selected date | Yes |
| AC-03 | Daily logging UI can add an intake entry through presentation-layer orchestration | Yes |
| AC-04 | Daily logging UI can add a burn entry through presentation-layer orchestration without putting business rules in composables | Yes |
| AC-05 | Daily logging UI can delete an existing entry and reflect the updated state | Yes |
| AC-06 | Date navigation changes the displayed day and refreshes the visible dashboard/logging state for that day | Yes |
| AC-07 | Validation errors are shown clearly from presentation/domain error mapping rather than ad hoc UI rules | Yes |
| AC-08 | Screen state transitions are deterministic and testable at the ViewModel level | Yes |
| AC-09 | No business logic resides in composables; composables remain render-and-intent surfaces only | Yes |
| AC-10 | The slice remains limited to core UI only: no charts, Google Fit, Google login, settings, export/reset, or persistence redesign | Yes |

## Out of Scope

- Debt charts, timelines, and other advanced visualization widgets.
- Google Fit, Google login, HealthKit, or remote sync.
- Settings screens, preferences, reset flows, export flows, and release polish.
- Changes to completed domain logic or repository behavior.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | How should the UI represent burn entry capture when the current logging validator accepts non-negative calorie amounts only? Resolved: the slice plans explicit presentation-layer burn intent mapping while keeping domain changes out of scope; implementation must work within the existing audited contracts or surface unsupported cases clearly. | RESOLVED |
| 2 | Should charts be included because the dashboard read model already exposes chart points? Resolved: no, chart rendering is deferred to SLICE-0008. | RESOLVED |
| 3 | Should onboarding include settings/profile polish beyond the required metrics? Resolved: no, this slice captures only height, current weight, and goal weight. | RESOLVED |
