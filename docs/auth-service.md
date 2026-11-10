# Auth service: initial login API

The initial auth-service implementation provides local signup/login, RS256 access tokens,
JWKS publication, hashed rotating refresh tokens, and Google OAuth login. Administrative user
management is not implemented yet.

## Local key setup

The service requires `JWT_PRIVATE_KEY_B64`: base64-encoded DER bytes of an RSA PKCS#8 private
key. Keep it in `deploy/compose/.env` or your deployment secret store; never commit the generated
key or share its value. Generate a local development key with:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  | openssl pkcs8 -topk8 -nocrypt -outform DER \
  | base64 -w0
```

Set the output as `JWT_PRIVATE_KEY_B64`. The service derives the public key and serves it at
`GET http://localhost:8081/.well-known/jwks.json`. Other services use `JWT_JWK_SET_URI` to
retrieve the public key. Configure the same private key persistently across restarts so previously
issued tokens remain verifiable.

## Endpoints

All request bodies are JSON. Passwords must be between 12 and 72 characters.

| Method and path | Description |
|---|---|
| `POST /api/v1/auth/signup` | Create an ACTIVE CUSTOMER account and issue access/refresh tokens. |
| `POST /api/v1/auth/login` | Verify email or mobile number and password and issue access/refresh tokens. |
| `POST /api/v1/auth/refresh` | Revoke the supplied refresh token and rotate it. |
| `POST /api/v1/auth/logout` | Revoke the supplied refresh token; safe to repeat. |
| `GET /oauth2/authorization/google` | Start Google Authorization Code login (requires OAuth credentials). |
| `GET /login/oauth2/code/google` | Google redirects here after consent; auth-service validates and exchanges the authorization code. |
| `POST /api/v1/auth/token` | Exchange the short-lived Google login code for SwiftDrop tokens. |
| `GET /.well-known/jwks.json` | Publish the public RSA key for token verification. |
| `GET /internal/users/{userId}/contact` | Return the user's email, phone, and first name for authenticated backend callers. Not routed through the API gateway. |

Signup example:

```json
{
  "fullName": "Demo Customer",
  "email": "customer@example.test",
  "mobileNumber": "0771234567",
  "password": "a-long-demo-password"
}
```

The response contains a 15-minute Bearer access token, a seven-day refresh token, and the
authenticated user's ID, name, email, and roles. Refresh tokens are stored as SHA-256 hashes.
Use only dummy data for local demonstrations.

Internal contact lookup example:

```json
{
  "email": "customer@example.test",
  "phone": "+94771234567",
  "firstName": "Demo"
}
```

The endpoint requires a valid bearer token and is intended for service-to-service use on the
internal network. The frontend should not call it; the login/token response already contains the
user ID that a backend consumer can use for this lookup.

## Google OAuth local setup

1. In Google Cloud, create a Web OAuth client. Add
   `http://localhost:8081/login/oauth2/code/google` as an authorized redirect URI for Docker
   Compose local demos.
2. Set `AUTH_GOOGLE_ENABLED=true`, `AUTH_GOOGLE_CLIENT_ID`, and `AUTH_GOOGLE_CLIENT_SECRET`
   in `deploy/compose/.env`. Keep the credentials out of Git.
3. Keep `AUTH_OAUTH_SUCCESS_REDIRECT_URI=http://localhost:3000/auth/callback` when using the
   Compose frontend. If using the Vite dev server at port 5173, change it to
   `http://localhost:5173/auth/callback`.
4. Set `VITE_GOOGLE_LOGIN_ENABLED=true` and build/restart the frontend so the Google button is
   enabled.

Google accounts must provide a verified email. New Google users receive CUSTOMER. Existing users
with the same verified email are linked to that Google account. The browser receives a random
two-minute login code, while only its SHA-256 hash is stored; the code can be exchanged once at
`POST /api/v1/auth/token`.
