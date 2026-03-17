# Slice Index

Registry of all development slices and their current states.

| Slice ID | Name | Current State | Owner Role | Slice Folder | Last Updated |
|---|---|---|---|---|---|
| SLICE-0001 | <!-- Short descriptive name --> | NOT_STARTED | Planner | docs/slices/SLICE-0001 | 2026-03-16 |
| SLICE-0002 | Calorie Debt Engine | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0002 | 2026-03-16 |
| SLICE-0003 | User Profile + Goal Domain | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0003 | 2026-03-16 |
| SLICE-0004 | Daily Logging Domain + Persistence | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0004 | 2026-03-16 |
| SLICE-0005 | Weight History + Debt Recalculation | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0005 | 2026-03-16 |
| SLICE-0006 | Dashboard Read Model | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0006 | 2026-03-16 |
| SLICE-0007 | Core UI | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0007 | 2026-03-17 |
| SLICE-0008 | Charts & Visual Progress | AUDIT_APPROVED | Auditor | docs/slices/SLICE-0008 | 2026-03-17 |

## Rules

1. **Registration is mandatory.** Every slice must be registered here before any work begins. A slice that is not in this index does not exist.
2. **State sync.** The `Current State` column must match the slice's `state.md` file exactly. If they diverge, the slice is in an invalid state.
3. **Naming convention.** Slice IDs use the format `SLICE-NNNN` (zero-padded, sequential). No gaps, no renaming.
4. **Owner Role values.** Must be one of: `Planner`, `Architect`, `Engineer`, `Reviewer`, `QA`, `Auditor`. Reflects the role currently responsible for the slice.
5. **Slice Folder link.** Must point to `docs/slices/SLICE-NNNN/`. The folder must contain at minimum `state.md`.
6. **Update on every transition.** When a slice changes state, this index must be updated in the same commit.
