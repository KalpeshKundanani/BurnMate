# Scripts — Framework Validators

Deterministic validation scripts that enforce framework governance. Python 3.12+ stdlib only.

## Scripts

| Script | Purpose |
|---|---|
| `validate_slice_registry.py` | Verify `index.md` ↔ disk consistency, role vocabulary, folder links |
| `validate_required_artifacts.py` | Verify required artifacts exist per slice state |
| `validate_doc_freeze.py` | Verify frozen docs (PRD/HLD/LLD) not modified after `LLD_DEFINED` |
| `validate_pr_checklist.py` | Verify PR template structure and required sections |
| `validate_all.sh` | Run all validators in sequence (exits on first failure) |

## Usage

```bash
# Run all validators
./scripts/validate_all.sh

# Run individual validator
python3 scripts/validate_slice_registry.py

# Run tests
python3 -m unittest scripts.tests.test_validators -v

# CI mode for PR body validation
PR_BODY="..." python3 scripts/validate_pr_checklist.py
```

## Rules

- Scripts are read-only. They never modify state, artifacts, or docs.
- Exit 0 = pass, 1 = violations found, 2 = configuration error.
- CI integration via `.github/workflows/validators.yml`.
- Full documentation: `docs/architecture/PHASE4_VALIDATORS.md`.
