# HLD: SLICE-0007 — Core UI

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0007/prd.md`

---

## Purpose

This design defines BurnMate's first presentation layer on top of the completed domain and dashboard read-model slices. It introduces a small Compose Multiplatform navigation shell, screen-focused ViewModels, immutable UI state, and presentation mappers that adapt existing domain outputs into renderable screen models.

## System Context Diagram

```text
┌──────────────────────────────────────────────┐
│ SLICE-0007: Core UI                          │
│                                              │
│  Navigation Host                             │
│    ├── OnboardingScreen                      │
│    ├── DashboardScreen                       │
│    └── DailyLoggingScreen                    │
│                                              │
│  ViewModels                                  │
│    ├── OnboardingViewModel                   │
│    ├── DashboardViewModel                    │
│    └── DailyLoggingViewModel                 │
│                                              │
│  Presentation Mappers                        │
└──────────────┬──────────────┬────────────────┘
               │              │
               v              v
┌─────────────────────┐   ┌─────────────────────┐
│ Existing Domain     │   │ Existing Read Model │
│ profile/logging/    │   │ dashboard           │
│ weight/caloriedebt  │   │                     │
└─────────────────────┘   └─────────────────────┘
```

## Architecture Overview

- Presentation and UI sit above the existing domain/read-model layers and do not replace them.
- ViewModels expose immutable screen state plus intent handlers.
- Composables render state and emit user intents upward.
- A small navigation host wires the onboarding, dashboard, and logging routes only.
- Presentation mappers translate domain models, domain failures, and dashboard snapshots into UI-safe state objects.

## Core Screens

| Screen | Purpose | Primary Dependency |
|---|---|---|
| `OnboardingScreen` | Capture initial profile/goal metrics and show validation feedback | `profile` domain |
| `DashboardScreen` | Show selected-day summary, debt summary, and weight summary | `DashboardReadModelService` |
| `DailyLoggingScreen` | Show entries for the selected date and allow add/delete actions | `logging` domain and repository |

## Core Presentation Components

| Component | Responsibility |
|---|---|
| `OnboardingViewModel` + `OnboardingUiState` | Own form state, validation/error mapping, submit flow, and next-route decision |
| `DashboardViewModel` + `DashboardUiState` | Load dashboard snapshot for the selected date and map loading/content/empty/error states |
| `DailyLoggingViewModel` + `DailyLoggingUiState` | Load entries for the selected date, orchestrate add/delete actions, and coordinate date navigation |
| Navigation routes | Define the route contract between onboarding, dashboard, and logging |
| Presentation mappers | Convert `UserProfileSummary`, `DashboardSnapshot`, entry lists, and domain errors into UI state |

## Data Flow

```text
1. App starts in `BurnMateNavigationHost`.
2. Navigation selects onboarding or dashboard as the entry route.
3. Screen composable subscribes to immutable UI state from its ViewModel.
4. User actions are emitted as intents to the ViewModel.
5. ViewModel delegates to existing domain/read-model services and repositories.
6. ViewModel maps results into screen state and emits a new immutable snapshot.
7. Composables re-render from state only.
```

## Key Interactions

| Interaction | Flow |
|---|---|
| Onboarding submit | UI intent -> `OnboardingViewModel` -> profile domain validation/factory -> UI state update or navigate to dashboard |
| Dashboard load | Route/date intent -> `DashboardViewModel` -> `DashboardReadModelService` -> dashboard UI mapping |
| Logging load | Route/date intent -> `DailyLoggingViewModel` -> entry repository fetch -> list/empty/error state |
| Add/delete log entry | UI intent -> `DailyLoggingViewModel` -> logging domain/repository -> refresh selected-date state |
| Date change | UI intent -> ViewModel selected date update -> reload dashboard/logging data for that date |

## Architectural Rules

- No domain calculations inside composables.
- No repository calls inside composables.
- ViewModels own screen orchestration and convert raw results into stable UI state.
- Screen state must distinguish loading, content, empty, and error modes.
- The slice must stay chart-free even though chart data exists in `DashboardSnapshot`.
- The design must remain simple enough for later chart and integration slices to extend without reworking the core navigation shell.

## Failure Handling

| Failure | UI Behavior |
|---|---|
| Profile validation failure | Onboarding state shows field-level or banner errors without leaving the screen |
| Dashboard read-model failure | Dashboard state shows error/retry mode |
| No dashboard data yet | Dashboard state shows empty placeholder values, not a crash |
| Logging create/delete failure | Logging state preserves current entries and shows actionable error feedback |
| Unsupported burn-path constraint in current domain contracts | Logging state surfaces a deterministic unsupported/error state instead of hiding the failure |

## Out of Scope

- Chart UI, visual debt history widgets, or animation-heavy progress visuals
- Google login, Google Fit, or any external integration
- Settings, export, reset, or persistence redesign
- Changes to upstream domain interfaces or business rules

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one UI or presentation component
- [x] ViewModel and composable boundaries are explicit
- [x] Data flow from UI to existing domain/read-model dependencies is defined
- [x] Failure/empty/loading behavior is specified
- [x] Out-of-scope items are explicitly excluded
