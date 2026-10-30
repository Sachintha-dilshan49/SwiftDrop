# SwiftDrop Merchant Platform Specification & Documentation

**Role 5: Merchant Platform**  
**Service:** `services/merchant-service` (:8085)  
**Database:** `merchant_db` (PostgreSQL)  
**Owner:** Role 5 (Sudari)

---

## 1. Overview and Integration Boundaries

The SwiftDrop Merchant Platform enables business merchants to register, receive administrative approval, generate secure API keys, and place automated delivery orders directly from e-commerce backends (e.g. Shopify, WooCommerce, ERPs).

### Integration Boundaries
- **Role 1 (API Gateway & Common):**
  - Public routes under `/api/v1/merchants/**` and `/api/v1/admin/merchants/**` routed via gateway.
  - External order endpoint `POST /merchant-api/orders` routed with rate limiting by API key.
  - Standard error handling via RFC 7807 `ProblemDetail` and exceptions from `common`.
- **Role 2 (Auth Service):**
  - Admin operations secured via JWT token with `ROLE_ADMIN`.
  - Merchant dashboard and settings secured via JWT token with `ROLE_MERCHANT`.
- **Role 3 (Parcel Service):**
  - Synchronous `POST /internal/parcels` called by `merchant-service` to create parcel deliveries associated with `merchantId`.
  - Parcels are owned by parcel-service; merchant-service maintains local reference records (`merchant_orders`) for rapid dashboard querying and order history.
- **Role 6 (Notification Service):**
  - Internal endpoint `GET /internal/merchants/{merchantId}/webhook` exposed by `merchant-service` to supply `webhookUrl` and HMAC signing `secret` to Role 6's webhook consumer.
- **Role 7 (Frontend):**
  - React components created under `frontend/src/features/merchant/` and client under `frontend/src/api/merchantApi.ts`.

---

## 2. Security Architecture

1. **API Keys:**
   - Keys follow the prefix format `sd_live_<random_base64url>` (minimum 32 bytes cryptographically secure entropy via `SecureRandom`).
   - The raw key is returned **strictly once** upon generation.
   - Keys are stored exclusively as **SHA-256 hex hashes** (`varchar(64)`); raw keys and hashes are never exposed through API responses or logged.
   - Validation uses constant-time hash comparison (`MessageDigest.isEqual`) to protect against timing attacks.
   - Missing, invalid, expired, or revoked keys return **401 Unauthorized**.
2. **Merchant Approval Gate:**
   - Only merchants with status `APPROVED` can generate API keys or create orders.
   - Public registrations default strictly to `PENDING`.
3. **Data Isolation:**
   - Server-side tenant isolation: logged-in merchants can only list, view, and revoke their own API keys, orders, and webhook settings. Authenticated identity is resolved from the JWT subject, never trusting a client-supplied merchant ID.

---

## 3. API Surface Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/merchants/register` | Public / JWT | Submit merchant registration (status starts at `PENDING`) |
| `GET` | `/api/v1/merchants/me` | Bearer (JWT) | View logged-in merchant profile and approval status |
| `GET` | `/api/v1/admin/merchants` | Bearer (`ADMIN`) | List merchant registrations (filter by `status=PENDING`) |
| `PATCH` | `/api/v1/admin/merchants/{id}/approve` | Bearer (`ADMIN`) | Approve a merchant registration |
| `PATCH` | `/api/v1/admin/merchants/{id}/reject` | Bearer (`ADMIN`) | Reject a merchant registration with a reason |
| `POST` | `/api/v1/merchants/me/api-keys` | Bearer (`MERCHANT`) | Create an API key (returns raw key once) |
| `GET` | `/api/v1/merchants/me/api-keys` | Bearer (`MERCHANT`) | List API keys showing safe prefix and metadata |
| `DELETE` | `/api/v1/merchants/me/api-keys/{id}` | Bearer (`MERCHANT`) | Revoke an API key |
| `POST` | `/merchant-api/orders` | `X-API-Key` | Create an automated parcel delivery order |
| `GET` | `/api/v1/merchants/me/orders` | Bearer (`MERCHANT`) | Paginated dashboard listing of merchant orders |
| `GET` | `/api/v1/merchants/me/orders/{id}` | Bearer (`MERCHANT`) | Get specific merchant order details |
| `GET` | `/api/v1/merchants/me/webhook` | Bearer (`MERCHANT`) | View webhook settings (secret hidden) |
| `PUT` | `/api/v1/merchants/me/webhook` | Bearer (`MERCHANT`) | Configure webhook URL & optionally rotate secret |
| `GET` | `/internal/merchants/{merchantId}/webhook` | Internal | Endpoint for Role 6 to obtain webhook URL & HMAC secret |

