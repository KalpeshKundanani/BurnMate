# HLD: SLICE-0010 - Settings + Final Polish

**Author:** Architect
**Date:** 2026-03-17
**PRD Reference:** `docs/slices/SLICE-0010/prd.md`

---

## Purpose

This design adds BurnMate's final app-management surface on top of the completed onboarding, dashboard, logging, chart, and Google integration slices. It introduces a dedicated settings route, app-scoped coordination for preferences/export/reset, and narrowly defined release-polish touchpoints while preserving existing domain and repository behavior.

## System Context Diagram

```text
┌──────────────────────────────────────────────────────────────────────┐
│ SLICE-0010: Settings + Final Polish                                 │
│                                                                      │
│  App shell / navigation                                              │
│    └── SettingsScreen                                                │
│          ├── Preferences section                                     │
│          ├── Integration status section                              │
│          ├── Export section                                          │
│          └── Reset confirmation flow                                 │
│                                                                      │
│  SettingsViewModel                                                   │
│    ├── AppPreferencesStore                                           │
│    ├── AppExportCoordinator                                          │
│    ├── AppResetCoordinator                                           │
│    └── Existing Google integration contracts                         │
└───────────────────┬───────────────────────────────┬──────────────────┘
                    │                               │
                    v                               v
      ┌────────────────────────────┐   ┌──────────────────────────────┐
      │ Existing BurnMate app data │   │ Existing UI / integration    │
      │ active profile             │   │ dashboard header entry point │
      │ entry repository           │   │ dashboard status section      │
      │ weight repository          │   │ navigation host               │
      └────────────────────────────┘   └──────────────────────────────┘
```

## Architecture Overview

- Settings becomes a first-class route in the existing navigation shell.
- The dashboard header action is repurposed from a placeholder into the primary settings entry point.
- A small app-scoped settings/data-management layer owns preferences, export generation, and reset execution.
- Existing Google integration contracts remain the source of truth for sign-out/disconnect status; settings consumes them instead of redefining integration state.
- Export and reset are modeled as explicit action flows with transient confirmation/progress/result state inside `SettingsViewModel`.
- Final polish remains narrow: settings-related navigation/copy touchpoints only, not a general redesign.

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `SettingsViewModel` | Owns immutable settings UI state, preference updates, export/reset lifecycle, and integration-management actions |
| `SettingsUiState` | Holds section state, confirmation state, progress flags, and user-facing result messages |
| `AppPreferencesStore` | Reads and updates app-level preference values needed by settings and dependent app services |
| `AppExportCoordinator` | Builds a deterministic export payload from app-managed state and delegates platform delivery to an adapter |
| `AppResetCoordinator` | Clears app-managed state in a defined order and returns the app to a post-reset baseline |
| `SettingsScreen` | Renders settings sections, launches confirmation UI, and emits intents only |
| `SettingsSection` composables | Render reusable preference, status, and destructive-action rows without owning business logic |
| `BurnMateNavigationHost` / `BurnMateAppRoot` changes | Register the settings route, wire shared dependencies, and route post-reset navigation back to onboarding |
| Existing dashboard touchpoint | Keeps a lightweight entry or status hint that routes users into settings where appropriate |

## Data Flow

```text
1. User taps the existing header action from the dashboard.
2. Navigation opens `SettingsScreen`.
3. `SettingsViewModel` reads app preferences, current profile presence, and Google integration state.
4. The screen renders sections from immutable `SettingsUiState`.
5. Preference change intents update `AppPreferencesStore` and publish a new settings snapshot.
6. Export intent asks `AppExportCoordinator` to assemble a deterministic export payload and invoke the platform export adapter.
7. Reset intent opens confirmation state first; only confirmed reset calls `AppResetCoordinator`.
8. After reset succeeds, the app shell clears active profile/navigation state and routes back to onboarding.
```

## Preference Strategy

- The slice exposes `dailyTargetCalories` as the app-level preference because the existing navigation dependencies already parameterize dashboard service creation with that value.
- Preference reads and writes are centralized so dashboard service construction and settings UI stay aligned.
- Additional preferences are out of scope unless they already exist as explicit app-managed configuration before implementation starts.

## Export Strategy

- Export operates on an app-defined snapshot rather than on raw repository internals.
- The snapshot includes only app-managed data already available inside BurnMate: active profile summary, preference values, calorie entries, weight entries, and integration metadata needed for user understanding.
- Export generation is pure and deterministic; platform-specific share/save behavior is delegated to a thin adapter.
- Export never mutates app state.

## Reset Strategy

- Reset is modeled as a destructive, all-or-nothing action.
- The reset coordinator clears app preferences, local calorie entries, local weight entries, active profile/session state, and future Google integration access state.
- If integration disconnect is needed, the coordinator delegates to existing Google integration contracts rather than duplicating disconnect logic.
- Post-reset navigation returns the app to the same baseline as a clean first run.

## Integration Status Strategy

- Settings reuses the already-built Google integration state model and disconnect path.
- The dashboard may continue to show the integration status section for quick visibility, but the durable management action path belongs in settings.
- No new auth or sync platform behavior is introduced here.

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Existing `GoogleIntegrationViewModel` collaborators or extracted shared integration facade | Internal | Source of status/disconnect behavior for settings |
| Existing `EntryRepository` | Internal | Read/export/reset of calorie-entry data |
| Existing `WeightHistoryRepository` / `WeightHistoryService` | Internal | Read/export/reset of weight-entry data |
| Existing app navigation shell | Internal | Hosts the new settings route and post-reset navigation |
| Platform export launcher adapter | Internal platform adapter | Needed only to hand the generated export payload to Android or future platform UX |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Preferences fail to load or persist | Settings cannot reflect the intended current value | ViewModel shows explicit settings-level error state and keeps destructive actions separate |
| Export payload generation fails | User cannot export data | Surface non-destructive error state; do not mutate app data |
| Platform export handoff fails | Payload exists but cannot be shared/saved | Surface actionable error copy; keep generated snapshot available for retry within the flow when practical |
| Reset fails after partial clear attempt | App state could drift | Reset coordinator must define ordered operations and return success only after the full sequence completes; failures surface an error and stop navigation reset |
| Google disconnect fails during reset | Future access might remain active | Reset coordinator surfaces failure and does not mark reset complete until the disconnect step resolves or is intentionally bypassed per explicit policy |
| Navigation returns to a route that assumes an active profile after reset | App would land in an invalid state | App shell routes to onboarding only after reset success and active profile is cleared |

## Observability

| Signal | Type | Description |
|---|---|---|
| `settings.opened` | Event / metric hook | Settings route became visible |
| `settings.preference.updated` | Event / metric hook | An in-scope app preference changed |
| `settings.export.completed` | Event / metric hook | Export payload built and handoff succeeded |
| `settings.export.failed` | Event / metric hook | Export generation or platform handoff failed |
| `settings.reset.completed` | Event / metric hook | Full reset completed successfully |
| `settings.reset.failed` | Event / metric hook | Reset failed before completion |

## Security and Privacy Notes

- Export payloads contain only user-visible BurnMate data already stored in-app.
- The design must not leak raw Google SDK objects or credentials into settings UI state.
- Reset and export actions require clear user intent and must not occur implicitly during navigation.
- No remote upload, analytics exfiltration, or hidden background sync is introduced.

## Out of Scope

- Replacing existing repositories with a new persistence engine.
- Editing profile metrics in settings unless a completed upstream slice already exposes a stable contract for it.
- New global navigation paradigms, account systems, or provider integrations.
- Arbitrary polish passes across unrelated screens.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
