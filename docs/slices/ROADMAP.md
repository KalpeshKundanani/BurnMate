# BurnMate Slice Roadmap (Locked)

This document defines the official slice execution order for the BurnMate project.
Agents must not reorder slices unless explicitly instructed by the project owner.

--------------------------------

SLICE-0001 — Architecture Bootstrap ✅
SLICE-0002 — Calorie Debt Engine

SLICE-0003 — User Profile + Goal Domain
- height
- current weight
- goal weight
- BMI helper
- goal validation

SLICE-0004 — Daily Logging Domain + Persistence
- calorie intake entry
- calorie burn entry
- date-based records
- local storage
- retrieve/update/delete by date

SLICE-0005 — Weight History + Debt Recalculation
- daily weight logs
- recalculate debt when weight changes
- support historical edits

SLICE-0006 — Dashboard Read Model
- combine profile + logs + debt engine + weight history
- today summary
- remaining debt summary
- chart data preparation

SLICE-0007 — Core UI
- onboarding UI
- dashboard UI
- daily logging UI
- history/date navigation

SLICE-0008 — Charts + Visual Progress
- debt trend chart
- -15 / +7 range logic
- progress visualization

SLICE-0009 — Google Fit + Google Login
- Google authentication
- permissions
- steps import
- burn estimation mapping

SLICE-0010 — Settings + Final Polish
- preferences
- reset/export
- release readiness

--------------------------------

Execution Rule

A slice may begin only after the previous slice reaches:

AUDIT_APPROVED