# HLD: SLICE-XXXX — <!-- Slice Name -->

**Author:** `Architect`
**Date:** <!-- YYYY-MM-DD -->
**PRD Reference:** `docs/slices/SLICE-XXXX/prd.md`

---

## Purpose

<!-- 1-2 sentences tying this design to the PRD. What problem does this design solve? -->

## System Context Diagram

<!-- Show where this slice fits in the overall system. ASCII is fine. -->

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│  Client  │────>│  This Slice  │────>│ Database │
└──────────┘     └──────────────┘     └──────────┘
                       │
                       v
                 ┌──────────┐
                 │ External │
                 │ Service  │
                 └──────────┘
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| <!-- Component A --> | <!-- What it does --> |
| <!-- Component B --> | <!-- What it does --> |

## Data Flow

<!-- Show the sequence of operations. ASCII sequence diagram or numbered steps. -->

```
1. Client sends request to API endpoint
2. API validates input against schema
3. Service layer processes business logic
4. Repository persists to database
5. Response returned to client
```

## Dependencies

| Dependency | Type | Notes |
|---|---|---|
| <!-- e.g., Auth service --> | Internal / External | <!-- version, contract --> |

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| <!-- e.g., Database unavailable --> | <!-- e.g., Requests fail --> | <!-- e.g., Retry with backoff --> |

## Observability

| Signal | Type | Description |
|---|---|---|
| <!-- e.g., `invoice.overdue.email.sent` --> | Metric / Log / Trace | <!-- What it tracks --> |

## Security and Privacy Notes

<!-- Remove section if not applicable. -->

- <!-- e.g., PII handling requirements -->
- <!-- e.g., Auth/authz requirements -->

## Out of Scope

<!-- Design decisions explicitly deferred. Must align with PRD non-goals. -->

- <!-- Item 1 -->

## HLD Acceptance Checklist

- [ ] All PRD MUST requirements are addressed by at least one component
- [ ] Dependencies are identified and versioned
- [ ] Failure modes have mitigations
- [ ] Observability signals are defined
- [ ] Security/privacy concerns are documented or explicitly marked N/A
- [ ] No implementation detail is specified (that belongs in LLD)
