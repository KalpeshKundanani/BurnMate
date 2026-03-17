# HLD: SLICE-0009 - Google Fit + Google Login

**Author:** Architect
**Date:** 2026-03-17
**PRD Reference:** `docs/slices/SLICE-0009/prd.md`

---

## Purpose

This design adds an Android-only Google integration layer on top of BurnMate's existing dashboard, logging, and repository flows. It introduces Google account auth, Google Fit permission and history adapters, deterministic burn mapping, and a dashboard-level integration status section while keeping completed domain slices read-only.

## System Context Diagram

```text
┌──────────────────────────────────────────────────────────────────────┐
│ SLICE-0009: Google Fit + Google Login                               │
│                                                                      │
│  DashboardScreen                                                     │
│    └── GoogleIntegrationStatusSection                                │
│          └── GoogleIntegrationViewModel                              │
│                ├── GoogleAuthService                                 │
│                ├── PermissionCoordinator                             │
│                ├── GoogleFitService                                  │
│                ├── BurnImportMapper                                  │
│                └── ImportedBurnSyncService                           │
└───────────────────────┬───────────────────────────────┬──────────────┘
                        │                               │
                        v                               v
          ┌────────────────────────┐      ┌────────────────────────────┐
          │ Android Google Adapter │      │ Existing BurnMate Flows    │
          │ Credential Manager     │      │ EntryRepository            │
          │ Google account session │      │ DashboardReadModelService  │
          │ Google Fit History API │      │ DailyLoggingViewModel      │
          └──────────┬─────────────┘      └────────────────────────────┘
                     │
                     v
          ┌────────────────────────┐
          │ Google account + Fit   │
          │ Android platform data  │
          └────────────────────────┘
```

## Architecture Overview

- The dashboard remains the only user-facing entry point for connecting Google in this slice.
- Common `integration/` packages define platform-agnostic models, mapping rules, sync contracts, and presentation orchestration.
- Android adapters in `androidMain` own Credential Manager, Google account session, Google Fit permission, runtime permission, and Fit history SDK calls.
- Imported Google data is normalized into BurnMate burn entries before it reaches the existing repository; the dashboard and daily logging flows continue to read from the repository and existing read models.
- The integration section publishes status and actions only; it does not replace dashboard or logging view models.

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `GoogleIntegrationViewModel` | Orchestrates auth, permission, import, disconnect, and integration UI state transitions |
| `GoogleAuthService` | Reads cached Google account session, launches sign-in, and signs out or disconnects the current account |
| `PermissionCoordinator` | Resolves and requests Android runtime activity-recognition permission plus Google Fit OAuth access |
| `GoogleFitService` | Reads daily Google Fit steps and calories for the requested date range and exposes platform availability |
| `BurnImportMapper` | Converts imported Google Fit samples into BurnMate burn samples using deterministic rules |
| `ImportedBurnSyncService` | Upserts mapped burn samples into the existing `EntryRepository` using integration-owned deterministic IDs |
| `GoogleIntegrationStatusSection` | Renders signed-out, permission-required, syncing, imported, error, and unavailable UI states in the dashboard |
| `BurnMateAppRoot` / navigation wiring | Creates dependencies, wires the integration view model, and reloads existing dashboard/logging state after successful import |

## Core State Model

| State | Meaning |
|---|---|
| `SignedOut` | No cached Google account session is available |
| `Authenticating` | Sign-in is actively running |
| `SignedIn` | Google account exists but Google Fit access is not yet fully evaluated or granted |
| `PermissionRequired` | Runtime and/or Google Fit permissions are missing |
| `Syncing` | Google Fit data is being read, mapped, and written into BurnMate |
| `Imported` | The last import completed successfully, including empty-but-successful windows |
| `Error` | A recoverable auth, permission, or import failure occurred |
| `Unavailable` | Integration cannot proceed because the platform is unsupported, Google Fit is unavailable for this project, or required configuration is missing |

## Data Flow

