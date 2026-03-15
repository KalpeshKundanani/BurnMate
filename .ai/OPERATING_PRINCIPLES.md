# Operating Principles

These are non-negotiable rules that govern all work within this framework. Every agent, automation, and human participant must follow them. Violations invalidate the affected slice.

---

## 1. Documents Are the Source of Truth

The committed markdown files in the repository are the canonical record of all decisions, designs, and outcomes. If it is not in a document, it did not happen. Verbal agreements, chat logs, or memory of previous sessions have zero authority.

## 2. No Code Before LLD

No implementation work may begin until the Low-Level Design document is written, committed, and the slice state is `LLD_DEFINED`. Writing code based on a vague idea, a PRD alone, or an HLD alone is prohibited. The LLD is the contract the Engineer implements against.

## 3. No Skipping States

The state machine defines the only valid path through the slice lifecycle. Every state must be entered and exited through a valid transition. There are no fast tracks, no shortcuts, and no exceptions. If a state feels unnecessary for a particular slice, the slice is too small or the framework is being misused.

## 4. Small Slices Only

A slice must be completable in a single focused iteration. If a slice requires more than one HLD, it is too large. If a slice touches more than one bounded context, it is too large. Break it down further. Large slices are where quality goes to die.

## 5. Role Isolation Is Absolute

Each role has defined responsibilities and boundaries. No role may perform the work of another role within the same slice. The Engineer does not review their own code. The Architect does not write implementation. The Auditor does not fix defects. Separation of duties is not a suggestion.

## 6. Every Invocation Requires a Context Capsule

Every AI model invocation must include a complete Context Capsule as defined in `CONTEXT_CAPSULE.md`. An invocation without a capsule is uncontrolled and its output is invalid. The capsule prevents scope drift, role confusion, and state corruption.

## 7. Resume-From-State Capability

Any agent must be able to pick up a slice at any valid state and continue work. This means all context required to act is captured in the slice's documents and state file. No implicit knowledge. No "you had to be there." The state file plus the artifact directory must be sufficient.

## 8. Deterministic Transitions Require Evidence

Moving from one state to the next requires the output artifact of the current state to exist and be committed. `NOT_STARTED -> PRD_DEFINED` requires `prd.md` to be present. `PRD_DEFINED -> HLD_DEFINED` requires `hld.md`. `CODE_COMPLETE -> REVIEW_REQUIRED` requires code to be committed. No artifact, no transition.

## 9. Audit Is Mandatory Before Merge

No slice may be merged without passing the Auditor's compliance check. The Auditor verifies that all artifacts exist, all transitions were valid, and role separation was maintained. This is the final gate. There is no bypass.

## 10. Outputs Are Committed, Not Ephemeral

Every artifact produced by any role must be committed to the repository in the correct location. Design documents, review reports, QA results, and audit reports all live in `docs/slices/<slice-id>/`. If the output is not committed, it does not exist.

## 11. No Gold-Plating

Agents must produce exactly what the current state and capsule require. No extra features. No speculative improvements. No "while I'm here" additions. Scope is defined by the LLD and constrained by the capsule. Anything beyond that is unauthorized work.

## 12. Failures Are Recorded, Not Hidden

If a review requests changes, the feedback is documented. If QA finds defects, they are listed. If an audit rejects a slice, the reasons are recorded. The framework's value comes from its traceability. Hiding failures defeats the purpose.

## 13. Doc Freeze Policy

Once a slice enters `LLD_DEFINED`, the PRD (`prd.md`), HLD (`hld.md`), and LLD (`lld.md`) are **frozen**. No role may edit a frozen document directly. Any modification requires:

1. A `change-request.md` to be filed in the slice folder (see `docs/slices/_templates/change-request.md`).
2. The change request must identify which documents are impacted and the earliest state that must be rolled back to.
3. Approval from the role that owns the impacted state (Planner for PRD, Architect for HLD/LLD).
4. Upon approval, the slice state rolls back to the earliest impacted state.
5. All downstream artifacts produced after the rollback point are invalidated and must be re-produced through the normal state machine flow.

There are no partial rollbacks. There are no informal edits.
