# Security Decisions

This document captures the security posture and implementation guidance for this stack.

Note: files under `docs/examples/**` are reference snippets and are not auto-wired into runtime unless you explicitly integrate them.

## 1) Why OIDC with Keycloak was chosen

OIDC + Keycloak was selected to separate authentication/identity from business logic.

Key benefits:

- Standards-based auth (`OAuth2` / `OIDC`) with JWT access tokens.
- Centralized identity management (users, roles, realm config, password policy, MFA options).
- Built-in federation support (SSO, external identity providers) if needed later.
- Role and claim mapping in tokens reduces custom auth code in backend services.
- Works well for both local development (realm import) and production hardening.

Architecture intent:

- Keycloak is the identity provider and token issuer.
- Backend is both an OAuth2 confidential client (BFF pattern) and an OAuth2 resource server.
- Frontend delegates all token handling to the backend; it communicates via session cookie only.

## 2) Token storage: current implementation (BFF pattern)

The application implements the **Backend-for-Frontend (BFF)** pattern. Tokens are stored exclusively on the server. The browser never receives or stores a JWT.

### How it works

- The Spring Boot backend acts as a confidential OAuth2 client.
- At login, the backend performs the authorization code exchange with Keycloak server-to-server.
- The resulting access and refresh tokens are stored in **Redis** (`BffSession`, 30-minute TTL).
- The backend sets an `HttpOnly` session cookie (`BFF_SESSION_ID`) in the browser response.
- All subsequent API calls carry only this cookie — no `Authorization` header, no token in JavaScript.
- On logout, the backend performs a **backchannel revocation** call to Keycloak (using the internal Docker hostname, not the public URL) before deleting the Redis session and clearing the cookie.

### Security properties

| Property | Value |
|---|---|
| Token location | Redis (server-side) |
| Token visible to JavaScript | No |
| XSS token exfiltration | Not possible |
| Session revocation | Immediate (delete Redis key) |
| CSRF mitigation | Double-submit cookie (`XSRF-TOKEN` + `X-XSRF-TOKEN` header) |
| Cookie flags | `HttpOnly`, `SameSite=Lax` |
| Logout | Backchannel revocation to Keycloak + cookie clear |

### BFF endpoints

| Endpoint | Auth required | Purpose |
|---|---|---|
| `GET /api/auth/csrf` | No | Return CSRF token for the current session |
| `GET /api/auth/me` | Session cookie | Return current user info from Redis session |
| `GET /oauth2/authorization/keycloak` | No | Initiate OAuth2 authorization code flow |
| `POST /api/auth/logout` | No (permitAll) | Backchannel revoke + delete session + clear cookie |

### Why not in-memory (Keycloak.js)?

The previous implementation used `keycloak-js` in the Angular frontend, which held tokens in browser memory. This meant:

- Any XSS vulnerability could exfiltrate a valid access token.
- There was no server-side mechanism to revoke sessions instantly.
- Token refresh required client-side logic that was sensitive to page lifecycle.

The BFF pattern eliminates all of these risks. The tradeoff is higher implementation complexity and a Redis dependency, both of which are justified for a production security posture.

### Why not localStorage / sessionStorage?

- Tokens in `localStorage` or `sessionStorage` are readable by any JavaScript on the page.
- This is the highest-risk storage option and is not used.

## 3) CSP, SRI, and secure headers (Nginx)

Current state:

- Frontend Nginx config currently focuses on SPA routing and cache headers.
- Strict security headers/CSP from the example below are **recommended** but not fully enforced in the current local `frontend/nginx/default.conf`.

Recommended hardening snippet:

Example `nginx` snippet:

```nginx
# Security headers
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
add_header Cross-Origin-Opener-Policy "same-origin" always;
add_header Cross-Origin-Resource-Policy "same-site" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

# CSP (adjust connect-src for your backend + keycloak hosts)
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self' https://auth.example.com https://api.example.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'" always;
```

Notes:

- HSTS only when TLS is enforced end-to-end.
- Keep `script-src` strict; avoid `unsafe-inline` and `unsafe-eval` in production.
- Include Keycloak and backend origins in `connect-src` only as needed.

