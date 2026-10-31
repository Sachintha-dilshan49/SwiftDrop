#!/usr/bin/env bash
# Builds SwiftDrop_Design_Report.docx and .pdf from SwiftDrop_Design_Report.md with pandoc.
# Needs pandoc and, for the PDF, a LaTeX engine (xelatex from MiKTeX). On Windows:
#   winget install --id JohnMacFarlane.Pandoc -e
#   winget install --id MiKTeX.MiKTeX -e
# Usage: bash docs/report/build.sh
set -euo pipefail
cd "$(dirname "$0")"

pandoc SwiftDrop_Design_Report.md -o SwiftDrop_Design_Report.docx \
  --resource-path=. --toc --toc-depth=1

pandoc SwiftDrop_Design_Report.md -o SwiftDrop_Design_Report.pdf \
  --resource-path=. --pdf-engine=xelatex \
  -V geometry:a4paper,margin=2cm -V mainfont="Arial" -V fontsize=10pt \
  -V colorlinks=true
