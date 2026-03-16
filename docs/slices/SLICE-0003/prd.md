# PRD: SLICE-0003 — User Profile + Goal Domain

**Author:** Planner
**Date:** 2026-03-16
**Status:** APPROVED

---

## Problem Statement

BurnMate needs a deterministic profile domain that captures the user's core body metrics and validates whether a weight goal is safe before any UI or persistence is added. Without a shared domain contract for height, current weight, goal weight, BMI, and healthy-goal checks, later slices would risk duplicating rules and producing inconsistent coaching or validation behavior across platforms.

## Users

- BurnMate users who need the app to reason about their height, current weight, and goal weight consistently.
- Future onboarding, profile, and dashboard slices that need a trusted domain model for user body metrics.
- Engineers and QA validating that safe-goal rules are deterministic in shared Kotlin code on Android and iOS.

## Non-Goals

- Profile entry UI, onboarding forms, or validation messaging copy.
- Persistence, repository implementations, or syncing profile data across devices.
- Nutrition coaching, calorie-target recommendation, or medical advice beyond deterministic goal validation.

## Success Metrics

| Metric | Target |
|---|---|
| Shared profile calculations return identical outputs for identical inputs across platforms | 100% deterministic |
| Domain validation covers all MUST acceptance criteria with automated tests | 100% of MUST acceptance criteria |
| BMI and healthy-goal helper functions execute without I/O or platform dependencies | 100% pure-domain execution |

## Constraints

- Must be implemented in shared Kotlin Multiplatform domain code with no Android- or iOS-specific APIs.
- Must use deterministic pure functions only; no persistence, network access, clocks, or randomness.
- Must keep scope limited to the profile and goal-validation bounded context: height, current weight, goal weight, BMI helper logic, and healthy goal validation logic.
- Must represent validation failures as domain-level deterministic errors rather than UI-side concerns.

## Non-Functional Requirements

- Deterministic: the same height and weight inputs must always produce the same validation and BMI outputs.
- Explainable: validation results must expose explicit reasons so future UI slices can render actionable feedback.
- Testable: all business rules in this slice must be unit-testable in shared code without device setup.
- Safe-by-default: impossible or unhealthy targets must be rejected or flagged by domain logic before later slices can persist or display them.

## Functional Requirements

### MUST

- [ ] Define domain models for user height, current weight, goal weight, BMI summary, and healthy-goal validation result.
- [ ] Accept height, current weight, and goal weight using explicit metric units only for this slice.
- [ ] Validate that height, current weight, and goal weight are all positive non-zero values.
- [ ] Compute BMI from height and weight deterministically using the standard metric formula `weightKg / (heightMeters^2)`.
- [ ] Return BMI helper output for both current weight and goal weight.
- [ ] Validate whether a goal weight is healthy relative to the user's height by checking the goal BMI stays within an allowed healthy range.
- [ ] Reject goal weights that are greater than or equal to the current weight for a weight-loss profile.
- [ ] Return structured validation output describing whether the goal is valid and, if invalid, why.

### SHOULD

- [ ] Return derived helper values such as the kilograms to lose and the BMI delta between current and goal state.
- [ ] Distinguish validation failures caused by impossible measurements from failures caused by an unhealthy target range.

### COULD

- [ ] Classify BMI values into coarse categories (`UNDERWEIGHT`, `HEALTHY`, `OVERWEIGHT`, `OBESE`) for future UI use, provided the rules remain deterministic and pure.

## Acceptance Criteria

| ID | Criterion | Testable? |
|---|---|---|
| AC-01 | Given positive metric inputs for height, current weight, and goal weight, the domain creates a profile aggregate exposing those values without platform dependencies | Yes |
| AC-02 | When height or either weight is zero or negative, the domain returns a validation error instead of a partial result | Yes |
| AC-03 | BMI helper logic returns the same numeric BMI for the same height and weight inputs every time using the metric BMI formula | Yes |
| AC-04 | The domain returns both current BMI and goal BMI helper outputs for a valid profile input | Yes |
| AC-05 | When goal weight is greater than or equal to current weight, healthy-goal validation fails with a deterministic domain error for a weight-loss profile | Yes |
| AC-06 | When goal BMI falls below the healthy lower bound of `18.5`, healthy-goal validation fails with a deterministic domain error | Yes |
| AC-07 | When goal BMI is between `18.5` and `24.9` inclusive and goal weight is below current weight, healthy-goal validation succeeds | Yes |
| AC-08 | Validation output exposes an explicit validity flag and machine-readable reason code so later UI slices can render user-facing copy | Yes |
| AC-09 | Derived helper values include kilograms-to-lose and BMI delta for valid profiles | Yes |
| AC-10 | All behavior is pure-domain only with no persistence, networking, login, chart, or onboarding concerns introduced by this slice | Yes |

## Out of Scope

- Compose or SwiftUI forms for entering height and weight.
- Persistence schemas, repository interfaces, or profile storage.
- Google login, Google Fit, charts, dashboards, and onboarding flow behavior.
- Calorie-target recommendation, body-fat analysis, or any feature requiring medical or external data.

## Open Questions

| # | Question | Status |
|---|---|---|
| 1 | What unit system should this slice support first? Resolved: metric-only domain inputs (`centimeters` and `kilograms`) for deterministic scope control. | RESOLVED |
| 2 | What healthy-goal range should govern validation? Resolved: goal BMI must be within `18.5..24.9` inclusive for this slice. | RESOLVED |
| 3 | Does this slice support gaining weight or maintenance goals? Resolved: no, this slice covers weight-loss goal validation only. | RESOLVED |
