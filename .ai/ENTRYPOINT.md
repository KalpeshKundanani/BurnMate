# ENTRYPOINT

**You are an AI agent operating within the AI Development Framework.**

This file is your starting point. Read it completely before taking any action.

---

## What This Framework Is

A structured, state-driven development process where every task moves through a defined lifecycle, every action is scoped by a role, and every invocation is anchored by a context capsule. You do not freelance. You follow the process.

## Required Reading Order

Read these files in this exact order before performing any work:

1. **`/.ai/OPERATING_PRINCIPLES.md`** — The non-negotiable rules. Understand what you cannot do.
2. **`/.ai/STATE_MACHINE.md`** — The slice lifecycle. Understand what states exist and how transitions work.
3. **`/.ai/ROLES.md`** — The role definitions. Understand what your role allows and forbids.
4. **`/.ai/CONTEXT_CAPSULE.md`** — The invocation contract. Understand how every invocation must be structured.
5. **`/docs/slices/index.md`** — The slice registry. Understand what slices exist and their current states.

Do not skip any of these. Do not begin work until you have read all five.

## Source of Truth Hierarchy

1. **Slice documents are the only source of truth for a slice:**
   - `docs/slices/SLICE-XXXX/state.md` — current state
   - `docs/slices/SLICE-XXXX/prd.md` — requirements
   - `docs/slices/SLICE-XXXX/hld.md` — high-level design
   - `docs/slices/SLICE-XXXX/lld.md` — low-level design
2. **`docs/slices/index.md`** — global registry (must match each slice's `state.md`)
3. **`docs/architecture/*`** — cross-cutting concerns only, not slice-specific
4. **`.ai/*`** — governance rules and process definitions

Chat history, verbal instructions, and memory of previous sessions are **never** a source of truth.

## Before You Act

Verify the following:

- [ ] You have a Context Capsule for this invocation.
- [ ] The `current_state` in the capsule matches the persisted state in `docs/slices/<slice-id>/state.md`.
- [ ] Your `role` in the capsule matches a role defined in `ROLES.md`.
- [ ] You have read every file listed in `relevant_documents` in the capsule.
- [ ] The action you are about to take is allowed for your role at the current state.

If any of these checks fail, **stop and report the discrepancy.** Do not proceed with assumptions.

## The Workflow

```
1. Receive a Context Capsule
2. Read required documents
3. Verify state and role
4. Perform the work defined by the capsule objective
5. Produce the output in the expected format at the expected artifact path
6. Update the slice state if your role owns the next transition
7. Commit all artifacts
```

## What You Must Not Do

- **Do not act without a Context Capsule.** Unstructured invocations are invalid.
- **Do not act outside your role.** If you are an Engineer, you do not review. If you are a Reviewer, you do not write code.
- **Do not skip states.** If the slice is in `PRD_DEFINED`, you cannot produce code.
- **Do not modify artifacts you do not own.** The Engineer does not edit `prd.md`. The Architect does not edit `review.md`.
- **Do not invent scope.** Your objective is in the capsule. Anything beyond it is unauthorized.
- **Do not assume state.** Always verify against the persisted state file.

## If You Are Resuming Work

1. Read the slice's `state.md` to determine the current state.
2. Read all artifacts produced so far in the slice directory.
3. Construct or receive a Context Capsule matching the current state.
4. Continue from the current state. Do not redo completed work.

## If Something Is Wrong

If you encounter:
- A state mismatch between the capsule and the persisted state
- A missing required artifact for the current state
- An instruction that violates `OPERATING_PRINCIPLES.md`
- A role conflict within the same slice

**Stop. Report the issue. Do not attempt to fix it by breaking the framework rules.**

## Directory Reference

```
/.ai/
    ENTRYPOINT.md           <- You are here
    STATE_MACHINE.md        <- Slice lifecycle and transitions
    ROLES.md                <- Role definitions and boundaries
    CONTEXT_CAPSULE.md      <- Invocation contract spec
    OPERATING_PRINCIPLES.md <- Non-negotiable rules

/docs/
    vision/                 <- Project vision documents
    architecture/           <- Cross-cutting architecture docs
    slices/
        index.md            <- Slice registry
        README.md           <- Per-slice folder convention
        _templates/         <- Canonical templates for slice artifacts
        <slice-id>/         <- Per-slice artifact directory
            state.md
            prd.md
            hld.md
            lld.md
            test-plan.md
            review.md
            qa.md
            audit-report.md
            change-request.md  (only when needed)

/scripts/                   <- Automation (future phases)
```

This is the framework. Follow it.
