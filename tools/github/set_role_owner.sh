#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $0 SLICE-0008 Reviewer [options]" >&2
    exit 1
fi

SLICE_ID="$1"
OWNER="$2"
shift 2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 "$SCRIPT_DIR/github_execution.py" set-owner "$SLICE_ID" "$OWNER" "$@"
