# BurnMate GitHub Execution Tooling

This folder adds a GitHub visibility layer on top of the existing `docs/slices/` workflow. Slice docs remain the source of truth. GitHub becomes the dashboard layer for issues, project state, and PR linkage.

## What It Manages

- Project V2: `BurnMate Execution`
- GitHub issues for each discovered `docs/slices/SLICE-*`
- Project fields for execution state and role owner
- Managed issue body block with links back to repo docs
- Stable prompt-facing scripts for status, owner, and PR updates

## Prerequisites

- `gh`, `jq`, and `python3` installed
- `gh auth status` must show `project`
- If scopes are missing, run:

```bash
gh auth refresh -s project
```

## Commands

Bootstrap GitHub labels and project structure:

```bash
./tools/github/bootstrap_github_execution.sh
```

Sync every discovered slice:

```bash
python3 tools/github/sync_slices_to_github.py --all
```

Sync one slice:

```bash
./tools/github/sync_single_slice.sh SLICE-0008
```

Set a slice to review:

```bash
./tools/github/set_slice_status.sh SLICE-0008 "Review" --owner Reviewer --comment "Implementation done. Ready for review."
```

Set a slice to QA:

```bash
./tools/github/set_slice_status.sh SLICE-0008 "QA" --owner QA --comment "Review approved. Ready for QA."
```

Set a slice to done after audit pass:

```bash
./tools/github/set_slice_status.sh SLICE-0008 "Done" --comment "Audit passed."
```

Set a slice live after merge:

```bash
./tools/github/set_slice_status.sh SLICE-0008 "Live" --comment "Merged to main."
```

Update role owner only:

```bash
./tools/github/set_role_owner.sh SLICE-0008 Reviewer
```

Attach a PR to a slice:

```bash
./tools/github/attach_pr_to_slice.sh SLICE-0008 --pr 8 --comment "PR linked for review."
```

## Behavior Notes

- Scripts do not rewrite `docs/slices/`; they mirror local docs into GitHub.
- GitHub `Execution State` is simplified to: `Todo`, `Planning`, `Dev`, `Review`, `QA`, `Audit`, `Done`, `Live`.
- Detailed file-based states remain unchanged in `docs/slices/`; the GitHub state is derived from local docs plus merged-PR detection for `Live`.
- `Todo`, `Done`, and `Live` issues are closed on sync. Active work states remain open.
- `Role Owner` mirrors the real repo roles: `Planner`, `Architect`, `Engineer`, `Reviewer`, `QA`, `Auditor`, `Unassigned`.
- `set_slice_status.sh` and `set_role_owner.sh` validate local docs first unless `--allow-drift` is passed.
- `sync_slices_to_github.py` runs the existing registry and state-machine validators by default unless `--no-validate` is passed.
- `--dry-run` is available on sync/status/owner/attach commands.

## Auto-Detected BurnMate Inputs

- Slice registry: `docs/slices/index.md`
- Locked roadmap order: `docs/slices/ROADMAP.md`
- Slice state: `docs/slices/SLICE-*/state.md`
- Slice artifacts: `state.md`, `contract.md`, `prd.md`, `hld.md`, `lld.md`, `review.md`, `qa.md`, `test-plan.md`, `audit-report.md`, `change-request.md`
- GitHub repo owner/name/default branch from `git remote` plus `gh repo view`
- Branch and PR linkage from `state.md` links plus `gh pr list`

## Managed GitHub Conventions

- Issue title: `SLICE-0008: Charts & Visual Progress`
- Machine-managed issue block markers:
  - `<!-- burnmate:slice-id:SLICE-0008 -->`
  - `<!-- burnmate:slice-sync:start -->`
  - `<!-- burnmate:slice-sync:end -->`
- Machine-managed PR/issue linkage markers:
  - `<!-- burnmate:pr-link:SLICE-0008 -->`
  - `<!-- burnmate:slice-ref:start -->`
  - `<!-- burnmate:slice-ref:end -->`

## Manual GitHub Step

GitHub CLI supports project creation, field creation, item linking, and field updates, but view layout/grouping is still manual here:
1. Open the `BurnMate Execution` project.
2. Create a table view named `Execution Table`.
3. Show columns: `Execution State`, `Role Owner`, `Priority`, `Effort`, `Sprint`, `Target Release`, `Epic`, `Blocked Reason`.
4. Create a board view named `Execution Board`.
5. Group the board by `Execution State`.
