# BurnMate – Technical Approach

## 1. Overview

This document defines the **technical strategy and architectural principles** used to build BurnMate.

The goal of the technical approach is to ensure:

- maintainability
- testability
- scalability
- deterministic AI-assisted development
- strong separation of concerns
- reusable UI components
- cross-platform code reuse

BurnMate will be developed as a **cross-platform mobile application** using **Kotlin and Compose Multiplatform**, targeting both Android and iOS from a shared codebase.

The architecture will follow **Clean Architecture principles** combined with **MVVM for presentation** and **Atomic Design for UI composition**.

---

# 2. Platform Strategy

BurnMate will be implemented using **Kotlin Multiplatform with Compose Multiplatform (CMP)**.

Target platforms:

- Android
- iOS

This approach allows the application to share:

- business logic
- domain models
- data layer
- ViewModels
- UI components (where possible)

Platform-specific code will be limited to:

- authentication integrations
- health APIs
- permission handling
- platform UI wrappers where necessary

This ensures high code reuse while preserving platform capabilities.

---

# 3. Architecture Overview

BurnMate follows **Clean Architecture** with clearly defined layers.

presentation
domain
data
platform

Each layer has a single responsibility.

---

## 3.1 Presentation Layer

Responsibilities:

- UI rendering
- UI state management
- user interaction handling

Components:

- Screens
- ViewModels
- UI components

The presentation layer will follow **MVVM architecture**.

ViewModels:

- coordinate domain use cases
- transform domain results into UI state
- expose immutable UI state to the UI

---

## 3.2 Domain Layer

The domain layer contains **pure business logic**.

Responsibilities:

- core calorie debt calculations
- validation rules
- use cases
- domain models

Characteristics:

- no platform dependencies
- fully unit testable
- deterministic logic

Examples of domain use cases:

- CalculateCalorieDebt
- UpdateDailyDeficit
- ComputeRemainingDebt
- GenerateProgressTrend

---

## 3.3 Data Layer

Responsibilities:

- data persistence
- data retrieval
- repository implementations

This layer bridges:

domain ↔ persistence

Repositories expose interfaces to the domain layer.

Implementations interact with:

- local database
- platform integrations
- external services

---

## 3.4 Platform Layer

This layer handles platform-specific integrations.

Examples:

- Google Fit integration
- HealthKit integration
- authentication
- permission APIs
- device services

The platform layer provides adapters that the data layer consumes.

---

# 4. UI Architecture

BurnMate will implement a **design system based on Atomic Design principles**.

The hierarchy is:

Atoms
Molecules
Organisms
Templates
Screens

---

## 4.1 Atoms

Smallest UI building blocks.

Examples:

- Text
- Button
- Icon
- InputField
- ChartPoint
- Divider

Atoms contain no complex logic.

---

## 4.2 Molecules

Combinations of atoms.

Examples:

- CalorieInputField
- ExerciseInputRow
- DateSelector
- MetricCard

Molecules represent small functional UI units.

---

## 4.3 Organisms

Complex UI sections composed of molecules.

Examples:

- DashboardHeader
- DebtSummaryCard
- ActivitySummarySection
- DebtTrendChart

Organisms form meaningful UI sections.

---

## 4.4 Templates

Layout compositions that define screen structures.

Examples:

- DashboardTemplate
- DailyEntryTemplate

Templates contain layout but minimal logic.

---

## 4.5 Screens

Complete UI screens.

Examples:

- OnboardingScreen
- DashboardScreen
- DailyLogScreen
- HistoryScreen

Screens connect UI templates to ViewModels.

---

# 5. Design System Source

UI design references will be generated using **Stitch**.

The workflow:

1. Generate visual designs in Stitch
2. Extract design components
3. Convert components into Atomic Design UI elements
4. Implement reusable UI library

The design system will ensure:

- visual consistency
- reusable components
- faster feature development

---

# 6. Visual Design Philosophy

BurnMate’s UI aims to be **premium, minimal, and motivational**.

Design characteristics:

- AMOLED dark theme
- neon accent colors
- strong typography for metrics
- subtle translucent glass effects
- high contrast for readability

The interface should emphasize **data clarity and motivation**.

The primary visual focus is:

Remaining Calorie Debt

---

# 7. State Management

BurnMate will use **ViewModel-driven state management**.

Principles:

- UI state is immutable
- ViewModels produce UI state
- UI observes state changes

Data flow:

User Action
→ ViewModel
→ Domain Use Case
→ Repository
→ Result
→ ViewModel
→ UI State Update
→ UI Recomposition

Reactive streams will be used where necessary.

---

# 8. Data Persistence

BurnMate will follow a **local-first data strategy**.

All user data will be stored locally on the device.

Examples of stored data:

- user profile
- weight data
- daily calorie intake
- exercise entries
- step data
- calorie debt progress

Persistence will use:

SQLDelight

Benefits:

- multiplatform support
- strong type safety
- efficient queries

---

# 9. External Integrations

### Google Fit

BurnMate will integrate Google Fit to obtain:

- step count
- estimated calories burned

Steps will be automatically imported daily.

Calories burned from steps will be added to daily activity.

---

### Manual Activity Entry

Users can manually log:

- exercise calories burned
- food calorie intake

Manual entries remain important because:

- not all activities are tracked automatically
- food databases are excluded from MVP

---

# 10. Charting and Visualization

The dashboard will include a **trend chart** representing calorie debt changes over time.

Chart characteristics:

- line chart
- scrollable timeline
- historical window: 15 days
- projection window: 7 days

The chart allows users to visually understand:

- good days
- bad days
- long-term trend

---

# 11. Authentication Strategy

The MVP will use **Google login**.

Purpose:

- simple onboarding
- minimal account friction

Authentication will enable:

- account identification
- future data sync capabilities

However, data will remain **local-first for MVP**.

---

# 12. Data Model

Core data entities include:

UserProfile
- gender
- height
- currentWeight
- goalWeight

DailyEntry
- date
- caloriesConsumed
- caloriesBurnedManual
- caloriesBurnedSteps
- netDeficit

DebtProgress
- remainingDebt
- totalDebt
- historicalTrend

These entities form the basis for domain logic.

---

# 13. Testing Strategy

Testing will focus on the **domain layer first**.

Tests will cover:

- calorie debt calculations
- daily deficit updates
- trend generation
- edge cases

UI testing will be minimal in the MVP.

---

# 14. Development Governance

BurnMate development will follow a deterministic AI-assisted workflow using the **AI Dev Framework**.

Development is divided into **slices**.

Each slice passes through the following lifecycle:

PRD
HLD
LLD
Code
Review
QA
Audit

This ensures:

- traceability
- AI coordination
- controlled development

---

# 15. Initial Development Strategy

Development will begin with **project architecture bootstrap**.

The first slice establishes:

- project structure
- architecture skeleton
- dependency configuration
- UI design system scaffolding

Subsequent slices will implement:

- calorie debt engine
- persistence layer
- dashboard ViewModel
- dashboard UI
- Google Fit integration

---

# 16. Non-Goals for Initial Version

The following features are intentionally excluded from MVP:

- meal database search
- macro nutrient tracking
- AI diet planning
- social sharing
- coaching systems
- wearable integrations beyond Google Fit

These may be considered in later versions.

---

# 17. Guiding Principle

The architecture must always support the product's core promise:

Make the user clearly see how today’s actions affect their remaining calorie debt.

All technical decisions should reinforce:

- clarity
- simplicity
- long-term maintainability