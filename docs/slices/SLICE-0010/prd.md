# PRD: SLICE-0010 - Settings + Final Polish

**Author:** Planner
**Date:** 2026-03-17
**Status:** APPROVED

---

## Problem Statement

BurnMate now covers onboarding, daily logging, dashboard summaries, charts, and Google Fit import, but the app still lacks a dedicated place to manage app-level preferences or data-management actions. Users cannot review integration status outside the dashboard, cannot export or reset their data through a supported flow, and some release-facing messages remain tied to slice-specific surfaces instead of a final app-management experience. This slice defines the final settings and release-polish scope needed to make BurnMate feel complete without expanding the product beyond its existing roadmap.

## Users

- BurnMate users who need a clear settings destination for app preferences and data-management tasks.
- BurnMate users who need a safe, explicit way to export their current app data.
- BurnMate users who need a destructive reset flow with unmistakable confirmation and predictable post-reset behavior.
- BurnMate users with Google integration already enabled who need to review status or disconnect from a settings surface.
- Engineers and QA who need deterministic settings, reset, and export behavior that does not rewrite completed slices.

## Non-Goals

- New health-provider integrations, cloud backup, or account systems.
- Replacing the existing dashboard, chart, onboarding, or logging feature set with redesigned flows.
- Redesigning domain models or persistence internals outside the minimal hooks needed for reset/export.
- Sharing, analytics, coaching, or social features.
- Manual GitHub Project or GitHub Issue automation not already defined in the repository.

## Success Metrics

| Metric | Target |
|---|---|
| Settings surface exposes all in-scope preferences and data-management actions from one navigable route | 100% of in-scope flows |
| Reset and export flows are deterministic, explicitly confirmed where destructive, and covered by automated tests | 100% of MUST acceptance criteria |
| Existing dashboard, logging, and integration behavior remains intact outside the touchpoints named in this PRD | 0 unauthorized regressions |
| Repository slice visibility remains valid under the repo's existing validators and tracking artifacts | 100% validator compliance |

## Constraints

- The slice must preserve the existing architecture and keep business logic outside composables.
- The app currently holds profile state in navigation coordination and local data in in-memory repositories, so settings/reset/export must use explicit app-scoped coordination rather than hidden global mutations.
- Export must be deterministic and based only on app-managed data already present in BurnMate.
- Reset must require explicit confirmation and must not partially clear state.
- Integration management is limited to already-built Google sign-in / Google Fit status and disconnect behaviors; no new provider or auth work is allowed.
- GitHub visibility work must use only the repository-approved mechanism discovered in this repo: `docs/slices/index.md`, per-slice `state.md`, and the validators/workflow that enforce them.

## Non-Functional Requirements

- Deterministic: identical app state and user actions must produce identical settings state, export payloads, and reset outcomes.
- Thin UI: composables render settings state, confirmation prompts, and action controls only.
- Testable: settings state mapping, reset/export orchestration, and navigation wiring must be unit-testable in shared code.
- Safe: destructive actions must be clearly labeled, confirmed, and isolated from unrelated app operations.
- Bounded: final polish is limited to the release-facing settings/data-management experience and explicitly named message or navigation touchpoints.

## UX Notes

- Settings is a dedicated screen reached from the existing header action in the app shell rather than a new onboarding or authentication route.
- The settings screen groups content into small sections: app preferences, integrations, data management, and app/release info.
- Reset uses a confirmation dialog or sheet with clear destructive copy and a final action label that cannot be triggered accidentally.
- Export communicates exactly what is being exported and whether the export completed or failed.
- Existing dashboard-level Google integration status may remain as a quick-status entry point, but settings becomes the durable management destination for release readiness.
- Final polish includes consistent release-facing copy for settings-related status and outcome messages only where this slice explicitly touches an existing surface.

## Functional Requirements

### MUST

