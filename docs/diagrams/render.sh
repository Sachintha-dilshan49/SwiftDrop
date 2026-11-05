#!/usr/bin/env bash
# Renders the diagrams in docs/diagrams to PNG next to their sources.
#   Mermaid (.mmd in the area folders): mermaid-cli, mermaid-config.json, white background, scale 2.
#   PlantUML use case diagrams (usecase/*.puml and the customer UC-*.puml): PlantUML jar via Java,
#   Smetana layout so Graphviz is not needed.
# Usage: bash docs/diagrams/render.sh [file.mmd|file.puml ...]   (no arguments = everything)
# PlantUML jar (git-ignored), once:
#   mkdir -p tools/plantuml && curl -L -o tools/plantuml/plantuml-1.2026.9.jar \
#     https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/1.2026.9/plantuml-1.2026.9.jar
set -euo pipefail
files=()
for f in "$@"; do files+=("$(realpath "$f")"); done
cd "$(dirname "$0")"

MMDC="npx -y @mermaid-js/mermaid-cli@11.4.2"
PLANTUML_JAR="$(realpath ../../tools/plantuml/plantuml-1.2026.9.jar)"
if [ ${#files[@]} -eq 0 ]; then
  mapfile -t files < <(ls */*.mmd usecase/uc-*.puml UC-00_customer_overview.puml UC-01_customer_account.puml UC-04_customer_ai_assistant.puml)
fi

for f in "${files[@]}"; do
  echo "render $f"
  case "$f" in
    *.mmd)  $MMDC -q -i "$f" -o "${f%.mmd}.png" -c mermaid-config.json -b white -s 2 ;;
    *.puml) java -jar "$PLANTUML_JAR" -tpng "$f" ;;
  esac
done