```text
1. Dashboard loads and the integration section asks `GoogleIntegrationViewModel` to refresh state.
2. The view model checks `GoogleFitService.availability()` and `GoogleAuthService.readCachedSession()`.
3. If no session exists, the dashboard renders `SignedOut` with a connect CTA.
4. If the user taps connect, `GoogleAuthService` performs sign-in through Android-specific auth adapters.
5. After auth success, `PermissionCoordinator` evaluates runtime and Google Fit read permissions.
6. If permissions are missing, the dashboard renders `PermissionRequired`; the user can grant permissions from the same section.
7. Once permissions are granted, `GoogleFitService` reads daily Google Fit samples for the 30-day window ending on the selected dashboard date.
8. `BurnImportMapper` converts the external samples into deterministic BurnMate burn samples.
9. `ImportedBurnSyncService` replaces Google-owned entries in the same range inside `EntryRepository`, preserving manual entries.
10. `BurnMateAppRoot` triggers the existing dashboard and daily logging view models to reload so imported burn appears through current app flows.
```

## Integration Storage Strategy

- Imported Google data is not kept in a parallel integration store.
- Each imported day is represented as a synthetic BurnMate burn entry written through the existing `EntryRepository`.
- Integration-owned entries use deterministic IDs and a reserved ID prefix so re-import can replace only Google-owned data for the target range.
- Manual user-created entries remain untouched and keep their current semantics.

## UI Touchpoints

| Touchpoint | Purpose |
|---|---|
| Dashboard integration section | Sign in, request permissions, show sync state, retry import, and disconnect |
| Dashboard existing summary cards and charts | Reflect imported burn after reload through current read-model and visualization paths |
| Daily logging existing list | Reflect imported burn entries as standard burn rows after reload, without a new integration-specific screen |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` integration layer | Owns interfaces, models, mappers, sync rules, view model, and UI state |
| Shared `commonMain` UI/navigation layer | Hosts the dashboard integration section and wires post-import reload behavior |
| Android `androidMain` integration layer | Owns Credential Manager, Google account handling, Fit permission requests, runtime permission requests, and Fit history queries |
| Existing BurnMate repository/read models | Continue to own dashboard and daily logging read behavior; integration only writes compatible data |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Existing `EntryRepository` | Internal | Write target for imported burn entries |
| Existing `DashboardReadModelService` and dashboard presentation stack | Internal | Consume imported entries after reload |
| Existing daily logging presentation stack | Internal | Consume imported entries after reload |
| `androidx.credentials` + Google ID support library | External | Android Google auth entry point for sign-in |
| Google Play Services auth | External | Required for Google account + Google Fit permission interop |
| Google Play Services Fitness | External | Required for Google Fit read access |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| BurnMate is not using an approved Google Fit project | Google Fit import cannot legally or technically proceed | Surface `Unavailable` state with blocking explanation; do not substitute another platform |
| User cancels sign-in | No session is created | Return to `SignedOut` with neutral status messaging |
| User denies runtime or Google Fit permission | Import cannot start | Render `PermissionRequired` with retry CTA |
| Google Fit returns no data for the requested range | No burn samples exist to import | Treat as successful empty import; clear prior Google-owned entries in range and show empty-success status |
| Mapping or sync fails mid-import | Imported state could drift | Apply sync only after mapping succeeds; if sync fails, keep prior repository contents and show `Error` |
| Android-only dependencies are unavailable on non-Android builds | Shared UI still compiles but cannot connect | Surface `Unavailable` state via no-op platform wiring |

## Observability

| Signal | Type | Description |
|---|---|---|
| `integration.google.auth.started` | Event / metric hook | User initiated Google sign-in |
| `integration.google.permission.result` | Event / metric hook | Runtime or Google Fit permission request resolved |
| `integration.google.import.completed` | Event / metric hook | Import succeeded, with imported entry count and date range |
| `integration.google.import.failed` | Event / metric hook | Import, mapping, or sync failed with classified error |

## Security and Privacy Notes

- Raw Google tokens and raw SDK models stay inside Android adapter boundaries only.
- The common integration layer works only with sanitized session metadata and normalized daily activity samples.
- Google Fit scopes remain read-only and limited to the data types required by this slice.
- Disconnect clears future access only; history cleanup, export, and account management remain out of scope.

## Out of Scope

- Any non-Google health integration or migration to Health Connect.
- Settings polish, account screens, export, reset, or backend identity management.
- Manual logging redesign or changes to existing calorie-debt and dashboard calculations.
- Background sync scheduling, analytics instrumentation rollout, or push-style notifications.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
