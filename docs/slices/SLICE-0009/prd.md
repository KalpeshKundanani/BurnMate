# PRD: SLICE-0009 - Google Fit + Google Login

**Author:** Planner
**Date:** 2026-03-17
**Status:** APPROVED

---

## Problem Statement

BurnMate has completed the core profile, logging, dashboard, and UI slices, but users still have to enter burn manually. That creates friction and limits the usefulness of the dashboard for users who already carry activity data in their Google account. This slice adds an optional Android-only Google sign-in and Google Fit import flow so BurnMate can reuse approved Google activity data without changing the existing domain model or dashboard experience.

## Users

- BurnMate users on Android who want to connect a Google account and reuse Google Fit activity data.
- Returning dashboard users who want imported burn to appear in the existing dashboard and daily logging experience.
- Engineers and QA who need a deterministic, testable integration layer that does not rewrite completed domain or UI slices.

## Non-Goals

- Apple Health, HealthKit, Health Connect, or any non-Google health integration.
- Settings redesign, account-management screens, subscription, billing, export, or reset flows.
- Backend token exchange, cloud sync, analytics, or remote account storage.
- New calorie-debt, logging, profile, weight, or dashboard domain features unrelated to Google auth and Fit import.
- Replacing onboarding or blocking the existing BurnMate flow behind Google sign-in.

## Success Metrics

| Metric | Target |
|---|---|
| Users can connect a Google account, grant Google Fit access, and complete an initial import from the dashboard | 100% of in-scope happy path |
| Imported burn contributes to the existing dashboard and daily logging flows without a parallel read model | 100% of imported data paths |
| Presentation, mapping, and sync logic remain deterministic and covered by automated tests | 100% of MUST acceptance criteria |
| Unauthorized domain, settings, or non-Google integration changes introduced by this slice | 0 |

## Constraints

- The slice is Android-only for platform integration; non-Android builds must surface integration unavailable state rather than attempt Google Fit behavior.
- The sign-in path must use modern Google account integration for Android and must not add auth/business logic to composables.
- Google Fit API availability is a hard product constraint: Google blocked new developer sign-up after May 1, 2024, and the Fit platform is deprecated in 2026. This slice therefore assumes BurnMate uses an already approved Fit-enabled Google project; if that prerequisite is not met, implementation must surface an unavailable or blocked state instead of substituting another platform.
- Imported data must map into existing BurnMate burn-facing models and repository/read-model flows; no parallel integration-only dashboard state is allowed.
- Initial and manual refresh imports are limited to a deterministic 30-day window ending on the selected dashboard date or current date on first connect.
- No settings polish, export/reset behavior, or unrelated domain redesign is allowed in this slice.

## Non-Functional Requirements

- Deterministic: identical auth, permission, and Google Fit sample inputs must produce identical UI state and identical imported burn entries.
- Thin UI: composables render state and emit intents only.
- Testable: auth-state transitions, permission handling, mapping, and sync-upsert behavior must be unit-testable at the presentation and integration layers.
- Privacy-aware: the app must not persist raw Google tokens or expose raw SDK objects outside Android adapter boundaries.
- Scope-contained: the slice must preserve existing onboarding, dashboard, logging, and chart flows.

## UX Notes

- Existing onboarding remains unchanged; Google connection is optional and starts from the existing dashboard experience.
- The dashboard gains a dedicated Google integration status section below the summary cards and above the existing action cards.
- Successful imports do not create a separate sync screen. Imported burn shows up inside the existing dashboard summaries and daily logging list once the current view models reload.
- Denied permission, cancelled sign-in, unavailable platform, and import failure states must be visible in the integration section with clear recovery actions.
- Disconnect is in scope as a dashboard action, but deleting imported history is not.

## Functional Requirements

### MUST

