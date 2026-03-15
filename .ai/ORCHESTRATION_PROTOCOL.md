# Orchestration Protocol

This document defines how multiple AI models collaborate on a single project through deterministic, state-driven invocation. It is the execution layer above the state machine, role definitions, and context capsule spec.

Every model invocation must follow this protocol. No exceptions.

---

## A. Execution Model

### A1. State-Driven Workflow

Work is organized into slices. Each slice progresses through the state machine defined in `STATE_MACHINE.md`. The orchestrator (human or automation) advances work by invoking the correct model in the correct role for the current state. Models do not decide what to do next. The state determines what happens next.

```
Orchestrator reads state.md
    → Determines current state
    → Identifies next transition
    → Identifies owning role (ROLES.md)
    → Constructs context capsule
    → Invokes model in that role
    → Model produces output artifact
    → Orchestrator commits artifact
    → Orchestrator updates state.md
    → Loop
```

### A2. Capsule-First Invocation

Every model invocation begins with a context capsule (see `CONTEXT_CAPSULE.md`). The model must:

1. Parse the capsule before reading any other input.
2. Verify `current_state` against `state.md` on disk.
3. Verify `role` is authorized for the current transition.
4. Read every file in `relevant_documents`.
5. Produce output matching `expected_output.format` and `expected_output.artifact`.

If any verification fails, the model halts and reports the mismatch. It does not attempt recovery.

### A3. Deterministic Role Sequencing

The state machine enforces a fixed order of roles per slice:

```
Planner → Architect → Architect → Engineer → Engineer → Engineer
→ Reviewer → Engineer (if changes) → Reviewer (re-review)
→ QA → Engineer (if changes) → QA (re-test)
→ Auditor → Merge
```

No role may act out of sequence. The orchestrator must not invoke a Reviewer when the state is `LLD_DEFINED`. The state determines who runs.

### A4. No Cross-Role Leakage

A model invoked as Engineer must not produce review comments. A model invoked as Reviewer must not produce code patches. The capsule's `role` field is a hard constraint. If a model produces output outside its role boundary, that output is invalid and must be discarded.

---

## B. Model Assignment Strategy

### B1. Reference Mapping

The framework is model-agnostic. Any model capable of following structured instructions can fill any role. The following is a reference mapping, not a requirement:

| Role | Suggested Model | Rationale |
|---|---|---|
| Planner | Claude | Strong at requirements decomposition and structured writing |
| Architect | Claude | Strong at system design and document generation |
| Engineer | Codex / Antigravity | Optimized for code generation from specification |
| Reviewer | Claude / Codex | Strong at code analysis and structured feedback |
| QA | Claude | Strong at test plan execution and acceptance criteria verification |
| Auditor | Claude | Strong at compliance checking and traceability |

### B2. Model-Agnostic Rules

1. **No model identity leakage.** The capsule does not specify which model to use. It specifies the role. Model assignment is an orchestrator concern.
2. **Models are interchangeable within a role.** If Model A starts as Engineer and Model B continues as Engineer for the same slice, the result must be identical because both read the same `lld.md`.
3. **No model-specific instructions in slice docs.** PRD, HLD, and LLD must not contain prompts, temperature settings, or model-specific syntax.
4. **Capability requirements.** The orchestrator must verify the assigned model can: (a) parse YAML capsules, (b) read markdown documents, (c) produce output in the required format, (d) follow structured constraints.

### B3. Model Switching Mid-Slice

Switching models within a role during a slice is permitted because:
- All context lives in committed documents, not model memory.
- The capsule re-establishes full context on every invocation.
- `state.md` records what has been done.

The new model reads the same artifacts and produces output against the same spec. No context is lost.

---

## C. Invocation Flow Per State

### C1. NOT_STARTED → PRD_DEFINED

| Field | Value |
|---|---|
| **Role** | Planner |
| **Required Inputs** | `docs/vision/*`, `docs/slices/index.md` |
| **Allowed Documents** | Vision docs, existing slice PRDs (for context), `docs/architecture/*` |
| **Expected Output** | `docs/slices/SLICE-XXXX/prd.md` |
| **State Transition** | Planner sets state to `PRD_DEFINED` |