- [ ] Provide a dedicated settings route and screen that is reachable from the existing app shell.
- [ ] Surface at least one app-level preference already implied by existing BurnMate behavior, with deterministic state initialization and updates.
- [ ] Show the current Google integration status and allow disconnect using the already-built integration contracts.
- [ ] Provide an export action for the app-managed user data that is deterministic and clearly communicates success or failure.
- [ ] Provide a reset action that requires explicit confirmation before clearing app-managed user data and returning the app to a clean post-reset state.
- [ ] Keep export, reset, and integration-management logic outside composables and behind explicit interfaces or coordinators.
- [ ] Preserve existing dashboard, chart, and logging capabilities except for the exact navigation or copy touchpoints required to integrate settings and final polish.
- [ ] Keep the slice limited to settings, reset/export, and release-polish work only.
- [ ] Update repository tracking artifacts so SLICE-0010 is represented through the existing registry/state mechanism enforced by repository validators.
- [ ] Maintain deterministic, testable ViewModel transitions for settings actions and confirmation flows.

### SHOULD

- [ ] Persist the selected app preference across app navigation within the current app session and make it available to the surfaces that already depend on it.
- [ ] Show concise export metadata such as export timestamp, included sections, or record counts when available from app-managed state.
- [ ] Provide release-facing app info or support copy in settings without introducing new remote services or marketing flows.

### COULD

- [ ] Add lightweight message cleanup on the dashboard or logging surfaces that points users to settings for integration or data-management actions, as long as the logic stays unchanged and the touchpoint is explicitly documented in the design.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | A settings screen exists and is reachable from the existing application shell without altering onboarding eligibility or the current dashboard/logging flow | Yes |
| AC-02 | The settings screen initializes deterministic UI state for in-scope preferences, integration status, and data-management actions | Yes |
| AC-03 | Reset requires explicit confirmation and does not execute until the user confirms the destructive action | Yes |
| AC-04 | After a confirmed reset, app-managed profile, local logging data, local weight data, settings preferences, and future Google integration access state are cleared through the defined reset path | Yes |
| AC-05 | Export produces a deterministic app-data payload and surfaces success or failure clearly without mutating existing data | Yes |
| AC-06 | Settings exposes the current Google integration status and allows disconnect through existing integration contracts only | Yes |
| AC-07 | Existing dashboard, logging, and chart surfaces remain functionally consistent, with only explicitly planned settings-entry or copy-polish touchpoints changed | Yes |
| AC-08 | No business logic resides in composables; reset/export/preferences/integration orchestration is isolated in ViewModel-owned collaborators | Yes |
| AC-09 | SLICE-0010 remains represented in the repository tracking flow through `docs/slices/index.md` and `docs/slices/SLICE-0010/state.md`, matching the repo validators and workflow | Yes |
| AC-10 | ViewModel transitions, confirmation-state handling, and action results are deterministic and testable | Yes |

## Out of Scope

- Health Connect, Apple Health, HealthKit, or any new sync source.
- Background backup, cloud restore, or remote export destinations.
- A new profile-editing domain, food database, coaching, or subscription experience.
- Arbitrary visual redesign of completed screens beyond explicit settings/final-polish touchpoints.
- GitHub automation beyond the repository-managed registry/state validation already present.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | Which existing app-level preference should settings expose without creating new product scope? Resolved: the slice exposes the existing daily target calorie setting already implied by `DEFAULT_DAILY_TARGET_CALORIES` and dashboard-service construction. | RESOLVED |
| 2 | How should settings become reachable without inventing a new global navigation pattern? Resolved: the existing dashboard header action becomes the primary settings entry point, and the rest of navigation remains intact. | RESOLVED |
| 3 | What counts as app-managed data for export and reset in the current architecture? Resolved: active profile, app preference values, local calorie entries, local weight entries, and Google integration access/session state managed by BurnMate are in scope; new cloud data stores are not. | RESOLVED |
| 4 | Does the repository define a planning-time GitHub Project or Issue sync script for slice visibility? Resolved: no such automation exists in the repo; the approved planning-time visibility mechanism is the slice registry/state artifacts enforced by `validate_slice_registry.py` and CI. | RESOLVED |
