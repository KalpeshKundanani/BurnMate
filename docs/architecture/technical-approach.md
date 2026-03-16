# BurnMate Technical Architecture

## Overview

BurnMate is implemented using a **Clean Architecture approach** with strict separation between layers.

The system is built using **Kotlin Multiplatform with Compose Multiplatform** to share business logic across platforms.

Architecture prioritizes:

- deterministic domain logic
- testability
- incremental development
- autonomous slice execution

---

# Core Architecture Layers

The application is structured into four logical layers.

UI Layer  
Application Layer  
Domain Layer  
Data Layer

---

## Domain Layer

The domain layer contains pure business logic.

Characteristics:

- no platform dependencies
- deterministic logic
- pure functions
- full unit test coverage

Examples:

- calorie debt calculation
- validation rules
- trend classification
- BMI helpers

The domain layer must not import:

- UI frameworks
- database libraries
- networking libraries

---

## Application Layer

The application layer orchestrates domain use cases.

Responsibilities include:

- coordinating domain services
- exposing use cases to the UI layer
- preparing view models

The application layer may depend on domain interfaces.

---

## Data Layer

The data layer implements persistence and external integrations.

Examples:

- local storage
- API integrations
- Google Fit connectors

The data layer implements repository interfaces defined in the domain layer.

---

## UI Layer

The UI layer renders application state.

The UI layer must:

- depend only on ViewModels
- avoid direct domain manipulation
- remain thin and declarative

---

# Technology Stack

BurnMate uses the following technologies.

Language: Kotlin

UI Framework:
Compose Multiplatform

Architecture Pattern:
MVVM

Build System:
Gradle

Testing:
JUnit / Kotlin test

---

# Project Structure

High-level structure:

composeApp/
  domain/
  application/
  data/
  ui/

docs/
  slices/
  vision/
  architecture/

.ai/
  framework configuration

scripts/
  framework validators

---

# Slice-Based Development

BurnMate is developed using **slice-based architecture**.

Each slice introduces one coherent capability.

Slices progress through the following lifecycle:

NOT_STARTED  
PRD_DEFINED  
HLD_DEFINED  
LLD_DEFINED  
CODE_IN_PROGRESS  
CODE_COMPLETE  
REVIEW_APPROVED  
QA_APPROVED  
AUDIT_APPROVED

Only after audit approval is a slice considered complete.

---

# Slice Isolation Rules

Each slice must:

- operate within defined scope
- avoid modifying unrelated components
- maintain backward compatibility with prior slices

Changes outside the slice scope are prohibited.

---

# Validation System

The repository contains validation scripts that enforce framework rules.

These validators check:

- slice registry consistency
- artifact presence
- state machine transitions
- documentation freeze rules

All validators must pass before commits related to slice transitions.

---

# Testing Strategy

Testing is mandatory for domain logic.

Each slice must define explicit test identifiers:

T-01  
T-02  
T-03  
...

Tests must verify:

- deterministic behavior
- validation failures
- boundary conditions

---

# Determinism Requirement

Domain logic must be deterministic.

The following are prohibited in domain code:

- random number generation
- time-dependent behavior without injection
- network calls
- file system operations

All domain logic must be testable in isolation.

---

# Dependency Direction

Dependencies must follow this rule:

UI → Application → Domain

Data implements interfaces defined by Domain.

Domain must not depend on other layers.

---

# Performance Considerations

BurnMate prioritizes simplicity over premature optimization.

Domain computations are lightweight and designed for predictable execution.

---

# Security Principles

BurnMate does not store sensitive credentials in the domain layer.

External integrations must use secure platform APIs.

---

# Autonomous Development Goal

The architecture supports autonomous AI-driven development.

Key enabling factors:

- slice isolation
- deterministic domain logic
- strict framework validation
- contract-based slice execution

This allows multiple slices to be developed incrementally with minimal human intervention.