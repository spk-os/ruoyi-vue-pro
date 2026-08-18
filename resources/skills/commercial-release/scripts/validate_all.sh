#!/usr/bin/env bash
set -euo pipefail

pack_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$pack_root/scripts/validate_skill_pack.py" "$pack_root"
python3 -m unittest discover -s "$pack_root/tests" -v
