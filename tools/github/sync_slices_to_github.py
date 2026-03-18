#!/usr/bin/env python3
"""Entry point for syncing BurnMate slices into GitHub."""

from __future__ import annotations

import sys

from github_execution import main


if __name__ == "__main__":
    raise SystemExit(main(["sync", *sys.argv[1:]]))