- [ ] Provide a sign-in entry point in the current dashboard UI that does not block existing onboarding, dashboard, or daily logging flows.
- [ ] Support Google sign-in success, cancellation, and failure handling.
- [ ] Request and reflect Android activity-recognition permission plus Google Fit read access for steps and calories, only when required.
- [ ] Import Google Fit daily activity data for a fixed 30-day window ending on the selected dashboard date or the current date on the initial connect flow.
- [ ] Prefer Google Fit calorie data when available; when a day exposes steps but not calories, apply a deterministic step-to-burn estimate in the integration layer.
- [ ] Map imported burn into existing BurnMate burn-facing data structures and write it through the existing repository path so current dashboard and daily logging features reflect the result.
- [ ] Keep repeated imports duplicate-safe and deterministic so the Google-owned data for a day is replaced rather than duplicated.
- [ ] Surface signed-out, authenticating, permission-required, syncing, imported, empty, denied, unavailable, disconnected, and error states clearly in UI state.
- [ ] Allow the user to retry import and disconnect from the dashboard integration section.
- [ ] Keep auth, permission, import, and mapping logic outside composables and outside completed domain slices.

### SHOULD

- [ ] Show the connected Google account display name or email in the integration status section.
- [ ] Show a concise summary of the most recent import window and number of imported burn entries.
- [ ] Reload the existing dashboard and daily logging presentation state immediately after a successful import so the user sees the result without leaving the flow.

### COULD

- [ ] Trigger a lightweight state refresh on dashboard entry when a cached Google session and granted permissions already exist, provided it stays deterministic and does not add hidden background sync behavior.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | The dashboard exposes a Google connection CTA that lets the user initiate sign-in without altering the existing onboarding or navigation flow | Yes |
| AC-02 | A successful Google sign-in updates the integration state to an authenticated state tied to the selected account | Yes |
| AC-03 | Sign-in cancellation, sign-in failure, and permission denial are surfaced clearly and do not corrupt existing dashboard or logging state | Yes |
| AC-04 | When required permissions are granted, the app can import Google Fit daily activity data for the defined 30-day window | Yes |
| AC-05 | Imported Google Fit activity data is adapted into BurnMate burn information using deterministic mapping rules and duplicate-safe sync behavior | Yes |
| AC-06 | After a successful import, the existing dashboard and daily logging experiences reflect the imported burn contribution through current repository/read-model flows | Yes |
| AC-07 | Empty Google Fit data, unavailable Google Fit access, and blocked-project states are visible to the user as explicit integration states | Yes |
| AC-08 | Disconnect clears future Google auth/Fit access for the slice without introducing export/reset behavior or deleting manual entries | Yes |
| AC-09 | No business logic is embedded in composables, and raw Google SDK types do not escape adapter boundaries | Yes |
| AC-10 | The slice remains limited to Google auth plus Google Fit import only; behavior is deterministic and testable at the presentation/integration layer | Yes |

## Out of Scope

- Any Apple or cross-platform health connector.
- A dedicated settings or account-management screen.
- Historical backfill beyond the defined 30-day import window.
- Exporting, resetting, or deleting previously imported Google-owned entries during disconnect.
- Manual logging redesign, domain validation redesign, or chart feature changes unrelated to exposing imported burn already supported by the app.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | Where should Google connection start so the app preserves current flows? Resolved: the dashboard owns the sign-in and status entry point; onboarding remains unchanged. | RESOLVED |
| 2 | What import horizon keeps the slice small while still supporting the current dashboard range needs? Resolved: a fixed 30-day window ending on the selected dashboard date or current date. | RESOLVED |
| 3 | How should steps-only days map into BurnMate burn without redesigning domain logic? Resolved: the integration layer prefers Google Fit calories and falls back to a documented deterministic step-to-burn estimate. | RESOLVED |
| 4 | What happens if BurnMate does not have an approved Google Fit project? Resolved: the slice must surface an unavailable or blocked integration state; it must not silently substitute Health Connect or another platform in this slice. | RESOLVED |
| 5 | What does disconnect remove in this slice? Resolved: disconnect removes auth and future sync ability only; reset/export or historical data cleanup is out of scope. | RESOLVED |