**Invocation steps:**
1. Orchestrator creates `docs/slices/SLICE-XXXX/` with `state.md` at `NOT_STARTED`.
2. Orchestrator registers slice in `index.md`.
3. Orchestrator invokes Planner with capsule.
4. Planner reads vision docs and produces `prd.md`.
5. Orchestrator commits `prd.md`, updates `state.md` to `PRD_DEFINED`, updates `index.md`.

### C2. PRD_DEFINED → HLD_DEFINED

| Field | Value |
|---|---|
| **Role** | Architect |
| **Required Inputs** | `prd.md` |
| **Allowed Documents** | `prd.md`, `docs/architecture/*` |
| **Expected Output** | `docs/slices/SLICE-XXXX/hld.md` |
| **State Transition** | Architect sets state to `HLD_DEFINED` |

### C3. HLD_DEFINED → LLD_DEFINED

| Field | Value |
|---|---|
| **Role** | Architect |
| **Required Inputs** | `prd.md`, `hld.md` |
| **Allowed Documents** | `prd.md`, `hld.md`, `docs/architecture/*` |
| **Expected Output** | `docs/slices/SLICE-XXXX/lld.md` |
| **State Transition** | Architect sets state to `LLD_DEFINED` |

**Doc Freeze activates.** PRD, HLD, and LLD are now immutable.

### C4. LLD_DEFINED → CODE_IN_PROGRESS → CODE_COMPLETE

| Field | Value |
|---|---|
| **Role** | Engineer |
| **Required Inputs** | `lld.md`, `hld.md` |
| **Allowed Documents** | `lld.md`, `hld.md`, `prd.md` (read-only context) |
| **Expected Output** | Source code + unit tests per LLD specification |
| **State Transition** | Engineer sets `CODE_IN_PROGRESS` then `CODE_COMPLETE` |

**Engineer invocation rules:**
- The Engineer may be invoked multiple times to complete the work.
- Each invocation receives a capsule scoped to a specific LLD section or file.
- The Engineer must not produce code for sections not in the capsule objective.
- On final invocation, Engineer requests transition to `CODE_COMPLETE`.

### C5. CODE_COMPLETE → REVIEW_REQUIRED → REVIEW_APPROVED / REVIEW_CHANGES

| Field | Value |
|---|---|
| **Role** | Reviewer |
| **Required Inputs** | `lld.md`, source code, `review.md` from prior cycles (if any) |
| **Allowed Documents** | `lld.md`, `hld.md`, source code, prior `review.md` |
| **Expected Output** | `docs/slices/SLICE-XXXX/review.md` |
| **State Transition** | Reviewer sets `REVIEW_APPROVED` or `REVIEW_CHANGES` |

**Review loop:**
```
CODE_COMPLETE → REVIEW_REQUIRED → Reviewer invoked
    → If APPROVED: state = REVIEW_APPROVED
    → If CHANGES_REQUESTED: state = REVIEW_CHANGES
        → Engineer invoked to fix → state = REVIEW_REQUIRED
        → Reviewer re-invoked → loop until APPROVED
```

### C6. REVIEW_APPROVED → QA_REQUIRED → QA_APPROVED / QA_CHANGES

| Field | Value |
|---|---|
| **Role** | QA |
| **Required Inputs** | `prd.md`, `lld.md`, `test-plan.md`, source code, test results |
| **Allowed Documents** | `prd.md`, `lld.md`, `test-plan.md`, source code |
| **Expected Output** | `docs/slices/SLICE-XXXX/qa.md` |
| **State Transition** | QA sets `QA_APPROVED` or `QA_CHANGES` |

**QA loop:**
```
REVIEW_APPROVED → QA_REQUIRED → QA invoked
    → If APPROVED: state = QA_APPROVED
    → If CHANGES_REQUESTED: state = QA_CHANGES
        → Engineer invoked to fix → state = QA_REQUIRED
        → QA re-invoked → loop until APPROVED
```

### C7. QA_APPROVED → AUDIT_REQUIRED → AUDIT_APPROVED

