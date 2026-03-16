# HLD: SLICE-0003 — User Profile + Goal Domain

**Author:** Architect
**Date:** 2026-03-16
**PRD Reference:** `docs/slices/SLICE-0003/prd.md`

---

## Purpose

This design defines a shared pure-domain module for BurnMate user profile and goal validation. It translates the PRD into deterministic models and services that later onboarding, profile, and dashboard slices can consume without duplicating measurement, BMI, or healthy-goal rules.

## System Context Diagram

```text
┌──────────────────────────────┐
│ Future UI / Onboarding Slice │
└──────────────┬───────────────┘
               │ heightCm, currentWeightKg, goalWeightKg
               v
┌────────────────────────────────────────────┐
│ SLICE-0003: User Profile + Goal Domain     │
│                                            │
│  Profile Input Validation                  │
│  BMI Calculation Helpers                   │
│  Healthy Goal Validation                   │
│  Derived Profile Summary Mapping           │
└──────────────┬─────────────────────────────┘
               │ UserProfileSummary / GoalValidationResult
               v
┌──────────────────────────────┐
│ Future Persistence / UI Use  │
└──────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `UserProfileFactory` | Public entry point that validates raw metric inputs and builds the profile aggregate plus derived helpers |
| `ProfileMetricsValidator` | Validates positive measurements, weight-loss direction, and healthy goal range preconditions |
| `BmiCalculator` | Computes BMI values and optional BMI classification from metric inputs deterministically |
| `HealthyGoalValidator` | Evaluates whether a goal weight is safe for the supplied height and current weight and emits structured reasons |
| Domain models | Carry normalized measurements, derived BMI values, and validation outcomes for future slices |

## Domain Model

| Entity / Value Object | Description |
|---|---|
| `BodyMetrics` | Core metric measurements: height, current weight, and goal weight |
| `BmiSnapshot` | BMI value plus optional category for a single weight state |
| `GoalValidationReason` | Enum identifying why a goal is invalid or confirming validity |
| `GoalValidationResult` | Structured result with validity flag, reason, and relevant derived values |
| `UserProfileSummary` | Aggregate response containing normalized metrics, current BMI, goal BMI, BMI delta, and kilograms-to-lose |

## Data Flow

```text
1. Caller sends raw metric inputs to `UserProfileFactory`.
2. `ProfileMetricsValidator` verifies all values are positive and goal weight is below current weight.
3. `BmiCalculator` computes current BMI and goal BMI using metric units.
4. `HealthyGoalValidator` checks whether goal BMI is within the allowed healthy range of 18.5..24.9.
5. The factory assembles `UserProfileSummary` with derived helper values.
6. The factory returns the summary plus a `GoalValidationResult`.
```

## Service Interfaces

| Interface | Responsibility |
|---|---|
| `UserProfileFactory` | Creates a validated profile summary from raw metric inputs |
| `ProfileMetricsValidator` | Validates inputs and direction rules before profile creation |
| `BmiCalculator` | Computes BMI and categorizes it using pure functions |
| `HealthyGoalValidator` | Evaluates whether the goal weight is healthy for the supplied height |

## Data Structures

| Structure | Notes |
|---|---|
| `BodyMetrics` | Immutable metric input aggregate used across all services |
| `UserProfileSummary` | Immutable output aggregate consumed by future UI and persistence slices |
| `GoalValidationResult` | Explicit validity contract so future UI can render deterministic messages |
| `ClosedFloatingPointRange<Double>` | Internal constant used for the healthy BMI range |

## Platform Responsibilities

| Platform / Layer | Responsibility |
|---|---|
| Shared `commonMain` domain layer | Owns all profile validation, BMI calculations, enums, and result models |
| Android / iOS presentation layers | Collect metric inputs and display derived results or validation messages |
| Future data layer | Persists profile fields after this slice, using the domain contracts defined here |

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| Kotlin standard library | Internal | Numeric operations and immutable model support |
| Kotlin test | Internal | Shared unit tests for deterministic profile behavior |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Zero or negative height | BMI cannot be computed safely | Fail fast with field-specific validation error |
| Zero or negative weight | Profile metrics are invalid | Reject request with explicit validation reason |
| Goal weight greater than or equal to current weight | Weight-loss profile intent is violated | Reject request before persistence or UI rendering |
| Goal BMI below healthy lower bound | Unsafe goal could be suggested downstream | Return invalid goal result with deterministic reason code |
| Floating-point rounding ambiguity | Inconsistent BMI displays across platforms | Use a single shared calculator and document rounding behavior in the LLD |

## Observability

| Signal | Type | Description |
|---|---|---|
| `profile_domain.create.invoked` | Metric | Count of profile summary creation attempts |
| `profile_domain.validation_failed` | Metric | Count of invalid profile or goal submissions by reason code |
| `profile_domain.goal_validation` | Log | Structured summary of valid vs invalid goal evaluations in future app layers |

## Security and Privacy Notes

- Height and weight are health-adjacent personal data and must remain in-memory only for the duration of domain computation in this slice.
- This slice must not write health data to logs, storage, or external services.
- No authentication or authorization logic is in scope because the module is an in-process domain component.

## Out of Scope

- UI state management, copywriting, or localized validation messages.
- Persistence schemas, repository interfaces, or cross-device sync.
- Networking, Google login, Google Fit, charts, or onboarding-step orchestration.

## HLD Acceptance Checklist

- [x] All PRD MUST requirements are addressed by at least one component
- [x] Dependencies are identified and versioned
- [x] Failure modes have mitigations
- [x] Observability signals are defined
- [x] Security/privacy concerns are documented or explicitly marked N/A
- [x] No implementation detail is specified (that belongs in LLD)