### SRI guidance

- For third-party CDN assets, use Subresource Integrity (`integrity` + `crossorigin="anonymous"`).
- For Angular CLI-managed local bundles, hashing/caching is already handled by build output names.
- Prefer self-hosting critical scripts/styles when possible.

## 4) Recommended CORS policy values

Current state:

- Backend does not define a custom CORS configuration bean.
- Local deployment uses same-origin frontend-to-backend calls through Nginx (`/api` proxy), so broad CORS rules are not required for that path.

Recommended baseline:

- `allowedOrigins`: exact frontend origin(s), e.g. `https://app.example.com`.
- `allowedMethods`: `GET,POST,PUT,PATCH,DELETE,OPTIONS`.
- `allowedHeaders`: `Authorization,Content-Type,X-Correlation-ID`.
- `exposedHeaders`: minimal set only if required by client.
- `allowCredentials`: `false` for bearer-token auth; `true` only for cookie-based auth.
- `maxAge`: e.g. `3600` seconds.

Do not use wildcard origin (`*`) in production when credentials are used.

## 5) How backend verifies roles and checks termination

The backend supports two principal types depending on how the request is authenticated:

**BFF session principal** (`BffSessionPrincipal`) — used by the Angular frontend:
- `BffSessionAuthenticationFilter` reads the `BFF_SESSION_ID` cookie, loads the `BffSession` from Redis, and constructs a `BffSessionPrincipal`.
- Roles are stored in the session at login time (extracted from the OIDC token).
- `employee_id` is stored in the session; if absent at login (Keycloak mapper not configured), `BffOAuth2LoginSuccessHandler` falls back to a DB lookup by email.

**JWT bearer principal** (`EmployeeJwtPrincipal`) — used for direct API access:
- Spring Security OAuth2 Resource Server validates the bearer token.
- `KeycloakJwtAuthenticationConverter` reads `realm_access.roles` and maps to `ROLE_*` authorities.
- Extracts `employee_id` claim into a custom principal.

Both principal types expose `employee_id` under `authentication.principal.employee_id` for use in `@PreAuthorize` SpEL expressions.

Current backend enforcement path:

1. **JWT validation (bearer)** — Resource server validates signature and issuer via Spring Security.

2. **Session validation (BFF)** — `BffSessionAuthenticationFilter` validates the `BFF_SESSION_ID` cookie and loads the Redis session.

3. **Role extraction**
   - For JWT: `backend/.../security/KeycloakJwtAuthenticationConverter.java` reads `realm_access.roles`.
   - For BFF session: roles stored in `BffSession` at login, loaded into `BffSessionPrincipal`.

4. **Authorization checks**
   - `backend/.../employee/EmployeeController.java` (V1 API)
   - `backend/.../employee/EmployeeControllerV2.java` (V2 API with manager support)
   - Uses `@PreAuthorize` with role checks and ownership checks.
   - V2 additions: subordinates endpoint allows `HR_ADMIN` OR employee viewing own reports.

5. **Termination enforcement**
   - `backend/.../security/TerminatedEmployeeFilter.java`
   - Runs after authentication for both principal types.
   - Looks up `employee_id` in DB and denies requests when `dateOfTermination <= today`.
   - Returns `403` with JSON payload including `reason: terminated`.
   - Attempts to disable the Keycloak account when email is present.

## Employee credential bootstrap

- For newly added employees, Keycloak `username` is set to the employee `emailAddress`.
- New employee accounts are initialized with default password `ChangeMe123!`, which users can change on first login.

## Action checklist

- [x] Keep Keycloak realm role model minimal (`HR_ADMIN`, `EMPLOYEE`) for current scope.
- [x] Protect employee endpoints with explicit authorization annotations.
- [x] Keep termination filter active for authenticated requests.
- [x] Migrate to BFF + HttpOnly cookie strategy with CSRF protection.
- [x] Backchannel logout to terminate Keycloak SSO session on logout.
- [x] `employee_id` fallback to DB lookup for users without custom claim.
- [ ] Enforce explicit CORS policy for cross-origin production deployments.
- [ ] Add strict Nginx security headers + CSP in deployment config.
