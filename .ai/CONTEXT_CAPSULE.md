# Context Capsule Specification

A Context Capsule is a structured block of metadata that **must** be included at the start of every AI model invocation within this framework. It anchors the model to the current task, prevents hallucination of scope, and enables deterministic resumability.

## Template

```yaml
context_capsule:
  project: "<project-name>"
  version: "<framework-version>"
  slice_id: "<slice-identifier>"
  current_state: "<current state from STATE_MACHINE.md>"
  role: "<role from ROLES.md>"
  relevant_documents:
    - "<path/to/document-1>"
    - "<path/to/document-2>"
  objective: "<one-sentence description of what this invocation must accomplish>"
  constraints:
    - "<constraint-1>"
    - "<constraint-2>"
  expected_output:
    format: "<markdown | code | yaml | json>"
    artifact: "<path where the output will be saved>"
```

## Field Definitions

| Field | Required | Description |
|---|---|---|
| `project` | Yes | Name of the project this slice belongs to. |
| `version` | Yes | Version of the AI Dev Framework being used. |
| `slice_id` | Yes | Unique identifier for the slice (e.g., `SLICE-0042`). |
| `current_state` | Yes | The slice's current state as defined in `STATE_MACHINE.md`. Must be an exact match. |
| `role` | Yes | The role the model is operating as for this invocation. Must match `ROLES.md`. |
| `relevant_documents` | Yes | List of file paths the model should read before acting. Minimum one document. |
| `objective` | Yes | A single sentence describing the goal of this invocation. No ambiguity. |
| `constraints` | Yes | Boundaries the model must not cross. At minimum: role boundaries and state boundaries. |
| `expected_output.format` | Yes | The format the output must be in. |
| `expected_output.artifact` | Yes | The file path where the output will be committed. |

## Example

```yaml
context_capsule:
  project: "invoice-service"
  version: "1.0.0"
  slice_id: "SLICE-0012"
  current_state: "LLD_DEFINED"
  role: "Engineer"
  relevant_documents:
    - "docs/slices/SLICE-0012/lld.md"
    - "docs/slices/SLICE-0012/hld.md"
  objective: "Implement the invoice PDF generation module as specified in the LLD."
  constraints:
    - "Do not modify any design documents."
    - "Do not add features not specified in lld.md."
    - "Do not transition state beyond CODE_COMPLETE."
  expected_output:
    format: "code"
    artifact: "src/invoices/pdf_generator.py"
```

## Rules

1. **Every invocation must include a capsule.** No exceptions. An invocation without a context capsule is an uncontrolled invocation and its output must be discarded.
2. **The capsule is the single source of scope.** The model must not act outside the objective, constraints, and expected output defined in the capsule.
3. **State must be verified before acting.** The model must confirm that `current_state` matches the actual persisted state of the slice. If there is a mismatch, the model must halt and report the discrepancy.
4. **Role must be verified before acting.** The model must only perform actions allowed by the role specified in the capsule, as defined in `ROLES.md`.
5. **Relevant documents must be read.** The model must read all documents listed in `relevant_documents` before producing output.
6. **Output must match the expected format and artifact path.** The model produces exactly what the capsule specifies, nothing more.
7. **Capsules are not modified mid-invocation.** If the scope needs to change, a new invocation with a new capsule is required.

## Why Context Capsules Prevent Drift

Without a capsule, a model will:
- Invent scope beyond the task
- Ignore upstream design decisions
- Skip reading relevant documents
- Produce output in the wrong format or location
- Lose track of which state the slice is in
- Conflate responsibilities across roles

The capsule eliminates these failure modes by constraining every invocation to a well-defined box. The model knows what it is, what state it's in, what it must read, what it must do, and what it must produce. Nothing else.
