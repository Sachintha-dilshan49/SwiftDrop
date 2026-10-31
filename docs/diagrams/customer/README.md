# Customer use case diagrams

Owner: Role 7 (Frontend and real-time UI) · Design PDF section: customer use case diagram

| File | Shows |
|---|---|
| `UC-00_customer_overview` | Every customer-facing use case on one page |
| `UC-01_customer_account` | Sign up, log in, Google login, sessions, log out |
| `UC-02_customer_booking` | Booking a delivery with the fee estimate |
| `UC-03_customer_tracking` | Delivery history, parcel details, live and public tracking, receipts, notifications |
| `UC-04_customer_ai_assistant` | AI assistant questions and guardrails |

Each diagram has a `.puml` source, a `.png` for the design PDF and a `.svg` for the docs site.
Use case descriptions are in `customer-use-cases.md`.

## Editing

Diagrams are written in PlantUML. All files share `_style.iuml` for the SwiftDrop colours.

```bash
# from docs/diagrams/customer
java -jar plantuml.jar -tpng *.puml
java -jar plantuml.jar -tsvg *.puml
```

Or install the PlantUML extension in VS Code and press Alt+D to preview.