---

## 4. Request & Response Examples

### A. Register Merchant
**Request:**
`POST /api/v1/merchants/register`
```json
{
  "businessName": "Apex Technologies (Pvt) Ltd",
  "businessRegNo": "PV-998877",
  "contactName": "Nimal Jayasinghe",
  "email": "orders@apextech.lk",
  "phone": "+94771234567",
  "address": "No. 45, Galle Road, Colombo 03"
}
```

**Response (201 Created):**
```json
{
  "id": "c1f7b92a-3e1b-4f8e-a23d-8812c345a901",
  "userId": null,
  "businessName": "Apex Technologies (Pvt) Ltd",
  "businessRegNo": "PV-998877",
  "contactName": "Nimal Jayasinghe",
  "email": "orders@apextech.lk",
  "phone": "+94771234567",
  "address": "No. 45, Galle Road, Colombo 03",
  "status": "PENDING",
  "rejectionReason": null,
  "createdAt": "2026-10-27T14:30:00Z",
  "updatedAt": null
}
```

### B. Create API Key
**Request:**
`POST /api/v1/merchants/me/api-keys`
```json
{
  "name": "Production Store Integration"
}
```

**Response (201 Created):**
```json
{
  "id": "e4a2c1f0-7b3d-4c8e-a991-654321fedcba",
  "name": "Production Store Integration",
  "keyPrefix": "sd_live_Ab9Z8yXw",
  "rawApiKey": "sd_live_Ab9Z8yXw1234567890abcdef1234567890abcdef",
  "expiresAt": null,
  "createdAt": "2026-10-27T14:35:00Z"
}
```

### C. Create Parcel Order via External API
**Request:**
`POST /merchant-api/orders`  
`X-API-Key: sd_live_Ab9Z8yXw1234567890abcdef1234567890abcdef`  
`Content-Type: application/json`
```json
{
  "recipientName": "Kasun Perera",
  "recipientPhone": "+94719876543",
  "pickupAddress": "No. 45, Galle Road, Colombo 03",
  "dropoffAddress": "No. 88, Kandy Road, Kiribathgoda",
  "weightKg": 2.50,
  "size": "MEDIUM",
  "idempotencyKey": "shopify-order-10029"
}
```

**Response (201 Created):**
```json
{
  "id": "b3e21a88-2931-41fb-992a-384112e87901",
  "parcelId": "5a77f980-87a2-4a0b-9321-df6210e7b892",
  "trackingNo": "SD-449102",
  "recipientName": "Kasun Perera",
  "recipientPhone": "+94719876543",
  "dropoffAddress": "No. 88, Kandy Road, Kiribathgoda",
  "status": "CREATED",
  "feeAmount": 450.00,
  "feeCurrency": "LKR",
  "createdAt": "2026-10-27T14:40:00Z"
}
```

### D. Internal Webhook Lookup (Role 6 Contract)
**Request:**
`GET /internal/merchants/c1f7b92a-3e1b-4f8e-a23d-8812c345a901/webhook`

**Response (200 OK):**
```json
{
  "webhookUrl": "https://apextech.lk/api/swiftdrop/webhook",
  "secret": "whsec_7x8K9pLmN2vQwRtYuIzA1bCdEfGhJkL0"
}
```

---

## 5. cURL Examples

```bash
# 1. Register Merchant
curl -X POST http://localhost:8085/api/v1/merchants/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Apex Technologies",
    "businessRegNo": "PV-12345",
    "contactName": "Nimal Perera",
    "email": "orders@apex.lk",
    "phone": "+94771234567",
    "address": "123 Galle Rd, Colombo"
  }'

# 2. Admin Approves Merchant
curl -X PATCH http://localhost:8085/api/v1/admin/merchants/<MERCHANT_ID>/approve \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"

# 3. Create Merchant API Key
curl -X POST http://localhost:8085/api/v1/merchants/me/api-keys \
  -H "Authorization: Bearer <MERCHANT_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Production Store"}'

# 4. Create Order using X-API-Key
curl -X POST http://localhost:8085/merchant-api/orders \
  -H "X-API-Key: sd_live_your_actual_key_here" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientName": "Kasun Perera",
    "recipientPhone": "+94719876543",
    "pickupAddress": "No. 45 Galle Rd, Colombo",
    "dropoffAddress": "No. 88 Kandy Rd, Kiribathgoda",
    "weightKg": 2.50,
    "size": "MEDIUM",
    "idempotencyKey": "order-101"
  }'
```

---

## 6. Postman Collection Export

A complete Postman Collection is provided in `docs/postman/SwiftDrop_Merchant_API.postman_collection.json`.
