#!/usr/bin/env bash
# Renders every docs/diagrams/<area>/*.mmd to a PNG next to it (white background, scale 2).
# Usage: bash docs/diagrams/render.sh [file.mmd ...]   (no arguments = all areas)
set -euo pipefail
cd "$(dirname "$0")"

MMDC="npx -y @mermaid-js/mermaid-cli@11.4.2"
files=("$@")
if [ ${#files[@]} -eq 0 ]; then
  mapfile -t files < <(ls */*.mmd)
fi

for f in "${files[@]}"; do
  echo "render $f"
  $MMDC -q -i "$f" -o "${f%.mmd}.png" -c mermaid-config.json -b white -s 2
done
