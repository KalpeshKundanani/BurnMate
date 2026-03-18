#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 SLICE-0008 [extra sync args]" >&2
    exit 1
fi

SLICE_ID="$1"
shift

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 "$SCRIPT_DIR/github_execution.py" sync --slice "$SLICE_ID" "$@"
