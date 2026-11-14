# Merchant Service (`services/merchant-service`)

The **Merchant Platform** (Port `8085`, `merchant_db`) enables e-commerce businesses to register, manage store credentials, and automate parcel delivery workflows across SwiftDrop.

### Key Capabilities
- **Onboarding & Approval**: Business registration requiring user authentication; stores linked `user_id`; admin approval triggers role-granting (`MERCHANT`) via `auth-service`.
- **API Key Management**: Generates prefixed API keys (`sd_live_...`) with cryptographic entropy; securely stores SHA-256 hashes with constant-time verification.
- **Order Placement**: External order API (`/api/v1/merchant-api/orders`) secured via API keys; supports idempotency and forwards parcel creation to `parcel-service`.
- **Webhooks & Tracking**: Configurable webhook endpoints with HMAC signing secrets queried internally by `notification-service` for delivery events.
- **Frontend Portal**: Dedicated merchant dashboard, order tracking, and admin merchant management views.

### Tech Stack & Integration
- **Runtime**: Java 21, Spring Boot 4.1.x, Spring Security (OAuth2 Resource Server), Spring Data JPA, Flyway migrations (`merchant_db`).
- **Integration**: Communicates with `auth-service` (role assignments), `parcel-service` (parcel creation), and `notification-service` (webhook lookup).