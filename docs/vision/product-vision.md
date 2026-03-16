# BurnMate Product Vision

## Overview

BurnMate is a weight management application built around a **calorie debt model** rather than traditional daily calorie tracking.

Instead of focusing on a strict daily calorie limit, BurnMate tracks the **accumulated calorie balance (debt or surplus)** over time. This provides a more flexible and psychologically sustainable approach to weight management.

Users can recover from occasional overeating by maintaining a negative calorie balance over subsequent days.

The system emphasizes long-term trends rather than daily perfection.

---

# Core Concept — Calorie Debt

Traditional calorie tracking:

daily_calories = intake - burn

BurnMate model:

calorie_debt = cumulative(intake - burn)

Debt increases when calorie intake exceeds expenditure and decreases when expenditure exceeds intake.

The application provides visibility into:

- current debt
- debt trend
- recovery progress

---

# Product Goals

BurnMate aims to:

1. Provide a sustainable weight management system
2. Focus on long-term trends instead of daily fluctuations
3. Simplify calorie tracking
4. Visualize progress clearly
5. Reduce user anxiety around occasional overeating

---

# Target Users

BurnMate is designed for:

- people trying to lose weight
- users interested in habit-based health tracking
- individuals who prefer flexible dieting approaches

---

# Core Capabilities

BurnMate provides the following core capabilities.

## 1 Calorie Logging

Users can record:

- calorie intake
- calorie burn

Entries are associated with a specific date.

---

## 2 Calorie Debt Calculation

The system continuously calculates cumulative calorie debt using logged entries.

Outputs include:

- current debt
- severity classification
- trend direction
- debt streak

---

## 3 Weight Tracking

Users can record body weight over time.

Weight data allows correlation between calorie debt and physical progress.

---

## 4 Trend Visualization

The application displays:

- calorie debt trends
- historical data
- recovery patterns

Charts show short-term and long-term trends.

---

## 5 Daily Summary

The dashboard provides a concise daily overview including:

- today's intake
- today's burn
- current calorie debt
- recovery trend

---

# Product Principles

BurnMate follows these product principles.

## Long-Term Thinking

Weight management is evaluated across weeks and months rather than individual days.

---

## Psychological Sustainability

Users should feel empowered rather than punished by the system.

The interface avoids guilt-inducing feedback.

---

## Simplicity

Data entry should be minimal and fast.

Complex calculations remain internal.

---

## Transparency

Users should clearly understand:

- current debt
- direction of progress
- recovery possibilities

---

# Out of Scope

BurnMate does not attempt to provide:

- meal planning
- diet coaching
- nutrition education
- social features
- complex calorie databases

The system focuses on **simple logging and debt tracking**.

---

# Product Architecture Strategy

The product is built incrementally using **slice-based development**.

Each slice introduces a small, testable capability.

Slices follow this lifecycle:

NOT_STARTED  
→ PRD_DEFINED  
→ HLD_DEFINED  
→ LLD_DEFINED  
→ CODE_IN_PROGRESS  
→ CODE_COMPLETE  
→ REVIEW_APPROVED  
→ QA_APPROVED  
→ AUDIT_APPROVED

Slices are frozen after audit approval.

---

# Current Slice Roadmap

SLICE-0001 — Architecture Bootstrap  
SLICE-0002 — Calorie Debt Engine  

Next slices include:

SLICE-0003 — User Profile + Goal Domain  
SLICE-0004 — Logging + Persistence  
SLICE-0005 — Weight Tracking  
SLICE-0006 — Dashboard Read Model  
SLICE-0007 — Core UI  
SLICE-0008 — Visualization  
SLICE-0009 — Integrations  
SLICE-0010 — Settings

---

# Success Criteria

The product is considered successful if users can:

- easily log calorie data
- understand their calorie debt
- visualize long-term trends
- maintain sustainable weight progress

---

# Future Expansion

Potential future features may include:

- wearable integrations
- health data imports
- predictive progress models
- adaptive calorie targets

These are not part of the initial product scope.