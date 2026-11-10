# SwiftDrop – Customer use case descriptions

## Actors

| Actor | Type | Description |
|---|---|---|
| Visitor | Primary | Anyone using SwiftDrop without logging in. Can sign up, log in and track a parcel by tracking number. |
| Customer | Primary | A logged-in user with the CUSTOMER role. Books and follows their own deliveries. |
| Google (OAuth 2.0) | External system | Confirms the user's identity for "Log in with Google". |
| Email / SMS gateway | External system | Delivers status notifications (Mailpit in development, mock SMS). |
| LLM provider | External system | Generates the AI assistant's replies. Configured through environment variables. |

## Use cases

| ID | Use case | Actor | Description | Related |
|---|---|---|---|---|
| UC-C01 | Sign up | Visitor | Create an account with name, email and password. Input is validated and the password is hashed with BCrypt. The new user gets the CUSTOMER role. | Includes: validate input, issue tokens |
| UC-C02 | Log in | Visitor | Log in with email and password. Returns a 15-minute access token (RS256) and a 7-day refresh token. | Includes: issue tokens |
| UC-C03 | Log in with Google | Visitor, Google | OAuth 2.0 Authorization Code flow. First-time Google users are created as CUSTOMER. | Extends UC-C02 |
| UC-C04 | Log out | Customer | Ends the session and revokes the refresh token. | Includes: revoke refresh token |
| UC-C05 | Book a delivery | Customer | Enter pickup and drop-off addresses, package size and weight, see the fee, and confirm. The parcel is created with status CREATED and a tracking number such as SD-7K3Q-9P2M. | Includes UC-C06, UC-C07, UC-C08, UC-C09 |
| UC-C06 | Enter pickup and drop-off address | Customer | Addresses are required and validated before the fee is shown. | Included in UC-C05 |
| UC-C07 | Enter package size and weight | Customer | Size and weight are required and must be within the allowed limits. | Included in UC-C05 |
| UC-C08 | Get fee estimate | Customer | The pricing service returns the fee from the zone, base fare and per-km rate. Shown before confirming. | Included in UC-C05 |
| UC-C09 | Confirm booking | Customer | Creates the parcel, returns the tracking number and publishes a ParcelStatusChanged event. | Included in UC-C05 |
| UC-C10 | View my deliveries | Customer | List of the customer's own parcels with current status. | – |
| UC-C11 | View parcel details | Customer | Details of one parcel with its status history (who, when, note, proof). Other customers' parcels return 403/404. | Includes status history |
| UC-C12 | Track delivery live | Customer | The parcel page updates in real time through Server-Sent Events while the parcel is not yet delivered. | Extends UC-C11 |
| UC-C13 | Download receipt (PDF) | Customer | Available once the parcel is DELIVERED. | Extends UC-C11 |
| UC-C14 | Track parcel by tracking number | Visitor | Public, phone-first page. Shows status and progress only, with no personal data. | Includes UC-C12 |
| UC-C15 | Receive status notifications | Customer, Email/SMS gateway | An email and a mock SMS are sent on each status change: CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED or FAILED. | – |
| UC-C16 | Chat with AI assistant | Customer, LLM provider | Ask questions in plain language. The assistant's tools call the API gateway with the customer's own token. | Extended by UC-C17 – UC-C20 |
| UC-C17 | Ask where my parcel is | Customer | "Where is my parcel?" uses the `track_parcel` tool. | Extends UC-C16, includes UC-C11 |
| UC-C18 | Ask for a fee estimate | Customer | "How much to send 2 kg to Kandy?" uses the `estimate_fee` tool. | Extends UC-C16, includes UC-C08 |
| UC-C19 | List my parcels | Customer | Uses the `my_parcels` tool. | Extends UC-C16, includes UC-C10 |
| UC-C20 | Refuse unrelated or unauthorised questions | Customer | The assistant declines off-topic questions and never shows another user's data. | Extends UC-C16 |

## Main flows

### UC-C05 Book a delivery
**Precondition:** the customer is logged in.

1. The customer opens Book delivery.
2. The customer enters the pickup and drop-off addresses.
3. The customer enters the package size and weight.
4. The system shows the fee estimate from the pricing service.
5. The customer confirms.
6. The system creates the parcel with status CREATED and shows the tracking number.


**Alternative flow:** at step 2 or 3, invalid input shows an error next to the field and the customer cannot continue.

**Postcondition:** the parcel appears in My deliveries and a notification is sent.

### UC-C11 / UC-C12 View parcel details and track live
**Precondition:** the customer is logged in and owns the parcel.

1. The customer opens a parcel from My deliveries.
2. The system shows the details and status history.
3. While the parcel is not delivered, the page receives live status updates.
4. When the status is DELIVERED, the receipt download becomes available.


**Alternative flow:** if the parcel belongs to another customer, the system returns 403/404.

### UC-C14 Track parcel by tracking number

1. The visitor opens the public tracking page and enters a tracking number.
2. The system shows the current status and progress, with no personal data.
3. The page updates live as the status changes.


**Alternative flow:** an unknown tracking number shows "No parcel found".

### UC-C16 Chat with AI assistant
**Precondition:** the customer is logged in.

1. The customer types a question.
2. The assistant picks a tool and calls the gateway with the customer's token.
3. The assistant replies with the result, such as the status or a fee.


**Alternative flow:** off-topic questions or requests for another user's data are refused.