| Field | Value |
|---|---|
| **Role** | Auditor |
| **Required Inputs** | All slice artifacts, `state.md` history |
| **Allowed Documents** | Every file in `docs/slices/SLICE-XXXX/`, source code |
| **Expected Output** | `docs/slices/SLICE-XXXX/audit-report.md` |
| **State Transition** | Auditor sets `AUDIT_APPROVED` (or rejects) |

### C8. AUDIT_APPROVED → MERGED

This transition is performed by the orchestrator (human or CI), not by an AI model. The PR is merged to main.

---

## D. Resume Protocol

When a model session ends (context window exhausted, timeout, or explicit stop), the next invocation must be able to continue without loss.

### D1. Resume Steps

1. **Read `state.md`** to determine the current state and last transition.
2. **Read all artifacts** that exist in the slice folder.
3. **Receive a new context capsule** with `current_state` matching the persisted state.
4. **Read every file in `relevant_documents`** listed in the capsule.
5. **Verify consistency:**
   - Does the code on disk match the last committed state?
   - Are all artifacts present for the current state?
   - Is `index.md` in sync with `state.md`?
6. **If consistent:** Continue from the current state. Do not redo completed work.
7. **If inconsistent:** Halt and report the discrepancy.

### D2. Required Capsule Fields for Resume

All fields are required per `CONTEXT_CAPSULE.md`. For resume scenarios, the `objective` field should indicate continuation:

```yaml
objective: "Continue implementation of invoice PDF module from where previous session ended. Files already committed: pdf_generator.py, test_pdf_generator.py. Remaining: pdf_formatter.py per LLD section 3.2."
```

### D3. What to Re-Read on Resume

| Role | Must Re-Read |
|---|---|
| Planner | Vision docs, existing PRDs for other slices |
| Architect | `prd.md`, existing `hld.md` (if resuming LLD) |
| Engineer | `lld.md`, `hld.md`, all source code committed so far for this slice |
| Reviewer | `lld.md`, full source code, prior `review.md` |
| QA | `prd.md`, `lld.md`, `test-plan.md`, test results |
| Auditor | All slice artifacts, `state.md` history |

### D4. Anti-Patterns

- **Do not rely on model memory.** The previous session's context is gone. Everything must come from committed documents.
- **Do not re-read chat history.** Chat is not a source of truth. Only committed artifacts matter.
- **Do not assume partial work was committed.** If code was generated but not committed, it does not exist. The resume starts from the last committed state.

---

## E. Drift Prevention Rules

These rules exist to prevent models from deviating from the specification. They apply to every invocation regardless of role.

### E1. Absolute Prohibitions

| # | Rule | Consequence of Violation |
|---|---|---|
| 1 | No code without `LLD_DEFINED` state | Output discarded. State not advanced. |
| 2 | No design change without `change-request.md` | Change ignored. Frozen doc unchanged. |
| 3 | No skipping review | Slice cannot reach `QA_REQUIRED`. |
| 4 | No speculative improvements | Unauthorized code removed. Review flags it. |
| 5 | No scope expansion beyond capsule objective | Output truncated to capsule scope. |
| 6 | No modifying upstream artifacts | Modification reverted. Role violation logged. |
| 7 | No acting outside assigned role | Output discarded. Role violation logged. |
| 8 | No invocation without context capsule | Output discarded entirely. |

### E2. Drift Detection Checklist (For Orchestrator)

After every model invocation, verify:

- [ ] Output matches `expected_output.format` from the capsule.
- [ ] Output was saved to `expected_output.artifact` path.
- [ ] No files were modified outside the expected artifact path.
- [ ] No design documents were modified (if role is Engineer/Reviewer/QA).
- [ ] State transition (if any) is valid per `STATE_MACHINE.md`.
- [ ] Output does not contain work beyond the capsule `objective`.

### E3. Recovery from Drift

If drift is detected:

1. **Do not commit the drifted output.**
2. Identify which rule was violated (E1 table).
3. Re-invoke the model with a corrected capsule that explicitly constraints away from the drift.
4. Add a `constraints` entry to the capsule: `"Previous invocation produced [X]. Do not repeat. Stay within [Y]."`
5. If drift persists across 3 invocations, escalate to human review.
