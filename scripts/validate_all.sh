#!/usr/bin/env bash
# validate_all.sh — Run all framework validators
#
# Runs each validator in sequence. Exits non-zero on first failure.
# Designed for both local development and CI.
#
# Usage:
#   ./scripts/validate_all.sh          # run all
#   ./scripts/validate_all.sh --help   # show this help
#
# Exit codes:
#   0 = all validators passed
#   1 = one or more validators failed
#   2 = script error (missing python, etc.)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors (disabled if not a terminal)
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    BOLD='\033[1m'
    NC='\033[0m'
else
    GREEN=''
    RED=''
    BOLD=''
    NC=''
fi

if [[ "${1:-}" == "--help" ]]; then
    echo "Usage: ./scripts/validate_all.sh"
    echo ""
    echo "Runs all framework validators in sequence."
    echo "Exits non-zero on first failure."
    echo ""
    echo "Validators:"
    echo "  1. validate_slice_registry.py    — index.md ↔ disk consistency"
    echo "  2. validate_required_artifacts.py — artifacts exist per state"
    echo "  3. validate_state_machine_transitions.py — state history follows allowed transitions"
    echo "  4. validate_doc_freeze.py        — frozen docs not modified"
    echo "  5. validate_pr_checklist.py      — PR template structure"
    exit 0
fi

# Check python
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}ERROR: python3 not found${NC}" >&2
    exit 2
fi

VALIDATORS=(
    "validate_slice_registry.py"
    "validate_required_artifacts.py"
    "validate_state_machine_transitions.py"
    "validate_doc_freeze.py"
    "validate_pr_checklist.py"
)

echo ""
echo -e "${BOLD}================================================================${NC}"
echo -e "${BOLD}  AI Dev Framework — Validation Suite${NC}"
echo -e "${BOLD}================================================================${NC}"
echo ""

PASSED=0
FAILED=0

for validator in "${VALIDATORS[@]}"; do
    echo -e "${BOLD}Running: ${validator}${NC}"
    echo "----------------------------------------------------------------"

    if python3 "${SCRIPT_DIR}/${validator}"; then
        echo -e "${GREEN}✓ ${validator} PASSED${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}✗ ${validator} FAILED${NC}"
        FAILED=$((FAILED + 1))
        echo ""
        echo -e "${RED}Stopping on first failure.${NC}"
        echo ""
        echo "================================================================"
        echo -e "  Results: ${GREEN}${PASSED} passed${NC}, ${RED}${FAILED} failed${NC}"
        echo "================================================================"
        exit 1
    fi
    echo ""
done

echo "================================================================"
echo -e "  ${GREEN}All ${PASSED} validators passed.${NC}"
echo "================================================================"
exit 0
